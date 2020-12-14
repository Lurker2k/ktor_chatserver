package chat.servercommunication

import chat.api_enums.AdminStatus
import chat.api_enums.ClientStatus
import chat.api_enums.Types
import chat.api_enums.UserInfos
import chat.daos.ChatUserDAO
import chat.daos.ChatUserInfoDAO
import chat.dataclass.ChatUser
import com.google.gson.JsonObject


object AdminToServer {


    /**
     * Entrypoint to handle communication from the Admin to the Server
     * @param jsonObject Data and selectors from the admin
     */
    suspend fun handleMessage(jsonObject: JsonObject){
         when(jsonObject[Types.TYPE.name].asString){
            AdminStatus.ONLINE.name->{
                ServerData.setOnline()
            }
            AdminStatus.OFFLINE.name->{
                ServerData.setOffline()
            }
             ClientStatus.SOCKETID.name->{
                 ServerToAdmin.getCurrentChat(jsonObject.getAsJsonPrimitive(ClientStatus.SOCKETID.name).asString)
                 ServerToAdmin.getUserInfo(jsonObject.getAsJsonPrimitive(ClientStatus.SOCKETID.name).asString)
             }
             ClientStatus.RENAMEUSER.name->{
                 renameUser(jsonObject)
             }
             UserInfos.ADDUSERINFO.name->{
                addInfo(jsonObject)
             }
             UserInfos.REMOVEINFO.name->{
                 deleteInfo(jsonObject)
             }
             Types.MESSAGE.name->{
                nachrichtSenden(jsonObject)
             }

         }
   }

    /**
     * Send a Message to a specific User
     * @param jsonObject JsonObject which contains all Data to send a message to an User.
     * Contains a message and an ID where the message should be sent
     */
    private suspend fun nachrichtSenden(jsonObject: JsonObject){
    val nachricht = jsonObject.getAsJsonPrimitive(Types.MESSAGE.name).asString
    val socketID = jsonObject.getAsJsonPrimitive(ClientStatus.SOCKETID.name).asString
    ServerData.addMessage(false, nachricht, socketID)
}

    /**
     * Delete the info added to an user
     * @param jsonObject Contains the ID of the Info which will be deleted
     */
    private fun deleteInfo(jsonObject: JsonObject){
        val infoID = jsonObject.getAsJsonPrimitive(UserInfos.REMOVEINFO.name).asInt
        ChatUserInfoDAO.deleteInfo(infoID)
    }

    /**
     * Add an info to an user
     * @param jsonObject the object which was sent by the admin
     */
    private suspend fun addInfo(jsonObject: JsonObject){
        val socketId = jsonObject.getAsJsonPrimitive(ClientStatus.SOCKETID.name).asString
        val info = jsonObject.getAsJsonPrimitive(UserInfos.ADDUSERINFO.name).asString
        ChatUserInfoDAO.addInfo(socketId,info)
    }

    /**
     * rename an existing user, only visible for the admin
     * @param jsonObject the object with the data which was sent by the admin
     */
    private fun renameUser(jsonObject: JsonObject){
        val name = jsonObject.getAsJsonPrimitive(ClientStatus.RENAMEUSER.name).asString
        val socketID = jsonObject.getAsJsonPrimitive(ClientStatus.SOCKETID.name).asString
        ChatUserDAO.renameUser(name, socketID)

    }

}