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
            courseRepo.clear()
            lessonRepo.clear()

            val mainCourses = listOf(
                Course(
                    title = "Веб-разработка для начинающих",
                    description = "Погрузитесь в мир веб-технологий. Вы изучите HTML, CSS и основы JavaScript, чтобы создавать свои первые сайты.",
                    category = "Programming",
                    authorId = "admin",
                    isPublished = true
                ),
                Course(
                    title = "Android Jetpack Compose",
                    description = "Современный подход к созданию пользовательских интерфейсов на Android. Изучите декларативный стиль разработки.",
                    category = "Android",
                    authorId = "admin",
                    isPublished = true
                ),
                Course(
                    title = "Основы Python",
                    description = "Идеальный курс для старта в программировании. Простой синтаксис и огромные возможности.",
                    category = "Programming",
                    authorId = "admin",
                    isPublished = true
                )
            )

            val extraCourses = listOf(
                "Продвинутый Backend на Kotlin", "Дизайн мобильных интерфейсов", "Машинное обучение: Введение",
                "SQL и базы данных", "Управление IT-проектами", "Тестирование ПО (QA)",
                "Разработка игр на Unity", "Кибербезопасность для всех", "Анализ данных с Pandas",
                "Swift: Разработка под iOS", "React.js: Современный Frontend", "Docker и Kubernetes"
            ).map { title ->
                Course(
                    title = title,
                    description = "Краткое описание курса по теме $title. Изучите основы и продвинутые техники.",
                    category = when {
                        title.contains("Android") || title.contains("iOS") || title.contains("мобильных") -> "Android"
                        title.contains("Backend") -> "Backend"
                        title.contains("SQL") -> "SQL"
                        title.contains("Frontend") || title.contains("React") -> "Frontend"
                        title.contains("Дизайн") -> "Design"
                        else -> "Programming"
                    },
                    authorId = "admin",
                    isPublished = true
                )
            }

            (mainCourses + extraCourses).forEach { course ->
                val created = courseRepo.create(course)
                
                val lessons = when (course.title) {
                    "Веб-разработка для начинающих" -> {
                        listOf(
                            Lesson(
                                courseId = created.id, 
                                title = "Введение в HTML", 
                                order = 1, 
                                type = LessonType.TEXT, 
                                durationMinutes = 10, 
                                markdownContent = """
                                    # Что такое HTML?
                                    
                                    HTML (HyperText Markup Language) — это стандартный язык разметки для документов, предназначенных для отображения в веб-браузере.
                                    
                                    ### Основные теги:
                                    - `<html>` - корень документа
                                    - `<head>` - метаданные
                                    - `<body>` - видимая часть страницы
                                    
                                    Попробуйте создать свой первый файл `index.html`!
                                """.trimIndent()
                            ),
                            Lesson(
                                courseId = created.id, 
                                title = "Стилизация с помощью CSS", 
                                order = 2, 
                                type = LessonType.VIDEO, 
                                durationMinutes = 15, 
                                videoUrl = "uploads/sample.mp4"
                            ),
                            Lesson(
                                courseId = created.id, 
                                title = "Основы JavaScript", 
                                order = 3, 
                                type = LessonType.TEXT, 
                                durationMinutes = 20, 
                                markdownContent = """
                                    # JavaScript: Делаем страницы живыми
                                    
                                    JavaScript — это мультипарадигменный язык программирования. С его помощью можно обрабатывать события, изменять HTML и CSS на лету.
                                    
                                    ```javascript
                                    console.log('Hello, Studli!');
                                    ```
                                    
                                    Изучайте переменные, циклы и функции!
                                """.trimIndent()
                            ),
                            Lesson(
                                courseId = created.id, 
                                title = "Работа с DOM", 
                                order = 4, 
                                type = LessonType.VIDEO, 
                                durationMinutes = 12, 
                                videoUrl = "https://www.youtube.com/watch?v=tY8vj6aI7zQ"
                            )
                        )
                    }
                    "Android Jetpack Compose" -> {
                        listOf(
                            Lesson(
                                courseId = created.id,
                                title = "Введение в декларативный UI",
                                order = 1,
                                type = LessonType.TEXT,
                                durationMinutes = 15,
                                markdownContent = """
                                    # Что такое Jetpack Compose?
                                    
                                    Jetpack Compose — это современный инструментарий для создания нативного пользовательского интерфейса для Android. Он упрощает и ускоряет разработку UI на Android с помощью декларативного подхода.
                                    
                                    ### Почему Compose?
                                    - **Меньше кода**: Описывайте только состояние вашего UI, и Compose позаботится об остальном.
                                    - **Декларативность**: UI автоматически обновляется при изменении состояния.
                                    - **Совместимость с Kotlin**: Полная поддержка всех возможностей языка Kotlin.
                                    
                                    В традиционном подходе (XML) мы работали императивно: `textView.setText("Hello")`. В Compose мы просто говорим: `Text("Hello")`.
                                """.trimIndent()
                            ),
                            Lesson(
                                courseId = created.id,
                                title = "Создание первого экрана",
                                order = 2,
                                type = LessonType.VIDEO,
                                durationMinutes = 25,
                                videoUrl = "https://www.youtube.com/watch?v=6rL4S85-2U0"
                            ),
                            Lesson(
                                courseId = created.id,
                                title = "Состояние (State) в Compose",
                                order = 3,
                                type = LessonType.TEXT,
                                durationMinutes = 20,
                                markdownContent = """
                                    # Управление состоянием
                                    
                                    Состояние в приложении — это любое значение, которое может меняться со временем. В Compose состояние определяет, что отображается на экране.
                                    
                                    ### Ключевые функции:
                                    - `remember`: Сохраняет значение в памяти во время рекомпозиции.
                                    - `mutableStateOf`: Создает наблюдаемое состояние, изменение которого триггерит обновление UI.
                                    
                                    Пример:
                                    ```kotlin
                                    val count = remember { mutableStateOf(0) }
                                    Button(onClick = { count.value++ }) {
                                        Text("Нажато ${'$'}{count.value} раз")
                                    }
                                    ```
                                    
                                    Это позволяет создавать динамические и интерактивные интерфейсы без лишнего бойлерплейта.
                                """.trimIndent()
                            ),
                            Lesson(
                                courseId = created.id,
                                title = "Списки и LazyColumn",
                                order = 4,
                                type = LessonType.VIDEO,
                                durationMinutes = 18,
                                videoUrl = "https://www.youtube.com/watch?v=1ANt65oeLpE"
                            ),
                            Lesson(
                                courseId = created.id,
                                title = "Навигация и архитектура",
                                order = 5,
                                type = LessonType.TEXT,
                                durationMinutes = 22,
                                markdownContent = """
                                    # Навигация в Compose
                                    
                                    Для перемещения между экранами используется библиотека `navigation-compose`. Основным компонентом является `NavHost`.
                                    
                                    ### Основные шаги:
                                    1. Создать `NavController`.
                                    2. Описать граф навигации с помощью `NavHost`.
                                    3. Использовать `navController.navigate("route")` для перехода.
                                    
                                    Compose также отлично работает с **ViewModel** и **Clean Architecture**, позволяя отделять бизнес-логику от представления.
                                """.trimIndent()
                            ),
                            Lesson(
                                courseId = created.id,
                                title = "Продвинутая анимация",
                                order = 6,
                                type = LessonType.VIDEO,
                                durationMinutes = 20,
                                videoUrl = "https://www.youtube.com/watch?v=MInX9W67jO4"
                            )
                        )
                    }
                    else -> {
                        listOf(
                            Lesson(
                                courseId = created.id, 
                                title = "Вводный урок", 
                                order = 1, 
                                type = LessonType.TEXT, 
                                durationMinutes = 10, 
                                markdownContent = "Добро пожаловать в курс \"${course.title}\"! В этом уроке мы обсудим план обучения и основные цели."
                            ),
                            Lesson(
                                courseId = created.id, 
                                title = "Обзор инструментов", 
                                order = 2, 
                                type = LessonType.VIDEO, 
                                durationMinutes = 15, 
                                videoUrl = "http://192.168.1.13:8080/uploads/sample.mp4"
                            )
                        )
                    }
                }

                lessons.forEach { lessonRepo.create(it) }
                courseRepo.update(created.copy(lessons = lessons.map { it.id }))
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "Database seeded successfully with variety of courses and YouTube links!"))
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
