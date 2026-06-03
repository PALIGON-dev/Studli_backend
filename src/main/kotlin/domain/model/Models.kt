package com.learnapp.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.bson.types.ObjectId

@Serializable
data class Course(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val title: String,
    val description: String,
    val coverUrl: String = "",
    val category: String = "Другое",
    val authorId: String,
    val lessons: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = false,
)

@Serializable
enum class LessonType { VIDEO, TEXT, QUIZ }

@Serializable
data class Lesson(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val courseId: String,
    val title: String,
    val order: Int,
    val type: LessonType,
    val durationMinutes: Int = 0,
    val videoUrl: String? = null,
    val durationSeconds: Int? = null,
    val markdownContent: String? = null,
    val quizId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class User(
    @SerialName("_id")
    val uid: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CreateUserRequest(
    val displayName: String,
    val email: String,
)

@Serializable
data class Achievement(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val userId: String = "",
    val key: String,
    val title: String,
    val description: String,
    val iconEmoji: String = "🏆",
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
)

@Serializable
data class UserStats(
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val activeCourses: Int,
    val remainingLessons: Int,
    val completedLessons: Int,
    val achievements: List<Achievement>,
)

@Serializable
data class LessonProgress(
    @SerialName("_id")
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
    val remainingLessons: Int,
    val totalMinutes: Int,
    val progressPercent: Float,
)
