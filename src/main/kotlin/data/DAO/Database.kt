package com.learnapp.data.DAO

import com.learnapp.domain.model.Course
import com.learnapp.domain.model.Lesson
import com.learnapp.domain.model.LessonProgress
import com.learnapp.domain.model.User
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider

object Database {

    private lateinit var client: MongoClient
    private lateinit var db: MongoDatabase

    fun init(uri: String, dbName: String) {
        val codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromProviders(KotlinSerializerCodecProvider()),
            MongoClientSettings.getDefaultCodecRegistry(),
        )

        client = MongoClient.create(uri)
        db = client.getDatabase(dbName).withCodecRegistry(codecRegistry)
    }

    val users: MongoCollection<User>
        get() = db.getCollection("users")

    val courses: MongoCollection<Course>
        get() = db.getCollection("courses")

    val lessons: MongoCollection<Lesson>
        get() = db.getCollection("lessons")

    val progress: MongoCollection<LessonProgress>
        get() = db.getCollection("lesson_progress")
}
