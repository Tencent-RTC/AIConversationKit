package com.trtc.uikit.aiconversationkit.manager;

import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.LISTENED;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.LISTENING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.SPEAKING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.THINKING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.THOUGHT;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TRTCCloudObserver extends TRTCCloudListener implements TRTCCloudListener.TRTCAudioFrameListener {
    private static final String TAG = "TRTCCloudObserver";
    private final ConversationState mConversationState;

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
            Log.e(TAG, "onRecvCustomCmdMsg JSONException");
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
            Log.w(TAG, "handleAIStateData JSONException");
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
        } catch (JSONException e) {
            Log.w(TAG, "handleSpeechText JSONException");
        }
        if (TextUtils.equals(speechText.sender, mConversationState.localUserId)) {
            mConversationState.localSpeechText.set(speechText);
        } else if (TextUtils.equals(speechText.sender, mConversationState.aiRobotUserId)) {
            mConversationState.aiSpeechText.set(speechText);
        }
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
}
