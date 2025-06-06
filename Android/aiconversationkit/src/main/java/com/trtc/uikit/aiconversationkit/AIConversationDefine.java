package com.trtc.uikit.aiconversationkit;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class AIConversationDefine {
    public static final String KEY_START_AI_CONVERSATION = "KEY_START_AI_CONVERSATION";

    public static class StartAIConversationParams implements Serializable {
        public String      secretId  = ""; // Required field
        public String      secretKey = ""; // Required field
        public AgentConfig agentConfig; // Required field
        public STTConfig   sttConfig = STTConfig.generateDefaultConfig();
        public String      llmConfig = ""; // Required field
        public String      ttsConfig = ""; // Required field
        public String      region    = "ap-beijing";
        public String      roomId    = "";
        public Denoise     denoise   = Denoise.MODERATE;
    }

    public static class AgentConfig implements Serializable {
        public String  aiRobotId               = ""; // Required field
        public String  aiRobotSig              = ""; // Required field
        public String  welcomeMessage          = "";
        public int     maxIdleTime             = 60;
        public int     interruptMode           = 0;
        public int     interruptSpeechDuration = 500;
        public int     turnDetectionMode       = 0;
        public int     welcomeMessagePriority  = 0;
        public boolean filterOneWord           = true;

        public static AgentConfig generateDefaultConfig(String aiRobotId, String aiRobotSig) {
            AgentConfig config = new AgentConfig();
            config.aiRobotId = aiRobotId;
            config.aiRobotSig = aiRobotSig;
            return config;
        }
    }

    public static class STTConfig implements Serializable {
        public String       language            = "zh";
        public List<String> alternativeLanguage = new LinkedList<>();
        public String       customParam         = "";
        public int          vadSilenceTime      = 1000;
        public String       hotWordList         = "";

        public static STTConfig generateDefaultConfig() {
            return new STTConfig();
        }
    }

    public enum Denoise {
        LIGHT(0),
        MILD(1),
        MODERATE(2),
        HIGH(3),
        STRONG(4);

        private final int mValue;

        Denoise(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static Denoise fromInt(int value) {
            for (Denoise denoise : Denoise.values()) {
                if (denoise.mValue == value) {
                    return denoise;
                }
            }
            return MODERATE;
        }
    }
}
