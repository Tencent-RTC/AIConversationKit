package com.trtc.uikit.aiconversationkit

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.trtc.uikit.aiconversationkit.store.AIConversationConfig
import com.trtc.uikit.aiconversationkit.view.AIConversationControlView
import com.trtc.uikit.aiconversationkit.view.AIConversationCoreView
import com.trtc.uikit.aiconversationkit.view.AIConversationSubtitleView
import io.trtc.tuikit.atomicxcore.api.CompletionHandler

enum class Feature(val value: String) {
    MIC("mic"),
    AI_INTERACTION("aiInteraction"),
    SUBTITLE("subtitle"),
}

class AIConversationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var conversationCoreView: AIConversationCoreView? = null
    private var conversationSubtitleView: AIConversationSubtitleView? = null
    private var conversationControlView: AIConversationControlView? = null

    fun startAIConversation(config: AIConversationConfig?, completion: CompletionHandler?) {
        conversationCoreView?.startAIConversation(config, completion)
    }

    fun stopAIConversation(completion: CompletionHandler?) {
        conversationCoreView?.stopAIConversation(completion)
    }

    fun setBackgroundImage(resourceId: Int) {
        conversationCoreView?.setBackgroundImage(resourceId)
    }

    fun disableFeatures(features: List<Feature>) {
        features.forEach { feature ->
            when (feature) {
                Feature.MIC, Feature.AI_INTERACTION ->
                    conversationControlView?.disableFeature(feature)
                Feature.SUBTITLE ->
                    conversationSubtitleView?.isVisible = false
            }
        }
    }

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.conversation_view_ai_conversation, this)
        conversationCoreView = findViewById(R.id.conversation_view_ai_conversation_core)
        conversationSubtitleView = findViewById(R.id.conversation_view_ai_subtitle)
        conversationControlView = findViewById(R.id.conversation_view_control)
    }
}
