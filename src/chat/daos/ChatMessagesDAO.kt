package chat.daos

import chat.servercommunication.ServerData
import chat.dataclass.ChatMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Object to handle operations with chatMessages
 */
object ChatMessagesDAO {

    /**
     * Get the Date as a String, when the last Message has been sent
     * @param socketID ID of the user from whom you want to retrieve the date
     * @return returns the date as a string, when the last message was sent, or an empty String when the user has never
     * sent a message
     */
    fun getLastMessage(socketID : String) : String{
        @Language("SQL")
        val query = "SELECT lastmessage FROM chatuser WHERE SocketID = ?;"
        val con = DatabaseConnectionDAO.getConnection()
        val preparedStatement = con.prepareStatement(query).apply {
            setString(1,socketID)
        }
        val res = preparedStatement.executeQuery()
        return if (!res.next()) {
            preparedStatement.close()
            con.close()
            ""
        } else {
            val lastMessage = res.getString(1)
            preparedStatement.close()
            con.close()
            lastMessage
        }

    }

    /**
     *  Add sent message into Database
     *  @param nachricht The Message which has been sent
     *  @param userGesendet has the message been sent by the client
     *  @param socketID ID of the client
     *  @return returns a Chatmessage object with the given parameters
     */
    fun addMessage(nachricht: String,userGesendet : Boolean,socketID: String) : ChatMessage {

        val formattedTime = with(LocalDateTime.now()){
            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uu HH:mm:ss")
            format(dateTimeFormatter)
        }

        @Language("SQL")
        val query ="INSERT INTO chatnachrichten(nachricht,user_gesendet,gesendet_um) values (?,?,?);"
        val con = DatabaseConnectionDAO.getConnection()
        val preparedStatement = con.prepareStatement(query,PreparedStatement.RETURN_GENERATED_KEYS).apply {
            setString(1,nachricht)
            setBoolean(2,userGesendet)
            setString(3,formattedTime)
            execute()
        }
        val messageID = with(preparedStatement.generatedKeys){
            next()
            getInt(1)
        }
        preparedStatement.close()
        con.close()

        ChatUserDatabaseDAO.addNewMessageID(messageID, socketID)
        ChatUserDatabaseDAO.setLastMessage(socketID, formattedTime)
        ServerData.activeChatUser[socketID]?.lastMessage = formattedTime
        if (userGesendet){
            LoggerFactory.getLogger("infologger").info("User Nachricht gesendet;$socketID")
        }
        else{
            LoggerFactory.getLogger("infologger").info("User Nachricht empfangen;$socketID")

        }
        return ChatMessage(messageID,formattedTime,userGesendet,nachricht)

    }

    /**
     * Get all Messages which has been sent and received by the client
     * @param socketID ID of the client from who to retrieve the messages
     * @return returns an JsonArray which contains all Chatmessages related to the given ID as JSONobjects
     */
    fun getMessages(socketID: String) : JsonArray{
        val jsonArray = JsonArray()
        val messageIds = ChatUserDatabaseDAO.getMessageIDs(socketID)
        if (messageIds.isEmpty()) return jsonArray

        @Language("SQL")
        val query = "SELECT * FROM chatnachrichten WHERE chatnachrichten.nachrichtenID IN ($messageIds);"
        val con = DatabaseConnectionDAO.getConnection()
        val statement = con.createStatement()
        val gson = Gson()

        statement.executeQuery(query).apply {
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

        statement.close()
        con.close()
        return jsonArray
    }

}