package com.visionrt.feature.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.ConfirmTaps
import com.visionrt.feature.accessibility.ScreenNarrator
import com.visionrt.feature.databinding.FragmentHelpBinding
import com.visionrt.feature.voice.ScreenVoice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Help and limitations screen (SAF-003, SAF-005, FR-003.4): known
 * limitations, complement-only positioning and responsible-use guidance.
 */
@AndroidEntryPoint
class HelpFragment : Fragment(R.layout.fragment_help) {

    @Inject
    lateinit var voice: ScreenVoice

    @Inject
    lateinit var taps: ConfirmTaps

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taps.attach(binding.backHomeButton, viewLifecycleOwner.lifecycleScope) {
            binding.root.findNavController().navigate(R.id.action_help_to_home)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            voice.narrate(
                ScreenNarrator.describe(
                    requireContext(),
                    binding.root,
                    getString(R.string.help_title),
                    getString(R.string.help_body),
                ),
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
