//
//  AIConversationStoreImpl.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import Foundation
import AVFoundation
import RTCCommon
import TXLiteAVSDK_Professional
import TUICore

class AIConversationStoreImpl: AIConversationStore {
    
    private var trtcCloud: TRTCCloud?
    private var request: AIConversationRequest?
    private var roomId: String = ""
    private var taskId: String?
    private var aiRobotUserId: String = ""
    private var interruptMode: Int = 0
    private var experimentalParams: String?
    private lazy var delegateProxy = TRTCDelegateProxy(store: self)
    
    private enum ErrorCode {
        static let invalidParameter = -1001
        static let permissionDenied = -1003
    }
    
    // MARK: - Public API
    
    override func startAIConversation(config: AIConversationConfig, completion: CompletionHandler?) {
        state.update { $0.aiStatus = .initializing }
        
        let userId = TUILogin.getUserID() ?? ""
        let userSig = TUILogin.getUserSig() ?? ""
        let sdkAppId = TUILogin.getSdkAppID()
        
        guard !userId.isEmpty, !userSig.isEmpty, sdkAppId > 0 else {
            Logger.error("startConversation failed: invalid login info, userId:\(userId) sdkAppId:\(sdkAppId)")
            state.update { $0.aiStatus = .offline }
            completion?(ErrorCode.invalidParameter, "invalid login info")
            return
        }
        
        let hash = userId.utf8.reduce(0) { $0 + Int($1) }
        let seed = UInt64(Date().timeIntervalSince1970) + UInt64(hash)
        srand48(Int(seed))
        let currentRoomId = String(Int(drand48() * Double(Int32.max)))
        
        Logger.info("startConversation sdkAppId:\(sdkAppId)"
                    + " userId:\(userId) aiRobotId:\(config.agentConfig?.aiRobotId ?? "")")
        
        experimentalParams = config.experimentalParams
        
        requestMicPermission { [weak self] granted in
            guard let self else { return }
            if granted {
                self.enterRoom(sdkAppId: sdkAppId, roomId: currentRoomId, userId: userId, userSig: userSig)
                
                self.request = PackageService.isInternalDemo ? ServerAIConversationRequest() : ClientAIConversationRequest()
                self.request?.config = config
                self.request?.roomId = currentRoomId
                self.request?.start { [weak self] taskID, robotId in
                    guard let self else { return }
                    self.taskId = taskID
                    if let robotId {
                        self.aiRobotUserId = robotId
                    }
                }
                
                self.openLocalAudio()
                
                self.roomId = currentRoomId
                self.interruptMode = config.agentConfig?.interruptMode ?? 0
                self.aiRobotUserId = config.agentConfig?.aiRobotId ?? ""
                
                if PackageService.isInternalDemo {
                    ConversationManager.shared.fetchFeedback()
                    ConversationManager.shared.startExperienceDurationMonitor()
                }
                
                completion?(0, "")
            } else {
                self.state.update { $0.aiStatus = .offline }
                completion?(ErrorCode.permissionDenied, "mic permission denied")
            }
        }
    }
    
    override func stopAIConversation(completion: CompletionHandler?) {
        Logger.info("stopConversation taskId:\(taskId ?? "nil")")
        trtcCloud?.stopLocalAudio()
        request?.stop(taskID: taskId ?? "")
        ConversationManager.shared.invalidateTimer()
        trtcCloud?.exitRoom()
        resetState()
        completion?(0, "")
    }
    
    override func interruptSpeech() {
        guard !aiRobotUserId.isEmpty else { return }
        let userId = TUILogin.getUserID() ?? ""
        let sdkAppId = TUILogin.getSdkAppID()
        let timestamp = String(Int(Date().timeIntervalSince1970))
        let dict: [String: Any] = [
            "type": Constants.interruptMessageType,
            "sender": userId,
            "receiver": [aiRobotUserId],
            "payload": [
                "id": "\(sdkAppId)_\(roomId)",
                "timestamp": timestamp,
            ],
        ]
        Logger.info("interruptAI, dict: \(dict)")
        guard let jsonData = try? JSONSerialization.data(withJSONObject: dict) else { return }
        trtcCloud?.sendCustomCmdMsg(Constants.customCmdId, data: jsonData, reliable: true, ordered: true)
        
        clearRemoteAudioBuffer()
    }
    
    override func openLocalMicrophone(completion: CompletionHandler?) {
        Logger.info("muteLocalAudio: false")
        trtcCloud?.muteLocalAudio(false)
        state.update { $0.isMicOpened = true }
        completion?(0, "")
    }
    
    override func closeLocalMicrophone() {
        Logger.info("muteLocalAudio: true")
        trtcCloud?.muteLocalAudio(true)
        state.update { $0.isMicOpened = false }
    }
}

// MARK: - Constants

private extension AIConversationStoreImpl {
    
    enum Constants {
        static let interruptMessageType = 20_001
        static let customCmdId: Int = 0x2
    }
}

// MARK: - TRTC Setup

private extension AIConversationStoreImpl {
    
    func enterRoom(sdkAppId: Int32, roomId: String, userId: String, userSig: String) {
        trtcCloud = TRTCCloud.sharedInstance()
        trtcCloud?.addDelegate(delegateProxy)
        
        updateAudioConfig()
        
        let param = TRTCParams()
        param.sdkAppId = UInt32(sdkAppId)
        if PackageService.isInternalDemo {
            param.roomId = UInt32(roomId) ?? 0
        } else {
            param.strRoomId = roomId
        }
        param.userId = userId
        param.userSig = userSig
        trtcCloud?.enterRoom(param, appScene: .audioCall)
    }
    
    func openLocalAudio() {
        configureAudioSession()
        let volumeParam = TRTCAudioVolumeEvaluateParams()
        volumeParam.enableSpectrumCalculation = true
        volumeParam.interval = 100
        volumeParam.enableVadDetection = true
        trtcCloud?.enableAudioVolumeEvaluation(true, with: volumeParam)
        trtcCloud?.startLocalAudio(.speech)
        trtcCloud?.setAudioRoute(.modeSpeakerphone)
        state.update { $0.isMicOpened = true }
    }
    
    func updateAudioConfig() {
        setExperimentConfig(key: "setFramework",
                            params: ["component": 25, "framework": 1, "language": 2])
        setExperimentConfig(key: "enableAIDenoise", params: ["enable": true])
        setExperimentConfig(key: "setPrivateConfig",
                            params: ["configs": ["key": "Liteav.Audio.common.ans.version",
                                                 "default": "0",
                                                 "value": "4"]])
        setExperimentConfig(key: "setAudioAINSStyle", params: ["style": 4])
        setExperimentConfig(key: "enableAudioAGC", params: ["enable": false])
        trtcCloud?.setAudioRoute(.modeSpeakerphone)
        trtcCloud?.setSystemVolumeType(.media)
        setExperimentConfig(key: "setLocalAudioMuteMode", params: ["mode": 0])
        
        setExperimentConfig(key: "setPrivateConfig",
                            params: ["configs": ["key": "Liteav.Audio.common.ains.near.field.threshold",
                                                 "default": "50",
                                                 "value": "0"]])
    }
    
    func clearRemoteAudioBuffer() {
        setExperimentConfig(key: "pauseRemoteAudioStream", params: ["pause": 0, "maxCacheTimeInMs": 0])
    }
    
    func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, options: [.defaultToSpeaker, .allowBluetooth])
            try session.setActive(true)
        } catch {
            Logger.error("configureAudioSession failed: \(error.localizedDescription)")
        }
    }
    
    func setExperimentConfig(key: String, params: [String: Any]) {
        let json: [String: Any] = ["api": key, "params": params]
        guard let data = try? JSONSerialization.data(withJSONObject: json),
              let str = String(data: data, encoding: .utf8) else { return }
        trtcCloud?.callExperimentalAPI(str)
    }
}

// MARK: - Private Helpers

private extension AIConversationStoreImpl {
    
    var currentTimestamp: Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
    
    func requestMicPermission(completion: @escaping (Bool) -> Void) {
        let mainCallback = { (granted: Bool) in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
        switch AVAudioSession.sharedInstance().recordPermission {
        case .granted:
            mainCallback(true)
        case .denied:
            mainCallback(false)
        case .undetermined:
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                mainCallback(granted)
            }
        @unknown default:
            mainCallback(false)
        }
    }
    
    func resetState() {
        trtcCloud?.removeDelegate(delegateProxy)
        trtcCloud = nil
        request = nil
        taskId = nil
        roomId = ""
        aiRobotUserId = ""
        interruptMode = 0
        experimentalParams = nil
        
        state.update {
            $0.conversationMessageList = []
            $0.aiStatus = .offline
            $0.isMicOpened = false
        }
        AIConversationState.instance.expireDuraSec.value = AIConversationState.defaultExpireDuration
    }
}

// MARK: - Message Constants & Handling

private extension AIConversationStoreImpl {
    
    enum MessageType {
        static let text    = 10_000
        static let status  = 10_001
        static let latency = 10_020
        static let error   = 10_030
    }
    
    func handleMessage(_ data: [String: Any]) {
        guard let type = data["type"] as? Int,
              let payload = data["payload"] as? [String: Any] else { return }
        let sender = data["sender"] as? String ?? ""
        
        switch type {
        case MessageType.text:
            handleSpeechText(sender: sender, payload: payload)
        case MessageType.status:
            handleAIStateData(sender: sender, payload: payload)
        case MessageType.latency:
            Logger.info("[aic-api] AI server latency:\(data)")
        case MessageType.error:
            Logger.error("AI server error:\(data)")
        default:
            break
        }
    }
    
    func handleAIStateData(sender: String, payload: [String: Any]) {
        guard sender != TUILogin.getUserID(),
              let stateVal = payload["state"] as? Int,
              let aiStatus = AIStatus(rawValue: stateVal) else { return }
        state.update { $0.aiStatus = aiStatus }
    }
    
    func handleSpeechText(sender: String, payload: [String: Any]) {
        let text = payload["text"] as? String ?? ""
        let roundId = payload["roundid"] as? String ?? ""
        let isEnd = payload["end"] as? Bool ?? false
        let isUser = sender == TUILogin.getUserID()
        let isCompleted = text.isEmpty ? isEnd : false
        
        guard !roundId.isEmpty else { return }
        
        state.update { state in
            let existingIndex = state.conversationMessageList.lastIndex(where: { $0.roundId == roundId })
            
            if let idx = existingIndex {
                var updated = state.conversationMessageList[idx]
                if isUser {
                    if !text.isEmpty { updated.userSpeechText = text }
                } else {
                    updated.aiSpeechText += text
                    if text.isEmpty { updated.isCompleted = isCompleted }
                }
                state.conversationMessageList[idx] = updated
            } else {
                let newMsg = ConversationMessage(
                    roundId: roundId,
                    userSpeechText: isUser ? text : "",
                    aiSpeechText: isUser ? "" : text,
                    timestamp: currentTimestamp,
                    isCompleted: isCompleted
                )
                state.conversationMessageList.append(newMsg)
            }
        }
    }
}

// MARK: - TRTCCloudDelegate Proxy

extension AIConversationStoreImpl {
    
    fileprivate class TRTCDelegateProxy: NSObject, TRTCCloudDelegate {
        weak var store: AIConversationStoreImpl?
        
        init(store: AIConversationStoreImpl) {
            self.store = store
        }
        
        func onEnterRoom(_ result: Int) {
            Logger.info("aiconversation--enterroom:\(result)")
            if result < 0 {
                Logger.error("enterRoom failed, error:\(result)")
                store?.state.update { $0.aiStatus = .offline }
            }
        }
        
        func onExitRoom(_ reason: Int) {
            store?.handleExitRoom(reason)
        }
        
        func onRemoteUserEnterRoom(_ userId: String) {
            Logger.info("onRemoteUserEnterRoom userId=\(userId)")
        }
        
        func onRemoteUserLeaveRoom(_ userId: String, reason: Int) {
            Logger.info("onRemoteUserLeaveRoom userId=\(userId) reason=\(reason)")
            if let robotId = store?.aiRobotUserId, !robotId.isEmpty, userId == robotId {
                store?.state.update { $0.aiStatus = .offline }
            }
        }
        
        func onError(_ errCode: TXLiteAVError, errMsg: String?, extInfo: [AnyHashable: Any]?) {
            Logger.error("onError errCode=\(errCode.rawValue) errMsg=\(errMsg ?? "")")
        }
        
        func onRecvCustomCmdMsgUserId(_ userId: String, cmdID: Int, seq: UInt32, message: Data) {
            guard cmdID == 1,
                  let json = try? JSONSerialization.jsonObject(with: message) as? [String: Any] else { return }
            store?.handleMessage(json)
        }
    }
    
    fileprivate func handleExitRoom(_ reason: Int) {
        Logger.info("onExitTRTCRoom, reason:\(reason)")
        if reason != 0 {
            ConversationManager.shared.invalidateTimer()
        }
        resetState()
    }
}
