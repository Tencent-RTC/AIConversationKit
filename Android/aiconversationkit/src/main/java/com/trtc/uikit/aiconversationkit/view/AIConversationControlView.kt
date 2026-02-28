package com.trtc.uikit.aiconversationkit.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.trtc.uikit.aiconversationkit.Feature
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.view.feature.HangUpView
import com.trtc.uikit.aiconversationkit.view.feature.AIInteractionView
import com.trtc.uikit.aiconversationkit.view.feature.MicView

class AIConversationControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var hangUpView: HangUpView
    private lateinit var aiInteractionView: AIInteractionView
    private lateinit var micView: MicView

    init {
        initView()
    }

    fun disableFeature(feature: Feature) {
        when (feature) {
            Feature.MIC -> micView.visibility = GONE
            Feature.AI_INTERACTION -> aiInteractionView.visibility = GONE
            else -> {}
        }
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.conversation_view_ai_conversation_control, this, true)

        micView = findViewById(R.id.conversation_mic_view)
        aiInteractionView = findViewById(R.id.conversation_ai_interaction_view)
        hangUpView = findViewById(R.id.conversation_hang_up_view)
    }
}