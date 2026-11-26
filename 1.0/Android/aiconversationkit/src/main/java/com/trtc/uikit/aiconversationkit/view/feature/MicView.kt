package com.trtc.uikit.aiconversationkit.view.feature

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.utils.widget.ImageFilterButton
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.store.AIConversationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageFilterButton(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var micStatusJob: Job? = null

    init {
        setOnClickListener {
            if (AIConversationStore.shared.conversationState.isMicOpened.value) {
                AIConversationStore.shared.closeLocalMicrophone()
            } else {
                AIConversationStore.shared.openLocalMicrophone(null)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        micStatusJob = scope.launch {
            AIConversationStore.shared.conversationState.isMicOpened.collect { isOpened ->
                updateMicIcon(isOpened)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        micStatusJob?.cancel()
        micStatusJob = null
    }

    private fun updateMicIcon(isOpened: Boolean) {
        val iconRes = if (isOpened) {
            R.drawable.conversation_ic_mic_state_on
        } else {
            R.drawable.conversation_ic_mic_state_off
        }
        setImageResource(iconRes)
    }
}