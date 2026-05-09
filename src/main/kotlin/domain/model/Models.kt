package com.learnapp.domain.model

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class Course(
    @BsonId
    val id: String = ObjectId().toHexString(),
    val title: String,
    val description: String,
    val coverUrl: String = "",
    val authorId: String,
    val lessons: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = false,
)

@Serializable
enum class LessonType { VIDEO, TEXT, QUIZ }

@Serializable
data class Lesson(
    @BsonId
    val id: String = ObjectId().toHexString(),
    val courseId: String,
    val title: String,
    val order: Int,
    val type: LessonType,
    // VIDEO
    val videoUrl: String? = null,
    val durationSeconds: Int? = null,
    // TEXT
    val markdownContent: String? = null,
    // QUIZ (added later)
    val quizId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)


@Serializable
data class User(
    @BsonId
    val uid: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class LessonProgress(
    @BsonId
    val id: String = ObjectId().toHexString(),
    val userId: String,
    val courseId: String,
    val lessonId: String,
    val completed: Boolean = false,
    val videoPositionSeconds: Int = 0,
    val completedAt: Long? = null,
)

@Serializable
data class CourseProgress(
    val courseId: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val progressPercent: Float,
)
