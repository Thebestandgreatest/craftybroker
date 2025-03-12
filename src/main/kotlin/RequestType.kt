package io.github.thebestandgreatest

import io.ktor.http.*

/**
 * Basic enum class to categorize all the different valid requests this broker can make, the url to make the request to, and the type of http request that should be used
 */
enum class RequestType(val request: String, val method: HttpMethod) {
	START("action/start_server", HttpMethod.Post),
	STOP("action/stop_server", HttpMethod.Post),
	DELETE("", HttpMethod.Delete),
	STATUS("stats", HttpMethod.Get),
	//CREATE("", HttpMethod.Post),
}