package com.learnapp.plugins

import com.learnapp.data.repository.*
import com.learnapp.domain.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val userRepo = UserRepository()
private val courseRepo = CourseRepository()
private val lessonRepo = LessonRepository()
private val progressRepo = ProgressRepository()
private val achievementRepo = AchievementRepository()
private val statsRepo = UserStatsRepository()

fun Application.configureRouting() {
    routing {

        get("/health") {
            call.respond(mapOf("status" to "ok", "version" to "0.0.1"))
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
                    val body = call.receive<User>()
                    val user = body.copy(uid = principal.uid, email = principal.email ?: body.email)
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
                    val completed = allProgress.count { it.completed }
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

                    progressRepo.markCompleted(principal.uid, lessonId)
                    userRepo.addXp(principal.uid, 10)

                    val totalCompleted = progressRepo.countCompletedForUser(principal.uid)
                    if (totalCompleted >= 1) achievementRepo.unlock(principal.uid, "first_lesson")

                    val activeCourses = progressRepo.getAllForUser(principal.uid)
                        .map { it.courseId }.distinct().size
                    if (activeCourses >= 4) achievementRepo.unlock(principal.uid, "four_courses")

                    val user = userRepo.findById(principal.uid)
                    if ((user?.level ?: 0) >= 10) achievementRepo.unlock(principal.uid, "level_10")

                    call.respond(HttpStatusCode.OK, mapOf("completed" to true))
                }
            }
        }
    }
}

