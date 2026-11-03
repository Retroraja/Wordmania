package com.retroraja.wordnoid.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.Music
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Daily
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Reminders
import com.retroraja.wordnoid.data.Words

private sealed interface Screen {
    data object Intro : Screen
    data object FirstTime : Screen
    data object Town : Screen
    data object Map : Screen
    data class World(val world: Int) : Screen
    data object Shop : Screen
    data object Wordbook : Screen
    data object Boosters : Screen
    data object Daily : Screen
    data object Settings : Screen
    data object HowTo : Screen
    data class Quiz(val world: Int?, val slot: Int) : Screen
    data class Start(val level: Int) : Screen
    data class Play(val level: Int, val attempt: Int, val boosters: List<Booster>) : Screen
}

@Composable
fun WordnoidApp() {
    val context = LocalContext.current
    val activity = context as Activity
    val progress = remember { Progress(context) }
    val sounds = remember { SoundFx(context).also { Fx.sounds = it } }
    val music = remember { Music() }
    val haptics = remember { Haptics(context) }
    DisposableEffect(Unit) { onDispose { sounds.release(); music.release() } }
    LaunchedEffect(progress.sound) { sounds.enabled = progress.sound }
    LaunchedEffect(progress.music) { music.enabled = progress.music }
    LaunchedEffect(progress.vibration) { haptics.enabled = progress.vibration }
    LaunchedEffect(Unit) { Reminders.schedule(context) }

    // Pause the music when the app goes to the background; App Open ad when coming back after a while.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) { music.pauseAll(); Ads.onPause() }
            if (e == Lifecycle.Event.ON_RESUME) { if (progress.music) music.resume(); Ads.onResume(activity) }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // Ask for notification permission once (Android 13+), after the first-time welcome.
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    var dict by remember { mutableStateOf<Dictionary?>(null) }
    LaunchedEffect(Unit) { dict = Words.load(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.Intro) }
    var back by remember { mutableStateOf<Screen>(Screen.Town) }
    var attempts by remember { mutableIntStateOf(0) }

    LaunchedEffect(screen) {
        music.play(
            when (screen) {
                is Screen.Play -> Music.Piece.LEVEL
                Screen.Town, Screen.Intro, Screen.FirstTime -> Music.Piece.HOME
                else -> Music.Piece.MAP
            }
        )
    }

    // Ads start only after the intro and the birth-year question (child-safe settings), a moment after Town shows.
    val inMenus = screen != Screen.Intro && screen != Screen.FirstTime
    LaunchedEffect(inMenus) {
        if (inMenus) { kotlinx.coroutines.delay(2500); try { Ads.start(activity, progress) } catch (_: Throwable) {} }
    }

    fun go(s: Screen) { back = screen; screen = s }
    fun levelScreen(n: Int): Screen = Screen.World(((n - 1) / 10).coerceIn(0, WORLDS.size - 1))

    BackHandler(enabled = screen != Screen.Town) {
        screen = when (val s = screen) {
            is Screen.Play, is Screen.Start -> levelScreen((s as? Screen.Play)?.level ?: (s as Screen.Start).level)
            is Screen.World -> Screen.Map
            is Screen.Quiz -> if (s.world != null) Screen.World(s.world) else Screen.Daily
            Screen.HowTo -> Screen.Settings
            Screen.Intro, Screen.FirstTime -> screen
            else -> Screen.Town
        }
    }

    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = BodyFont)) {
        when (val s = screen) {
            Screen.Intro -> StudioIntro(sounds) { screen = if (progress.seen("first_time")) Screen.Town else Screen.FirstTime }
            Screen.FirstTime -> FirstTimeScreen(progress) {
                progress.markSeen("first_time")
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                screen = Screen.Town
            }
            Screen.Town -> TownScreen(
                ready = dict != null, progress = progress, sounds = sounds,
                onPlay = { val n = progress.unlocked.coerceAtMost(Levels.all.size); screen = Screen.Start(n) },
                onMap = { screen = Screen.Map },
                onShop = { go(Screen.Shop) },
                onBoosters = { go(Screen.Boosters) },
                onWordbook = { go(Screen.Wordbook) },
                onDaily = { go(Screen.Daily) },
                onSettings = { go(Screen.Settings) },
            )
            Screen.Map -> LevelMapScreen(progress, sounds, onTown = { screen = Screen.Town }, onWorld = { screen = Screen.World(it) })
            is Screen.World, is Screen.Start -> {
                val w = if (s is Screen.World) s.world else ((s as Screen.Start).level - 1) / 10
                WorldScreen(
                    w, progress, sounds,
                    onBack = { screen = Screen.Map },
                    onTown = { screen = Screen.Town },
                    onLevel = { screen = Screen.Start(it) },
                    onQuiz = { slot -> screen = Screen.Quiz(w, slot) },
                )
                if (s is Screen.Start) {
                    val level = Levels.get(s.level)
                    if (level != null) LevelStartCard(
                        level, progress, sounds,
                        onPlay = { boosters -> screen = Screen.Play(s.level, ++attempts, boosters) },
                        onClose = { screen = Screen.World(w) },
                    )
                }
            }
            Screen.Shop -> ShopScreen(progress, sounds, onBack = { screen = back })
            Screen.Wordbook -> WordbookScreen(progress, onBack = { screen = back })
            Screen.Boosters -> BoostersGuide(progress, onBack = { screen = back })
            Screen.Daily -> DailyGiftsScreen(progress, sounds, onBack = { screen = Screen.Town }, onMiniQuiz = { screen = Screen.Quiz(null, 0) })
            Screen.Settings -> SettingsScreen(progress, sounds, onBack = { screen = Screen.Town }, onHowToPlay = { screen = Screen.HowTo })
            Screen.HowTo -> HowToPlayScreen(progress, onBack = { screen = Screen.Settings })
            is Screen.Quiz -> QuizScreen(
                s.world, s.slot, progress, dict, sounds,
                onClose = { screen = if (s.world != null) Screen.World(s.world) else Screen.Daily },
                onFinished = { right -> if (s.world == null && right >= 2) Daily.report(progress, Daily.Kind.QUIZ) },
            )
            is Screen.Play -> {
                val level = Levels.get(s.level)
                val d = dict
                if (level == null || d == null) screen = Screen.Town
                else key(s.level, s.attempt) {
                    GameScreen(
                        level = level, dict = d, progress = progress, sounds = sounds, haptics = haptics,
                        startBoosters = s.boosters,
                        onMap = { screen = levelScreen(s.level) },
                        onTown = {
                            screen = Screen.Town
                            if (level.boss) askReview(activity, progress)
                        },
                        onRestart = { screen = Screen.Start(s.level) },
                        onNext = { n ->
                            // After the 4th and 8th level of a world, it's Quiz Time.
                            val inWorld = (s.level - 1) % 10
                            val w = (s.level - 1) / 10
                            screen = when {
                                inWorld == 3 && progress.quizScore(w, 0) < 0 -> Screen.Quiz(w, 0)
                                inWorld == 7 && progress.quizScore(w, 1) < 0 -> Screen.Quiz(w, 1)
                                Levels.get(n) != null -> Screen.Start(n)
                                else -> Screen.Town
                            }
                        },
                    )
                }
            }
        }
    }
}
