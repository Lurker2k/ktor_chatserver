package chat.servercommunication

import chat.api_enums.ClientStatus
import chat.api_enums.Types
import chat.daos.ChatUserDAO
import com.google.gson.JsonObject
import io.ktor.websocket.DefaultWebSocketServerSession


object UserToServer  {

    /**
     * Entrypoint for all messages and data the user sends to the server
     * @param jsonObject Object which contains data and the info what to do with the data
     * @param socket The socket of the User
     */
    suspend fun handleMsg(jsonObject: JsonObject, socket: DefaultWebSocketServerSession) {

        when(jsonObject.getAsJsonPrimitive(Types.TYPE.name).asString){
            Types.MESSAGE.name->{
                val socketID = socket.call.attributes[ServerData.cookieAttribute]
                val nachricht = jsonObject.getAsJsonPrimitive(Types.MESSAGE.name).asString
                nachrichtGesendet(nachricht,socketID)
            }
        }
    }

    /**
     * The User has sent a Message to the admin
     * @param nachricht The Text which the User has sent to the admin
     * @param socketID The ID of the User
     */
    private suspend fun nachrichtGesendet(nachricht: String,socketID : String){
        if (!ServerData.activeChatUser.containsKey(socketID)) ServerData.addActiveChatUser(socketID)
        ServerData.addMessage(true,nachricht,socketID)
    }



}