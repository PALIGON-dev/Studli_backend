package com.learnapp.plugins

import com.learnapp.auth.FirebaseAdmin
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.response.*
import java.io.File

data class FirebasePrincipal(val uid: String, val email: String?) : Principal

fun Application.configureAuth() {
    val serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT") ?: "serviceAccountKey.json"
    
    if (File(serviceAccountPath).exists()) {
        FirebaseAdmin.init(serviceAccountPath)
    } else {
        log.warn("Firebase service account key not found at ${serviceAccountPath}. Auth will fail.")
    }

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
