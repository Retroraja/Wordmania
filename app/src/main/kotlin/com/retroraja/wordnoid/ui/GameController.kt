package com.retroraja.wordnoid.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.ClearStep
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.FoundWord
import com.retroraja.wordnoid.core.Game
import com.retroraja.wordnoid.core.GameStatus
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.MoveResult
import com.retroraja.wordnoid.core.Pos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Big text that pops up over the board, e.g. the word just made. */
data class Popup(val title: String, val subtitle: String, val key: Int, val big: Boolean = true)

/** A burst of particles. Centres are in tile units (column, row). */
data class Burst(val id: Int, val centres: List<Triple<Float, Float, Color>>)

/**
 * Holds what the game screen shows, and plays each move step by step with animations and sounds.
 * The rules themselves live in [Game] (core module).
 */
class GameController(
    val level: Level,
    dict: Dictionary,
    bonusMoves: Int,
    private val sounds: SoundFx,
    private val scope: CoroutineScope,
    private val haptic: (strong: Boolean) -> Unit,
) {
    val game = Game(level, dict, bonusMoves)

    var board by mutableStateOf(game.board); private set
    var moves by mutableIntStateOf(game.movesLeft); private set
    var score by mutableIntStateOf(0); private set
    var coins by mutableIntStateOf(0); private set
    val found = mutableStateListOf<FoundWord>()
    var selected by mutableStateOf<Pos?>(null); private set
    var flashing by mutableStateOf<Set<Long>>(emptySet()); private set
    var busy by mutableStateOf(false); private set
    var popup by mutableStateOf<Popup?>(null); private set
    var combo by mutableIntStateOf(0); private set
    val bursts = mutableStateListOf<Burst>()
    var shakeKey by mutableIntStateOf(0); private set
    var status by mutableStateOf(GameStatus.PLAYING); private set
    var hint by mutableStateOf<Pair<Pos, Pos>?>(null); private set
    var tutorial by mutableStateOf<Pair<Pos, Pos>?>(null)
    var lastAction by mutableLongStateOf(System.currentTimeMillis()); private set
    var swapsMade by mutableIntStateOf(0); private set
    /** Coins from this game already added to the wallet. */
    var coinsSaved by mutableIntStateOf(0)

    private var popupKey = 0
    private var burstId = 0
    private var popupJob: Job? = null

    fun tap(p: Pos) {
        if (busy || status != GameStatus.PLAYING) return
        lastAction = System.currentTimeMillis()
        hint = null
        val s = selected
        when {
            s == null -> selected = p
            s == p -> selected = null
            s.isNeighbour(p) -> trySwap(s, p)
            else -> selected = p
        }
    }

    fun drag(from: Pos, dRow: Int, dCol: Int) {
        val to = Pos(from.row + dRow, from.col + dCol)
        if (board.contains(to)) trySwap(from, to)
    }

    fun trySwap(a: Pos, b: Pos) {
        if (busy || !game.canSwap(a, b)) return
        busy = true
        selected = null
        hint = null
        tutorial = null
        swapsMade++
        val result = game.swap(a, b)
        scope.launch { play(result) }
    }

    fun showHint(): Boolean {
        if (busy || status != GameStatus.PLAYING) return false
        hint = game.hint()
        return hint != null
    }

    fun clearHint() { hint = null; lastAction = System.currentTimeMillis() }

    fun buyMoves(n: Int) {
        game.addMoves(n)
        moves = game.movesLeft
        status = game.status
        lastAction = System.currentTimeMillis()
        showPopup("+$n moves!", "Keep going!", big = false)
        sounds.play("ding")
    }

    fun showPopup(title: String, subtitle: String, big: Boolean = true, millis: Long = 1800) {
        val k = ++popupKey
        popup = Popup(title, subtitle, k, big)
        popupJob?.cancel()
        popupJob = scope.launch {
            delay(millis)
            if (popup?.key == k) popup = null
        }
    }

    fun removeBurst(id: Int) { bursts.removeAll { it.id == id } }

    private fun praise(step: ClearStep): String {
        val longest = step.words.maxOf { it.length }
        return when {
            step.combo >= 3 || longest >= 6 -> "WORDNOID!"
            step.combo == 2 || longest == 5 -> "Amazing!"
            longest == 4 || step.words.size >= 2 -> "Awesome!"
            step.words.any { it.common } -> "Great!"
            else -> "Nice find!"
        }
    }

    private suspend fun play(r: MoveResult) {
        var shownMoves = moves - 1
        moves = shownMoves
        sounds.play("clack")
        haptic(false)
        board = r.swappedBoard
        delay(230)

        for (step in r.steps) {
            combo = step.combo
            flashing = step.words.flatMap { w -> w.cells.map { board[it].id } }.toSet()
            val words = step.words.joinToString("  ") { it.word }
            val extras = buildList {
                add(praise(step))
                if (step.bonusMoves > 0) add("+${step.bonusMoves} moves")
            }.joinToString("  ·  ")
            showPopup(words, extras, millis = 2200)
            sounds.word(step.words.maxOf { it.length }, startNote = (step.combo - 1) * 2)
            if (step.combo >= 2) sounds.play("combo")
            haptic(true)
            if (step.combo >= 2 || step.words.any { it.length >= 5 }) shakeKey++
            delay(520)

            bursts += Burst(++burstId, step.words.flatMap { w ->
                w.cells.map { p -> Triple(p.col + 0.5f, p.row + 0.5f, letterColor(board[p].letter)) }
            })
            found += step.words
            score += step.points
            coins += step.coins
            sounds.play("coin")
            if (step.bonusMoves > 0) {
                shownMoves += step.bonusMoves
                moves = shownMoves
                sounds.play("ding")
            }
            flashing = emptySet()
            board = step.boardAfter
            delay(450)
        }
        combo = 0

        r.shuffledBoard?.let {
            showPopup("Shuffle!", "No words left, mixing the board", big = false)
            sounds.play("shuffle")
            delay(350)
            board = it
            delay(450)
        }

        moves = game.movesLeft
        if (r.status == GameStatus.WON) {
            delay(250)
            // Word Rain: every move left turns into points.
            val left = game.movesLeft
            if (left > 0) showPopup("Word Rain!", "+${Game.LEFTOVER_MOVE_POINTS} points for every move left", big = false)
            for (i in left downTo 1) {
                moves = i - 1
                score += Game.LEFTOVER_MOVE_POINTS
                sounds.play("tick")
                val rc = board.positions().let { it[(i * 7919) % it.size] }
                bursts += Burst(++burstId, listOf(Triple(rc.col + 0.5f, rc.row + 0.5f, letterColor(board[rc].letter))))
                delay(140)
            }
            score = game.score
            delay(350)
            sounds.play("win")
        } else if (r.status == GameStatus.LOST) {
            delay(200)
            sounds.play("lose")
        }
        status = r.status
        busy = false
        lastAction = System.currentTimeMillis()
    }
}
