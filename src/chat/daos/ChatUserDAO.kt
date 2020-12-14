package chat.daos


import chat.servercommunication.ServerData
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.websocket.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object ChatUserDAO {

    private lateinit var salt : String
    /**
     * Checks if there is a user in the database with the given socketID
     *  @param socketID ID to check if there is an entry for it in the database
     *  @return returns if there is an entry for the given ID
     */
    fun validUser(socketID: String) : Boolean{

        @Language("SQL")
        val query = "SELECT * FROM chatuser WHERE socketID = ?"
        val values = arrayListOf<Any>(socketID)
        return DatabaseConnectionDAO.executePreparedStatement(query,values){
            it.next()
        } as Boolean

    }

    /**
     * Set the current time as the last time a client has been connected
     * @param socketID ID of the client which gets updated in the database
     * @return the actual time which is stored as the time of the lastlogintime in the database
     */
    fun setLastLogin(socketID : String) : String{

        val formattedTime = with(LocalDateTime.now()){
            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uu HH:mm:ss")
            format(dateTimeFormatter)
        }
        @Language("SQL")
        val query = "update chatuser set lastlogin = ? WHERE socketID = ?;"
        val values = arrayListOf<Any>(formattedTime,socketID)

        DatabaseConnectionDAO.executePreparedStatement(query,values)

        return formattedTime
    }

    /**
     * Create a unique Identifier for the connected user
     * @return the ID of the user to identify him later
     */
    fun createSocketID() : String{
        var socket = ""
        while (socket.isEmpty()){
            val socketID = (1..Long.MAX_VALUE).random()
            socket = hashSocketID(socketID)
            @Language("SQL")
            val query = "SELECT * FROM chatuser WHERE socketID  = ?"
            val values = arrayListOf<Any>(socket)
            if (DatabaseConnectionDAO.executePreparedStatement(query,values){
                        it.next()
                    } as Boolean) socket = ""
        }

        val formattedTime = with(LocalDateTime.now()){
            format(DateTimeFormatter.ofPattern("dd/MM/uu HH:mm:ss"))

        }

        @Language("SQL")
        val query = "INSERT INTO chatuser(socketID,messageIDs,lastlogin,infoIDs,name) values (?,'[]',?,'[]','');"

        val values = arrayListOf<Any>(socket,formattedTime)
        DatabaseConnectionDAO.executePreparedStatement(query,values)

        LoggerFactory.getLogger("infologger").info("New user connected;$socket")
        return socket
    }


    /**
     * Rename the user with a given SocketID
     * The name is only visible to the admin himself
     * @param name the new assigned name for the userID
     */
    fun renameUser(name: String, userID: String){
        @Language("SQL")
        val query =  "UPDATE chatuser SET name = ? WHERE socketID = ?"
        val values = arrayListOf<Any>(name,userID)
        DatabaseConnectionDAO.executePreparedStatement(query,values)
        ServerData.activeChatUser[userID]?.apply { this.name = name }
    }

    /**
     * Check if there the user with this socketID has a name
     * @param socketID ID of the user
     */
    fun userHasName(socketID: String) : String?{
        @Language("SQL")
        val query = "SELECT name FROM chatuser WHERE socketID = ?"
        val values = arrayListOf<Any>(socketID)
        return DatabaseConnectionDAO.executePreparedStatement(query,values){
            if (!it.next()) null
            else if (it.getString(1).isEmpty()) null
                else it.getString(1)
        } as String?
    }

    /**
     * Create a hash from a given ID with the salt of the server
     * @param socketID a random generated number which gets hashed
     * @return the hashed value of the socketID with an added salt
     */
    private fun hashSocketID(socketID: Long) : String{


        val digestString = socketID.toString()+salt
        val digestBytes = MessageDigest.getInstance("SHA-512").
                                    digest(digestString.toByteArray())

        return Base64.getEncoder().encodeToString(digestBytes)
    }

    /**
     * Set the Salt which shall be used for the SocketID.
     * @param environment the applicationenvironment with which the server is started
     */

    @KtorExperimentalAPI
    fun setupSocketIDSalt(environment : ApplicationEnvironment){
       salt = environment.config.propertyOrNull("SALT")?.getString()?: ""
    }

}