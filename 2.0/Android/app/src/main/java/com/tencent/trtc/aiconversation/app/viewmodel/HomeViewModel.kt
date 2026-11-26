// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 首页 ViewModel

package com.tencent.trtc.aiconversation.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    /** 轻提示文案，为空表示不展示。 */
    var toast by mutableStateOf("")
        private set

    fun dismissToast() {
        toast = ""
    }
}
