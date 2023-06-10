package com.example.responses

import kotlinx.serialization.Serializable

@Serializable
data  class SignInResponse(
    val message :String = "Wrong email or password",
    val status:Boolean = false
)