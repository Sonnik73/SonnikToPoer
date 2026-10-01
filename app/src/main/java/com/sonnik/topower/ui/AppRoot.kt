package com.sonnik.topower.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonnik.topower.R
import com.sonnik.topower.data.Repository
import com.sonnik.topower.ui.common.LocalOpenSettings
import com.sonnik.topower.ui.exercises.ExercisesScreen
import com.sonnik.topower.ui.inventory.InventoryScreen
import com.sonnik.topower.ui.history.HistoryScreen
import com.sonnik.topower.ui.settings.SettingsScreen
import com.sonnik.topower.ui.stats.ExerciseStatsScreen
import com.sonnik.topower.ui.stats.StatsScreen
import com.sonnik.topower.ui.templates.TemplateEditScreen
import com.sonnik.topower.ui.workout.WorkoutHomeScreen
import com.sonnik.topower.ui.workout.WorkoutScreen

val LocalRepository = staticCompositionLocalOf<Repository> { error("Repository is not provided") }

object Routes {
    const val WORKOUT_HOME = "workout_home"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val EXERCISES = "exercises"
    const val INVENTORY = "inventory"
    const val SETTINGS = "settings"
    const val WORKOUT = "workout/{id}"
    const val TEMPLATE = "template/{id}"
    const val EXERCISE_STATS = "exercise_stats/{id}"

    fun workout(id: String) = "workout/$id"
    fun template(id: String) = "template/$id"
    fun exerciseStats(id: String) = "exercise_stats/$id"
}

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Workout(Routes.WORKOUT_HOME, R.string.nav_workout, Icons.Filled.PlayArrow),
    History(Routes.HISTORY, R.string.nav_history, Icons.Filled.DateRange),
    Stats(Routes.STATS, R.string.nav_stats, Icons.Filled.Star),
    Exercises(Routes.EXERCISES, R.string.nav_exercises, Icons.AutoMirrored.Filled.List),
    Inventory(Routes.INVENTORY, R.string.nav_inventory, Icons.Filled.Build),
}

private fun tabFor(route: String?): Tab? = when {
    route == null -> null
    route.startsWith("workout") || route.startsWith("template") -> Tab.Workout
    route.startsWith("exercise_stats") -> Tab.Stats
    else -> Tab.entries.firstOrNull { it.route == route }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentTab = tabFor(backStack?.destination?.route)

    val openSettings: () -> Unit = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } }

    CompositionLocalProvider(LocalOpenSettings provides openSettings) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { nav.openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.WORKOUT_HOME,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.WORKOUT_HOME) {
                    WorkoutHomeScreen(
                        openWorkout = { nav.navigate(Routes.workout(it)) },
                        openTemplate = { nav.navigate(Routes.template(it)) },
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(openWorkout = { nav.navigate(Routes.workout(it)) })
                }
                composable(Routes.STATS) {
                    StatsScreen(openExercise = { nav.navigate(Routes.exerciseStats(it)) })
                }
                composable(Routes.EXERCISES) {
                    ExercisesScreen(openStats = { nav.navigate(Routes.exerciseStats(it)) })
                }
                composable(Routes.INVENTORY) {
                    InventoryScreen()
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { nav.popBackStack() })
                }
                composable(Routes.WORKOUT) { entry ->
                    WorkoutScreen(
                        workoutId = entry.arguments?.getString("id").orEmpty(),
                        onBack = { nav.popBackStack() },
                        openStats = { nav.navigate(Routes.exerciseStats(it)) },
                    )
                }
                composable(Routes.TEMPLATE) { entry ->
                    TemplateEditScreen(
                        templateId = entry.arguments?.getString("id").orEmpty(),
                        onBack = { nav.popBackStack() },
                        openWorkout = { id ->
                            nav.navigate(Routes.workout(id)) {
                                popUpTo(Routes.WORKOUT_HOME)
                            }
                        },
                    )
                }
                composable(Routes.EXERCISE_STATS) { entry ->
                    ExerciseStatsScreen(
                        exerciseId = entry.arguments?.getString("id").orEmpty(),
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
