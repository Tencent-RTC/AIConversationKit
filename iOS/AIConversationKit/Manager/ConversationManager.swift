//
//  ConversationManager.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/22.
//

import Foundation
import TUICore
import RTCCommon

class ConversationManager {
    static var shared = ConversationManager()
    
    let remainingExperienceTimeS = AIConversationState.instance.expireDuraSec
    
    private var feedbackRequest: AIConversationRequest?
    private var timer: Timer?
    private var heartbeatCount = 0
    
    private init() {
        if PackageService.isInternalDemo {
            feedbackRequest = ServerAIConversationRequest()
        } else {
            feedbackRequest = ClientAIConversationRequest()
        }
        Logger.info("ConversationManager sharedInstance: \(self)")
    }
    
    // MARK: - Public API
    
    func fetchFeedback() {
        Logger.info("fetchFeedback")
        feedbackRequest?.fetchFeedBack()
    }
    
    func deductionExperienceTime() {
        TUICore.callService("TUICore_AIConversationService",
                            method: "TUICore_AIConversationService_Time_Deduction",
                            param: [:]) { code, _, result in
            guard code == 0 else { return }
            let time = result["time"] as? Int
            Logger.info("AI-heartbeat--time:\(time ?? 0)")
        }
    }
    
    func startExperienceDurationMonitor() {
        AIConversationState.instance.expireDuraSec.addObserver(self) { [weak self] seconds, _ in
            guard let self else { return }
            AIConversationState.instance.expireDuraSec.removeObserver(self)
            if !AIConversationState.isUnlimitedTime(seconds), seconds > 0 {
                self.startCountTime()
            }
        }
        feedbackRequest?.fetchExperienceDuration()
    }
    
    func invalidateTimer() {
        timer?.invalidate()
        timer = nil
        heartbeatCount = 0
    }
    
    func uploadFeedback(_ feedback: [String: String], completion: ((_ code: Int, _ message: String) -> Void)?) {
        feedbackRequest?.uploadFeedback(feedback, completion: completion)
    }
    
    func destroySharedInstance() {
        invalidateTimer()
        feedbackRequest = nil
    }
}

// MARK: - Timer

private extension ConversationManager {
    
    func startCountTime() {
        guard timer == nil else { return }
        let newTimer = Timer(timeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateExpDuration()
        }
        RunLoop.main.add(newTimer, forMode: .common)
        timer = newTimer
    }
    
    func updateExpDuration() {
        heartbeatCount += 1
        if heartbeatCount % 10 == 0 {
            deductionExperienceTime()
        }
        let seconds = AIConversationState.instance.expireDuraSec.value
        
        guard !AIConversationState.isUnlimitedTime(seconds) else { return }
        
        if seconds > 0 {
            AIConversationState.instance.expireDuraSec.value = seconds - 1
        } else {
            invalidateTimer()
        }
    }
}
