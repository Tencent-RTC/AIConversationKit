package com.trtc.uikit.aiconversationkit.view.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.trtc.uikit.aiconversationkit.store.AIStatus
import com.trtc.uikit.aiconversationkit.store.AIConversationStore

object AIConversationLiveData {

    @JvmStatic
    fun aiStatusLiveData(): LiveData<AIStatus> =
        AIConversationStore.shared.conversationState.aiStatus.asLiveData()
}
