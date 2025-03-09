package io.github.thebestandgreatest

import club.arson.impulse.api.config.ServerConfig
import club.arson.impulse.api.server.Broker
import club.arson.impulse.api.server.Status
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
	private val client = HttpClient(CIO) {
		engine {
			https {
				trustManager = TrustAllX509TrustManger()
			}
		}
		install(ContentNegotiation) {
			json(Json {
				prettyPrint = true
				isLenient = true
			})
		}
	}

	/**
	 * Creates a new instance and sets up the broker based on the config
	 */
	init {
		craftyConfig = serverConfig.config as CraftyControllerBrokerConfig
	}

	override fun address(): Result<InetSocketAddress> {
		if (craftyConfig.address == null) {
			return Result.failure(IllegalArgumentException("No address specified in config"))
		}
		val port = craftyConfig.address?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 25565
		return runCatching { InetSocketAddress(craftyConfig.address, port) }
	}

	override fun getStatus(): Status {
		if (apiRequest(RequestType.STATUS).data?.running == true) {
			return Status.RUNNING
		}
		return Status.STOPPED
	}

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
	 * Attempts to remove the server
	 *
	 * If the server is running it will attempt to stop it. Will permanently delete all data associated
	 * with this server
	 *
	 * requires CONFIG permission
	 *
	 * @return success if the server was removed, else an error
	 */
	override fun removeServer(): Result<Unit> {
		stopServer()
		if (apiRequest(RequestType.DELETE).status == "ok") {
			return Result.success(Unit)
		}

		return Result.failure(Throwable("ERROR! Unable to delete server: ${craftyConfig.serverID}"))
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

	private fun apiRequest(type: RequestType): ApiData = runBlocking {
		val serverID = craftyConfig.serverID
		val action: String = type.request
		val token = craftyConfig.token
		val craftyAddress = craftyConfig.craftyAddress

		val response: ApiData = client.request(craftyAddress) {
			method = when (type) {
				RequestType.START -> HttpMethod.Post
				RequestType.STOP -> HttpMethod.Post
				RequestType.STATUS -> HttpMethod.Get
				RequestType.DELETE -> HttpMethod.Delete
			}
			url {
				appendPathSegments(serverID, action)
			}
			headers {
				append(
					HttpHeaders.Authorization, "Bearer $token"
				)
			}
		}.body()

		return@runBlocking response
	}

}

/**
 * Basic enum class to categorize all the different valid requests this broker can make, as well as the url for the api to preform those requests
 */
enum class RequestType(val request: String) {
	START("action/start_server"),
	STOP("action/stop_server"),
	DELETE(""),
	STATUS("stats")
}

/**
 * Top level data class for the crafty api response json object
 */
@Serializable
data class ApiData(
	val status: String,
	val data: ServerData? = null
)

/**
 * Middle level data class for the crafty api response json object
 */
@Serializable
data class ServerData(
	@SerialName("stats_id")
	val statsID: Long,

	val created: String,

	@SerialName("server_id")
	val serverID: ServerConfig,

	val started: String,
	val running: Boolean,
	val cpu: Double,
	val mem: String,

	@SerialName("mem_percent")
	val memPercent: Double,

	@SerialName("world_name")
	val worldName: String,

	@SerialName("world_size")
	val worldSize: String,

	@SerialName("server_port")
	val serverPort: Long,

	@SerialName("int_ping_results")
	val intPingResults: String,

	val online: Long,
	val max: Long,
	val players: String,
	val desc: String,
	val version: String,
	val updating: Boolean,

	@SerialName("waiting_start")
	val waitingStart: Boolean,

	@SerialName("first_run")
	val firstRun: Boolean,

	val crashed: Boolean,
	val importing: Boolean
)

/**
 * Bottom level data class for the crafty api response json object
 */
@Serializable
data class ServerConfig(
	@SerialName("server_id")
	val serverID: String,

	val created: String,

	@SerialName("server_name")
	val serverName: String,

	val path: String,

	val executable: String,

	@SerialName("log_path")
	val logPath: String,

	@SerialName("execution_command")
	val executionCommand: String,

	@SerialName("auto_start")
	val autoStart: Boolean,

	@SerialName("auto_start_delay")
	val autoStartDelay: Long,

	@SerialName("crash_detection")
	val crashDetection: Boolean,

	@SerialName("stop_command")
	val stopCommand: String,

	@SerialName("executable_update_url")
	val executableUpdateURL: String,

	@SerialName("server_ip")
	val serverIP: String,

	@SerialName("server_port")
	val serverPort: Long,

	@SerialName("logs_delete_after")
	val logsDeleteAfter: Long,

	val type: String
)

/**
 * Trust manager class to bypass the self-signed cert that crafty config uses by default by trusting all certs, valid, expired and self signed
 *
 * Horrible thing to do in basically all scenarios, but is probably okay when using a localhost address
 * Use with extreme caution
 *
 * TODO potential fixes for this might including grabbing the crafty controller keys from storage and adding them to the CA list
 */
class TrustAllX509TrustManger : X509TrustManager {
	override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
	override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
	override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
}