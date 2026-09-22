package com.visionrt.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.AnnouncementUtil
import com.visionrt.feature.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * App shell home (FR-004): primary start/stop control, a persistent speech
 * mute control, mode entries (FR-003) and navigation to settings and help.
 * Real assistance modes and audible/haptic feedback arrive in M2+.
 */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var assistanceActive = false
    private var silenced = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startStopButton.setOnClickListener { toggleAssistance() }
        binding.silenceButton.setOnClickListener { toggleSilence() }

        binding.obstacleModeButton.setOnClickListener { announcePlaceholder(it) }
        binding.objectModeButton.setOnClickListener { announcePlaceholder(it) }
        binding.textModeButton.setOnClickListener { announcePlaceholder(it) }

        binding.settingsButton.setOnClickListener {
            binding.root.findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.helpButton.setOnClickListener {
            binding.root.findNavController().navigate(R.id.action_home_to_help)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun toggleAssistance() {
        assistanceActive = !assistanceActive
        binding.startStopButton.text = getString(
            if (assistanceActive) R.string.home_stop else R.string.home_start,
        )
        binding.statusLine.text = getString(
            if (assistanceActive) R.string.home_status_active else R.string.home_status_idle,
        )
        AnnouncementUtil.announce(
            binding.startStopButton,
            getString(
                if (assistanceActive) R.string.home_started_announce else R.string.home_stopped_announce,
            ),
        )
    }

    private fun toggleSilence() {
        silenced = !silenced
        binding.silenceButton.text = getString(
            if (silenced) R.string.home_silence_off else R.string.home_silence_on,
        )
        AnnouncementUtil.announce(
            binding.silenceButton,
            getString(
                if (silenced) R.string.home_silenced_announce else R.string.home_unsilenced_announce,
            ),
        )
    }

    private fun announcePlaceholder(anchor: View) {
        AnnouncementUtil.announce(anchor, getString(R.string.mode_placeholder))
    }
}
