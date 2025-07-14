package io.github.thebestandgreatest

import kotlinx.serialization.SerialName

data class ResponseData(
    @SerialName("server_id")
    var serverID: String? = null
)