package app.eob.me.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Language : Screen("language")
    data object Intro : Screen("intro")
    data object AuthChoice : Screen("auth_choice")
    data object Auth : Screen("auth")
    data object MainHub : Screen("main_hub")
}
