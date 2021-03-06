package com.quickpoint.snookerboard.fragments.game

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.quickpoint.snookerboard.R
import com.quickpoint.snookerboard.domain.*
import com.quickpoint.snookerboard.domain.DomainBall.*
import com.quickpoint.snookerboard.domain.PotType.*
import com.quickpoint.snookerboard.repository.SnookerRepository
import com.quickpoint.snookerboard.utils.*
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    private val snookerRepository: SnookerRepository
) : AndroidViewModel(application) {

    // Observables
    private val _displayFrame = MutableLiveData<DomainFrame>()
    val displayFrame: LiveData<DomainFrame> = _displayFrame

    private val _displayScore = MutableLiveData<CurrentScore>()
    val displayScore: LiveData<CurrentScore> = _displayScore

    private val _eventMatchAction = MutableLiveData<Event<MatchAction>>()
    val eventMatchAction: LiveData<Event<MatchAction>> = _eventMatchAction

    private val _eventFrameUpdated = MutableLiveData<Event<Unit>>()
    val eventFrameUpdated: LiveData<Event<Unit>> = _eventFrameUpdated

    private val _isFrameUpdateInProgress = MutableLiveData(false)
    val isFrameUpdateInProgress: LiveData<Boolean> = _isFrameUpdateInProgress

    // Variables
    private var ballStack: MutableList<DomainBall> = mutableListOf()
    private var score: CurrentScore = CurrentScore.PlayerA
    private var frameStack: MutableList<DomainBreak> = mutableListOf()
    private val sharedPref: SharedPreferences = application.getSharedPref()
    private var frameCount = 1
    private var frameMax = 0

    // Match settings
    private var matchFrames = 0
    private var matchReds = 0
    private var matchFoul = 0
    private var matchFirst = 0

    suspend fun saveMatch() {
        if (score.isMatchInProgress()) {
            snookerRepository.saveCurrentFrame(displayFrame.value!!)
            sharedPref.edit().apply {
                getApplication<Application>().resources.apply {
                    putInt(getString(R.string.shared_pref_match_frames), matchFrames)
                    putInt(getString(R.string.shared_pref_match_reds), matchReds)
                    putInt(getString(R.string.shared_pref_match_foul), matchFoul)
                    putInt(getString(R.string.shared_pref_match_first), matchFirst)
                    putInt(getString(R.string.shared_pref_match_crt_player), score.getPlayerAsInt())
                    putInt(getString(R.string.shared_pref_match_crt_frame), frameCount)
                    apply()
                }
            }
        } else {
            sharedPref.setMatchInProgress(false)
        }
    }

    fun loadMatch(frame: DomainFrame?) = frame?.let {
        viewModelScope.launch {
            snookerRepository.deleteCurrentFrame(frameCount)
        }
        score = frame.frameScore.asCurrentScore() ?: score
        frameStack = frame.frameStack
        ballStack = frame.ballStack
        frameMax = frame.frameMax
        getSavedStateRules()
        updateFrameStatus()
    }

    fun cancelMatch() {
        resetMatch()
        sharedPref.setMatchInProgress(false)
    }

    fun startNewMatch() {
        resetMatch()
        sharedPref.setMatchInProgress(true)
    }

    fun matchEnded(matchAction: MatchAction) {
        if (matchAction == MatchAction.MATCH_END_CONFIRM_DISCARD) resetFrame() else frameEnded()
        sharedPref.setMatchInProgress(false)
    }

    private fun resetMatch() {
        getSavedStateRules()
        score.getFirst().resetMatchScore()
        score.getSecond().resetMatchScore()
        frameCount = 1
        resetFrame()
        viewModelScope.launch {
            snookerRepository.deleteCurrentMatch()
        }
        updateFrameStatus()
    }

    private fun getSavedStateRules() = sharedPref.apply {
        getApplication<Application>().resources.apply {
            matchFrames = getInt(getString(R.string.shared_pref_match_frames), matchFrames)
            matchReds = getInt(getString(R.string.shared_pref_match_reds), matchReds)
            matchFoul = getInt(getString(R.string.shared_pref_match_foul), matchFoul)
            matchFirst = getInt(getString(R.string.shared_pref_match_first), matchFirst)
            frameCount = getInt(getString(R.string.shared_pref_match_crt_frame), frameCount)
            score = score.getPlayerFromInt(getInt(getString(R.string.shared_pref_match_crt_player), 0))
        }
    }

    fun resetFrame() {
        if (score.isMatchInProgress()) {
            matchFirst = 1 - matchFirst
            score = score.getPlayerFromInt(matchFirst)
        } else {
            score = score.getPlayerFromInt(matchFirst)
        }
        frameMax = matchReds * 8 + 27
        score.getFirst().resetFrameScore()
        score.getSecond().resetFrameScore()
        frameStack.clear()
        ballStack.clear()
        ballStack.addBalls(WHITE(), BLACK(), PINK(), BLUE(), BROWN(), GREEN(), YELLOW())
        repeat(matchReds) { ballStack.addBalls(COLOR(), RED()) }
        _eventFrameUpdated.value = Event(Unit)
        updateFrameStatus()
    }

    fun queryEndFrame() = assignMatchAction(
        if (_displayFrame.value!!.isFrameInProgress()) {
            if (score.isWinningTheMatch(matchFrames)) MatchAction.MATCH_END_QUERY else MatchAction.FRAME_END_QUERY
        } else {
            if (score.isWinningTheMatch(matchFrames)) MatchAction.MATCH_END_CONFIRM else MatchAction.FRAME_END_CONFIRM
        }
    )

    fun queryEndMatch() = assignMatchAction(
        when {
            _displayFrame.value!!.isFrameInProgress() -> MatchAction.MATCH_END_QUERY
            score.isWinningTheMatch(matchFrames) -> MatchAction.MATCH_END_CONFIRM
            else -> MatchAction.MATCH_END_QUERY
        }
    )

    fun frameEnded() = score.apply {
        getWinner().addMatchPoint()
        getFirst().frameId = frameCount
        getSecond().frameId = frameCount
        updateFrameStatus()
        viewModelScope.launch {
            snookerRepository.saveCurrentFrame(displayFrame.value!!)
            this@GameViewModel.resetFrame()
            _eventFrameUpdated.value = Event(Unit)
        }
        frameCount += 1
        updateFrameStatus()
    }

    fun assignMatchAction(matchAction: MatchAction) {
        _eventMatchAction.value = Event(matchAction)
    }

    // Handler functions
    fun handleFrameEvent(frameEvent: FrameEvent, pot: DomainPot?, removeRed: Boolean?, freeBall: Boolean?) {
        _isFrameUpdateInProgress.value = true
        when (frameEvent) {
            FrameEvent.HANDLE_FOUL -> foul(pot!!, removeRed!!, freeBall!!)
            FrameEvent.HANDLE_POT -> updateFrame(pot!!)
            FrameEvent.HANDLE_UNDO -> undo()
        }
        updateFrameStatus()
        _isFrameUpdateInProgress.value = false
        _eventFrameUpdated.value = Event(Unit)
    }

    private fun foul(pot: DomainPot, removeRed: Boolean, freeBall: Boolean) {
        updateFrame(pot)
        if (removeRed) updateFrame(DomainPot.REMOVERED)
        if (freeBall) frameMax += ballStack.addFreeBall()
    }

    private fun updateFrame(pot: DomainPot) = ballStack.apply {
        frameStack.addToFrameStack(
            when (pot.potType) {
                in listOf(HIT, FREE, ADDRED) -> DomainPot.HIT(pot.ball)
                FOUL -> DomainPot.FOUL(pot.ball, pot.potAction)
                else -> pot
            },
            score.getPlayerAsInt(),
            frameCount
        )
        score.calculatePoints(pot, 1, this.last(), matchFoul, frameStack)
        when (pot.potType) {
            in listOf(HIT, FREE) -> removeBalls(1)
            in listOf(REMOVERED, ADDRED) -> removeBalls(2)
            else -> {
                if (last() is FREEBALL) {
                    frameStack.addToFrameStack(DomainPot.FREEMISS, score.getPlayerAsInt(), frameCount)
                    frameMax -= removeFreeBall()
                }
                if (last() is COLOR) removeBalls(1)
            }
        }
        if (size == 1) if (score.isFrameEqual()) addBalls(BLACK()) else queryEndFrame()
        if (pot.potAction == PotAction.SWITCH) score = score.getOther()
    }

    private fun undo(): Any = ballStack.apply {
        score = score.getPlayerFromInt(frameStack.last().player)
        val lastPot = frameStack.removeFromFrameStack()
        when (lastPot.potType) {
            HIT -> addBalls(if (isNextColor()) COLOR() else lastPot.ball)
            ADDRED -> addBalls(RED(), COLOR())
            FREE -> {
                frameMax += addFreeBall()
                undo()
            }
            REMOVERED -> {
                if (last() is FREEBALL) removeBalls(if (inColors()) 1 else 2)
                if (isNextColor()) addBalls(COLOR(), RED()) else addBalls(RED(), COLOR())
                undo()
            }
            FOUL -> {
                if (last() is FREEBALL) frameMax -= removeFreeBall()
                if (frameStack.isPreviousRed() && isNextColor()) addBalls(COLOR())
            }
            SAFE -> if (frameStack.isPreviousRed() && isNextColor()) addBalls(COLOR())
            MISS -> if (frameStack.isPreviousRed() && isNextColor()) {
                addBalls(COLOR())
            }
        }
        score.calculatePoints(lastPot, -1, ballStack.last(), matchFoul, frameStack)
    }

    // Match functions
    private fun updateFrameStatus() {
        _displayScore.value = score
        _displayFrame.value = DomainFrame(
            frameCount,
            mutableListOf(
                score.getFirst().asDomainPlayerScore(),
                score.getSecond().asDomainPlayerScore()
            ),
            frameStack,
            ballStack,
            frameMax
        )
    }
}