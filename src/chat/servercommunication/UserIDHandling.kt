package chat.servercommunication

import chat.daos.ChatUserDAO
import io.ktor.application.ApplicationCall
import io.ktor.http.Cookie


object UserIDHandling {


    /**
     * Check if the sent ID is valid, or if it has been altered
     * @param userID the userID sent by the client
     * @return is the userID valid
     */
    fun checkValidUserID(userID: String): Boolean{
        return ChatUserDAO.validUser(userID)
    }

    /**
     * Create a new userID for the connected Client and set a Cookie with the created userID
     * @return Cookie which will be set by the client
     */
    fun createNewUser(): Cookie{
            val userID = ChatUserDAO.createSocketID()
            return Cookie("ChatCookie",userID,maxAge = 60*60*24*365*3)

    }

    /**
     * Set the received userID as an Attribute to the current call
     * @param call Current connection
     * @param userID the userID which was either sent by the Client or created by the server for this connection
     */
    fun setUserID(call: ApplicationCall,userID: String){
        call.attributes.put(ServerData.cookieAttribute,userID)
    }


}