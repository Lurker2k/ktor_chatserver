package chat.daos


import chat.dataclass.UserInfo
import chat.servercommunication.ServerToAdmin
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.intellij.lang.annotations.Language

object ChatUserInfoDAO {


    /**
     * Add an Info to an user and sent the info back to the admin
     * @param socketID socketID of the user
     * @param info the info which will be added to the user
     */
    suspend fun addInfo(socketID: String,info : String){
        @Language("SQL")
        val query = "INSERT INTO userinfos (userinfo) values (?)"
        val values = arrayListOf<Any>(info)
        val infoID= DatabaseConnectionDAO.executePreparedStatement(query,values,true) as Int
        addInfoID(socketID, infoID)
        val userInfo = UserInfo(info,infoID)
        ServerToAdmin.userAdded(userInfo)

    }

    /**
     * Add the ID of the created info to the database
     * @param socketID the socketID of the user
     * @param infoID the ID of the created info
     */
    private fun addInfoID(socketID: String, infoID : Int){
        @Language("SQL")
        val query = "update chatuser set infoIDs = json_array_append(infoIDs,'$',?) WHERE socketID = ?;"
        val values = arrayListOf(infoID,socketID)
        DatabaseConnectionDAO.executePreparedStatement(query,values)
    }

    /**
     * Get the IDs of the infos added to an user
     * @param socketID the ID of the user
     * @return the IDs of the Infos formatted as a String for an SQL query with (IN)
     */
    private fun getInfoIDs(socketID: String) : String{
        @Language("SQL")
        val query = "SELECT JSON_EXTRACT(chatuser.infoIDs,'\$') FROM chatuser where socketID = ?;"
        val values = arrayListOf<Any>(socketID)
        return DatabaseConnectionDAO.executePreparedStatement(query,values){
            if (!it.next()) ""
            else {
                val string = it.getString(1)
                string.subSequence(1,string.length-1).toString()
            }
        } as String
    }

    /**
     * Get the Infos of an user and sent them to the admin
     * @param socketID the socketID of the user
     */
    fun getInfos(socketID : String) : JsonArray{
        val gson = Gson()
        val jsonArray = JsonArray()
        val infoIDs = getInfoIDs(socketID)
        if (infoIDs.isEmpty()) return jsonArray

        @Language("SQL")
        val query = "SELECT * FROM userinfos WHERE userinfoID IN ($infoIDs);"
        DatabaseConnectionDAO.executeStatement(query,false){
            it.apply {
                while (next()){
                    val userInfo = getString(UserInfo.info_column)
                    val userInfoID = getInt(UserInfo.infoID_column)
                    val info = UserInfo(userInfo,userInfoID)
                    val json = gson.toJsonTree(info)
                    jsonArray.add(json)
                }
            }
        }
        return jsonArray
    }

    /**
     * Delete the info added to an user
     * @param infoID ID of the Info, which will be deleted
     */
    fun deleteInfo(infoID: Int){
        @Language("SQL")
        val query = "DELETE FROM userinfos WHERE userinfoID = ?"
        val values = arrayListOf<Any>(infoID)
        DatabaseConnectionDAO.executePreparedStatement(query,values)

    }

}