//
//  ServerAIConversationRequest.swift
//  AFNetworking
//
//  Created by einhorn on 2024/10/22.
//

import UIKit
import TUICore
import TXLiteAVSDK_Professional

class ServerAIConversationRequest: AIConversationRequest {
    
    
    override func start(completion: @escaping (String) -> Void) {
        let lang = AIConversationState.instance.aiLang.value.rawValue
        let param = ["roomId": startAiConversationParams?.roomId,
                     "recognizeLang": lang,] as? [String: Any]
        TUICore.callService("TUICore_AIConversationSevice",
                            method: "TUICore_AIConversationSevice_Start",
                            param: param)
        { code, errMsg, resultParams in
            if code == 0 {
                let robotId = resultParams["robotUserId"] as? String
                AIConversationManager.instance.robotId  = robotId
                AIConversationManager.instance.taskId = resultParams["taskId"] as? String
            }
        }
    }
    override func stop(taskID: String? = nil) {
        let params = ["roomId" : Int(startAiConversationParams?.roomId ?? "") ?? 0,
                      "taskId" : taskID ?? "",] as? [AnyHashable : Any]
        TUICore.callService("TUICore_AIConversationSevice",
                            method: "TUICore_AIConversationSevice_Stop",
                            param: params) { code, message, resultParams in
            
        }
      }

    
    func stopAIConversationRequst() {
       
    }
    
    override func fetchExperienceDuration() {
        TUICore.callService("TUICore_AIConversationSevice", method: "TUICore_AIConversationSevice_Get_Time", param: [:])
        { code, errMsg, result in
            if (code == 0) {
                let aiDurationSecond = result["terminal"] as? Int
                AIConversationState.instance.expireDuraSec.value = aiDurationSecond ?? 300
            } else  {
                AIConversationState.instance.expireDuraSec.value = 300
            }
        }
    }
    
    
     @objc override func fetchFeedBack() {
        TUICore.callService("TUICore_AIConversationSevice",
                            method: "TUICore_AIConversationSevice_Get_Feedback", param: [: ]) { errCode, errMsg, resultParams in
            if errCode == 0 {
                let entiretyValue = resultParams["entirety"] as? Int
                let entirety = entiretyValue ?? 0
                if entirety > 0 {
                    AIConversationState.instance.isFirstTimeComment.value = false
                } else {
                    AIConversationState.instance.isFirstTimeComment.value = true
                }
                
            }
        }
    }

}
