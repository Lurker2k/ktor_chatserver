package chat.servercommunication

import chat.dataclass.ChatMessage
import chat.daos.ChatMessagesDAO
import chat.api_enums.AdminStatus
import chat.api_enums.ClientStatus
import chat.api_enums.Messages
import chat.api_enums.Types
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.http.cio.websocket.send
import io.ktor.websocket.DefaultWebSocketServerSession

object ServerToUser {


    /**
     * The admin connected to the server
     * Send a notification to the Users, that the admin is now online and ready for chatting
     */
    suspend fun hostLoggedIn(){
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, AdminStatus.ONLINE.name)
        }
        val users = ServerData.connectedUser
         users.forEach { user ->
             try {
             user.value.forEach {
                 it.send(jsonObject.toString())
             }

        } catch (e : Exception){}}
    }

    /**
     * The admin has disconnected from the server
     * Send a notification to all User, that the admin is now offline and chatting is locked
     */
    suspend fun hostLoggedOut(){
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, AdminStatus.OFFLINE.name)
        }
        val users = ServerData.connectedUser
        users.forEach { user ->
            try {
                user.value.forEach {
                    it.send(jsonObject.toString())
                }

            } catch (e : Exception){}}
    }

    /**
     * Send a new generated ID for a User to the User
     * @param socketId The ID of the User to identifiy him on return
     * @param socket the socket with which the user is connected to the server
     */
    suspend fun sendSocketID(socketId: String,socket: DefaultWebSocketServerSession){
        val json = JsonObject().apply {
            addProperty(Types.TYPE.name, ClientStatus.SETCOOKIE.name)
            addProperty(ClientStatus.COOKIE.name,socketId)
        }
         socket.send(json.toString())
    }

    /**
     * Request an ID from the User if he already has one
     *  @param socket Socket of the User
     */
    suspend fun requestSocketID(socket: DefaultWebSocketServerSession){
        val json = JsonObject().apply {
            addProperty(Types.TYPE.name, ClientStatus.SENDCOOKIE.name)
        }
         socket.send(json.toString())
    }

    suspend fun invalidCookie(socket: DefaultWebSocketServerSession){
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, ClientStatus.INVALIDCOOKIE.name)
        }
        socket.send(jsonObject.toString())
    }

    /**
     * Send the currently status of the admin to a new connected User
     * @param socket Socket of the User
     */
    suspend fun sendHostStatus(socket: DefaultWebSocketServerSession){
        val json = JsonObject().apply {
            if (ServerData.online) addProperty(Types.TYPE.name, AdminStatus.ONLINE.name)
            else addProperty(Types.TYPE.name, AdminStatus.OFFLINE.name)
        }
         socket.send(json.toString())
    }

    /**
     * Send the chatlog to the user
     * @param socketId ID of the User
     * @param socket Socket of the user
     * @return if there is a chatlog
     */
    suspend fun sendChatLog(socketId: String,socket: DefaultWebSocketServerSession) : Boolean{

        val jsonArray = ChatMessagesDAO.getMessages(socketId)
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, Messages.CHATLOG.name)
            add(Messages.CHATLOG.name,jsonArray)
        }
        socket.send(jsonObject.toString())
        if (jsonArray.size() == 0) return false
        return true
    }

    /**
     * Send a Message to an User which has been sent by the admin or by the User
     * @param socket Socket of the User
     * @param chatMessage The Chatmessage which contains all infos
     */
    suspend fun sendMessage(socket: ArrayList<DefaultWebSocketServerSession>?,chatMessage: ChatMessage){
        val json = JsonObject().apply {
            addProperty(Types.TYPE.name, Types.MESSAGE.name)
            add(Types.MESSAGE.name,Gson().toJsonTree(chatMessage))
        }
        socket?.forEach {
            it.send(json.toString())
        }

    }

}