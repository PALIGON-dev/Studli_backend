package com.learnapp.data.repository

import com.learnapp.data.DAO.Database
import com.learnapp.domain.model.*
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

private val upsertOptions = ReplaceOptions().upsert(true)

private const val XP_PER_LESSON = 10
private fun xpForLevel(level: Int) = level * 50

class UserRepository {
    private val col get() = Database.users

    suspend fun findById(uid: String): User? =
        col.find(Filters.eq("_id", uid)).firstOrNull()

    suspend fun upsert(user: User) {
        col.replaceOne(Filters.eq("_id", user.uid), user, upsertOptions)
    }

    suspend fun addXp(uid: String, xp: Int) {
        val user = findById(uid) ?: return
        var newXp = user.xp + xp
        var newLevel = user.level
        while (newXp >= xpForLevel(newLevel)) {
            newXp -= xpForLevel(newLevel)
            newLevel++
        }
        upsert(user.copy(xp = newXp, level = newLevel))
    }
}

class CourseRepository {
    private val col get() = Database.courses

    suspend fun getAll(publishedOnly: Boolean = true): List<Course> =
        if (publishedOnly) col.find(Filters.eq("isPublished", true)).toList()
        else col.find().toList()

    suspend fun getByCategory(category: String): List<Course> =
        col.find(
            Filters.and(
                Filters.eq("isPublished", true),
                Filters.eq("category", category),
            )
        ).toList()

    suspend fun search(query: String): List<Course> =
        col.find(
            Filters.and(
                Filters.eq("isPublished", true),
                Filters.regex("title", query, "i"),
            )
        ).toList()

    suspend fun getById(id: String): Course? =
        col.find(Filters.eq("_id", id)).firstOrNull()

    suspend fun create(course: Course): Course {
        col.insertOne(course)
        return course
    }

    suspend fun update(course: Course): Boolean =
        col.replaceOne(Filters.eq("_id", course.id), course).modifiedCount > 0

    suspend fun delete(id: String): Boolean =
        col.deleteOne(Filters.eq("_id", id)).deletedCount > 0
}

class LessonRepository {
    private val col get() = Database.lessons

    suspend fun getByCourseId(courseId: String): List<Lesson> =
        col.find(Filters.eq("courseId", courseId))
            .toList()
            .sortedBy { it.order }

    suspend fun getById(id: String): Lesson? =
        col.find(Filters.eq("_id", id)).firstOrNull()

    suspend fun create(lesson: Lesson): Lesson {
        col.insertOne(lesson)
        return lesson
    }

    suspend fun update(lesson: Lesson): Boolean =
        col.replaceOne(Filters.eq("_id", lesson.id), lesson).modifiedCount > 0

    suspend fun delete(id: String): Boolean =
        col.deleteOne(Filters.eq("_id", id)).deletedCount > 0
}

class ProgressRepository {
    private val col get() = Database.progress

    suspend fun getForUserAndLesson(userId: String, lessonId: String): LessonProgress? =
        col.find(
            Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("lessonId", lessonId),
            )
        ).firstOrNull()

    suspend fun getForUserAndCourse(userId: String, courseId: String): List<LessonProgress> =
        col.find(
            Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("courseId", courseId),
            )
        ).toList()

    suspend fun getAllForUser(userId: String): List<LessonProgress> =
        col.find(Filters.eq("userId", userId)).toList()

    suspend fun upsert(progress: LessonProgress) {
        col.replaceOne(Filters.eq("_id", progress.id), progress, upsertOptions)
    }

    suspend fun markCompleted(userId: String, lessonId: String) {
        val existing = getForUserAndLesson(userId, lessonId) ?: return
        col.replaceOne(
            Filters.eq("_id", existing.id),
            existing.copy(completed = true, completedAt = System.currentTimeMillis()),
            upsertOptions,
        )
    }

    suspend fun countCompletedForUser(userId: String): Int =
        getAllForUser(userId).count { it.completed }
}

class AchievementRepository {
    private val col get() = Database.achievements

    suspend fun getForUser(userId: String): List<Achievement> {
        val unlocked = col.find(Filters.eq("userId", userId)).toList()
        return ALL_ACHIEVEMENTS.map { template ->
            val found = unlocked.find { it.key == template.key }
            template.copy(isUnlocked = found != null, unlockedAt = found?.unlockedAt)
        }
    }

    suspend fun unlock(userId: String, key: String) {
        val existing = col.find(
            Filters.and(Filters.eq("userId", userId), Filters.eq("key", key))
        ).firstOrNull()
        if (existing == null) {
            val template = ALL_ACHIEVEMENTS.find { it.key == key } ?: return
            col.insertOne(
                template.copy(
                    id = org.bson.types.ObjectId().toHexString(),
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    companion object {
        val ALL_ACHIEVEMENTS = listOf(
            Achievement(key = "first_lesson", title = "Первый шаг", description = "Завершите первый урок", iconEmoji = "🎯"),
            Achievement(key = "four_courses", title = "Увлечённый", description = "Начните 4 курса", iconEmoji = "🔥"),
            Achievement(key = "course_done", title = "Покоритель курсов", description = "Завершите курс полностью", iconEmoji = "🏆"),
            Achievement(key = "level_10", title = "Мастер", description = "Достигните уровня 10", iconEmoji = "⭐"),
        )
    }
}

class UserStatsRepository(
    private val userRepo: UserRepository = UserRepository(),
    private val courseRepo: CourseRepository = CourseRepository(),
    private val lessonRepo: LessonRepository = LessonRepository(),
    private val progressRepo: ProgressRepository = ProgressRepository(),
    private val achievementRepo: AchievementRepository = AchievementRepository(),
) {
    suspend fun getStats(userId: String): UserStats {
        val user = userRepo.findById(userId) ?: error("User not found")
        val allProgress = progressRepo.getAllForUser(userId)
        val completedLessons = allProgress.count { it.completed }

        val activeCourseIds = allProgress.map { it.courseId }.distinct()
        val activeCourses = activeCourseIds.size

        val remainingLessons = activeCourseIds.sumOf { courseId ->
            val course = courseRepo.getById(courseId) ?: return@sumOf 0
            val done = allProgress.count { it.courseId == courseId && it.completed }
            (course.lessons.size - done).coerceAtLeast(0)
        }

        val achievements = achievementRepo.getForUser(userId)
        val xpForNext = xpForLevel(user.level) - user.xp

        return UserStats(
            level = user.level,
            xp = user.xp,
            xpForNextLevel = xpForNext,
            activeCourses = activeCourses,
            remainingLessons = remainingLessons,
            completedLessons = completedLessons,
            achievements = achievements,
        )
    }
}
