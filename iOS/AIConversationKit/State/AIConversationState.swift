//
//  AIConversationState.swift
//  AFNetworking
//
//  Created by einhorn on 2024/10/22.
//

import UIKit
import RTCCommon

enum RobotState {
    case undefined
    case listening
    case listened
    case thinking
    case replying
    case interrupted
    
}

enum AILanguageType: String {
    case en = "en"
    case zh = "zh"
}

enum ConversationState {
    case start
    case pause
    case stop
}


class AIConversationState: NSObject {
    static let instance = AIConversationState()

    let aiState: Observable<RobotState> = Observable(RobotState.listening)
    let audioMuted: Observable<Bool> = Observable(false)
    let speakerIsOpen: Observable<Bool> = Observable(true)
    let isServerRequestReady: Observable<Bool> = Observable(false)
    let conversationState: Observable<ConversationState> = Observable(ConversationState.start)
    let userSpectrumData: Observable<[Float]> = Observable([Float](repeating: -300.0, count: 256))
    let aiSpectrumData: Observable<[Float]> = Observable([Float](repeating: -300.0, count: 256))
    let userSubtitle: Observable<String> = Observable("")
    let robotSubtitle: Observable<String> = Observable("")
    let commentContent: Observable<String> = Observable("")
    let expireDuraSec: Observable<Int> = Observable(300)
    let isFirstTimeComment: Observable<Bool> = Observable(true)
    
    let entiretyMark: Observable<Int> = Observable(0)
    let callDelayMark: Observable<Int> = Observable(0)
    let noiseReduceMark: Observable<Int> = Observable(0)
    let aiMark: Observable<Int> = Observable(0)
    let toneMark: Observable<Int> = Observable(0)
    let interactionMark: Observable<Int> = Observable(0)
    let commentText: Observable<String> = Observable("")
    let aiLang: Observable<AILanguageType> = Observable(AILanguageType.en)

}
