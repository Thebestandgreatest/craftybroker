package io.github.thebestandgreatest

import club.arson.impulse.api.config.ServerConfig
import club.arson.impulse.api.server.Broker
import club.arson.impulse.api.server.Status
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * This broker is designed to send api requests to a crafty controller instance
 *
 * @property ServerConfig Server config to create a crafty broker for
 * @property logger Logger ref for log messages
 */
class CraftyControllerBroker(serverConfig: ServerConfig, private val logger: Logger? = null) : Broker {
	private var craftyConfig: CraftyControllerBrokerConfig
	private val client: HttpClient

	/**
	 * Creates a new instance and sets up the broker based on the config
	 */
	init {
		craftyConfig = serverConfig.config as CraftyControllerBrokerConfig
		client = HttpClient(CIO) {
			engine {
				https {
					if (craftyConfig.insecureMode) {
						trustManager = TrustAllX509TrustManger()
						logger?.warn("Running in insecure mode! Only enable this if you absolutely need to")
					}
				}
			}
			install(ContentNegotiation) {
				json(Json {
					prettyPrint = true
					isLenient = true
					ignoreUnknownKeys = true
				})
			}
		}
	}

	override fun address(): Result<InetSocketAddress> {
		if (craftyConfig.address == null) {
			return Result.failure(IllegalArgumentException("No address specified in config"))
		}
		val port = craftyConfig.address?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 25565
		return runCatching { InetSocketAddress(craftyConfig.address, port) }
	}

	/**
	 * Returns the status of the server
	 * @return Status type representing the server state
	 */
	override fun getStatus(): Status {
		if (apiRequest(RequestType.STATUS).data?.running == true) {
			return Status.RUNNING
		}
		return Status.STOPPED
	}

	/**
	 * @return true if the server is running
	 */
	override fun isRunning(): Boolean {
		return getStatus() == Status.RUNNING
	}


	/**
	 * Taken straight from CommandBroker.kt example
	 *
	 * Reconcile any changes to the config
	 *
	 * All changes require restart
	 *
	 * @param config Server config to reconcile
	 * @return the closure to actually to the reconciliation
	 */
	override fun reconcile(config: ServerConfig): Result<Runnable?> {
		if (config.type != "crafty") {
			return Result.failure(IllegalArgumentException("Expected CraftyControllerConfig and got something else!"))
		}

		val newConfig = config.config as CraftyControllerBrokerConfig
		return if (newConfig != craftyConfig) {
			Result.success(Runnable {
				stopServer()
				craftyConfig = newConfig
				startServer()
			})
		} else {
			Result.success(Runnable {
				craftyConfig = newConfig
			})
		}
	}


	/**
	 * Attempts to kill the server
	 *
	 * Used if the server won't stop for whatever reason
	 *
	 * requires CONFIG permission
	 *
	 * @return success if the server was killed, else an error
	 */
	override fun removeServer(): Result<Unit> {
		stopServer()
		if (apiRequest(RequestType.KILL).status == "ok") {
			return Result.success(Unit)
		}

		return Result.failure(Throwable("ERROR! Unable to kill server: ${craftyConfig.serverID}"))
	}


	/**
	 * Attempts to start the server
	 *
	 * requires COMMANDS permission
	 *
	 * @return success if the server was started, else an error
	 */
	override fun startServer(): Result<Unit> {
		if (apiRequest(RequestType.START).status == "ok") {
			return Result.success(Unit)
		}
		return Result.failure(Throwable("ERROR! Unable to start server: ${craftyConfig.serverID}"))
	}


	/**
	 * Attempts to stop the server
	 *
	 * requires COMMANDS permission
	 *
	 * @return success if the server was stopped, else an error
	 */
	override fun stopServer(): Result<Unit> {
		if (apiRequest(RequestType.STOP).equals("ok")) {
			return Result.success(Unit)
		}

		return Result.failure(Throwable("ERROR! Unable to stop server: ${craftyConfig.serverID}"))
	}

	/**
	 * Sends an api request to the crafty controller instance
	 *
	 * @param type type of the request to send to the server
	 * @return an ApiData object representing the received data
	 */
	private fun apiRequest(type: RequestType): ApiData = runBlocking {
		logger?.info("Trying RequestType: $type")
		var response: ApiData
		try {
			response = client.request(craftyConfig.craftyAddress) {
				method = type.method
				url {
					appendPathSegments("api/v2/servers", craftyConfig.serverID, type.request)
				}
				headers {
					append(
						HttpHeaders.Authorization, "Bearer ${craftyConfig.token}"
					)
				}
			}.body()
		} catch (e: JsonConvertException) {
			val responseString: String = client.request(craftyConfig.craftyAddress) {
				method = type.method
				url {
					appendPathSegments("api/v2/servers", craftyConfig.serverID, type.request)
				}
				headers {
					append(
						HttpHeaders.Authorization, "Bearer ${craftyConfig.token}"
					)
				}
			}.bodyAsText()
			logger?.error("invalid json response from crafty api")
			logger?.info(responseString)
			response = Json.decodeFromString(responseString)
		}

		return@runBlocking response
	}

}

/**
 * Trust manager class to bypass the self-signed cert that crafty config uses by default by trusting all certs, valid, expired and self signed
 *
 * Horrible thing to do in basically all scenarios, but is probably okay when using a localhost address
 * Use with extreme caution
 *
 * Requires the config option insecureMode to be enabled
 *
 * TODO potential fixes for this might including grabbing the crafty controller keys from storage and adding them to the CA list
 */
class TrustAllX509TrustManger : X509TrustManager {
	override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
	override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
	override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
}