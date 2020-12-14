package chat.servercommunication

import chat.dataclass.ChatMessage
import chat.dataclass.ChatUser
import chat.daos.ChatMessagesDAO
import chat.api_enums.*
import chat.daos.ChatUserInfoDAO
import chat.dataclass.UserInfo
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.http.cio.websocket.send


object ServerToAdmin {


    /**
     * Send notice to the admin, that an active chatting user has gone offline
     * @param socketId ID of the user who has gone offline
     */
    suspend fun activeUserOffline(socketId: String){
        val jsonObject = JsonObject().apply {
            addProperty(ClientStatus.SOCKETID.name,socketId)
            addProperty(Types.TYPE.name, ServerStatus.REMOVECHATUSER.name)
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
        }

        ServerData.adminSocket?.send(jsonObject.toString())
    }


    /**
     * Send notice to the admin, that an active chatting user has gone online
     * @param chatUser The Object which contains all necessary infos about the User
     * which gets sent to the admin
     */
    suspend fun activeUserOnline(chatUser: ChatUser){
        val jsonObject = JsonObject().apply {
            add(ServerStatus.ADDCHATUSER.name,Gson().toJsonTree(chatUser))
            addProperty(Types.TYPE.name, ServerStatus.ADDCHATUSER.name)
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Set the admin as Online, this notifys the connected User, unlocks the chatting feature
     * and changes the status at the server
     */
    suspend fun adminOnline(){
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, AdminStatus.ONLINE.name)
            addProperty(Types.VIEW.name, AdminScreen.MAINSCREEN.name)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Set the status of the admin as offline, this notifys the connected User
     * locks the chatting feature and changes the status at the server
     */
    suspend fun adminOffline(){
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, AdminStatus.OFFLINE.name)
            addProperty(Types.VIEW.name, AdminScreen.MAINSCREEN.name)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Get a list of all currently connected Users who chatted in the past
     * This list is sent to the admin at connection
     */
    suspend fun getOnlineChatUser(){
        val gson = Gson()
        val jsonObject = JsonObject().apply {
            addProperty(Types.TYPE.name, ServerStatus.CHATUSERS.name)
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
        }

        val jsonArray = JsonArray()
        ServerData.activeChatUser.values.forEach {
            jsonArray.add(gson.toJsonTree(it))
        }
        jsonObject.apply {
            add(ServerStatus.CHATUSERS.name,jsonArray)
            addProperty(ServerStatus.CONNECTEDUSER.name, ServerData.connectedUser.size)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Get the Chatlog of the user the admin has selected in his tool
     * and send it to the admin
     * @param socketId ID of the user of whom the chatlog should be retrieved
     */
    suspend fun getCurrentChat(socketId: String){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, Messages.CHATLOG.name)
        }

        jsonObject.add(Messages.CHATLOG.name,ChatMessagesDAO.getMessages(socketId))
        ServerData.adminHasSelectedID = socketId
        ServerData.adminSocket?.send(jsonObject.toString())

    }

    /**
     * When a user is selected this function requests all infos, which are added to this user
     * @param socketId the ID of the user
     */
    suspend fun getUserInfo(socketId: String){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, UserInfos.GETUSERINFO.name)
        }
        jsonObject.add(UserInfos.GETUSERINFO.name,ChatUserInfoDAO.getInfos(socketId))
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Sent the created Userinfo to the admin
     * @param userInfo the created UserInfo
     */
    suspend fun userAdded(userInfo: UserInfo){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, UserInfos.ADDUSERINFO.name)
            add(UserInfos.ADDUSERINFO.name,Gson().toJsonTree(userInfo))
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Send a Chatmessage to the admin
     * this gets called when the admin sends a chatmessage or the currently selected User sends a message
     * @param chatMessage Message which contains all necessary infos like the Text, ID of the user  and sending time
     */
    suspend fun sendMessage(chatMessage: ChatMessage){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, Messages.APPENDMESSAGE.name)
        }
        val jsonChat = Gson().toJson(chatMessage)
        jsonObject.addProperty(Messages.APPENDMESSAGE.name,jsonChat)
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * Send a notification to the admin, that the User with this ID has sent a message
     * @param socketId ID of the user who sent a message
     */
    suspend fun sendNewMessageReceived(socketId: String){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, Messages.NEWMESSAGE.name)
            addProperty(Messages.NEWMESSAGE.name,socketId)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * A new user has connected to the server
     */
    suspend fun userConnected(){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, ServerStatus.ADDCONNECTEDUSER.name)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

    /**
     * A user has been disconnected from the server
     */
    suspend fun userDisconnected(){
        val jsonObject = JsonObject().apply {
            addProperty(Types.VIEW.name, AdminScreen.CHATSCREEN.name)
            addProperty(Types.TYPE.name, ServerStatus.REMOVECONNECTEDUSER.name)
        }
        ServerData.adminSocket?.send(jsonObject.toString())
    }

}