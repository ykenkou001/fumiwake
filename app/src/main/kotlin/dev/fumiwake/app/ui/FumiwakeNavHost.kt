package dev.fumiwake.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.fumiwake.app.AppState

/** Route templates and builders for the 3-screen MVP (docs/IMPLEMENTATION_PLAN.md §5). */
object Routes {
    const val CONCEPT_MAP = "conceptMap"
    const val EXERCISE_LIST = "exerciseList/{conceptId}"
    const val EXERCISE = "exercise/{exerciseId}"

    fun exerciseList(conceptId: String) = "exerciseList/$conceptId"
    fun exercise(exerciseId: String) = "exercise/$exerciseId"
}

@Composable
fun FumiwakeNavHost(navController: NavHostController, appState: AppState) {
    NavHost(navController = navController, startDestination = Routes.CONCEPT_MAP) {
        composable(Routes.CONCEPT_MAP) {
            ConceptMapScreen(
                appState = appState,
                onConceptClick = { conceptId -> navController.navigate(Routes.exerciseList(conceptId)) },
            )
        }
        composable(
            Routes.EXERCISE_LIST,
            arguments = listOf(navArgument("conceptId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val conceptId = backStackEntry.arguments?.getString("conceptId")
            if (conceptId != null) {
                ExerciseListScreen(
                    appState = appState,
                    conceptId = conceptId,
                    onExerciseClick = { exerciseId -> navController.navigate(Routes.exercise(exerciseId)) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(
            Routes.EXERCISE,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId")
            if (exerciseId != null) {
                ExerciseScreen(
                    appState = appState,
                    exerciseId = exerciseId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
