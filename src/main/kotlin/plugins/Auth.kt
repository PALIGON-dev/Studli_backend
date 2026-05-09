package com.learnapp.plugins

import com.learnapp.auth.FirebaseAdmin
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.response.*

data class FirebasePrincipal(val uid: String, val email: String?) : Principal

fun Application.configureAuth() {
    val serviceAccountPath = "D:\\Study\\University\\3_year\\Клиент-сервер\\Studli_backend\\serviceAccountKey.json"

    FirebaseAdmin.init(serviceAccountPath)

    install(Authentication) {
        bearer("firebase") {
            authenticate { tokenCredential ->
                try {
                    val decoded = FirebaseAdmin.verifyIdToken(tokenCredential.token)
                    FirebasePrincipal(uid = decoded.uid, email = decoded.email)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
