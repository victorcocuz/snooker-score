package com.example.snookerscore.domain

import com.example.snookerscore.database.DatabaseFrameScore
import timber.log.Timber

enum class MatchAction {
    CANCEL_MATCH, END_FRAME, FRAME_ENDED, END_MATCH, MATCH_ENDED, CONTINUE_MATCH, NO_ACTION, START_NEW_MATCH
}

sealed class CurrentScore(
    var frameId: Int,
    var framePoints: Int,
    var matchPoints: Int,
    var successShots: Int,
    var missedShots: Int,
    var fouls: Int,
    var highestBreak: Int
) {
    object PlayerA : CurrentScore(0, 0, 0, 0, 0, 0, 0)
    object PlayerB : CurrentScore(0, 0, 0, 0, 0, 0, 0)

    fun initPlayer(
        frameId: Int,
        framePoints: Int,
        matchPoints: Int,
        successShots: Int,
        missedShots: Int,
        fouls: Int,
        highestBreak: Int
    ) {
        this.frameId = frameId
        this.framePoints = framePoints
        this.matchPoints = matchPoints
        this.successShots = successShots
        this.missedShots = missedShots
        this.fouls = fouls
        this.highestBreak = highestBreak
    }

    fun getFirst() = PlayerA
    fun getSecond() = PlayerB

    fun getOther() = when (this) {
        PlayerA -> PlayerB
        PlayerB -> PlayerA
    }

    fun getPlayerAsInt(): Int = when (this) {
        PlayerA -> 0
        PlayerB -> 1
    }

    fun getPlayerFromInt(player: Int) = if (player == 0) PlayerA else PlayerB

    fun getWinner() = if (this.framePoints > this.getOther().framePoints) this else this.getOther()

    fun isFrameEqual() = this.framePoints == this.getOther().framePoints

    fun isMatchEqual() = this.matchPoints == this.getOther().matchPoints

    fun addFramePoints(points: Int) {
        this.framePoints += points
    }

    fun addSuccessShots(pol: Int) {
        this.successShots += pol
    }

    fun addMissedShots(pol: Int) {
        this.missedShots += pol
    }

    fun addFouls(pol: Int) {
        this.fouls += pol
    }

    fun addFrameCount() {
        Timber.e("has been triggered")
        this.frameId += 1
    }

    fun addMatchPoint() {
        this.matchPoints += 1
    }

    fun findMaxBreak(frameStack: MutableList<Break>) {
        var highestBreak = 0
        frameStack.forEach { crtBreak ->
            if (this.getPlayerAsInt() == crtBreak.player && crtBreak.breakSize > highestBreak) {
                highestBreak = crtBreak.breakSize
            }
        }
        this.highestBreak = highestBreak
    }

    fun resetFrameScore() {
        this.framePoints = 0
        this.highestBreak = 0
        this.missedShots = 0
        this.successShots = 0
    }

    fun resetMatchScore() {
        this.matchPoints = 0
        this.frameId = 0
    }
}

fun CurrentScore.asDatabaseFrameScore(): DatabaseFrameScore {
    return DatabaseFrameScore(
        frameCount = this.frameId,
        playerId = when (this) {
            CurrentScore.PlayerA -> 0
            CurrentScore.PlayerB -> 1
        },
        framePoints = this.framePoints,
        matchPoints = this.matchPoints,
        successShots = this.successShots,
        missedShots = this.missedShots,
        fouls = this.fouls,
        highestBreak = this.highestBreak
    )
}

data class FrameScore(
    val frameCount: Int,
    val playerId: Int,
    val framePoints: Int,
    val matchPoints: Int,
    val successShots: Int,
    val missedShots: Int,
    val fouls: Int,
    val highestBreak: Int
)