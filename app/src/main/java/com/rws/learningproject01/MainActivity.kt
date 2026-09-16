package com.rws.learningproject01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rws.learningproject01.navigation.Routes
import com.rws.learningproject01.ui.gallery.GalleryScreen
import com.rws.learningproject01.ui.editor.EditorScreen
import com.rws.learningproject01.ui.theme.LearningProject01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LearningProject01Theme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Routes.GALLERY,
                    ) {
                        composable(Routes.GALLERY) {
                            GalleryScreen(
                                onOpenProject = { projectId ->
                                    navController.navigate(Routes.editor(projectId))
                                },
                            )
                        }
                        composable(
                            route = Routes.EDITOR,
                            arguments = listOf(
                                navArgument("projectId") { type = NavType.StringType },
                            ),
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId")
                                ?: return@composable
                            EditorScreen(
                                projectId = projectId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
