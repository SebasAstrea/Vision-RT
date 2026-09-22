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
import com.visionrt.feature.accessibility.FocusUtil
import com.visionrt.feature.databinding.FragmentOnboardingBinding
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

        binding.practiceButton.setOnClickListener { playSample() }
        binding.continueButton.setOnClickListener { onContinue() }
        binding.skipButton.setOnClickListener { onSkip() }
        binding.ackButton.setOnClickListener { onAcknowledge() }
        binding.backToTrainingButton.setOnClickListener { setStep(OnboardingStep.TRAINING) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupReplayMode() {
        binding.skipButton.visibility = View.GONE
        binding.continueButton.setText(R.string.common_back)
        binding.continueButton.setOnClickListener { binding.root.findNavController().navigateUp() }
    }

    private fun playSample() {
        AnnouncementUtil.announce(
            binding.practiceButton,
            getString(R.string.onboarding_sample_alert),
        )
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
