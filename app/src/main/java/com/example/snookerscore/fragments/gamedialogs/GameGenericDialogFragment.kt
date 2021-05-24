package com.example.snookerscore.fragments.gamedialogs

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.snookerscore.GenericEventsViewModel
import com.example.snookerscore.R
import com.example.snookerscore.databinding.FragmentDialogGenBinding
import com.example.snookerscore.domain.MatchAction
import com.example.snookerscore.fragments.game.GameViewModel
import com.example.snookerscore.utils.EventObserver
import com.example.snookerscore.utils.setSize


class GameGenericDialogFragment : DialogFragment() {
    private val eventsViewModel: GenericEventsViewModel by activityViewModels()
    private val gameViewModel: GameViewModel by activityViewModels()
    private lateinit var matchAction: MatchAction

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setSize(resources.getDimension(R.dimen.dialog_factor))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentDialogGenBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_dialog_gen, container, false)

        binding.apply {
            lifecycleOwner = this@GameGenericDialogFragment
            genericEventsViewModel = eventsViewModel
            gameViewModel = this@GameGenericDialogFragment.gameViewModel
            GameGenericDialogFragmentArgs.fromBundle(requireArguments()).apply {
                dialogMatchActionA = matchActionA
                dialogMatchActionB = matchActionB
                dialogMatchActionC = matchActionC
                if (dialogMatchActionC in listOf(MatchAction.MATCH_END_CONFIRM, MatchAction.FRAME_END_CONFIRM)) {
                    this@GameGenericDialogFragment.isCancelable = false
                    }
            }
        }

        // Observers
        eventsViewModel.eventMatchActionQueried.observe(viewLifecycleOwner, EventObserver {
            dismiss()
            matchAction = it
        })
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::matchAction.isInitialized) eventsViewModel.onEventMatchActionConfirmed(matchAction)
    }
}

