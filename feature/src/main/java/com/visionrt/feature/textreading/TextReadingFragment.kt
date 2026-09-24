package com.visionrt.feature.textreading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.visionrt.core.ocr.TextReadingService
import com.visionrt.feature.R
import com.visionrt.feature.accessibility.AnnouncementUtil
import com.visionrt.feature.accessibility.ConfirmTaps
import com.visionrt.feature.databinding.FragmentTextReadingBinding
import com.visionrt.feature.voice.ScreenVoice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * FR-009 Text Reading Mode: capture → OCR → read/repeat/next/stop controls.
 * All actions are two-tap confirmed (UX-001) and announced for TalkBack.
 */
@AndroidEntryPoint
@Suppress("TooManyFunctions") // FR-009 read/repeat/next/stop surface
class TextReadingFragment : Fragment(R.layout.fragment_text_reading) {

    @Inject
    lateinit var reading: TextReadingService

    @Inject
    lateinit var voice: ScreenVoice

    @Inject
    lateinit var taps: ConfirmTaps

    private var _binding: FragmentTextReadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTextReadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scope = viewLifecycleOwner.lifecycleScope
        wireButtons(scope)
        scope.launch {
            voice.narrate(
                getString(R.string.ocr_status_idle),
            )
        }
    }

    override fun onDestroyView() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { reading.stopReading() }
        }
        _binding = null
        super.onDestroyView()
    }

    private fun wireButtons(scope: androidx.lifecycle.LifecycleCoroutineScope) {
        taps.attach(binding.ocrStartButton, scope) { startReading() }
        taps.attach(binding.ocrRepeatButton, scope) { repeat() }
        taps.attach(binding.ocrNextButton, scope) { next() }
        taps.attach(binding.ocrStopButton, scope) { stop() }
        taps.attach(binding.ocrBackButton, scope) {
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching { reading.stopReading() }
                binding.root.findNavController().navigateUp()
            }
        }
    }

    private fun startReading() {
        setStatus(getString(R.string.ocr_running))
        viewLifecycleOwner.lifecycleScope.launch {
            val result = reading.startReading()
            result.fold(
                onSuccess = { text ->
                    setStatus(
                        getString(
                            R.string.ocr_block_loaded,
                            reading.blockCount(),
                        ),
                    )
                    // Text already spoken via OCR_RESULT alert; still narrate for UI path.
                    voice.speakNow(text)
                },
                onFailure = { t ->
                    val msg = if (t.message == TextReadingService.END_OF_TEXT) {
                        getString(R.string.ocr_end)
                    } else {
                        getString(R.string.ocr_unclear)
                    }
                    setStatus(msg)
                    announce(msg)
                },
            )
        }
    }

    private fun repeat() {
        viewLifecycleOwner.lifecycleScope.launch {
            reading.repeat().fold(
                onSuccess = { voice.speakNow(it) },
                onFailure = { announceNotReading() },
            )
        }
    }

    private fun next() {
        viewLifecycleOwner.lifecycleScope.launch {
            reading.next().fold(
                onSuccess = { voice.speakNow(it) },
                onFailure = { t ->
                    if (t.message == TextReadingService.END_OF_TEXT) {
                        setStatus(getString(R.string.ocr_end))
                        announce(getString(R.string.ocr_end))
                    } else {
                        announceNotReading()
                    }
                },
            )
        }
    }

    private fun stop() {
        viewLifecycleOwner.lifecycleScope.launch {
            reading.stopReading()
            setStatus(getString(R.string.ocr_stopped))
            announce(getString(R.string.ocr_stopped))
        }
    }

    private fun announceNotReading() {
        announce(getString(R.string.ocr_not_reading))
    }

    private fun setStatus(text: String) {
        binding.textReadingStatus.text = text
    }

    private fun announce(message: String) {
        AnnouncementUtil.announce(binding.root, message)
        viewLifecycleOwner.lifecycleScope.launch { voice.speakNow(message) }
    }
}
