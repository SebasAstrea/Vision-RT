package com.visionrt.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.visionrt.core.domain.Verbosity
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.AnnouncementUtil
import com.visionrt.feature.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Settings skeleton (FR-012): persisted verbosity level (FR-006.5) and replay
 * of the training flow. Covered by TalkBack with large touch targets.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var settings: SettingsRepository

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var isApplyingFromPreference = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectVerbosity()
        binding.verbosityGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingFromPreference) return@setOnCheckedChangeListener
            val selected = when (checkedId) {
                R.id.verbosity_minimal -> Verbosity.MINIMAL
                R.id.verbosity_detailed -> Verbosity.DETAILED
                else -> Verbosity.NORMAL
            }
            viewLifecycleOwner.lifecycleScope.launch { settings.setVerbosity(selected) }
            AnnouncementUtil.announce(
                binding.verbosityGroup,
                getString(R.string.settings_verbosity_announce, verbosityLabel(selected)),
            )
        }

        binding.replayTrainingButton.setOnClickListener {
            binding.root.findNavController().navigate(R.id.action_settings_to_onboarding)
        }
    }

    private fun verbosityLabel(verbosity: Verbosity): String = when (verbosity) {
        Verbosity.MINIMAL -> getString(R.string.settings_verbosity_minimal)
        Verbosity.NORMAL -> getString(R.string.settings_verbosity_normal)
        Verbosity.DETAILED -> getString(R.string.settings_verbosity_detailed)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun collectVerbosity() {
        viewLifecycleOwner.lifecycleScope.launch {
            settings.verbosity.collect { verbosity ->
                isApplyingFromPreference = true
                binding.verbosityGroup.check(radioIdFor(verbosity))
                isApplyingFromPreference = false
            }
        }
    }

    private fun radioIdFor(verbosity: Verbosity): Int = when (verbosity) {
        Verbosity.MINIMAL -> R.id.verbosity_minimal
        Verbosity.DETAILED -> R.id.verbosity_detailed
        Verbosity.NORMAL -> R.id.verbosity_normal
    }
}
