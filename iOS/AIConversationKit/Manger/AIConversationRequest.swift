//
//  AIConversationRequst.swift
//  AIConversationKit
//
//  Created by einhorn on 2025/2/14.
//

import UIKit

class AIConversationRequest {
    var startAiConversationParams: StartAIConversationParams?
    
    func start(completion: @escaping (String) -> Void) {
        fatalError("This method must be overridden by subclass")
    }
    
    func stop(taskID: String) {
        fatalError("This method must be overridden by subclass")
    }
    
    func fetchFeedBack() {}
    func fetchExperienceDuration() {}
}
