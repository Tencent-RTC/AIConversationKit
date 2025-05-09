//
//  AIConversationDefine.swift
//  AIConversationKit
//
//  Created by einhorn on 2025/2/13.
//

import UIKit
import Foundation
import RTCCommon
import TUICore

public typealias StartAIConversationParams = AIConversationDefine.StartAIConversationParams

let RTCubeBDID = "com.tencent.mrtc"
let TencentRTCBDID = "com.tencent.rtc.app"
public class AIConversationDefine {
    
    public class StartAIConversationParams: ConfigEncodable {
        public var secretId: String = ""                    // Required field
        public var secretKey: String = ""                   // Required field
        public var agentConfig: AgentConfig = AgentConfig() // Required field
        public var sttConfig: STTConfig?
        public var llmConfig: String = ""                   // Required field
        public var ttsConfig: String = ""                   // Required field
        public var region: String?
        public var roomId: String?
        public var denoise: Int?
        enum CodingKeys: String, CodingKey {
            case agentConfig = "AgentConfig"
            case sttConfig = "STTConfig"
            case llmConfig = "LLMConfig"
            case ttsConfig = "TTSConfig"
            case region = "Region"
            case sdkAppid = "SdkAppId"
            case roomId = "RoomId"
            case roomIdType = "RoomIdType"
        }
        
        public init() {
            self.agentConfig = AgentConfig()
            self.llmConfig = ""
            self.ttsConfig = ""
            self.sttConfig = STTConfig()
            self.region = "ap-beijing"
        }

        
        
        public func encode(to encoder: Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encodeIfPresent(agentConfig, forKey: .agentConfig)
            try container.encodeIfPresent(sttConfig, forKey: .sttConfig)
            try container.encodeIfPresent(llmConfig, forKey: .llmConfig)
            try container.encodeIfPresent(ttsConfig, forKey: .ttsConfig)
            try container.encodeIfPresent(roomId, forKey: .roomId)
            try container.encode(1, forKey: .roomIdType)
            try container.encodeIfPresent(TUILogin.getSdkAppID(), forKey: .sdkAppid)
        }
        
        required public init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            self.agentConfig = try container.decodeIfPresent(AgentConfig.self, forKey: .agentConfig) ?? AgentConfig()
            self.sttConfig = try container.decodeIfPresent(STTConfig.self, forKey: .sttConfig)
            self.llmConfig = try container.decodeIfPresent(String.self, forKey: .llmConfig) ?? ""
            self.ttsConfig = try container.decodeIfPresent(String.self, forKey: .ttsConfig) ?? ""
            self.roomId = try container.decode(String.self, forKey: .roomId)
            self.region = try container.decodeIfPresent(String.self, forKey: .region) ?? "ap-beijing"
        }
        
    }

    public class AgentConfig: ConfigEncodable {

        
        public var aiRobotId: String // Required field
        public var aiRobotSig: String // Required field
        public var welcomeMessage: String?
        public var maxIdleTime: Int?
        public var interruptMode: Int?
        public var interruptSpeechDuration: Int?
        public var turnDetectionMode: Int?
        public var welcomeMessagePriority: Int?
        public var filterOneWord: Bool?
        
        enum CodingKeys: String, CodingKey {
            case aiRobotId = "UserId"
            case aiRobotSig = "UserSig"
            case targetUserId = "TargetUserId"
            case targetUserSig = "targetUserSig"
            case welcomeMessage = "WelcomeMessage"
            case maxIdleTime = "MaxIdleTime"
            case interruptMode = "InterruptMode"
            case interruptSpeechDuration = "InterruptSpeechDuration"
            case turnDetectionMode = "TurnDetectionMode"
            case welcomeMessagePriority = "WelcomeMessagePriority"
            case filterOneWord = "FilterOneWord"
        }
        
        public init(aiRobotId: String = "",
                    aiRobotSig: String = "",
                    welcomeMessage: String = "",
                    maxIdleTime: Int = 60,
                    interruptMode: Int = 0,
                    interruptSpeechDuration: Int = 500,
                    turnDetectionMode: Int = 0,
                    welcomeMessagePriority: Int = 0,
                    filterOneWord: Bool = true) {
            self.aiRobotId = aiRobotId
            self.aiRobotSig = aiRobotSig
            self.welcomeMessage = welcomeMessage
            self.maxIdleTime = maxIdleTime
            self.interruptMode = interruptMode
            self.interruptSpeechDuration = interruptSpeechDuration
            self.turnDetectionMode = turnDetectionMode
            self.welcomeMessagePriority = welcomeMessagePriority
            self.filterOneWord = filterOneWord
        }
        
        public func encode(to encoder: Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encodeIfPresent(aiRobotId, forKey: .aiRobotId)
            try container.encodeIfPresent(aiRobotSig, forKey: .aiRobotSig)
            try container.encodeIfPresent(TUILogin.getUserID(), forKey: .targetUserId)
            try container.encodeIfPresent(welcomeMessage, forKey: .welcomeMessage)
            try container.encodeIfPresent(maxIdleTime, forKey: .maxIdleTime)
            try container.encodeIfPresent(interruptMode, forKey: .interruptMode)
            try container.encodeIfPresent(interruptSpeechDuration, forKey: .interruptSpeechDuration)
            try container.encodeIfPresent(turnDetectionMode, forKey: .turnDetectionMode)
            try container.encodeIfPresent(welcomeMessagePriority, forKey: .welcomeMessagePriority)
        }
        
        required public init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            self.aiRobotId = try container.decodeIfPresent(String.self, forKey: .aiRobotId) ?? ""
            self.aiRobotSig = try container.decodeIfPresent(String.self, forKey: .aiRobotSig) ?? ""
            self.welcomeMessage = try container.decodeIfPresent(String.self, forKey: .welcomeMessage)
            self.maxIdleTime = try container.decodeIfPresent(Int.self, forKey: .maxIdleTime)
            self.interruptMode = try container.decodeIfPresent(Int.self, forKey: .interruptMode)
            self.interruptSpeechDuration = try container.decodeIfPresent(Int.self, forKey: .interruptSpeechDuration)
            self.turnDetectionMode = try container.decodeIfPresent(Int.self, forKey: .turnDetectionMode)
            self.welcomeMessagePriority = try container.decodeIfPresent(Int.self, forKey: .welcomeMessagePriority)
            self.filterOneWord = try container.decodeIfPresent(Bool.self, forKey: .filterOneWord)
        }
        
        
        public static func generateDefaultConfig(aiRobotId: String, aiRobotSig: String) -> AgentConfig {
            let config = AgentConfig()
            config.aiRobotId = aiRobotId
            config.aiRobotSig = aiRobotSig
            return config
        }
    }

    public class STTConfig: ConfigEncodable {

        public var language: String?
        public var alternativeLanguage: [String]?
        public var customParam: String?
        public var vadSilenceTime: Int?
        public var hotWordList: String?
        
        enum CodingKeys: String, CodingKey {
            case language = "Language"
            case alternativeLanguage = "AlternativeLanguage"
            case customParam = "CustomParam"
            case vadSilenceTime = "VadSilenceTime"
            case hotWordList = "HotWordList"
        }
        
        
        public init(language: String = "zh",
                    alternativeLanguage: [String] = [],
                    customParam: String = "",
                    vadSilenceTime: Int = 1_000,
                    hotWordList: String = "") {
            self.language = language
            self.vadSilenceTime = vadSilenceTime
           
        }
        
        public func encode(to encoder: Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encodeIfPresent(language, forKey: .language)
            try container.encodeIfPresent(alternativeLanguage, forKey: .alternativeLanguage)
            try container.encodeIfPresent(customParam, forKey: .customParam)
            try container.encodeIfPresent(vadSilenceTime, forKey: .vadSilenceTime)
            try container.encodeIfPresent(hotWordList, forKey: .hotWordList)
        }
        
        public static func generateDefaultConfig() -> STTConfig {
            return STTConfig()
        }
    }

    public class LLMOpenAI: ConfigEncodable {
        public var apiKey: String // Required field
        public var apiUrl: String // Required field
        public var llmType: String
        public var model: String
        public var streaming: Bool
        public var systemPrompt: String
        
        enum CodingKeys: String, CodingKey {
            case apiKey = "APIKey"
            case apiUrl = "APIUrl"
            case llmType = "LLMType"
            case model = "Model"
            case streaming = "Streaming"
            case systemPrompt = "SystemPrompt"
        }
        
        public init(apiKey: String = "",
                    apiUrl: String = "",
                    llmType: String = "openai",
                    model: String = "abab6.5s-chat",
                    streaming: Bool = true,
                    systemPrompt: String = "") {
            self.apiKey = apiKey
            self.apiUrl = apiUrl
            self.llmType = llmType
            self.model = model
            self.streaming = streaming
            self.systemPrompt = systemPrompt
        }
        
        public func encode(to encoder: Encoder) throws {

            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encodeIfPresent(apiKey, forKey: .apiKey)
            try container.encodeIfPresent(apiUrl, forKey: .apiUrl)
            try container.encodeIfPresent(llmType, forKey: .llmType)
            try container.encodeIfPresent(model, forKey: .model)
            try container.encodeIfPresent(streaming, forKey: .streaming)
            try container.encodeIfPresent(systemPrompt, forKey: .systemPrompt)
        }
        
        public static func generateDefaultConfig(apiKey: String, apiUrl: String) -> String {
            let llmOpenAI = LLMOpenAI(apiKey: apiKey, apiUrl: apiUrl)
            let encoder = JSONEncoder()
            encoder.outputFormatting = .prettyPrinted
            if let jsonData = try? encoder.encode(llmOpenAI),
               let jsonString = String(data: jsonData, encoding: .utf8) {
                return jsonString
            }
            return ""
        }
    }

}

public protocol ConfigEncodable: Encodable, Codable {}
