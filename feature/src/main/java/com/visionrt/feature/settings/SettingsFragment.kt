package com.visionrt.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.visionrt.core.diagnostics.DiagnosticsPort
import com.visionrt.core.domain.DeviceProfile
import com.visionrt.core.domain.DeviceProfileProvider
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
 * Settings (FR-012): verbosity (FR-006.5), training replay, device profile
 * read-only (OR-001.4), and one-shot detector benchmark (OR-004 diagnostics).
 */
@AndroidEntryPoint
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
        collectVerbosity()
        showDeviceProfile()
        binding.verbosityGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingFromPreference) return@setOnCheckedChangeListener
            val selected = when (checkedId) {
                R.id.verbosity_minimal -> Verbosity.MINIMAL
                R.id.verbosity_detailed -> Verbosity.DETAILED
                else -> Verbosity.NORMAL
            }
            viewLifecycleOwner.lifecycleScope.launch { settings.setVerbosity(selected) }
            val message = getString(R.string.settings_verbosity_announce, verbosityLabel(selected))
            AnnouncementUtil.announce(binding.verbosityGroup, message)
            viewLifecycleOwner.lifecycleScope.launch { voice.speakNow(message) }
        }

        taps.attach(binding.replayTrainingButton, viewLifecycleOwner.lifecycleScope) {
            binding.root.findNavController().navigate(R.id.action_settings_to_onboarding)
        }

        taps.attach(binding.runBenchmarkButton, viewLifecycleOwner.lifecycleScope) {
            runDetectorBenchmark()
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

    private fun showDeviceProfile() {
        val label = when (deviceProfile.profile) {
            DeviceProfile.LOW_END -> getString(R.string.settings_device_profile_low_end)
            DeviceProfile.STANDARD -> getString(R.string.settings_device_profile_standard)
        }
        binding.deviceProfileValue.text = label
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

    private fun fmtMs(value: Double): String =
        String.format(Locale.US, "%.0f", value)

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
