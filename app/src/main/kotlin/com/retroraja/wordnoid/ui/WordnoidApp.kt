package com.retroraja.wordnoid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.retroraja.wordnoid.audio.Music
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Words

private sealed interface Screen {
    data object Home : Screen
    data object Map : Screen
    data object Shop : Screen
    data object Wordbook : Screen
    data class Start(val level: Int) : Screen
    data class Play(val level: Int, val attempt: Int, val boosters: List<Booster>) : Screen
}

@Composable
fun WordnoidApp() {
    val context = LocalContext.current
    val progress = remember { Progress(context) }
    val sounds = remember { SoundFx(context) }
    val music = remember { Music() }
    val haptics = remember { Haptics(context) }
    DisposableEffect(Unit) { onDispose { sounds.release(); music.release() } }
    LaunchedEffect(progress.sound) { sounds.enabled = progress.sound }
    LaunchedEffect(progress.music) { music.enabled = progress.music }
    LaunchedEffect(progress.vibration) { haptics.enabled = progress.vibration }

    // Pause the music when the app goes to the background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) music.pauseAll()
            if (e == Lifecycle.Event.ON_RESUME && progress.music) music.resume()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    var dict by remember { mutableStateOf<Dictionary?>(null) }
    LaunchedEffect(Unit) { dict = Words.load(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var back by remember { mutableStateOf<Screen>(Screen.Home) }
    var attempts by remember { mutableIntStateOf(0) }

    LaunchedEffect(screen) {
        music.play(
            when (screen) {
                is Screen.Play -> Music.Piece.LEVEL
                Screen.Home -> Music.Piece.HOME
                else -> Music.Piece.MAP
            }
        )
    }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            is Screen.Play, is Screen.Start -> Screen.Map
            Screen.Shop, Screen.Wordbook -> back
            else -> Screen.Home
        }
    }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            ready = dict != null, progress = progress, sounds = sounds,
            onPlay = { screen = Screen.Map },
            onShop = { back = Screen.Home; screen = Screen.Shop },
            onWordbook = { back = Screen.Home; screen = Screen.Wordbook },
        )
        Screen.Map, is Screen.Start -> {
            MapScreen(
                progress, sounds,
                onBack = { screen = Screen.Home },
                onLevel = { screen = Screen.Start(it) },
                onShop = { back = Screen.Map; screen = Screen.Shop },
            )
            if (s is Screen.Start) {
                val level = Levels.get(s.level)
                if (level != null) LevelStartCard(
                    level, progress, sounds,
                    onPlay = { boosters -> screen = Screen.Play(s.level, ++attempts, boosters) },
                    onClose = { screen = Screen.Map },
                )
            }
        }
        Screen.Shop -> ShopScreen(progress, sounds, onBack = { screen = back })
        Screen.Wordbook -> WordbookScreen(progress, onBack = { screen = back })
        is Screen.Play -> {
            val level = Levels.get(s.level)
            val d = dict
            if (level == null || d == null) screen = Screen.Map
            else key(s.level, s.attempt) {
                GameScreen(
                    level = level, dict = d, progress = progress, sounds = sounds, haptics = haptics,
                    startBoosters = s.boosters,
                    onMap = { screen = Screen.Map },
                    onRestart = { screen = Screen.Start(s.level) },
                    onNext = { n -> screen = if (Levels.get(n) != null) Screen.Start(n) else Screen.Map },
                )
            }
        }
    }
}
