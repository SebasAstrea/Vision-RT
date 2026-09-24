package com.visionrt.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.visionrt.core.diagnostics.DiagnosticsPort
import com.visionrt.core.domain.DeviceProfile
import com.visionrt.core.domain.DeviceProfileProvider
import com.visionrt.core.domain.HapticIntensity
import com.visionrt.core.domain.Verbosity
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.AnnouncementUtil
import com.visionrt.feature.accessibility.ConfirmTaps
import com.visionrt.feature.accessibility.ScreenNarrator
import com.visionrt.feature.databinding.FragmentSettingsBinding
import com.visionrt.feature.voice.ScreenVoice
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Settings (FR-012): verbosity (FR-006.5), speech rate (FR-010.5), haptic
 * enable/intensity (FR-011.5–6), earcons (FR-012.4), training replay, device
 * profile read-only (OR-001.4), and one-shot detector benchmark (OR-004).
 */
@AndroidEntryPoint
@Suppress("TooManyFunctions") // FR-012 settings surface (verbosity, rate, haptics, earcons, bench)
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var voice: ScreenVoice

    @Inject
    lateinit var taps: ConfirmTaps

    @Inject
    lateinit var deviceProfile: DeviceProfileProvider

    @Inject
    lateinit var diagnostics: DiagnosticsPort

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
        collectPreferences()
        showDeviceProfile()
        wireVerbosity()
        wireSpeechRate()
        wireHaptics()
        wireEarcons()

        taps.attach(binding.replayTrainingButton, viewLifecycleOwner.lifecycleScope) {
            binding.root.findNavController().navigate(R.id.action_settings_to_onboarding)
        }

        taps.attach(binding.runBenchmarkButton, viewLifecycleOwner.lifecycleScope) {
            runDetectorBenchmark()
        }

        taps.attach(binding.runOcrBenchmarkButton, viewLifecycleOwner.lifecycleScope) {
            runOcrBenchmark()
        }

        taps.attach(binding.exportDiagnosticsButton, viewLifecycleOwner.lifecycleScope) {
            exportDiagnostics()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            voice.narrate(
                ScreenNarrator.describe(
                    requireContext(),
                    binding.root,
                    getString(R.string.settings_title),
                    getString(R.string.settings_verbosity_title),
                ),
            )
        }
    }

    private fun wireVerbosity() {
        binding.verbosityGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingFromPreference) return@setOnCheckedChangeListener
            val selected = when (checkedId) {
                R.id.verbosity_minimal -> Verbosity.MINIMAL
                R.id.verbosity_detailed -> Verbosity.DETAILED
                else -> Verbosity.NORMAL
            }
            viewLifecycleOwner.lifecycleScope.launch { settings.setVerbosity(selected) }
            announceAndSpeak(binding.verbosityGroup, verbosityLabel(selected), R.string.settings_verbosity_announce)
        }
    }

    private fun wireSpeechRate() {
        binding.speechRateSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser || isApplyingFromPreference) return
                    binding.speechRateValue.text =
                        getString(R.string.settings_speech_rate_value, progressToRate(progress))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (isApplyingFromPreference) return
                    val rate = progressToRate(seekBar?.progress ?: DEFAULT_SEEK_PROGRESS)
                    viewLifecycleOwner.lifecycleScope.launch {
                        settings.setSpeechRate(rate)
                        val label = getString(R.string.settings_speech_rate_value, rate)
                        announceAndSpeak(binding.speechRateSeek, label, R.string.settings_speech_rate_announce)
                    }
                }
            },
        )
    }

    private fun wireHaptics() {
        binding.hapticsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isApplyingFromPreference) return@setOnCheckedChangeListener
            viewLifecycleOwner.lifecycleScope.launch { settings.setHapticsEnabled(isChecked) }
            binding.hapticsWarning.visibility = if (isChecked) View.GONE else View.VISIBLE
            val state = getString(
                if (isChecked) R.string.settings_haptics_on else R.string.settings_haptics_off,
            )
            announceAndSpeak(binding.hapticsSwitch, state, R.string.settings_haptics_announce)
        }
        binding.hapticIntensityGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingFromPreference) return@setOnCheckedChangeListener
            val intensity = when (checkedId) {
                R.id.haptic_intensity_light -> HapticIntensity.LIGHT
                R.id.haptic_intensity_strong -> HapticIntensity.STRONG
                else -> HapticIntensity.MEDIUM
            }
            viewLifecycleOwner.lifecycleScope.launch { settings.setHapticIntensity(intensity) }
            announceAndSpeak(
                binding.hapticIntensityGroup,
                hapticIntensityLabel(intensity),
                R.string.settings_haptic_intensity_announce,
            )
        }
    }

    private fun wireEarcons() {
        binding.earconsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isApplyingFromPreference) return@setOnCheckedChangeListener
            viewLifecycleOwner.lifecycleScope.launch { settings.setEarconsEnabled(isChecked) }
            val state = getString(
                if (isChecked) R.string.settings_earcons_on else R.string.settings_earcons_off,
            )
            announceAndSpeak(binding.earconsSwitch, state, R.string.settings_earcons_announce)
        }
    }

    private fun announceAndSpeak(anchor: View, label: String, announceRes: Int) {
        val message = getString(announceRes, label)
        AnnouncementUtil.announce(anchor, message)
        viewLifecycleOwner.lifecycleScope.launch { voice.speakNow(message) }
    }

    private fun progressToRate(progress: Int): Float =
        (SPEECH_RATE_MIN + progress * SPEECH_RATE_STEP)
            .coerceIn(SPEECH_RATE_MIN, SPEECH_RATE_MAX)

    private fun rateToProgress(rate: Float): Int =
        ((rate - SPEECH_RATE_MIN) / SPEECH_RATE_STEP).toInt()
            .coerceIn(0, SPEECH_SEEK_MAX)

    private fun collectPreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            settings.verbosity.collect { verbosity ->
                isApplyingFromPreference = true
                binding.verbosityGroup.check(radioIdFor(verbosity))
                isApplyingFromPreference = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.speechRate.collect { rate ->
                isApplyingFromPreference = true
                binding.speechRateSeek.progress = rateToProgress(rate)
                binding.speechRateValue.text =
                    getString(R.string.settings_speech_rate_value, rate)
                isApplyingFromPreference = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.hapticsEnabled.collect { enabled ->
                isApplyingFromPreference = true
                binding.hapticsSwitch.isChecked = enabled
                binding.hapticsWarning.visibility = if (enabled) View.GONE else View.VISIBLE
                isApplyingFromPreference = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.hapticIntensity.collect { intensity ->
                isApplyingFromPreference = true
                binding.hapticIntensityGroup.check(hapticRadioId(intensity))
                isApplyingFromPreference = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.earconsEnabled.collect { enabled ->
                isApplyingFromPreference = true
                binding.earconsSwitch.isChecked = enabled
                isApplyingFromPreference = false
            }
        }
    }

    private fun showDeviceProfile() {
        binding.deviceProfileValue.text = when (deviceProfile.profile) {
            DeviceProfile.LOW_END -> getString(R.string.settings_device_profile_low_end)
            DeviceProfile.STANDARD -> getString(R.string.settings_device_profile_standard)
        }
    }

    private fun runDetectorBenchmark() {
        val scope = viewLifecycleOwner.lifecycleScope
        val running = getString(R.string.settings_benchmark_running)
        binding.benchmarkResult.text = running
        binding.runBenchmarkButton.isEnabled = false
        AnnouncementUtil.announce(binding.runBenchmarkButton, running)
        scope.launch {
            voice.speakNow(running)
            val result = diagnostics.runDetectorBenchmark()
            binding.runBenchmarkButton.isEnabled = true
            val message = result.fold(
                onSuccess = { s ->
                    getString(
                        if (s.withinLatencyBudget()) {
                            R.string.settings_benchmark_done
                        } else {
                            R.string.settings_benchmark_over_budget
                        },
                        fmtMs(s.p50Ms),
                        fmtMs(s.p95Ms),
                        fmtMs(s.p99Ms),
                    )
                },
                onFailure = { t ->
                    getString(
                        R.string.settings_benchmark_failed,
                        t.message ?: getString(R.string.settings_benchmark_failed_generic),
                    )
                },
            )
            binding.benchmarkResult.text = message
            AnnouncementUtil.announce(binding.benchmarkResult, message)
            voice.speakNow(message)
        }
    }

    private fun runOcrBenchmark() {
        val scope = viewLifecycleOwner.lifecycleScope
        val running = getString(R.string.settings_ocr_benchmark_running)
        binding.ocrBenchmarkResult.text = running
        binding.runOcrBenchmarkButton.isEnabled = false
        AnnouncementUtil.announce(binding.runOcrBenchmarkButton, running)
        scope.launch {
            voice.speakNow(running)
            val result = diagnostics.runOcrBenchmark()
            binding.runOcrBenchmarkButton.isEnabled = true
            val message = result.fold(
                onSuccess = { s ->
                    getString(
                        if (s.withinLatencyBudget()) {
                            R.string.settings_ocr_benchmark_done
                        } else {
                            R.string.settings_ocr_benchmark_over_budget
                        },
                        fmtMs(s.p50Ms),
                        fmtMs(s.p95Ms),
                    )
                },
                onFailure = { t ->
                    getString(
                        R.string.settings_ocr_benchmark_failed,
                        t.message ?: getString(R.string.settings_benchmark_failed_generic),
                    )
                },
            )
            binding.ocrBenchmarkResult.text = message
            AnnouncementUtil.announce(binding.ocrBenchmarkResult, message)
            voice.speakNow(message)
        }
    }

    private fun exportDiagnostics() {
        val scope = viewLifecycleOwner.lifecycleScope
        val running = getString(R.string.settings_export_diagnostics_running)
        binding.diagnosticsExportResult.text = running
        binding.exportDiagnosticsButton.isEnabled = false
        AnnouncementUtil.announce(binding.exportDiagnosticsButton, running)
        scope.launch {
            voice.speakNow(running)
            val result = diagnostics.collectDiagnosticsSnapshot()
            binding.exportDiagnosticsButton.isEnabled = true
            val message = result.fold(
                onSuccess = { s ->
                    getString(
                        R.string.settings_export_diagnostics_done,
                        s.deviceProfile,
                        s.degradationLevel,
                        fmtMs(s.memoryPeakMb),
                    )
                },
                onFailure = { t ->
                    getString(
                        R.string.settings_export_diagnostics_failed,
                        t.message ?: getString(R.string.settings_benchmark_failed_generic),
                    )
                },
            )
            binding.diagnosticsExportResult.text = message
            AnnouncementUtil.announce(binding.diagnosticsExportResult, message)
            voice.speakNow(message)
        }
    }

    private fun fmtMs(value: Double): String =
        String.format(Locale.US, "%.0f", value)

    private fun verbosityLabel(verbosity: Verbosity): String = when (verbosity) {
        Verbosity.MINIMAL -> getString(R.string.settings_verbosity_minimal)
        Verbosity.NORMAL -> getString(R.string.settings_verbosity_normal)
        Verbosity.DETAILED -> getString(R.string.settings_verbosity_detailed)
    }

    private fun hapticIntensityLabel(intensity: HapticIntensity): String = when (intensity) {
        HapticIntensity.LIGHT -> getString(R.string.settings_haptic_intensity_light)
        HapticIntensity.MEDIUM -> getString(R.string.settings_haptic_intensity_medium)
        HapticIntensity.STRONG -> getString(R.string.settings_haptic_intensity_strong)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun radioIdFor(verbosity: Verbosity): Int = when (verbosity) {
        Verbosity.MINIMAL -> R.id.verbosity_minimal
        Verbosity.DETAILED -> R.id.verbosity_detailed
        Verbosity.NORMAL -> R.id.verbosity_normal
    }

    private fun hapticRadioId(intensity: HapticIntensity): Int = when (intensity) {
        HapticIntensity.LIGHT -> R.id.haptic_intensity_light
        HapticIntensity.MEDIUM -> R.id.haptic_intensity_medium
        HapticIntensity.STRONG -> R.id.haptic_intensity_strong
    }

    private companion object {
        // Seek range 0..30 maps to 0.5x..2.0x in 0.05 steps (progress 10 = 1.0x).
        const val SPEECH_SEEK_MAX = 30
        const val DEFAULT_SEEK_PROGRESS = 10
        const val SPEECH_RATE_MIN = 0.5f
        const val SPEECH_RATE_MAX = 2.0f
        const val SPEECH_RATE_STEP = 0.05f
    }
}
