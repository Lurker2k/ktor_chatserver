package chat.daos

import chat.dataclass.ChatMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Object to handle operations with chatMessages
 */
object ChatMessagesDAO {


    /**
     *  Add sent message into Database
     *  @param nachricht The Message which has been sent
     *  @param userGesendet has the message been sent by the client
     *  @param socketID ID of the client
     *  @return returns a Chatmessage object with the given parameters
     */
    fun addMessage(nachricht: String,userGesendet : Boolean,socketID: String) : ChatMessage {

        val formattedTime = with(LocalDateTime.now()){
            format(DateTimeFormatter.ofPattern("dd/MM/uu HH:mm:ss"))

        }

        @Language("SQL")
        val query ="INSERT INTO chatnachrichten(nachricht,user_gesendet,gesendet_um) values (?,?,?);"
        val values = arrayListOf<Any>(nachricht,userGesendet,formattedTime)
        val messageID = DatabaseConnectionDAO.executePreparedStatement(query,values,true) as Int

        addNewMessageID(messageID, socketID)

        if (userGesendet){
            LoggerFactory.getLogger("infologger").info("User Nachricht gesendet;$socketID")
        }
        else{
            LoggerFactory.getLogger("infologger").info("User Nachricht empfangen;$socketID")

        }
        return ChatMessage(messageID,formattedTime,userGesendet,nachricht)

    }

    /**
     * Add the ID of a sent Message to the list of messages sent and received by a user
     * @param chatMessageID The ID of the message which has been sent or received by the user
     * @param socketID ID of the User
     */
    private fun addNewMessageID(chatMessageID : Int, socketID: String){

        @Language("SQL")
        val query = "update chatuser set messageIDs = json_array_append(messageIDs,'$',?) WHERE socketID = ?;"
        val values = arrayListOf(chatMessageID,socketID)
        DatabaseConnectionDAO.executePreparedStatement(query,values)

    }

    /**
     * Fetch the IDs of the messages sent and received by an User
     * The IDs are returned as a String formatted to work as a selector in an IN SQL Query
     * @param socketID ID of the User
     * @return String to user in an IN SQL Query
     */
    private fun getMessageIDs(socketID: String) : String{

        @Language("SQL")
        val query = "SELECT JSON_EXTRACT(chatuser.messageIDs,'\$') FROM chatuser where socketID = ?;"
        val values = arrayListOf<Any>(socketID)
        return DatabaseConnectionDAO.executePreparedStatement(query,values){
            if (!it.next()) ""
            else it.getString(1).subSequence(1,it.getString(1).length-1).toString()
        } as String

    }

    /**
     * Get all Messages which has been sent and received by the client
     * @param socketID ID of the client from who to retrieve the messages
     * @return returns an JsonArray which contains all Chatmessages related to the given ID as JSONobjects
     */
    fun getMessages(socketID: String) : JsonArray{
        val gson = Gson()
        val jsonArray = JsonArray()
        val messageIds = getMessageIDs(socketID)
        if (messageIds.isEmpty()) return jsonArray

        @Language("SQL")
        val query = "SELECT * FROM chatnachrichten WHERE chatnachrichten.nachrichtenID IN ($messageIds);"
        DatabaseConnectionDAO.executeStatement(query,false){
            it.apply {
                while (next()){
                    val gesendetUm = getString(ChatMessage.gesendet_um_column)
                    val nachricht = getString(ChatMessage.nachricht_column)
                    val userGesendet = getBoolean(ChatMessage.user_gesendet_column)
                    val nachrichtenID = getInt(ChatMessage.nachrichtenID_column)
                    val chatMessage = ChatMessage(nachrichtenID, gesendetUm, userGesendet, nachricht)
                    val json = gson.toJsonTree(chatMessage)
                    jsonArray.add(json)
                }
            }
        }

        return jsonArray
    }

}