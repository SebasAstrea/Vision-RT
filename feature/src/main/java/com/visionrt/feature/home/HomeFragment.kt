package com.visionrt.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.visionrt.core.assistance.AssistanceController
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.AnnouncementUtil
import com.visionrt.feature.accessibility.ConfirmTaps
import com.visionrt.feature.accessibility.ScreenNarrator
import com.visionrt.feature.databinding.FragmentHomeBinding
import com.visionrt.feature.voice.ScreenVoice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * App shell home (FR-004): primary start/stop control, a persistent speech
 * mute control, mode entries (FR-003) and navigation to settings and help.
 *
 * M3: Start requests CAMERA permission (human-approved) then drives the real
 * [AssistanceController] pipeline; Stop releases camera + detector.
 */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var voice: ScreenVoice

    @Inject
    lateinit var taps: ConfirmTaps

    @Inject
    lateinit var assistance: AssistanceController

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var startPending = false

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && startPending) {
                startPending = false
                startAssistance()
            } else if (!granted) {
                startPending = false
                speakStatus(R.string.home_camera_denied)
            }
        }

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
        val scope = viewLifecycleOwner.lifecycleScope
        wireButtons(scope)
        syncStartStopUi()
        viewLifecycleOwner.lifecycleScope.launch {
            settings.speechMuted.collect { muted ->
                binding.silenceButton.text = getString(
                    if (muted) R.string.home_silence_off else R.string.home_silence_on,
                )
            }
        }
        scope.launch {
            voice.narrate(
                ScreenNarrator.describe(
                    requireContext(),
                    binding.root,
                    getString(R.string.home_title),
                    getString(R.string.home_status_idle),
                ),
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun wireButtons(scope: androidx.lifecycle.LifecycleCoroutineScope) {
        taps.attach(binding.startStopButton, scope) { toggleAssistance() }
        taps.attach(binding.silenceButton, scope) { toggleSilence() }
        taps.attach(binding.obstacleModeButton, scope) { announcePlaceholder(it) }
        taps.attach(binding.objectModeButton, scope) { announcePlaceholder(it) }
        taps.attach(binding.textModeButton, scope) { announcePlaceholder(it) }
        taps.attach(binding.settingsButton, scope) {
            binding.root.findNavController().navigate(R.id.action_home_to_settings)
        }
        taps.attach(binding.helpButton, scope) {
            binding.root.findNavController().navigate(R.id.action_home_to_help)
        }
    }

    private fun toggleAssistance() {
        if (assistance.isActive()) {
            viewLifecycleOwner.lifecycleScope.launch {
                assistance.stop()
                syncStartStopUi()
                speakStatus(R.string.home_stopped_announce)
            }
        } else if (
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startAssistance()
        } else {
            startPending = true
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startAssistance() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = assistance.start()
            startPending = false
            syncStartStopUi()
            speakStatus(
                if (result.isSuccess) R.string.home_started_announce else R.string.home_start_failed,
            )
        }
    }

    private fun syncStartStopUi() {
        val active = assistance.isActive()
        binding.startStopButton.text = getString(
            if (active) R.string.home_stop else R.string.home_start,
        )
        binding.statusLine.text = getString(
            if (active) R.string.home_status_active else R.string.home_status_idle,
        )
    }

    private fun speakStatus(resId: Int) {
        val message = getString(resId)
        AnnouncementUtil.announce(binding.startStopButton, message)
        viewLifecycleOwner.lifecycleScope.launch { voice.speakNow(message) }
    }

    private fun toggleSilence() {
        viewLifecycleOwner.lifecycleScope.launch {
            val muted = !settings.speechMuted.first()
            settings.setSpeechMuted(muted)
            val res = if (muted) R.string.home_silenced_announce else R.string.home_unsilenced_announce
            AnnouncementUtil.announce(binding.silenceButton, getString(res))
            if (muted) voice.stop() else voice.speakNow(getString(res))
        }
    }

    private fun announcePlaceholder(anchor: View) {
        val message = getString(R.string.mode_placeholder)
        AnnouncementUtil.announce(anchor, message)
        viewLifecycleOwner.lifecycleScope.launch { voice.speakNow(message) }
    }
}
