package com.example.snookerscore.fragments.rankings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.snookerscore.GenericViewModelFactory
import com.example.snookerscore.R
import com.example.snookerscore.database.SnookerDatabase
import com.example.snookerscore.databinding.FragmentRankingsBinding
import com.example.snookerscore.repository.SnookerRepository

class RankingsFragment : Fragment() {
    //    private val gameFragmentViewModel: GameFragmentViewModel by activityViewModels()
    private val rankingsViewModel: RankingsFragmentViewModel by lazy {
        ViewModelProvider(
            this, GenericViewModelFactory(
                requireNotNull(this.activity).application,
                SnookerRepository(SnookerDatabase.getDatabase(requireNotNull(this.activity).application)),
                this,
                null
            )
        ).get(RankingsFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentRankingsBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_rankings, container, false)


        binding.apply {
            lifecycleOwner = this@RankingsFragment
            viewModel = rankingsViewModel
            rankingsRv.adapter = RankingsAdapter()
        }


        return binding.root
    }
}