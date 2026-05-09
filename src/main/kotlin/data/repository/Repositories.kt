package com.learnapp.data.repository

import com.learnapp.data.DAO.Database
import com.learnapp.domain.model.Course
import com.learnapp.domain.model.Lesson
import com.learnapp.domain.model.LessonProgress
import com.learnapp.domain.model.User
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

private val upsertOptions = ReplaceOptions().upsert(true)

class UserRepository {
    private val col get() = Database.users

    suspend fun findById(uid: String): User? =
        col.find(Filters.eq("_id", uid)).firstOrNull()

    suspend fun upsert(user: User) {
        col.replaceOne(Filters.eq("_id", user.uid), user, upsertOptions)
    }
}

class CourseRepository {
    private val col get() = Database.courses

    suspend fun getAll(publishedOnly: Boolean = true): List<Course> =
        if (publishedOnly)
            col.find(Filters.eq("isPublished", true)).toList()
        else
            col.find().toList()

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
}
