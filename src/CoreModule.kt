
import chat.daos.ChatUserDAO
import chat.daos.DatabaseConnectionDAO
import chat.servercommunication.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.auth.*
import io.ktor.gson.GsonConverter
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.routing.routing
import io.ktor.util.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import java.text.DateFormat



/**
 * Start the Server with a netty engine
 * This is the starting point for your whole application and there can be only one of it in the whole application
 */
fun main(args: Array<String>) : Unit =  io.ktor.server.netty.EngineMain.main(args)


@KtorExperimentalAPI
@kotlin.jvm.JvmOverloads
fun Application.coremodule(testing: Boolean = false) {

    val gson = Gson()
    val logger = LoggerFactory.getLogger("infologger")

    /**
     * Setup the environment of the server
     */
    setupEnvironment(environment)


    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
            register(ContentType.Text.Plain, GsonConverter(GsonBuilder().create()))
        }
    }

    /**
     * Forwaredheader to support the use of reverse proxy(should be removed when not using a reverse proxy)
     * @see https://ktor.io/servers/features/forward-headers.html for explanation
     */
    if (ServerData.behindProxy){
        install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
        install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    }

    /**
     * Installing Websockets and defining their behaviour
     * @see https://ktor.io/servers/features/websockets.html for explanation
     */
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    /**
     * Installing Authentication methods
     * If you need authentication methods you should define them all in this coremodule
     * and use them in your module.
     * @see https://ktor.io/servers/features/authentication.html for explanation
     */
    install(Authentication) {
            this.basic("admin"){
                realm = "Chatserver Adminlogin"
                validate { credentials ->
                    if (credentials.name != ServerData.adminuser || credentials.password != ServerData.adminpass) null
                    else UserIdPrincipal("admin")
                }
            }
    }


    routing {
        /**
     * Basic connection for visitors of the website
     */
        webSocket("/websocket") {
            try {
                ServerData.userConnected(call.attributes[ServerData.cookieAttribute],this)
                ServerToUser.sendHostStatus(this)
                if (ServerToUser.sendChatLog(call.attributes[ServerData.cookieAttribute],this)){
                    ServerData.addActiveChatUser(call.attributes[ServerData.cookieAttribute])
                }

                while (true) {

                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        log.info(frame.readText())
                        val json = gson.fromJson<JsonObject>(frame.readText(), JsonObject::class.java)
                        UserToServer.handleMsg(json, this)
                    }
                    else throw Exception()
                }
            }catch (e : Exception){
                val socketID = call.attributes.getOrNull(ServerData.cookieAttribute)
                socketID?.let {
                    logger.info("User disconnected;$socketID")
                }
                ServerData.userDisconnected(this)
            }
        }

    }.intercept(ApplicationCallPipeline.Monitoring){
        val cookies = call.request.cookies.rawCookies
        if (cookies.containsKey("ChatID")){
            var userID = call.request.cookies.rawCookies.getValue("ChatID")
            if (!UserIDHandling.checkValidUserID(cookies.getValue("ChatID"))){
                val userCookie = UserIDHandling.createNewUser()
                call.response.cookies.append(userCookie)
                userID = userCookie.value

            }
            UserIDHandling.setUserID(call,userID)
        }
        else{
            val newID = UserIDHandling.createNewUser()
            call.response.cookies.append(newID)
        }

    }



    routing {
        /**
         * Connection as Admin to the server needs an authentication
         */
        authenticate("admin") {
            webSocket("/websocketAdmin") {

                logger.info("User connected;Admin")
                ServerData.setHostSocket(this)
                try {
                    ServerToAdmin.getOnlineChatUser()
                    while (true) {
                        val frame = incoming.receive()
                        if (frame is Frame.Text) {
                            val json = gson.fromJson(frame.readText(), JsonObject::class.java)
                            logger.info("Admin sent;${json.toString()}")
                            AdminToServer.handleMessage(json)
                        }
                        else throw Exception()
                    }
                }catch (e: Exception){
                    logger.error("User disconnected;Admin ${e.message}")
                    ServerData.setOffline()
                    ServerData.adminSocket = null
                }
            }
        }


    }





}

/**
 * Setup the whole server environment
 * @param environment the applicationenvironment with which the server is started
 */
@KtorExperimentalAPI
private fun setupEnvironment(environment: ApplicationEnvironment){
    DatabaseConnectionDAO.setupDatabase(environment)
    ServerData.setupServer(environment)
    ChatUserDAO.setupSocketIDSalt(environment)
}


