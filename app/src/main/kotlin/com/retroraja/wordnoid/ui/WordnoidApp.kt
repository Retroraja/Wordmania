package com.retroraja.wordnoid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Words

private sealed interface Screen {
    data object Title : Screen
    data object Map : Screen
    data class Play(val level: Int, val attempt: Int) : Screen
}

@Composable
fun WordnoidApp() {
    val context = LocalContext.current
    val progress = remember { Progress(context) }
    val sounds = remember { SoundFx() }
    DisposableEffect(Unit) { onDispose { sounds.release() } }
    LaunchedEffect(progress.sound) { sounds.enabled = progress.sound }

    var dict by remember { mutableStateOf<Dictionary?>(null) }
    LaunchedEffect(Unit) { dict = Words.load(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.Title) }
    var attempts by remember { mutableStateOf(0) }

    when (val s = screen) {
        Screen.Title -> TitleScreen(
            ready = dict != null, progress = progress,
            onPlay = { screen = Screen.Map },
            onToggleSound = { progress.toggleSound() },
        )
        Screen.Map -> MapScreen(progress, onBack = { screen = Screen.Title }, onLevel = { screen = Screen.Play(it, ++attempts) })
        is Screen.Play -> {
            val level = Levels.get(s.level)
            val d = dict
            if (level == null || d == null) screen = Screen.Map
            else key(s.level, s.attempt) {
                GameScreen(
                    level = level, dict = d, progress = progress, sounds = sounds, attempt = s.attempt,
                    onMap = { screen = Screen.Map },
                    onRestart = { screen = Screen.Play(s.level, ++attempts) },
                    onNext = { screen = Screen.Play(it, ++attempts) },
                )
            }
        }
    }
}
