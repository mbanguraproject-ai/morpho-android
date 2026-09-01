package cc.devbangs.morpho.core

sealed class Dest(val route: String) {
    data object Home : Dest("home")
    data object Search : Dest("search")
    data object Settings : Dest("settings")
    data object Plus : Dest("plus")
    data object Categories : Dest("categories")
    data object Files : Dest("files")
    data object Stats : Dest("stats")
    data object AddTools : Dest("addtools")
    data object Arrange : Dest("arrange")
    data class Category(val id: String) : Dest("category/$id") {
        companion object { const val route = "category/{id}"; const val ARG = "id" }
    }
    data class Tool(val id: String) : Dest("tool/$id") {
        companion object { const val route = "tool/{id}"; const val ARG = "id" }
    }
}
