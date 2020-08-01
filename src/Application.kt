package de.kotlindevelopment

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.features.*
import org.slf4j.event.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.auth.*
import io.ktor.gson.*


/**
 * Start the Server with a netty engine
 * This is the starting point for your whole application and there can be only one of it in the whole application
 */
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


/**
 * Coremodule which loads all features, which will be used by the server.
 * This should be done to fulfill the single responsibility principle and to prevent
 * installing a feature accidently twice, which will throw an exception
 * Trying to install most features twice will throw an exception
 * (Route can be installed multiple times and should be implemented in the module itself)
 */

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    /**
     *  Installing Locations to define Paths as Locations
     *  @see https://ktor.io/servers/features/locations.html
     *  and bottom of the file for explanation and samples
     */
    install(Locations) {
    }

    /**
     * Forwaredheader to support the use of reverse proxy(should be removed when not using a reverse proxy)
     * @see https://ktor.io/servers/features/forward-headers.html for explanation
     */
    install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    /**
     * Installing Httpsredirect.
     * @see https://ktor.io/servers/features/https-redirect.html for explanation
     *
     * SSL Certificates for encrypting can be feteched from
     * @see https://letsencrypt.org/ for free
     *
     * To use without reverseproxy ( REMOVE FORWARDED HEADER SUPPORT! )
     * refer to @see https://ktor.io/quickstart/guides/ssl.html#configuring-ktor-to-use-the-generated-jks
     *
     * When you use a reverse proxy you can remove the HttpsRedirect and instead follow
     * this guide @see https://ktor.io/quickstart/guides/ssl.html#docker
     *
     */
    // https://ktor.io/servers/features/https-redirect.html#testing
    if (!testing) {
        install(HttpsRedirect) {
            // The port to redirect to. By default 443, the default HTTPS port.
            sslPort = 443
            // 301 Moved Permanently, or 302 Found redirect.
            permanentRedirect = true
        }
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
        basic("myBasicAuth") {
            realm = "Ktor Server"
            validate { if (it.name == "test" && it.password == "password") UserIdPrincipal(it.name) else null }
        }
    }
}


/**
 * Examples how to use Locations
 */

@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")

@Location("/type/{name}") data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}

