package com.trtc.uikit.aiconversationkit.manager;

import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.LISTENED;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.LISTENING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.SPEAKING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.THINKING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.THOUGHT;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TRTCCloudObserver extends TRTCCloudListener implements TRTCCloudListener.TRTCAudioFrameListener {
    private static final String TAG = "TRTCCloudObserver";

    private static final int AI_TEXT_DELAY_AFTER_AUDIO_MS = 1000;

    private final ConversationState mConversationState;
    private final Handler           mMainHandler    = new Handler(Looper.getMainLooper());
    private final TextComparator    mTextComparator = new TextComparator();

    public TRTCCloudObserver(ConversationState state) {
        mConversationState = state;
    }

    @Override
    public void onRecvCustomCmdMsg(String userId, int cmdID, int seq, byte[] message) {
        if (cmdID != 1 || message == null) {
            return;
        }
        JSONObject data;
        String type;
        try {
            data = new JSONObject(new String(message));
            type = data.getString("type");
        } catch (JSONException e) {
            Log.e(TAG, String.format("onRecvCustomCmdMsg JSONException : %s", e.getMessage()));
            return;
        }
        if (TextUtils.isEmpty(type)) {
            return;
        }
        if (TextUtils.equals("10001", type)) {
            handleAIStateData(data);
            return;
        }
        if (TextUtils.equals("10000", type)) {
            handleSpeechText(data);
            return;
        }
    }

    private void handleAIStateData(JSONObject data) {
        int state = -1;
        try {
            String sender = data.getString("sender");
            if (!TextUtils.equals(sender, mConversationState.aiRobotUserId)) {
                return;
            }
            JSONObject payload = data.getJSONObject("payload");
            state = payload.getInt("state");
        } catch (JSONException e) {
            Log.e(TAG, String.format("handleAIStateData JSONException : %s", e.getMessage()));
        }
        if (state == -1) {
            return;
        }
        switch (state) {
            case 1:
                mConversationState.aiStatus.set(LISTENING);
                ConversationState.SpeechText aiSpeechText = mConversationState.aiSpeechText.get();
                aiSpeechText.text = "";
                mConversationState.aiSpeechText.set(aiSpeechText);
                ConversationState.SpeechText localSpeechText = mConversationState.localSpeechText.get();
                localSpeechText.text = "";
                mConversationState.localSpeechText.set(localSpeechText);
                break;

            case 2:
                if (mConversationState.aiStatus.get() == LISTENING) {
                    mConversationState.aiStatus.set(LISTENED);
                }
                mConversationState.aiStatus.set(THINKING);
                break;

            case 3:
                if (mConversationState.aiStatus.get() == THINKING) {
                    mConversationState.aiStatus.set(THOUGHT);
                }
                mConversationState.aiStatus.set(SPEAKING);
                break;

            default:
                break;
        }
    }

    private void handleSpeechText(JSONObject data) {
        ConversationState.SpeechText speechText = new ConversationState.SpeechText();
        try {
            speechText.sender = data.getString("sender");
            JSONObject payload = data.getJSONObject("payload");
            speechText.roundId = payload.getString("roundid");
            speechText.text = payload.getString("text");
            speechText.isSpeechEnded = payload.getBoolean("end");
            if (TextUtils.equals(speechText.sender, mConversationState.aiRobotUserId)) {
                speechText.audioTimeStamp = payload.getLong("audio_timestamp");
            }
        } catch (JSONException e) {
            Log.e(TAG, String.format("handleSpeechText JSONException=%s data=%s", e.getMessage(), data.toString()));
        }
        if (TextUtils.equals(speechText.sender, mConversationState.localUserId)) {
            mConversationState.localSpeechText.set(speechText);
            return;
        }
        if (!mConversationState.aiSpeechTexts.contains(speechText)) {
            mConversationState.aiSpeechTexts.insert(speechText, mTextComparator);
            return;
        }
        ConversationState.SpeechText curText = mConversationState.aiSpeechTexts.find(speechText);
        if (curText.isSpeechEnded) {
            return;
        }
        mConversationState.aiSpeechTexts.change(speechText);
    }

    @Override
    public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
        for (TRTCCloudDef.TRTCVolumeInfo item : userVolumes) {
            if (TextUtils.equals(item.userId, mConversationState.aiRobotUserId)) {
                mConversationState.aiSpectrumData.set(item.spectrumData);
            }
            if (TextUtils.equals(item.userId, mConversationState.localUserId)) {
                mConversationState.localSpectrumData.set(item.spectrumData);
            }
        }
    }

    @Override
    public void onEnterRoom(long result) {
        super.onEnterRoom(result);
        Log.d(TAG, String.format("onEnterRoom result=%d", result));
    }

    @Override
    public void onRemoteUserAudioFrame(TRTCCloudDef.TRTCAudioFrame frame, String userId) {
        if (!TextUtils.equals(userId, mConversationState.aiRobotUserId)) {
            return;
        }
        List<ConversationState.SpeechText> textList = mConversationState.aiSpeechTexts.getList();
        int size = textList.size();
        long curTextTimeStamp = mConversationState.aiSpeechText.get().audioTimeStamp;
        for (int i = 0; i < size; i++) {
            ConversationState.SpeechText item = textList.get(i);
            if (item.audioTimeStamp > frame.timestamp) {
                continue;
            }
            if (curTextTimeStamp >= item.audioTimeStamp) {
                break;
            }
            if (frame.timestamp - item.audioTimeStamp > AI_TEXT_DELAY_AFTER_AUDIO_MS) {
                break;
            }
            mMainHandler.post(() -> mConversationState.aiSpeechText.set(item));
            break;
        }
    }

    @Override
    public void onRemoteUserEnterRoom(String userId) {
        Log.d(TAG, String.format("onRemoteUserEnterRoom userId=%s", userId));
    }

    @Override
    public void onRemoteUserLeaveRoom(String userId, int i) {
        Log.d(TAG, String.format("onRemoteUserLeaveRoom userId=%s", userId));
    }

    @Override
    public void onError(int errCode, String errMsg, Bundle extraInfo) {
        Log.d(TAG, String.format("onError errCode=%d errMsg=%s", errCode, errMsg));
    }


    @Override
    public void onCapturedAudioFrame(TRTCCloudDef.TRTCAudioFrame trtcAudioFrame) {
    }

    @Override
    public void onLocalProcessedAudioFrame(TRTCCloudDef.TRTCAudioFrame trtcAudioFrame) {

    }

    @Override
    public void onMixedPlayAudioFrame(TRTCCloudDef.TRTCAudioFrame trtcAudioFrame) {

    }

    @Override
    public void onMixedAllAudioFrame(TRTCCloudDef.TRTCAudioFrame trtcAudioFrame) {

    }

    @Override
    public void onVoiceEarMonitorAudioFrame(TRTCCloudDef.TRTCAudioFrame trtcAudioFrame) {

    }

    private static class TextComparator implements Comparator<ConversationState.SpeechText> {
        private static final int EQUAL = 0;

        @Override
        public int compare(ConversationState.SpeechText o1, ConversationState.SpeechText o2) {
            if (o1 == null || o2 == null) {
                return EQUAL;
            }
            return (int) (o2.audioTimeStamp - o1.audioTimeStamp);
        }
    }
}
