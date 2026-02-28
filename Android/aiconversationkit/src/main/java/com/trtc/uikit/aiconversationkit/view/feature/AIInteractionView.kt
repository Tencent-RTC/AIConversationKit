package com.trtc.uikit.aiconversationkit.view.feature

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.store.AIConversationStore
import com.trtc.uikit.aiconversationkit.store.AIStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AIInteractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var statusJob: Job? = null

    private var imageAIStateListening: CapsuleWaveAnimationView? = null
    private var imageAIStateInterrupting: AppCompatImageView? = null
    private var textAIInteraction: AppCompatTextView? = null
    private var btnInterruptSpeech: AppCompatButton? = null

    init {
        initView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        statusJob = scope.launch {
            AIConversationStore.shared.conversationState.aiStatus.collect { status ->
                val isCanInterrupt = status == AIStatus.THINKING || status == AIStatus.SPEAKING
                btnInterruptSpeech?.isEnabled = isCanInterrupt
                textAIInteraction?.setText(if (isCanInterrupt) R.string.conversation_ai_interaction_interrupting else R.string.conversation_ai_interaction_listening)
                imageAIStateListening?.isVisible = !isCanInterrupt
                imageAIStateInterrupting?.isVisible = isCanInterrupt
                if (isCanInterrupt) {
                    imageAIStateListening?.stopAnimating()
                } else {
                    imageAIStateListening?.startAnimating()
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        statusJob?.cancel()
        statusJob = null
    }

    private fun initView() {
        inflate(context, R.layout.conversation_view_ai_interaction, this)
        imageAIStateListening = findViewById(R.id.conversation_view_ai_state_listening)
        imageAIStateInterrupting = findViewById(R.id.conversation_image_ai_state_interrupting)
        textAIInteraction = findViewById(R.id.conversation_tv_ai_interaction)
        btnInterruptSpeech = findViewById(R.id.conversation_btn_interrupt_speech)
        btnInterruptSpeech?.setOnClickListener {
            AIConversationStore.shared.interruptSpeech()
        }
    }
}