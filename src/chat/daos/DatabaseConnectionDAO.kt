package chat.daos

import chat.exceptions.NoAdminCredentials
import chat.exceptions.NoDataBaseException
import chat.servercommunication.ServerData
import io.ktor.application.ApplicationEnvironment
import io.ktor.util.KtorExperimentalAPI
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.log


object DatabaseConnectionDAO {

    private lateinit var url :String
    private lateinit var user : String
    private lateinit var pass : String

    private val logger = LoggerFactory.getLogger("infologger")

    /**
     * Initialize the server
     * check if all necessary parameters are added when the server was started and throw exceptions when not
     * @param environment the Applicationenvironment, which contains the parameters for the server startup
     */

    fun setupDatabase(environment : ApplicationEnvironment){

        /**
         * Check if all parameters for a connection to a database are provided
         * This checks not if they are valid, if they are not valid an exception gets thrown at runtime
         */
        try {
            val con = environment.config.property("DATABASE").getString()
            init(con)
        } catch (e: Exception) {
            try {
                val url = environment.config.property("URL").getString()
                val user = environment.config.property("USER").getString()
                val pass = environment.config.property("PASS").getString()
                init(url, user, pass)
            } catch (e: Exception) {
                noDatabase()
            }
        }
        logger.info("Databasename;$url")
        logger.info("Databaseuser;$user")
        logger.info("Userpass;$pass")
        /**
         * Checks if there are credentials for the adminconnection
         * These are the credentials with which an admin can connect to the server to control it
         *
         */

        setupColumns()
    }

    /**
     * Set the values to connect to a database from a given url
     */
    private fun init(url : String){
       val databaseString = url.split(";")
       this.url = databaseString[0]
       this.user = databaseString[1].split("=")[1]
       this.pass = databaseString[2].split("=")[1]
       if (url == "error" || user == "error" || pass == "error") noDatabase()
    }

    /**
     * Set the values to connect to a database from seperate variables for the url, user and the password
     */
    private fun init(url: String, user :String, pass:String){
        if (url == "error" || user == "error" || pass == "error") noDatabase()
        this.url = url
        this.user = user
        this.pass= pass

    }

    /**
     * Create tables in the database for this server
     */
    private fun setupColumns(){
        val con = this.getConnection()
        var statement = con.createStatement()
        val createChatNachrichten = "create table if not exists chatnachrichten"+
        "( nachrichtenID int auto_increment, " +
                "gesendet_um text null, " +
                "user_gesendet tinyint(1) null, " +
                "nachricht text null, " +
                "primary key (nachrichtenID)); "

        val createChatUser = "    create table if not exists chatuser" +
                "        ( SocketID varchar(88) not null," +
                "        lastlogin text null," +
                "        lastmessage text null," +
                "        messageIDs json null," +
                "        creationtime timestamp default CURRENT_TIMESTAMP null," +
                "        primary key (SocketID));"

        statement.execute(createChatNachrichten)
        statement.close()
        statement = con.createStatement()
        statement.execute(createChatUser)
        statement.close()
        con.close()
    }

    /**
     * throw an exception, that there are arguments missing for the database at startup
     * @throws NoDataBaseException
     */
    private fun noDatabase(){
        val errorMessage = " You didn't define a Database/user and passwort. Add these as Arguments when starting the Application\n" +
                                  " '-P:database=' for a single URL or '-P:url=' '-P:user=' '-P:=pass' for 3 arguments\n"+
                                  " when using Docker you need to set ENV database or url,user and pass"
        throw NoDataBaseException(errorMessage)
    }


    /**
     * get a connection to the database
     * @return A connection to a database with the arguments submitted at startup
     */
    fun getConnection() : Connection {
        return DriverManager.getConnection(url, user, pass)
    }
}