package chat.daos

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.websocket.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Logger
import javax.crypto.spec.PBEKeySpec
import kotlin.math.log

object ChatUserDatabaseDAO {

    var salt : String? = null
    /**
     * Checks if there is a user in the database with the given socketID
     *  @param socketID ID to check if there is an entry for it in the database
     *  @return returns if there is an entry for the given ID
     */
    fun validUser(socketID: String,socket: DefaultWebSocketServerSession) : Boolean{

        @Language("SQL")
        val query = "SELECT * FROM chatuser WHERE SocketID = ?"
        val con = DatabaseConnectionDAO.getConnection()
        val validUser = con.prepareStatement(query).let {
            it.setString(1,socketID)
            it.executeQuery().next()
        }
        con.close()
        if (!validUser){
            val clientIP = socket.call.request.origin.remoteHost
            LoggerFactory.getLogger("warninglogger").warn("Falsified ClientID sent from IP;$clientIP")
        }
        return validUser
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

        val query = "update chatuser set lastlogin = ? WHERE SocketID = ?;"
        val con = DatabaseConnectionDAO.getConnection()
        con.prepareStatement(query).apply {
            setString(1,formattedTime)
            setString(2,socketID)
            execute()
            close()
        }
        con.close()
        return formattedTime
    }

    /**
     * Set the time, when the last message has been sent by the user
     * @param socketID ID of the user
     * @param currentTime Time as a String, when the the last Message has been sent
     */
    fun setLastMessage(socketID: String,currentTime : String){

        @Language("SQL")
        val query = "update chatuser set lastmessage = ? WHERE SocketID = ?;"
        val con = DatabaseConnectionDAO.getConnection()
        con.prepareStatement(query).apply {
            setString(1,currentTime)
            setString(2,socketID)
            execute()
            close()
        }
        con.close()
    }

    /**
     * Add the ID of a sent Message to the list of messages sent and received by a user
     * @param chatMessageID The ID of the message which has been sent or received by the user
     * @param socketID ID of the User
     */
    fun addNewMessageID(chatMessageID : Int, socketID: String){

        @Language("SQL")
        val query = "update chatuser set messageIDs = json_array_append(messageIDs,'$',?) WHERE SocketID = ?;"

        val con = DatabaseConnectionDAO.getConnection()
        con.prepareStatement(query).apply {
            setInt(1,chatMessageID)
            setString(2,socketID)
            execute()
            close()
        }
        con.close()
    }

    /**
     * Create a unique Identifier for the connected user
     * @return the ID of the user to identify him later
     */
    fun createSocketID() : String{
        var socket = ""
        while (socket.isEmpty()){
            val socketID = (1..Int.MAX_VALUE).random()
            socket = hashSocketID(socketID)
            @Language("SQL")
            val query = "SELECT * FROM chatuser WHERE SocketID  = ?"
            val con = DatabaseConnectionDAO.getConnection()
            val res = con.prepareStatement(query).apply {
                setString(1,socket)
            }.let {
                it.executeQuery()
            }
            if (res.next()) socket = ""
            con.close()
        }

        val formattedTime = with(LocalDateTime.now()){
            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uu HH:mm:ss")
            format(dateTimeFormatter)
        }

        @Language("SQL")
        val query = "INSERT INTO chatuser(chatuser.SocketID,chatuser.lastmessage,chatuser.messageIDs,chatuser.lastlogin) values (?,'','[]',?);"
        val con = DatabaseConnectionDAO.getConnection()
        con.prepareStatement(query).apply {
            setString(1,socket)
            setString(2,formattedTime)
            execute()
            close()
        }
       con.close()
        LoggerFactory.getLogger("infologger").info("New user connected;$socket")
        return socket
    }

    /**
     * Fetch the IDs of the messages sent and received by an User
     * The IDs are returned as a String formatted to work as a selector in an IN SQL Query
     * @param socketID ID of the User
     * @return String to user in an IN SQL Query
     */
    fun getMessageIDs(socketID: String) : String{

        @Language("SQL")
        val query = "SELECT JSON_EXTRACT(chatuser.messageIDs,'\$') FROM chatuser where SocketID = ?;"
        val con = DatabaseConnectionDAO.getConnection()

        val res = con.prepareStatement(query).let{
            it.setString(1,socketID)
            it.executeQuery()
        }
        return if (!res.next()) {
            con.close()
            ""
        }
        else {
            val data = res.getString(1)
            con.close()
            return data.subSequence(1,data.length-1).toString()
        }
    }

    /**
     * Create a hash from a given ID with the salt of the server
     * @param socketID a random generated number which gets hashed
     * @return the hashed value of the socketID with an added salt
     */
    private fun hashSocketID(socketID: Int) : String{

        val messageDigest = MessageDigest.getInstance("SHA-512")
        val digestString = socketID.toString()+salt
        val digestBytes = messageDigest.digest(digestString.toByteArray())
        return Base64.getEncoder().encodeToString(digestBytes)
    }

    /**
     * Set the Salt which shall be used for the SocketID.
     * @param environment the applicationenvironment with which the server is started
     */
    fun setupSocketIDSalt(environment : ApplicationEnvironment){
        environment.config.propertyOrNull("SALT")?.getString()?: ""
    }

}