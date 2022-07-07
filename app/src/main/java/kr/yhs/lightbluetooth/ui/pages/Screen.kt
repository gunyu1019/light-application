package kr.yhs.lightbluetooth.ui.pages


sealed class Screen(
    val route: String
) {
    object ControlPage: Screen("controlPage")
    object ConnectionPage: Screen("connectionPage")
    object ConnectionList: Screen("connectionList")
}