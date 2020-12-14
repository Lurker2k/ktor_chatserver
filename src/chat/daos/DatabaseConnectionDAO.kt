package chat.daos


import chat.exceptions.NoDataBaseException
import io.ktor.application.ApplicationEnvironment
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.LoggerFactory
import java.sql.*



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

    @KtorExperimentalAPI
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


        val createChatNachrichten = "create table if not exists chatnachrichten"+
        "( nachrichtenID int auto_increment, " +
                "gesendet_um text null, " +
                "user_gesendet tinyint(1) null, " +
                "nachricht text null, " +
                "primary key (nachrichtenID)); "
        executeStatement(createChatNachrichten)

        val createChatUser = "    create table if not exists chatuser" +
                "        ( socketID varchar(88) not null," +
                "        name text null,   " +
                "        lastlogin text null," +
                "        messageIDs json null," +
                "        infoIDs json null,"       +
                "        creationtime timestamp default CURRENT_TIMESTAMP null," +
                "        primary key (socketID));"

        executeStatement(createChatUser)

        val createUserInfo = "create table if not exists userinfos" +
                " (userinfoID int auto_increment," +
                " userinfo text null," +
                " creationtime timestamp default CURRENT_TIMESTAMP null," +
                " primary key (userinfoID));"

        executeStatement(createUserInfo)

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
     * Open a connection,create a statement and execute the given query
     * When data need to be retrieved the resultset can be accessed if this query creates one
     * requesting the generated autokeys with the parameter auotkeys returns the autokey as Int
     * @param query The query which shall be executed
     * @param autokeys do you want to retrieve the generated autokeys from this query?
     * @param resultSet If operation with the resultset need to be done, this can be done in this lambda function
     */
    fun executeStatement(query : String, autokeys : Boolean = false, resultSet: (ResultSet) -> Any? = {}): Any?{
        var returnValue : Any? = null
        DriverManager.getConnection(url, user, pass).use { connection ->

            connection.createStatement().use { statement ->

                if (autokeys) statement.execute(query,Statement.RETURN_GENERATED_KEYS)
                else statement.execute(query)
                returnValue = getReturnValue(autokeys,statement,resultSet)
            }
        }

        return  returnValue
    }


    /**
     * This creates a preparedStatement from the given query and uses the values given as values for the placeholders
     * @see executeStatement for more information
     * @param values Given values for the placeholders of the statement.
     * The class of the values will be identified and submitted to the
     * prepared statement according to their class
     */
    fun executePreparedStatement(query: String, values : ArrayList<Any>, autokeys : Boolean = false,
                                 resultSet: (ResultSet) -> Any? = {}) : Any? {
        var returnValue : Any? = null
        DriverManager.getConnection(url, user, pass).use { connection ->

            val statement = if (autokeys) connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)
            else connection.prepareStatement(query)

            statement.use { preparedStatement ->

                for (i in 0 until values.size) {
                    when (values[i]::class.simpleName) {
                        "String" -> preparedStatement.setString(i + 1, values[i] as String)
                        "Int" -> preparedStatement.setInt(i + 1, values[i] as Int)
                        "Boolean" -> preparedStatement.setBoolean(i + 1, values[i] as Boolean)
                    }
                }
                preparedStatement.execute()
                returnValue = getReturnValue(autokeys, preparedStatement, resultSet)
            }
        }

        return returnValue
    }

    /**
     * Helperfunction for executeStatement functions which returns the generated keys, when they are requested
     * otherwise calls the submitted lambda function for the resultset
     * @see executeStatement
     * @see executePreparedStatement
     */
    private fun getReturnValue(autokeys: Boolean,statement: Statement,resultSet: (ResultSet) -> Any?) : Any? {

        return if (autokeys) {
            with(statement.generatedKeys) {
                if (!next()) return@with null
                getInt(1)
            }
        }
        else resultSet(statement.resultSet?:return null)
    }
}