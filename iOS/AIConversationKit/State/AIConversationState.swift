//
//  AIConversationState.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/22.
//

import Foundation
import RTCCommon

class AIConversationState {
    static let instance = AIConversationState()
    static let defaultExpireDuration = 600
    
    let expireDuraSec = Observable<Int>(defaultExpireDuration)
    let isFirstTimeComment = Observable<Bool>(true)
    
    static func isUnlimitedTime(_ seconds: Int) -> Bool {
        seconds == -1
    }
}
