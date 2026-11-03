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
import com.retroraja.wordnoid.audio.Voice
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
    data class Story(val land: Int, val part: String, val then: Screen) : Screen
}

@Composable
fun WordnoidApp() {
    val context = LocalContext.current
    val activity = context as Activity
    val progress = remember { Progress(context) }
    val sounds = remember { SoundFx(context).also { Fx.sounds = it; it.voice = Voice(context) } }
    val music = remember { Music() }
    val haptics = remember { Haptics(context) }
    DisposableEffect(Unit) { onDispose { sounds.release(); sounds.voice?.release(); music.release() } }
    LaunchedEffect(progress.voice, progress.sound) { sounds.voice?.enabled = progress.voice && progress.sound }
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
    // Levels 1-3: the player arrives in town straight from the level (coins and bricks fly in, then build).
    var arrival by remember { mutableStateOf<TownArrival?>(null) }
    // Which Daily Gifts tab to open (the mini quiz returns to Challenges).
    var dailyTab by remember { mutableIntStateOf(0) }

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
    /** A story moment the first time only (the land prize is given inside). */
    fun storyOr(land: Int, part: String, then: Screen): Screen = if (!progress.seen(Story.seenKey(land, part))) Screen.Story(land, part, then) else then
    /** The first level of a new land starts with that land's story intro. */
    fun startLevel(n: Int): Screen = if ((n - 1) % 10 == 0) storyOr((n - 1) / 10, "intro", Screen.Start(n)) else Screen.Start(n)
    fun levelScreen(n: Int): Screen = Screen.World(((n - 1) / 10).coerceIn(0, WORLDS.size - 1))

    // 3D scenes live behind only these screens; everywhere else the 3D layer is just for effects.
    LaunchedEffect(screen) {
        if (screen != Screen.Intro && screen != Screen.FirstTime && screen != Screen.Town && screen !is Screen.Quiz && screen != Screen.Map && screen != Screen.Daily && !(screen is Screen.Start && Fx.ok)) Fx.backdrop(null)
    }

    // Back on the home screen asks before leaving.
    var leaveAsk by remember { mutableStateOf(false) }
    BackHandler(enabled = screen == Screen.Town) { leaveAsk = !leaveAsk }

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
                progress.markSeen("welcome_build3")
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                // New players go straight into the Level 1 demo.
                screen = if (progress.unlocked <= 1 && dict != null) storyOr(0, "intro", Screen.Play(1, ++attempts, emptyList())) else Screen.Town
            }
            Screen.Town -> TownScreen(
                ready = dict != null, progress = progress, sounds = sounds,
                onPlay = { val n = progress.unlocked.coerceAtMost(Levels.all.size); screen = startLevel(n) },
                onMap = { screen = Screen.Map },
                onShop = { go(Screen.Shop) },
                onBoosters = { go(Screen.Boosters) },
                onWordbook = { go(Screen.Wordbook) },
                onDaily = { tab -> dailyTab = tab; go(Screen.Daily) },
                onSettings = { go(Screen.Settings) },
                onQuit = { leaveAsk = true },
                arrival = arrival,
                onArrivalNext = { n -> arrival = null; screen = if (Levels.get(n) != null) startLevel(n) else Screen.Town },
                onLandComplete = { d -> screen = storyOr(d, "complete", Screen.Town) },
            )
            Screen.Map -> LevelMapScreen(progress, sounds, onTown = { screen = Screen.Town }, onWorld = { screen = Screen.World(it) })
            is Screen.World, is Screen.Start -> {
                val w = if (s is Screen.World) s.world else ((s as Screen.Start).level - 1) / 10
                // In 3D the level start is its own full screen (floating tiles behind a big card).
                if (s is Screen.World || !Fx.ok) WorldScreen(
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
            Screen.Daily -> DailyGiftsScreen(progress, sounds, startTab = dailyTab, onTab = { dailyTab = it }, onBack = { screen = Screen.Town }, onMiniQuiz = { dailyTab = 2; screen = Screen.Quiz(null, 0) })
            Screen.Settings -> SettingsScreen(progress, sounds, onBack = { screen = Screen.Town }, onHowToPlay = { screen = Screen.HowTo })
            Screen.HowTo -> HowToPlayScreen(progress, onBack = { screen = Screen.Settings })
            is Screen.Quiz -> QuizScreen(
                s.world, s.slot, progress, dict, sounds,
                onClose = { screen = if (s.world != null) Screen.World(s.world) else Screen.Daily },
                onFinished = { right -> if (s.world == null && right >= 2) Daily.report(progress, Daily.Kind.QUIZ) },
            )
            is Screen.Story -> StoryScreen(progress, sounds, s.land, s.part, onDone = { screen = s.then })
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
                            screen = if (level.boss) storyOr((s.level - 1) / 10, "boss", Screen.Town) else Screen.Town
                            if (level.boss) askReview(activity, progress)
                        },
                        onRestart = { screen = Screen.Start(s.level) },
                        onNext = { n ->
                            // After the 4th and 8th level of a world, it's Quiz Time.
                            val inWorld = (s.level - 1) % 10
                            val w = (s.level - 1) / 10
                            val to = when {
                                inWorld == 3 && progress.quizScore(w, 0) < 0 -> Screen.Quiz(w, 0)
                                inWorld == 7 && progress.quizScore(w, 1) < 0 -> Screen.Quiz(w, 1)
                                Levels.get(n) != null -> startLevel(n)
                                else -> Screen.Town
                            }
                            screen = if (level.boss) storyOr(w, "boss", to) else to
                        },
                        onAutoTown = { coins, bricks -> arrival = TownArrival(s.level, coins, bricks); screen = Screen.Town },
                    )
                }
            }
        }
        if (leaveAsk && screen == Screen.Town) LeaveDialog(onYes = { activity.finish() }, onNo = { leaveAsk = false })
    }

}
