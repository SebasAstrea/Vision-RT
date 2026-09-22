package com.visionrt.feature.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.visionrt.feature.R
import com.visionrt.feature.databinding.FragmentHelpBinding

/**
 * Help and limitations screen (SAF-003, SAF-005, FR-003.4): known
 * limitations, complement-only positioning and responsible-use guidance.
 */
class HelpFragment : Fragment(R.layout.fragment_help) {

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
        binding.backHomeButton.setOnClickListener {
            binding.root.findNavController().navigate(R.id.action_help_to_home)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
