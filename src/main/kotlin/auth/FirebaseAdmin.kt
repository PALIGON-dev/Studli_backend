package com.learnapp.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import java.io.FileInputStream

object FirebaseAdmin {

    fun init(serviceAccountPath: String) {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val stream = if (serviceAccountPath.startsWith("/")) {
            FileInputStream(serviceAccountPath)
        } else {
            // Relative path — resolve from working dir
            FileInputStream(serviceAccountPath)
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(stream))
            .build()

        FirebaseApp.initializeApp(options)
    }

    /** Throws FirebaseAuthException if the token is invalid or expired */
    fun verifyIdToken(idToken: String): FirebaseToken =
        FirebaseAuth.getInstance().verifyIdToken(idToken)
}
