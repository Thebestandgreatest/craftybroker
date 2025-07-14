package io.github.thebestandgreatest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//interface ResponseData {
//    @SerialName("status")
//    var status: String
//
//    @SerialName("data")
//    var data: String
//
//    @SerialName("error")
//    var error: String?
//
//    @SerialName("error_data")
//    var error_data: String?
//
//    @SerialName("info")
//    var info: String?
//}

/**
 * Data classes to hold the json response from the crafty controller api
 */
@Serializable
data class Response(
    @SerialName("status") var status: String,
    //@SerialName("data") var data: ServerStatistics? = ServerStatistics(),
    @SerialName("error") var error: String? = null,
    @SerialName("error_data") var errorData: String? = null,
    @SerialName("info") var info: String? = null,
)

