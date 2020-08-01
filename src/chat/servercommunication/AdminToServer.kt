package chat.servercommunication

import chat.api_enums.AdminStatus
import chat.api_enums.ClientStatus
import chat.api_enums.Types
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



}