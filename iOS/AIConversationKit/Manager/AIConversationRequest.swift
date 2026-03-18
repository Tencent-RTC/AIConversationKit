//
//  AIConversationRequest.swift
//  AIConversationKit
//
//  Created by einhorn on 2025/2/14.
//

import Foundation

class AIConversationRequest {
    
    var config: AIConversationConfig?
    var roomId: String = ""
    
    func start(completion: @escaping (_ taskId: String, _ robotId: String?) -> Void) {
        fatalError("This method must be overridden by subclass")
    }
    
    func stop(taskID: String) {
        fatalError("This method must be overridden by subclass")
    }
    
    func fetchFeedBack() {}
    func fetchExperienceDuration() {}
    func uploadFeedback(_ feedback: [String: String], completion: ((_ code: Int, _ message: String) -> Void)?) {}
}
