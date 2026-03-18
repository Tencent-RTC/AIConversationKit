//
//  ServerAIConversationRequest.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/22.
//

import Foundation
import TUICore

class ServerAIConversationRequest: AIConversationRequest {
    
    override func start(completion: @escaping (_ taskId: String, _ robotId: String?) -> Void) {
        let paramDict = encodeParamsToDictionary()
        guard !paramDict.isEmpty else {
            Logger.error("startConversation failed: encodeParamsToDictionary returned empty")
            completion("", nil)
            return
        }
        TUICore.callService("TUICore_AIConversationService",
                            method: "TUICore_AIConversationService_Start",
                            param: paramDict)
        { code, errMsg, resultParams in
            if code == 0 {
                let robotId = resultParams["robotUserId"] as? String
                let taskId = resultParams["taskId"] as? String ?? ""
                Logger.info("startConversation success, taskId:\(taskId)")
                completion(taskId, robotId)
            } else {
                Logger.error("startConversation error, code:\(code), message:\(errMsg)")
                completion("", nil)
            }
        }
    }
    
    override func stop(taskID: String) {
        let params: [AnyHashable: Any] = ["roomIdType": 1,
                                          "taskId": taskID]
        Logger.info("stopConversation, param:\(params)")
        TUICore.callService("TUICore_AIConversationService",
                            method: "TUICore_AIConversationService_Stop",
                            param: params) { code, message, resultParams in
            if code == 0 {
                Logger.info("stopConversation success")
            } else {
                Logger.error("stopConversation error, code:\(code), message:\(message)")
            }
        }
    }
    
    override func fetchExperienceDuration() {
        TUICore.callService("TUICore_AIConversationService", method: "TUICore_AIConversationService_Get_Time", param: [:])
        { code, errMsg, result in
            if (code == 0) {
                let aiDurationSecond = result["terminal"] as? Int
                AIConversationState.instance.expireDuraSec.value = aiDurationSecond ?? 300
            } else  {
                AIConversationState.instance.expireDuraSec.value = 300
            }
        }
    }
    
    
    override func fetchFeedBack() {
        TUICore.callService("TUICore_AIConversationService",
                            method: "TUICore_AIConversationService_Get_Feedback", param: [: ]) { errCode, errMsg, resultParams in
            if errCode == 0 {
                let entirety = resultParams["entirety"] as? Int ?? 0
                AIConversationState.instance.isFirstTimeComment.value = (entirety <= 0)
            }
        }
    }
    
    override func uploadFeedback(_ feedback: [String: String], completion: ((_ code: Int, _ message: String) -> Void)?) {
        var param: [String: Any] = [:]
        param["entirety"] = feedback["entirety"] ?? ""
        param["callDelay"] = feedback["callDelay"] ?? ""
        param["noiseReduce"] = feedback["noiseReduce"] ?? ""
        param["ai"] = feedback["ai"] ?? ""
        param["tone"] = feedback["tone"] ?? ""
        param["interaction"] = feedback["interaction"] ?? ""
        param["feedback"] = feedback["feedback"] ?? ""
        TUICore.callService("TUICore_AIConversationService",
                            method: "TUICore_AIConversationService_Add_Feedback",
                            param: param) { code, message, _ in
            Logger.info("uploadFeedback code=\(code) message=\(message)")
            completion?(code, message)
        }
    }
    
    private func encodeParamsToDictionary() -> [String: Any] {
        guard let config = config else {
            return [:]
        }
        
        do {
            let jsonData = try JSONEncoder().encode(config)
            guard var jsonDict = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any] else {
                Logger.error("Failed to convert JSON data to dictionary")
                return [:]
            }
            jsonDict["RoomId"] = roomId
            jsonDict["RoomIdType"] = 1
            jsonDict["SdkAppId"] = Int(TUILogin.getSdkAppID())
            if var agentDict = jsonDict["AgentConfig"] as? [String: Any] {
                agentDict["TargetUserId"] = TUILogin.getUserID() ?? ""
                jsonDict["AgentConfig"] = agentDict
            }
            return jsonDict
        } catch {
            Logger.error("Failed to encode config: \(error)")
            return [:]
        }
    }
}
