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
                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    }
                }

                post("/me") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val body = call.receive<User>()
                    val user = body.copy(uid = principal.uid, email = principal.email ?: body.email)
                    userRepo.upsert(user)
                    call.respond(HttpStatusCode.OK, user)
                }
            }

            route("/api/v1/courses") {
                get {
                    val courses = courseRepo.getAll(publishedOnly = true)
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
                    val created = courseRepo.create(course)
                    call.respond(HttpStatusCode.Created, created)
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
                    val lessons = lessonRepo.getByCourseId(courseId)
                    call.respond(lessons)
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
                    val lesson = body.copy(courseId = courseId)
                    val created = lessonRepo.create(lesson)
                    call.respond(HttpStatusCode.Created, created)
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
                    val progress = progressRepo.getForUserAndCourse(principal.uid, courseId)
                    call.respond(progress)
                }

                get("/{courseId}/summary") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val courseId = call.parameters["courseId"]
                        ?: throw IllegalArgumentException("Missing courseId")

                    val course = courseRepo.getById(courseId)
                        ?: throw NoSuchElementException("Course $courseId not found")
                    val allProgress = progressRepo.getForUserAndCourse(principal.uid, courseId)
                    val completed = allProgress.count { it.completed }
                    val total = course.lessons.size

                    val summary = CourseProgress(
                        courseId = courseId,
                        totalLessons = total,
                        completedLessons = completed,
                        progressPercent = if (total == 0) 0f else completed.toFloat() / total,
                    )
                    call.respond(summary)
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
                    call.respond(HttpStatusCode.OK, mapOf("completed" to true))
                }
            }
        }
    }
}
