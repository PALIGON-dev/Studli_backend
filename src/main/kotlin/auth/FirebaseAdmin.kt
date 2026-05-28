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

        val stream = FileInputStream(serviceAccountPath)

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(stream))
            .build()

        FirebaseApp.initializeApp(options)
    }

    fun verifyIdToken(idToken: String): FirebaseToken =
        FirebaseAuth.getInstance().verifyIdToken(idToken)
}
