
import chat.api_enums.ClientStatus
import chat.api_enums.Types
import chat.daos.ChatMessagesDAO
import chat.daos.ChatUserDatabaseDAO
import chat.daos.DatabaseConnectionDAO
import chat.dataclass.ChatMessage
import chat.exceptions.NoAdminCredentials
import chat.servercommunication.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.auth.*
import io.ktor.gson.GsonConverter
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.*

import io.ktor.utils.io.readFully
import io.ktor.websocket.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.DateFormat
import java.util.*


/**
 * Start the Server with a netty engine
 * This is the starting point for your whole application and there can be only one of it in the whole application
 */
fun main(args: Array<String>) : Unit =  io.ktor.server.netty.EngineMain.main(args)


@KtorExperimentalAPI
@kotlin.jvm.JvmOverloads
fun Application.coremodule(testing: Boolean = false) {

    val logger = LoggerFactory.getLogger("infologger")

    /**
     * Setup the environment of the server
     */
    setupEnvironment(environment)


    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
            register(ContentType.Text.Plain, GsonConverter(GsonBuilder().apply {}.create()))
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
    install(io.ktor.websocket.WebSockets) {
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
         * Connection as Admin to the server needs an authentication
         */
        authenticate("admin") {
            webSocket("/") {

                logger.info("User connected;Admin")
                val gson = Gson()
                ServerData.setHostSocket(this)
                try {
                    ServerToAdmin.getOnlineChatUser()
                    while (true) {
                        val frame = incoming.receive()
                        if (frame is Frame.Text) {
                            val json = gson.fromJson(frame.readText(), JsonObject::class.java)
                            AdminToServer.handleMessage(json)
                        }
                        else throw Exception()
                    }
                }catch (e: Exception){
                    logger.info("User disconnected;Admin")
                    ServerData.setOffline()
                    ServerData.adminSocket = null
                }
            }
        }

        /**
         * Basic connection for visitors of the website
         */
        webSocket("/myws/echo") {

            try {
                val gson = Gson()
                ServerToUser.sendHostStatus(this)
                ServerToUser.requestSocketID(this)

                while (true) {
                    this.closeReason
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
    }



}

/**
 * Setup the whole server environment
 * @param environment the applicationenvironment with which the server is started
 */
private fun setupEnvironment(environment: ApplicationEnvironment){
    DatabaseConnectionDAO.setupDatabase(environment)
    ServerData.setupServer(environment)
    ChatUserDatabaseDAO.setupSocketIDSalt(environment)
}


