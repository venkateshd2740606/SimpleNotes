package com.simplenotes.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.simplenotes.ads.AdManager
import com.simplenotes.analytics.AnalyticsManager
import com.simplenotes.domain.model.UserPreferences
import com.simplenotes.presentation.ui.screens.consent.ConsentScreen
import com.simplenotes.presentation.ui.screens.home.HomeScreen
import com.simplenotes.presentation.ui.screens.note.NoteEditorScreen
import com.simplenotes.presentation.ui.screens.onboarding.OnboardingScreen
import com.simplenotes.presentation.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Consent : Screen("consent")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object NoteEditor : Screen("note/{noteId}") {
        fun route(noteId: Long): String = "note/$noteId"
        fun newNote(): String = "note/0"
    }
    data object Settings : Screen("settings")
}

@Composable
fun SimpleNotesNavHost(
    navController: NavHostController,
    adManager: AdManager,
    analyticsManager: AnalyticsManager,
    preferences: UserPreferences,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current

    DisposableEffect(navBackStackEntry?.destination?.route) {
        navBackStackEntry?.destination?.route?.let { analyticsManager.logScreenView(it) }
        onDispose { }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Consent.route) {
            ConsentScreen(onComplete = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Consent.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToNoteEditor = { id -> navController.navigate(Screen.NoteEditor.route(id)) },
                onNavigateToNewNote = { navController.navigate(Screen.NoteEditor.newNote()) },
                adManager = adManager,
                adsEnabled = preferences.adsEnabled
            )
        }
        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            NoteEditorScreen(onNavigateBack = { navController.navigateUp() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPrivacy = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://simplenotes.app/privacy")))
                }
            )
        }
    }
}
