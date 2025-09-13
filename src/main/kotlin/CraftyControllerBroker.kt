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
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.net.SocketException

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
	 * Reconcile any changes to the config with what impulse will try to do
	 *
	 * @param config Server config to reconcile with
	 * @return Result of a success or failure
	 */
	override fun reconcile(config: ServerConfig): Result<Runnable?> {
		if (config.type != "crafty") {
			logger?.error("config type error")
			return Result.failure(IllegalArgumentException("Expected CraftyControllerConfig and got ${config.type}"))
		}

		val newConfig = config.config as CraftyControllerBrokerConfig
		if (newConfig != craftyConfig) { // if the config changed
			craftyConfig = newConfig
			return Result.success(null)
		} else { // if the config didn't change
			craftyConfig = newConfig
			return Result.success(null)
		}
	}

	/**
	 * Returns the status of the server
	 *
	 * @return Status type representing the server state
	 */
	override fun getStatus(): Status {
		val response = apiRequest(RequestType.STATUS)
		if (response.status != "ok") {
            logger?.error("Unable to send status request to ${craftyConfig.serverID} \n ${response.errorData}")
			return Status.UNKNOWN
		}
		if (response.data?.running == true) {
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
	 * Attempts to remove the server
	 *
	 * Requires CONFIG permission
	 *
	 * @return success if the server was killed, else an error
	 */
	override fun removeServer(): Result<Unit> {
		stopServer()
		val response = apiRequest(RequestType.DELETE)
		if (response.status == "ok") {
			return Result.success(Unit)
		}
		logger?.error("Unable to send delete request! Error: ${response.errorData}")
		return Result.failure(Throwable("ERROR! Unable to delete server: ${craftyConfig.serverID}, Error message: ${response.error}"))
	}


	/**
	 * Attempts to start the server
	 *
	 * Requires COMMANDS permission
	 *
	 * @return success if the server was started, else an error
	 */
	override fun startServer(): Result<Unit> {
		val response = apiRequest(RequestType.START)

        return if (response.status == "ok") { // sends a request to the crafty controller api to start the server
            return if (awaitServerStart().isSuccess) { // polls for the server to start running
                Result.success(Unit)
            } else { // server start timed out
                Result.failure(Throwable("Server failed to start! (timeout)"))
            }
        } else { // if unable to send a request, return an error
            Result.failure(Throwable("Unable to send start command to ${craftyConfig.serverID}"))
        }
	}


	/**
	 * Attempts to stop the server
	 *
	 * Requires COMMANDS permission
	 *
	 * @return success if the server was stopped, else an error
	 */
	override fun stopServer(): Result<Unit> {
        val response = apiRequest(RequestType.STOP)

        return if (response.status == "ok") { // sends a request to the crafty controller api to start the server
            return if (awaitServerStop().isSuccess) { // polls for the server to start running
                Result.success(Unit)
            } else { // server start timed out
                Result.failure(Throwable("Server failed to stop! (timeout)"))
            }
        } else { // if unable to send a request, return an error
            Result.failure(Throwable("Unable to send stop command to ${craftyConfig.serverID}"))
        }
	}

    /**
     * Waits for the server to start running, polling for status every 0.1 seconds for 10 seconds
     * @return success if the server started, else an error
     */
    private fun awaitServerStart(): Result<Unit> {
        val startTime = System.currentTimeMillis()
        var isRunning = false
        while (!isRunning && System.currentTimeMillis() - startTime < 10000) {
            val status = getStatus()
            if (status == Status.RUNNING) {
                isRunning = true
            } else if (status == Status.UNKNOWN) {
                return Result.failure(Throwable("Server stopped unexpectedly!"))
            }
            Thread.sleep(100)
        }
        return if (isRunning) {
            Result.success(Unit)
        } else {
            Result.failure(Throwable("Server failed to start! (timeout)"))
        }
    }

    /**
     * Waits for the server to stop running, polling for status every 0.1 seconds for 10 seconds
     * @return success if the server stopped, else an error
     */
    private fun awaitServerStop(): Result<Unit> {
        val startTime = System.currentTimeMillis()
        var isRunning = false
        while (!isRunning && System.currentTimeMillis() - startTime < 10000) {
            val status = getStatus()
            if (status == Status.STOPPED) {
                isRunning = true
            } else if (status == Status.UNKNOWN) {
                return Result.failure(Throwable("Server is in unknown state!"))
            }
            Thread.sleep(100)
        }
        return if (isRunning) {
            Result.success(Unit)
        } else {
            Result.failure(Throwable("Server failed to stop! (timeout)"))
        }
    }

	/**
	 * Sends an api request to the crafty controller api
	 *
	 * @param type type of the request to send to the server
	 * @return an ApiData object representing the received data
	 */
	private fun apiRequest(type: RequestType): ApiData = runBlocking {
		logger?.debug("Trying RequestType: {}", type)
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
		} catch (e: SocketException) { // handles if the connection fails because of an improper protocol
			if (e.message.equals("Connection reset")) {
				logger?.error("Unable to connect to the api! Check the protocol of the address!")
				response = ApiData(
					status = "error",
					errorData = "Connection reset",
				)
			} else {
				throw e
			}
		}

		logger?.debug("Valid json. Returning...")
		logger?.debug(response.toString())

		return@runBlocking response
	}
}