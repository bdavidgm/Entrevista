package com.bdavidgm.entrevista.data

data class InterviewQuestion(
    val id: Int,
    val category: Category,
    val question: String,
    val answer: String
)

enum class Category(val displayName: String) {
    KOTLIN_BASICS("Kotlin básico"),
    KOTLIN_ADVANCED("Kotlin avanzado"),
    ANDROID_BASICS("Android básico"),
    ANDROID_LIFECYCLE("Ciclo de vida"),
    COMPOSE("Jetpack Compose"),
    ARCHITECTURE("Arquitectura"),
    JETPACK("Android Jetpack"),
    COROUTINES("Coroutines"),
    TESTING("Testing"),
    PERFORMANCE("Rendimiento"),
    SECURITY("Seguridad"),
    UI_UX("UI/UX"),
    BUILD_GRADLE("Build y Gradle"),
    ANDROID_API("API de Android"),
    RUTA_ANDROID("Ruta Android (roadmap.sh)"),
    ANDROID_TECH("Técnico Android"),
    GENERAL("General")
}
