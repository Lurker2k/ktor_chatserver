package chat.servercommunication

import chat.dataclass.ChatUser
import chat.daos.ChatMessagesDAO
import chat.daos.ChatUserDAO
import chat.api_enums.ClientStatus
import chat.exceptions.NoAdminCredentials
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import io.ktor.websocket.DefaultWebSocketServerSession
import org.slf4j.LoggerFactory

object ServerData {

    var behindProxy  = false
    var isMobileActive = false
    val cookieAttribute = AttributeKey<String>(ClientStatus.COOKIE.name)
    var online = false
    val activeChatUser = HashMap<String, ChatUser>()
    val connectedUser = HashMap<String,ArrayList<DefaultWebSocketServerSession>>() // Mit ID speichern
    var adminSocket : DefaultWebSocketServerSession? = null
    var adminHasSelectedID = ""
    lateinit var adminuser : String
    lateinit var adminpass: String

    /**
     * Set the socket of the connected Admin
     * @param socket of the admin
     */
    fun setHostSocket(socket: DefaultWebSocketServerSession){
        adminSocket = socket
    }

    /**
     *  Set the status of the connected admin as Online
     *  User can now chat and interact with the admin
     */
    suspend fun setOnline(){
        online = true
        ServerToUser.hostLoggedIn()
        ServerToAdmin.adminOnline()

    }

    /**
     * Set the status of the admin offline
     * The chat window is now locked for user
     */
    suspend fun setOffline(){
            if (online) ServerToUser.hostLoggedOut()
            online = false
        ServerToAdmin.adminOffline()
    }

    /**
     * Add the socket of a User to the active chatting Users
     *
     * Only User which already got a chatlog are beeing added on connect
     * User which are sending their first message are also added here
     * @param socketId ID of the User
     * The chatlog is skipped on sending the first message
     *
     */
    suspend fun addActiveChatUser(socketId : String){
        val loginTime = ChatUserDAO.setLastLogin(socketId)
        if (!activeChatUser.containsKey(socketId)) {
            val name = ChatUserDAO.userHasName(socketId)
            val chatUser = ChatUser(socketId,loginTime, name)
            activeChatUser[socketId] = chatUser
            ServerToAdmin.activeUserOnline(chatUser)
            }

    }


    /**
     * An active chatting user has logged out
     * This sends a message to the admin, when he is online
     * @param socketId ID of the user who disconnected
     */
    private suspend fun chatUserLoggedOut(socketId: String){
        activeChatUser.remove(socketId)
        ServerToAdmin.activeUserOffline(socketId)
    }

    /**
     * A User has connected to the Server
     * @param userID ID of the user who connected
     * @param socket the Websockconnection to the connecting user
     */
    suspend fun userConnected(userID: String,socket: DefaultWebSocketServerSession){
        if (connectedUser.containsKey(userID)) connectedUser[userID]?.add(socket)
        else {
            connectedUser[userID] = arrayListOf(socket)
            ServerToAdmin.userConnected()
        }
        LoggerFactory.getLogger("infologger").info("User connected;$userID")
    }

    /**
     * A User has disconnected from the server
     * @param socket the Websocketconnection to the disconnecting user
     */
    suspend fun userDisconnected(socket: DefaultWebSocketServerSession){
        val socketId = socket.call.attributes.getOrNull(cookieAttribute)?:return
        connectedUser[socketId]?.remove(socket)
        val emptyUser = connectedUser[socketId]?.isNullOrEmpty()
        if (emptyUser == null || emptyUser) {
            connectedUser.remove(socketId)
            chatUserLoggedOut(socketId)
            ServerToAdmin.userDisconnected()
        }
    }

    /**
     * Add a Message sent by either the admin or the User to the database and send it to the User and the Admin
     * @param user_gesendet Has the message been sent by the user or the admin
     * @param nachricht The Text of the message
     * @param socketId The Id of the User
     */
    suspend fun addMessage(user_gesendet : Boolean, nachricht : String, socketId: String){

        val chatMessage = ChatMessagesDAO.addMessage(nachricht,user_gesendet,socketId)

        ServerToUser.sendMessage(connectedUser[socketId], chatMessage)

        if (adminHasSelectedID == socketId) ServerToAdmin.sendMessage(chatMessage)

        else ServerToAdmin.sendNewMessageReceived(socketId)
    }

    /**
     * Set the credentials for the Connection as Admin
     * @param environment The Applicationenvironment with which the server is started
     * @throws NoAdminCredentials
     */
    @KtorExperimentalAPI
    fun setupServer(environment : ApplicationEnvironment){

            adminuser = environment.config.property("ADMINUSER").getString()
            adminpass = environment.config.property("ADMINPASS").getString()
            if (adminuser == "error" || adminpass == "error")  {
                /**
                 * Throw an exception when there are no data for an adminlogin
                 */
                noAdminCredentials()
            }


        val logger = LoggerFactory.getLogger("infologger")
        logger.info("Adminconnection user;${adminuser}")
        logger.info("Adminconnection pass;${adminpass}")

        environment.config.propertyOrNull("BEHINDPROXY")?.getString()?.toBoolean()?.let {
            behindProxy = it
            logger.info("Server is behind proxy;$it")
        }
    }

    /**
     * throw an exception, that there are arguments missing for the credentials of the adminconnection at startup
     * @throws NoAdminCredentials
     */
    private fun noAdminCredentials(){
        val errorMessage = ("No Arguments for adminconnection, please start with values for 'adminuser' and 'adminpass'")
        throw NoAdminCredentials(errorMessage)
    }


}