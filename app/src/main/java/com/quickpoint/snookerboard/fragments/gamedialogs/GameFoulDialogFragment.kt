package com.quickpoint.snookerboard.fragments.gamedialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import com.quickpoint.snookerboard.GenericEventsViewModel
import com.quickpoint.snookerboard.R
import com.quickpoint.snookerboard.databinding.FragmentDialogFoulBinding
import com.quickpoint.snookerboard.domain.*
import com.quickpoint.snookerboard.domain.DomainBall.*
import com.quickpoint.snookerboard.fragments.game.BallAdapter
import com.quickpoint.snookerboard.fragments.game.BallListener
import com.quickpoint.snookerboard.fragments.game.GameViewModel
import com.quickpoint.snookerboard.utils.*
import java.util.*

class GameFoulDialogFragment : DialogFragment() {
    private val eventsViewModel: GenericEventsViewModel by activityViewModels()
    private val gameViewModel: GameViewModel by activityViewModels()
    private lateinit var matchAction: MatchAction

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setSize(resources.getDimension(R.dimen.dialog_factor))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentDialogFoulBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_dialog_foul, container, false)

        // Bind RV, VM, adapter
        val ballAdapter = BallAdapter(
            BallListener { ball -> eventsViewModel.onBallClicked(ball) },
            MutableLiveData(),
            BallAdapterType.FOUL
        )

        binding.apply {
            lifecycleOwner = this@GameFoulDialogFragment
            gameViewModel = this@GameFoulDialogFragment.gameViewModel
            varEventsViewModel = this@GameFoulDialogFragment.eventsViewModel
            foulBallsListRv.apply {
                layoutManager = GridLayoutManager(activity, 4)
                adapter = ballAdapter
            }
        }

        // VM Observers
        eventsViewModel.apply {
            eventMatchActionQueried.observe(viewLifecycleOwner, EventObserver {
                if (it == MatchAction.FOUL_QUERIED) {
                    if (foulIsValid()) {
                        matchAction = MatchAction.FOUL_CONFIRMED
                        dismiss()
                    } else requireContext().toast(getString(R.string.toast_foul_invalid))
                } else {
                    eventsViewModel.resetFoul()
                    matchAction = MatchAction.NO_ACTION
                    dismiss()
                }
            })
        }
        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        eventsViewModel.resetFoul()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::matchAction.isInitialized) eventsViewModel.onEventMatchActionConfirmed(matchAction)
    }


}