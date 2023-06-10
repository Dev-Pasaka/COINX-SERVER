package com.example.responses

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val message:String = "Failed to fetch userdata",

    val status:Boolean = false
)
