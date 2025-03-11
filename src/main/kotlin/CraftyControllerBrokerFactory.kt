package io.github.thebestandgreatest

import club.arson.impulse.api.config.ServerConfig
import club.arson.impulse.api.server.Broker
import club.arson.impulse.api.server.BrokerFactory
import org.slf4j.Logger

class CraftyControllerBrokerFactory : BrokerFactory {

	/**
	 * This broker is designed to call the crafty controller api to control servers
	 */

	override val provides: List<String> = listOf("crafty")


	/**
	 * Create a crafty broker from a ServerConfig Object
	 *
	 * Checks to make sure the ServerConfig is a valid crafty config
	 * @param config Server config to create a crafty broker for
	 * @param logger Logger ref for log messages
	 * @return A result containing a crafty broker if we were able to make on for the server, else an error
	 */

	override fun createFromConfig(config: ServerConfig, logger: Logger?): Result<Broker> {
		return when (config.config) {
			is CraftyControllerBrokerConfig -> Result.success(CraftyControllerBroker(config, logger))
			else -> Result.failure(IllegalArgumentException("Invalid configuration for crafty broker"))
		}
	}

}