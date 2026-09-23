package com.visionrt.feature.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.lifecycle.lifecycleScope
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.AnnouncementUtil
import com.visionrt.feature.accessibility.ConfirmTaps
import com.visionrt.feature.accessibility.FocusUtil
import com.visionrt.feature.accessibility.ScreenNarrator
import com.visionrt.feature.databinding.FragmentOnboardingBinding
import com.visionrt.feature.voice.ScreenVoice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * First-run safety onboarding (FR-001, FR-002). Two steps:
 *  1. Training: modes, alerts, controls, limitations and a practice alert.
 *  2. Safety disclaimer: explicit, non-bypassable acknowledgment.
 *
 * Reached from Settings with "replay"=true it becomes a read-only training
 * replay and never re-asks for acknowledgment.
 */
@AndroidEntryPoint
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var voice: ScreenVoice

    @Inject
    lateinit var taps: ConfirmTaps

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private var replay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        replay = arguments?.getBoolean(ARG_REPLAY) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (replay) {
            setupReplayMode()
        } else {
            // Routing lives in the feature (ARCHITECTURE.md §7): a returning
            // user who already acknowledged the disclaimer starts on home (FR-001).
            viewLifecycleOwner.lifecycleScope.launch {
                if (settings.onboardingAcknowledged.first()) {
                    binding.root.findNavController().navigate(R.id.action_onboarding_to_home)
                }
            }
        }

        val scope = viewLifecycleOwner.lifecycleScope
        taps.attach(binding.practiceButton, scope) { playSample() }
        taps.attach(binding.continueButton, scope) { onContinue() }
        taps.attach(binding.skipButton, scope) { onSkip() }
        taps.attach(binding.ackButton, scope) { onAcknowledge() }
        taps.attach(binding.backToTrainingButton, scope) { setStep(OnboardingStep.TRAINING) }

        scope.launch {
            voice.narrate(
                ScreenNarrator.describe(
                    requireContext(),
                    binding.root,
                    getString(R.string.training_step_heading),
                    getString(R.string.onboarding_title),
                ),
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupReplayMode() {
        binding.skipButton.visibility = View.GONE
        binding.continueButton.setText(R.string.common_back)
    }

    private fun playSample() {
        val sample = getString(R.string.onboarding_sample_alert)
        AnnouncementUtil.announce(binding.practiceButton, sample)
        viewLifecycleOwner.lifecycleScope.launch { voice.speakNow(sample) }
    }

    private fun onContinue() {
        if (replay) {
            binding.root.findNavController().navigateUp()
        } else {
            setStep(OnboardingStep.DISCLAIMER)
        }
    }

    private fun onSkip() {
        viewLifecycleOwner.lifecycleScope.launch { settings.setTrainingCompleted(true) }
        setStep(OnboardingStep.DISCLAIMER)
    }

    private fun setStep(step: OnboardingStep) {
        val training = step == OnboardingStep.TRAINING
        binding.trainingSection.visibility = if (training) View.VISIBLE else View.GONE
        binding.disclaimerSection.visibility = if (training) View.GONE else View.VISIBLE
        binding.stepHeading.setText(
            if (training) R.string.training_step_heading else R.string.disclaimer_step_heading,
        )
        FocusUtil.moveAccessibilityFocusTo(
            if (training) binding.onboardingTitle else binding.disclaimerTitle,
            getString(
                if (training) R.string.training_step_heading else R.string.disclaimer_step_heading,
            ),
        )
        val heading = getString(
            if (training) R.string.training_step_heading else R.string.disclaimer_step_heading,
        )
        viewLifecycleOwner.lifecycleScope.launch {
            voice.narrate(
                ScreenNarrator.describe(
                    requireContext(),
                    binding.root,
                    heading,
                    getString(
                        if (training) R.string.onboarding_title else R.string.disclaimer_title,
                    ),
                ),
            )
        }
    }

    private fun onAcknowledge() {
        viewLifecycleOwner.lifecycleScope.launch {
            settings.setOnboardingAcknowledged(true)
            AnnouncementUtil.announce(binding.ackButton, getString(R.string.disclaimer_ack_announce))
            binding.root.findNavController().navigate(R.id.action_onboarding_to_home)
        }
    }

    private enum class OnboardingStep { TRAINING, DISCLAIMER }

    companion object {
        private const val ARG_REPLAY = "replay"
    }
}
