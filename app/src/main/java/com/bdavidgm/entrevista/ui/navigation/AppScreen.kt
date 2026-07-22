package com.bdavidgm.entrevista.ui.navigation

internal sealed interface AppScreen {
    data object Saved : AppScreen
    data object List : AppScreen
    data class Detail(val questionId: Int) : AppScreen
}
