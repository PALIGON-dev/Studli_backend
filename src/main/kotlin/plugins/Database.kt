package com.learnapp.plugins

import com.learnapp.data.DAO.Database
import io.ktor.server.application.*

fun Application.configureDatabase() {
    val uri = environment.config.property("mongodb.uri").getString()
    val dbName = environment.config.property("mongodb.database").getString()
    Database.init(uri, dbName)
    log.info("MongoDB connected → $uri / $dbName")
}
