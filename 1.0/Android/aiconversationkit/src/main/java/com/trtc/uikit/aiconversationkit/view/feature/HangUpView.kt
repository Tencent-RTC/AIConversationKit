package com.trtc.uikit.aiconversationkit.view.feature

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.utils.widget.ImageFilterButton
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService
import com.trtc.uikit.aiconversationkit.store.AIConversationStore

class HangUpView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageFilterButton(context, attrs, defStyleAttr) {
    init {
        setImageResource(R.drawable.conversation_ic_hang_up)
        setOnClickListener {
            AIConversationStore.shared.stopAIConversation(null)
            if (!PackageService.isInternalDemo()) {
                finishActivity()
            }
        }
    }

    private fun finishActivity() {
        val activity = (context as? Activity)
        activity?.finish()
    }
}