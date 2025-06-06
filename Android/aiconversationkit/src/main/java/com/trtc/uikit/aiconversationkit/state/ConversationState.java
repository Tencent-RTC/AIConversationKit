package com.trtc.uikit.aiconversationkit.state;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.TUILogin;
import com.trtc.tuikit.common.livedata.LiveData;
import com.trtc.tuikit.common.livedata.LiveListData;

public class ConversationState {
    public String strRoomId          = "";
    public String conversationTaskId = "";
    public String aiRobotUserId      = "";
    public String localUserId        = TUILogin.getUserId();

    public LiveListData<SpeechText> aiSpeechTexts     = new LiveListData<>();
    public LiveData<SpeechText>     aiSpeechText      = new LiveData<>(new SpeechText());
    public LiveData<AIStatus>       aiStatus          = new LiveData<>(AIStatus.INITIALIZING);
    public LiveData<float[]>        aiSpectrumData    = new LiveData<>(new float[0]);
    public LiveData<SpeechText>     localSpeechText   = new LiveData<>(new SpeechText());
    public LiveData<float[]>        localSpectrumData = new LiveData<>(new float[0]);

    public LiveData<Boolean> isAudioOpened   = new LiveData<>(false);
    public LiveData<Boolean> isAudioMuted    = new LiveData<>(true);
    public LiveData<Boolean> isSpeakerOpened = new LiveData<>(true);
    public LiveData<Boolean> isPaused        = new LiveData<>(false);

    public boolean           isNeedFeedback           = false;
    public LiveData<Integer> remainingExperienceTimeS = new LiveData<>(10 * 60);

    public LiveData<Integer> interruptMode  = new LiveData<>(0);
    public LiveData<String>  welcomeMessage = new LiveData<>("");

    public static class SpeechText {
        public String  roundId        = "";
        public String  sender         = "";
        public String  text           = "";
        public long    audioTimeStamp = 0L;
        public boolean isSpeechEnded  = false;

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof SpeechText) {
                return TextUtils.equals(this.roundId, ((SpeechText) obj).roundId);
            }
            return false;
        }
    }

    public enum AIStatus {
        INITIALIZING,
        LISTENING,
        LISTENED,
        THINKING,
        THOUGHT,
        SPEAKING
    }
}
