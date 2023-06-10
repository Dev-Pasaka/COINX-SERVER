package com.example.responses

import kotlinx.serialization.Serializable


@Serializable
data class Registration(

    val message:String = "failed to register",

    val isRegistered:Boolean = false

)
