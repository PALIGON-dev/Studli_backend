package com.learnapp

import com.learnapp.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureDatabase()
    configureAuth()
    configureRouting()
}
