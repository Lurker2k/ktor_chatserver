package chat.servercommunication

import chat.api_enums.ClientStatus
import chat.api_enums.Types
import chat.daos.ChatUserDatabaseDAO
import com.google.gson.JsonObject
import io.ktor.util.AttributeKey
import io.ktor.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.cancel
import java.lang.NumberFormatException


object UserToServer  {

    /**
     * Entrypoint for all messages and data the user sends to the server
     * @param jsonObject Object which contains data and the info what to do with the data
     * @param socket The socket of the User
     */
    suspend fun handleMsg(jsonObject: JsonObject, socket: DefaultWebSocketServerSession) {


        when(jsonObject.getAsJsonPrimitive(Types.TYPE.name).asString){
            ClientStatus.COOKIE.name->{
            val socketId : String
                try {
                    socketId = jsonObject.getAsJsonPrimitive(ClientStatus.COOKIE.name).asString
                }catch (e : Exception){
                    ServerToUser.invalidCookie(socket)
                    return
                }
                if (!ChatUserDatabaseDAO.validUser(socketId,socket)) ServerToUser.invalidCookie(socket)
                else {
                    ServerData.userConnected(socketId,socket)
                    socket.call.attributes.put(ServerData.cookieAttribute,socketId)
                    ServerData.addActiveChatUser(socketId,socket) // User connected
                }
            }
            ClientStatus.GETCOOKIE.name->{
                ServerData.generateSocketId(socket)
            }
            Types.MESSAGE.name->{
                val socketID = socket.call.attributes[ServerData.cookieAttribute]
                val nachricht = jsonObject.getAsJsonPrimitive(Types.MESSAGE.name).asString
                nachrichtGesendet(nachricht,socketID,socket)
            }
        }
    }

    /**
     * The User has sent a Message to the admin
     * @param nachricht The Text which the User has sent to the admin
     * @param socketID The ID of the User
     */
    private suspend fun nachrichtGesendet(nachricht: String,socketID : String,socket: DefaultWebSocketServerSession){
        if (!ServerData.activeChatUserSockets.contains(socketID)) ServerData.addActiveChatUser(socketID, socket, true)
        ServerData.addMessage(true,nachricht,socketID)
    }

}