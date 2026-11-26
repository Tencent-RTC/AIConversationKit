package com.trtc.uikit.aiconversationkit.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.store.AIConversationConfig
import com.trtc.uikit.aiconversationkit.store.AIConversationStore
import com.trtc.uikit.aiconversationkit.store.CompletionHandler

class AIConversationCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        setBackgroundImage(R.drawable.conversation_bg_full_screen)
    }

    fun startAIConversation(config: AIConversationConfig?, completion: CompletionHandler?) {
        AIConversationStore.shared.startAIConversation(config, completion)
    }

    fun stopAIConversation(completion: CompletionHandler?) {
        AIConversationStore.shared.stopAIConversation(completion)
    }

    fun setBackgroundImage(resourceId: Int) {
        if (resourceId == 0) {
            setBackground(ColorDrawable(Color.TRANSPARENT))
        } else {
            setBackground(ContextCompat.getDrawable(context, resourceId))
        }
    }
}