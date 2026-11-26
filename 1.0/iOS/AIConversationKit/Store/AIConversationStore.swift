//
//  AIConversationStore.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import Foundation
import RTCCommon
import TUICore

public enum AIStatus: Int, Equatable {
    case initializing = 0
    case listening = 1
    case thinking = 2
    case speaking = 3
    case interrupted = 4
    case completed = 5
    case offline = 6
}

public typealias CompletionHandler = (_ code: Int, _ message: String) -> Void

public struct AgentConfig: Codable {
    public var aiRobotId: String = ""
    public var aiRobotSig: String = ""
    public var interruptMode: Int?
    public var welcomeMessage: String?
    public var interruptSpeechDuration: Int?
    public var turnDetectionMode: Int?
    
    enum CodingKeys: String, CodingKey {
        case aiRobotId = "UserId"
        case aiRobotSig = "UserSig"
        case interruptMode = "InterruptMode"
        case welcomeMessage = "WelcomeMessage"
        case interruptSpeechDuration = "InterruptSpeechDuration"
        case turnDetectionMode = "TurnDetectionMode"
    }
    
    public init(aiRobotId: String = "",
                aiRobotSig: String = "",
                interruptMode: Int? = 0,
                welcomeMessage: String? = nil,
                interruptSpeechDuration: Int? = 500,
                turnDetectionMode: Int? = nil) {
        self.aiRobotId = aiRobotId
        self.aiRobotSig = aiRobotSig
        self.interruptMode = interruptMode
        self.welcomeMessage = welcomeMessage
        self.interruptSpeechDuration = interruptSpeechDuration
        self.turnDetectionMode = turnDetectionMode
    }
}

public struct STTConfig: Codable {
    public var language: String?
    public var vadLevel: Int?
    public var vadSilenceTime: Int?
    
    enum CodingKeys: String, CodingKey {
        case language = "Language"
        case vadLevel = "VadLevel"
        case vadSilenceTime = "VadSilenceTime"
    }
    
    public init(language: String = "zh",
                vadLevel: Int = 2,
                vadSilenceTime: Int? = 1000) {
        self.language = language
        self.vadLevel = vadLevel
        self.vadSilenceTime = vadSilenceTime
    }
}

public struct AIConversationConfig: Codable {
    public var secretId: String = ""
    public var secretKey: String = ""
    public var agentConfig: AgentConfig? = AgentConfig()
    public var sttConfig: STTConfig? = STTConfig()
    public var llmConfig: String?
    public var ttsConfig: String?
    public var region: String?
    public var experimentalParams: String?
    
    enum CodingKeys: String, CodingKey {
        case agentConfig = "AgentConfig"
        case sttConfig = "STTConfig"
        case llmConfig = "LLMConfig"
        case ttsConfig = "TTSConfig"
    }
    
    public init(secretId: String = "",
                secretKey: String = "",
                agentConfig: AgentConfig? = AgentConfig(),
                sttConfig: STTConfig? = STTConfig(),
                llmConfig: String? = nil,
                ttsConfig: String? = nil,
                region: String? = "ap-beijing",
                experimentalParams: String? = nil) {
        self.secretId = secretId
        self.secretKey = secretKey
        self.agentConfig = agentConfig
        self.sttConfig = sttConfig
        self.llmConfig = llmConfig
        self.ttsConfig = ttsConfig
        self.region = region
        self.experimentalParams = experimentalParams
    }
}

public struct ConversationMessage: Equatable {
    public let roundId: String
    public var userSpeechText: String = ""
    public var aiSpeechText: String = ""
    public var timestamp: Int64 = 0
    public var isCompleted: Bool = false
    
    public init(roundId: String,
                userSpeechText: String = "",
                aiSpeechText: String = "",
                timestamp: Int64 = 0,
                isCompleted: Bool = false) {
        self.roundId = roundId
        self.userSpeechText = userSpeechText
        self.aiSpeechText = aiSpeechText
        self.timestamp = timestamp
        self.isCompleted = isCompleted
    }
}

public struct ConversationState: Equatable {
    public var conversationMessageList: [ConversationMessage] = []
    public var aiStatus: AIStatus = .offline
    public var isMicOpened: Bool = false
}

public class AIConversationStore {
    
    public static let shared: AIConversationStore = AIConversationStoreImpl()
    
    public private(set) var state: ObservableState<ConversationState>
    
    init() {
        state = ObservableState<ConversationState>(initialState: ConversationState())
    }
    
    public func startAIConversation(config: AIConversationConfig, completion: CompletionHandler?) {}
    public func stopAIConversation(completion: CompletionHandler?) {}
    public func interruptSpeech() {}
    public func openLocalMicrophone(completion: CompletionHandler?) {}
    public func closeLocalMicrophone() {}
}
