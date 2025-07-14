//
//  AIConversationManager.swift
//  AFNetworking
//
//  Created by einhorn on 2024/10/22.
//

import UIKit
import TXLiteAVSDK_Professional
import TUICore

class AIConversationManager: NSObject {
    var trtcCloud = TRTCCloud.sharedInstance()
    var timer: Timer?
    var robotId: String?
    var roomId: Int?
    var updatetimerCount = 0
    var taskId: String?
    var startAIParams: StartAIConversationParams?
    var request: AIConversationRequest? = nil
    
    static let instance = AIConversationManager()

    func enableAIDenoise() {
        setExperimentConfig(key: "enableAIDenoise", params:["enable": true,])
        
        setExperimentConfig(key: "setPrivateConfig",
                            params:["configs":
                                            ["key":"Liteav.Audio.common.ans.version",
                                             "default":"0",
                                             "value":"4",],
                                        ])
        setExperimentConfig(key: "setPrivateConfig",
                            params:["configs":
                                            ["key":"Liteav.Audio.common.ains.near.field.threshold",
                                             "default":"50",
                                             "value":"\(startAIParams?.denoise ?? 50)",],
                                        ])
        setExperimentConfig(key: "setAudioAINSStyle", params:["style": "4",])
        setExperimentConfig(key: "enableAudioAGC", params:["enable": false,])
        setExperimentConfig(key: "setLocalAudioMuteMode", params: ["mode": 0,])
    }
   
    
    func start(aiParams: StartAIConversationParams?) {
        setExperimentConfig(key: "setFramework", params:["component":25 ,"framework": 1, "language":3,])
        startAIParams = aiParams
        aiParams?.roomId = TUILogin.getUserID()
        setUpTRTC(withSDKAppId: TUILogin.getSdkAppID(),
                  roomId: aiParams?.roomId,
                  userId: TUILogin.getUserID(),
                  userSig:TUILogin.getUserSig())
        
  
        if !isAppStoreDemo() {
            request = ClientAIConversationRequest()
        } else {
            request = ServerAIConversationRequest()
            
        }
        request?.startAiConversationParams = aiParams
        request?.start { [weak self] taskID in
            guard let self = self else { return }
            print("AITastkID+\(taskID)")
            self.taskId = taskID
        }
        request?.fetchFeedBack()
        request?.fetchExperienceDuration()
        AIConversationState.instance.speakerIsOpen.value = true
        AIConversationState.instance.audioMuted.value = false
        AIConversationState.instance.conversationState.value = .start
        AIConversationManager.instance.roomId = roomId
        enableAIDenoise()
    }
    
    func setUpTRTC(withSDKAppId sdkappId: Int32?,  roomId: String?, userId: String?, userSig: String?) {
        let trtcParam = TRTCParams()
        trtcParam.sdkAppId = UInt32(sdkappId ?? 0)
        if (isAppStoreDemo()) {
            trtcParam.roomId = UInt32(roomId ?? "") ?? 0
        } else {
            trtcParam.strRoomId = roomId ?? ""
        }
        trtcParam.userId = userId ?? ""
        trtcParam.userSig = userSig ?? ""
        trtcCloud.enterRoom(trtcParam, appScene: .audioCall)
        trtcCloud.startLocalAudio(.speech)
        trtcCloud.setSystemVolumeType(.media)
        trtcCloud.addDelegate(self)
        trtcCloud.setAudioFrameDelegate(self)
        let volumeEvalParam = TRTCAudioVolumeEvaluateParams()
        volumeEvalParam.enableSpectrumCalculation = true
        volumeEvalParam.interval = 100
        volumeEvalParam.enableVadDetection = true
        trtcCloud.enableAudioVolumeEvaluation(true, with: volumeEvalParam)
        trtcCloud.setAudioRoute(.modeSpeakerphone)
    }
    
    func startCountTime() {
        if AIConversationManager.instance.timer == nil {
            AIConversationManager.instance.timer = Timer.scheduledTimer(timeInterval: 1.0,
                                                                        target: self,
                                                                        selector: #selector(updateExpDuration),
                                                                        userInfo: nil,
                                                                        repeats: true)
        }
      
    }
    
    func stop() {
        request?.stop(taskID: taskId ?? "")
        AIConversationState.instance.conversationState.value = .stop
        AIConversationManager.instance.timer?.invalidate()
        AIConversationManager.instance.timer = nil
        AIConversationManager.instance.trtcCloud.exitRoom()
    }
    
    func resume() {
        AIConversationState.instance.conversationState.value = .start
        trtcCloud.callExperimentalAPI("{\"api\":\"pauseRemoteAudioStream\",\"params\":{\"pause\":\(0), \"maxCacheTimeInMs\":\(0)}}")
    }
    
    func pause() {
        AIConversationState.instance.conversationState.value = .pause
        let maxCacheTimeInMs = 60 * 1_000
        trtcCloud.callExperimentalAPI("{\"api\":\"pauseRemoteAudioStream\",\"params\":{\"pause\":\(1), \"maxCacheTimeInMs\":\(maxCacheTimeInMs)}}")
    }
    
    func interuptAI() {
        if AIConversationState.instance.conversationState.value == .pause {
            return
        }
        let cmdId = 0x2
        let timestamp = Int(Date().timeIntervalSince1970 * 1_000)
        let payload = [
            "id": TUILogin.getUserID() ?? "" +
            "\(String(describing: AIConversationManager.instance.roomId))" +
            "\(timestamp)", 
            "timestamp": timestamp,
        ] as [String : Any]
        let dict = [
            "type": 20_001,
            "sender": TUILogin.getUserID() ?? "",
            "receiver": [AIConversationManager.instance.robotId],
            "payload": payload,
        ] as [String : Any]
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
            trtcCloud.sendCustomCmdMsg(cmdId, data: jsonData, reliable: true, ordered: true)
        } catch {
            print("Error serializing dictionary to JSON: \(error)")
        }
    }
    
    func muteLocalAudio(_ mute: Bool) {
        trtcCloud.muteLocalAudio(mute)
        AIConversationState.instance.audioMuted.value = mute
        
    }
    
    func openSpeaker(isOpen open: Bool) {
        if open == true {
            trtcCloud.setAudioRoute(.modeSpeakerphone)
            AIConversationState.instance.speakerIsOpen.value = true
        } else {
            trtcCloud.setAudioRoute(.modeEarpiece)
            AIConversationState.instance.speakerIsOpen.value = false
        }
       
    }
}



extension AIConversationManager: TRTCCloudDelegate {
    func onEnterRoom(_ result: Int) {
        if result >= 0 {
            if isAppStoreDemo() {
                startCountTime()
            }
        }
        debugPrint("aiconversation--enterroom:\(result)")
    }
    
    
    func setAiLangugage(lang: AILanguageType) {
        AIConversationState.instance.aiLang.value = lang
    }
    
    func setEntireMark(mark: Int) {
        AIConversationState.instance.entiretyMark.value = mark
    }
    
    func setCallDelayMark(mark: Int) {
        AIConversationState.instance.callDelayMark.value = mark
    }
    
    func setNoiseReduceMark(mark: Int) {
        AIConversationState.instance.noiseReduceMark.value = mark
    }
    
    func setAiMark(mark: Int) {
        AIConversationState.instance.aiMark.value = mark
    }
    
    
    func setInteractionMark(mark: Int) {
        AIConversationState.instance.interactionMark.value = mark
    }
    
    func setCommentText(text: String) {
        AIConversationState.instance.commentText.value = text
    }
    
    func onRecvCustomCmdMsgUserId(_ userId: String, cmdID: Int, seq: UInt32, message: Data) {
        if cmdID == 1 {
            do {
                if let jsonObject = try JSONSerialization.jsonObject(with: message, options: []) as? [String: Any] {
                    print("Dictionary: \(jsonObject)")
                    handleMessage(jsonObject)
                } else {
                    print("The data is not a dictionary.")
                }
            } catch {
                print("Error parsing JSON: \(error)")
            }
        }
    }
}

extension AIConversationManager {
    
    func handleMessage(_ data: [String: Any]) {
        guard let type = data["type"] as? Int else { return }
        let sender = String(describing: data["sender"] ?? "")
        guard let paylod = data["payload"] as? [String: Any] else { return }
        if type == 10_000 {
            let text = String(describing: paylod["text"] ?? "")
            if sender == TUILogin.getUserID() {
                AIConversationState.instance.userSubtitle.value = text
            } else {
                AIConversationState.instance.robotSubtitle.value = text
            }
        } else if type == 10_001 {
            if AIConversationState.instance.conversationState.value == .pause {
                return
            }
            guard let state = paylod["state"] as? Int else { return }
            switch state {
            case 1:
                updateAiStatus(status: .listening)
                AIConversationState.instance.robotSubtitle.value = AIConversationLocalize("AIConversation.AIReply.listening")
            case 2:
                updateAiStatus(status: .thinking)
                AIConversationState.instance.robotSubtitle.value = AIConversationLocalize("AIConversation.AIReply.thinking")
            case 3:
                updateAiStatus(status: .replying)
            case 4:
                updateAiStatus(status: .interrupted)
            default:
                updateAiStatus(status: .undefined)
            }
            
        }
    }
    
    func updateAiStatus(status: RobotState) {
        AIConversationState.instance.aiState.value = status
    }
}

extension AIConversationManager {

}

extension AIConversationManager {
    @objc func updateExpDuration() {
        updatetimerCount += 1
        if updatetimerCount % 10 == 0 {
            TUICore.callService("TUICore_AIConversationSevice",
                                method: "TUICore_AIConversationSevice_Time_Deduction",
                                param: [:]) { code, errMsg, result in
                if code == 0 {
                    let time = result["time"] as? Int
                    debugPrint("AI-hearbeat--time\(time ?? 0)")
                }
            }
        }
        let seconds = AIConversationState.instance.expireDuraSec.value
        if seconds > 0 {
            AIConversationState.instance.expireDuraSec.value = seconds - 1
        } else {
            AIConversationManager.instance.timer?.invalidate()
            AIConversationManager.instance.timer = nil
        }
    }
}

extension AIConversationManager: TRTCAudioFrameDelegate {
    func onCapturedAudioFrame(_ frame: TRTCAudioFrame) {
        let length = frame.data.count / MemoryLayout<Int16>.size
        var shortArray = [Int16](repeating: 0, count: length)
        
        frame.data.withUnsafeBytes { rawBufferPointer in
            let bufferPointer = rawBufferPointer.bindMemory(to: Int16.self)
            for i in 0..<length {
                shortArray[i] = bufferPointer[i]
            }
        }
        AIConversationState.instance.userSpectrumData.value = shortArray.map { Float($0) }
    }
    
    func onRemoteUserAudioFrame(_ frame: TRTCAudioFrame, userId: String) {
        if userId == AIConversationManager.instance.robotId {
            let length = frame.data.count / MemoryLayout<Int16>.size
            var shortArray = [Int16](repeating: 0, count: length)
            
            frame.data.withUnsafeBytes { rawBufferPointer in
                let bufferPointer = rawBufferPointer.bindMemory(to: Int16.self)
                for i in 0..<length {
                    shortArray[i] = bufferPointer[i]
                }
            }
            AIConversationState.instance.aiSpectrumData.value = shortArray.map { Float($0)}
            
        }
    }
}

extension AIConversationManager {
    func setExperimentConfig(key: String, params: [String: Any]) {
        let json: [String: Any] = [
            "api": key,
            "params": params,
        ]
        
        if let jsonData = try? JSONSerialization.data(withJSONObject: json, options: []),
           let jsonStr = String(data: jsonData, encoding: .utf8) {
            AIConversationManager.instance.trtcCloud.callExperimentalAPI(jsonStr)
        }
    }

}
extension AIConversationManager {
    func isAppStoreDemo() -> Bool {
        [RTCubeBDID, TencentRTCBDID].contains(Bundle.main.bundleIdentifier)
    }
}


