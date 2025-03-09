package io.github.thebestandgreatest

import club.arson.impulse.api.config.BrokerConfig
import kotlinx.serialization.Serializable

@BrokerConfig("crafty")
@Serializable
data class CraftyControllerBrokerConfig(
	var address: String? = null,
	var serverID: String,
	var token: String,
	var craftyAddress: String = "https://localhost:8443"
)
