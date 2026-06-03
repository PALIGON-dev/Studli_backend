package com.learnapp.plugins

import com.learnapp.data.repository.*
import com.learnapp.domain.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import java.io.File

private val userRepo = UserRepository()
private val courseRepo = CourseRepository()
private val lessonRepo = LessonRepository()
private val progressRepo = ProgressRepository()
private val achievementRepo = AchievementRepository()
private val statsRepo = UserStatsRepository()

fun Application.configureRouting() {
    routing {

        val uploadsDir = File("uploads")
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs()
        }
        staticFiles("/uploads", uploadsDir)

        get("/health") {
            call.respond(mapOf("status" to "ok", "version" to "0.0.1"))
        }

        get("/api/v1/seed") {
            val courses = listOf(
                Course(title = "Kotlin для начинающих", description = "Основы языка Kotlin и создание первых приложений.", category = "Программирование", authorId = "admin", isPublished = true),
                Course(title = "Android Jetpack Compose", description = "Современная разработка UI на Android.", category = "Android", authorId = "admin", isPublished = true),
                Course(title = "Ktor: Backend на Kotlin", description = "Создание высокопроизводительных серверов.", category = "Backend", authorId = "admin", isPublished = true)
            )

            courses.forEach { course ->
                val created = courseRepo.create(course)
                val lessons = listOf(
                    Lesson(courseId = created.id, title = "Введение", order = 1, type = LessonType.TEXT, durationMinutes = 10, markdownContent = "# Добро пожаловать!\nЭто вводный урок."),
                    Lesson(courseId = created.id, title = "Основные понятия", order = 2, type = LessonType.VIDEO, durationMinutes = 15, videoUrl = "http://192.168.1.13:8080/uploads/sample.mp4"),
                    Lesson(courseId = created.id, title = "Практика", order = 3, type = LessonType.QUIZ, durationMinutes = 20, quizId = "quiz_1")
                )
                lessons.forEach { lessonRepo.create(it) }
                
                courseRepo.update(created.copy(lessons = lessons.map { it.id }))
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "Database seeded successfully. Make sure to put a 'sample.mp4' file in the 'uploads' folder!"))
        }

        route("/api/v1/courses") {
            get {
                val search = call.request.queryParameters["search"]
                val category = call.request.queryParameters["category"]

                val courses = when {
                    !search.isNullOrBlank() -> courseRepo.search(search)
                    !category.isNullOrBlank() && category != "Все" -> courseRepo.getByCategory(category)
                    else -> courseRepo.getAll(publishedOnly = true)
                }
                call.respond(courses)
            }

            get("/{id}") {
                val id = call.parameters["id"]
                    ?: throw IllegalArgumentException("Missing course id")
                val course = courseRepo.getById(id)
                    ?: throw NoSuchElementException("Course $id not found")
                call.respond(course)
            }
        }

        get("/test-stats") {
            val stats = statsRepo.getStats("eaApf9vV0gSd01h91l6rDjuMuqU2")
            call.respond(stats)
        }

        authenticate("firebase") {

            route("/api/v1/users") {

                get("/me") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val user = userRepo.findById(principal.uid)
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    call.respond(user)
                }

                post("/me") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val body = call.receive<CreateUserRequest>()
                    val existingUser = userRepo.findById(principal.uid)
                    
                    val user = existingUser?.copy(
                        displayName = body.displayName,
                        email = body.email
                    ) ?: User(
                        uid = principal.uid,
                        displayName = body.displayName,
                        email = body.email
                    )
                    
                    userRepo.upsert(user)
                    call.respond(HttpStatusCode.OK, user)
                }

                get("/me/stats") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val stats = statsRepo.getStats(principal.uid)
                    call.respond(stats)
                }
            }

            route("/api/v1/courses") {

                get("/my") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val enrolledCourseIds = progressRepo.getAllForUser(principal.uid)
                        .map { it.courseId }
                        .distinct()
                    val courses = courseRepo.getAll(publishedOnly = true)
                        .filter { it.id in enrolledCourseIds }
                    call.respond(courses)
                }

                post("/{id}/unenroll") {
val p = call.principal<FirebasePrincipal>()!!
val i = call.parameters["id"]!!
progressRepo.deleteForUserAndCourse(p.uid, i)
call.respond(HttpStatusCode.OK, mapOf("status" to "unenrolled"))
}
post("/{id}/enroll") {
    val principal = call.principal<FirebasePrincipal>()!!
    val id = call.parameters["id"]
        ?: throw IllegalArgumentException("Missing course id")
    val course = courseRepo.getById(id)
        ?: throw NoSuchElementException("Course $id not found")

    course.lessons.forEach { lessonId ->
        progressRepo.upsert(
            LessonProgress(
                userId = principal.uid,
                courseId = id,
                lessonId = lessonId,
                completed = false
            )
        )
    }
    call.respond(HttpStatusCode.OK, mapOf("status" to "enrolled", "courseId" to id))
}

                post {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val body = call.receive<Course>()
                    val course = body.copy(authorId = principal.uid)
                    call.respond(HttpStatusCode.Created, courseRepo.create(course))
                }

                put("/{id}") {
                    val id = call.parameters["id"]
                        ?: throw IllegalArgumentException("Missing course id")
                    val body = call.receive<Course>()
                    val updated = courseRepo.update(body.copy(id = id))
                    if (updated) call.respond(HttpStatusCode.OK, body)
                    else throw NoSuchElementException("Course $id not found")
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: throw IllegalArgumentException("Missing course id")
                    val deleted = courseRepo.delete(id)
                    if (deleted) call.respond(HttpStatusCode.NoContent)
                    else throw NoSuchElementException("Course $id not found")
                }
            }

            route("/api/v1/courses/{courseId}/lessons") {

                get {
                    val courseId = call.parameters["courseId"]
                        ?: throw IllegalArgumentException("Missing courseId")
                    call.respond(lessonRepo.getByCourseId(courseId))
                }

                get("/{lessonId}") {
                    val lessonId = call.parameters["lessonId"]
                        ?: throw IllegalArgumentException("Missing lessonId")
                    val lesson = lessonRepo.getById(lessonId)
                        ?: throw NoSuchElementException("Lesson $lessonId not found")
                    call.respond(lesson)
                }

                post {
                    val courseId = call.parameters["courseId"]
                        ?: throw IllegalArgumentException("Missing courseId")
                    val body = call.receive<Lesson>()
                    call.respond(HttpStatusCode.Created, lessonRepo.create(body.copy(courseId = courseId)))
                }

                put("/{lessonId}") {
                    val lessonId = call.parameters["lessonId"]
                        ?: throw IllegalArgumentException("Missing lessonId")
                    val body = call.receive<Lesson>()
                    val updated = lessonRepo.update(body.copy(id = lessonId))
                    if (updated) call.respond(HttpStatusCode.OK, body)
                    else throw NoSuchElementException("Lesson $lessonId not found")
                }
            }

            route("/api/v1/progress") {

                get("/{courseId}") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val courseId = call.parameters["courseId"]
                        ?: throw IllegalArgumentException("Missing courseId")
                    call.respond(progressRepo.getForUserAndCourse(principal.uid, courseId))
                }

                get("/{courseId}/summary") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val courseId = call.parameters["courseId"]
                        ?: throw IllegalArgumentException("Missing courseId")
                    val course = courseRepo.getById(courseId)
                        ?: throw NoSuchElementException("Course $courseId not found")
                    val allProgress = progressRepo.getForUserAndCourse(principal.uid, courseId)
                    val lessons = lessonRepo.getByCourseId(courseId)
                    val completed = allProgress.filter { it.completed }.map { it.lessonId }.distinct().size
                    val total = course.lessons.size
                    val totalMinutes = lessons.sumOf { it.durationMinutes }
                    call.respond(
                        CourseProgress(
                            courseId = courseId,
                            totalLessons = total,
                            completedLessons = completed,
                            remainingLessons = (total - completed).coerceAtLeast(0),
                            totalMinutes = totalMinutes,
                            progressPercent = if (total == 0) 0f else completed.toFloat() / total,
                        )
                    )
                }

                post {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val body = call.receive<LessonProgress>()
                    val progress = body.copy(userId = principal.uid)
                    progressRepo.upsert(progress)
                    call.respond(HttpStatusCode.OK, progress)
                }

                post("/{lessonId}/complete") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val lessonId = call.parameters["lessonId"]
                        ?: throw IllegalArgumentException("Missing lessonId")

                    val lesson = lessonRepo.getById(lessonId)
                    val courseId = lesson?.courseId

                    if (progressRepo.markCompleted(principal.uid, lessonId, courseId)) {
                        userRepo.addXp(principal.uid, 10)

                        val totalCompleted = progressRepo.countCompletedForUser(principal.uid)
                        if (totalCompleted >= 1) achievementRepo.unlock(principal.uid, "first_lesson")

                        val activeCourses = progressRepo.getAllForUser(principal.uid)
                            .map { it.courseId }.distinct().size
                        if (activeCourses >= 4) achievementRepo.unlock(principal.uid, "four_courses")

                        val user = userRepo.findById(principal.uid)
                        if ((user?.level ?: 0) >= 10) achievementRepo.unlock(principal.uid, "level_10")
                        
                        if (courseId != null) {
                            val course = courseRepo.getById(courseId)
                            if (course != null) {
                                val allProgress = progressRepo.getForUserAndCourse(principal.uid, courseId)
                                val completedInCourse = allProgress.filter { it.completed }.map { it.lessonId }.distinct().size
                                if (completedInCourse >= course.lessons.size && course.lessons.isNotEmpty()) {
                                    achievementRepo.unlock(principal.uid, "course_done")
                                }
                            }
                        }

                        call.respond(HttpStatusCode.OK, mapOf("completed" to true, "new_xp" to true))
                    } else {
                        call.respond(HttpStatusCode.OK, mapOf("completed" to true, "new_xp" to false))
                    }
                }
            }
        }
    }
}




