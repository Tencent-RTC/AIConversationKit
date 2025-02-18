//
//  FunctionView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/15.
//

import UIKit
import SnapKit
import RTCCommon

class FucntionItemButton: UIButton {
 
    
    override func layoutSubviews() {
        super.layoutSubviews()

        guard let imageView = self.imageView, let titleLabel = self.titleLabel else {
            return
        }
        let totalHeight = imageView.frame.height + titleLabel.frame.height + 8
        imageView.frame.origin.y = (self.bounds.height - totalHeight) / 2
        imageView.center.x = self.bounds.width / 2
        titleLabel.frame = CGRect(
            x: 0,
            y: imageView.frame.maxY + 8,
            width: self.bounds.width,
            height: titleLabel.intrinsicContentSize.height
        )
        titleLabel.textAlignment = .center
        titleLabel.font = .systemFont(ofSize: 12)
    }
}


class FunctionAreaView: UIView {
    
    var hangupClosure: () -> Void = {}
    
    private let audioStateButton: FucntionItemButton = {
        let button = FucntionItemButton(type: .custom)
        button.setImage(UIImage(inAIBundleNamed: "audio_unmute"), for: .normal)
        button.setImage(UIImage(inAIBundleNamed: "audio_mute"), for: .selected)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.mute"), for: .normal)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.unmute"), for: .selected)
        return button
    }()
    
    private let speakerStateButton: FucntionItemButton = {
        let button = FucntionItemButton(type: .custom)
        button.setImage(UIImage(inAIBundleNamed: "speaker_on"), for: .normal)
        button.setImage(UIImage(inAIBundleNamed: "speaker_off"), for: .selected)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.speakerOff"), for: .normal)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.speakerOn"), for: .selected)
        return button
    }()
    
    private let playStateButton: FucntionItemButton = {
        let button = FucntionItemButton(type: .custom)
        button.setImage(UIImage(inAIBundleNamed: "ai_pause"), for: .normal)
        button.setImage(UIImage(inAIBundleNamed: "ai_play"), for: .selected)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.playPause"), for: .normal)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.playResume"), for: .selected)
        return button
    }()
    
    private let hangUpButton: FucntionItemButton = {
        let button = FucntionItemButton(type: .custom)
        button.setImage(UIImage(inAIBundleNamed: "ai_hangup"), for: .normal)
        button.setImage(UIImage(inAIBundleNamed: "ai_hangup"), for: .selected)
        button.setTitle(AIConversationLocalize("AIConversation.Fuction.hangUp"), for: .normal)
 
        return button
    }()
    

    var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
    
    private func constructViewHierarchy() {
        addSubview(audioStateButton)
        addSubview(speakerStateButton)
        addSubview(playStateButton)
        addSubview(hangUpButton)
    }
    
    private func activateConstraints() {
     
        audioStateButton.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(32.scale375())
            make.width.equalTo(74.scale375())
            make.top.bottom.equalToSuperview()
        }
        
        speakerStateButton.snp.makeConstraints { make in
            make.width.equalTo(74.scale375())
            make.trailing.equalToSuperview().inset(32.scale375())
            make.top.bottom.equalToSuperview()
        }
        
        hangUpButton.snp.makeConstraints { make in
            make.width.equalTo(101.scale375())
            make.centerX.equalToSuperview()
            make.top.bottom.equalToSuperview()
        }

    }
    
    private func bindInteraction() {
        audioStateButton.addTarget(self, action: #selector(audioMuteClicked), for: .touchUpInside)
        speakerStateButton.addTarget(self, action: #selector(speakerSwitchClicked), for: .touchUpInside)
        playStateButton.addTarget(self, action: #selector(playStateChangeClicked), for: .touchUpInside)
        hangUpButton.addTarget(self, action: #selector(hangupClicked), for: .touchUpInside)
    }
}

extension FunctionAreaView {
    @objc func audioMuteClicked() {
        audioStateButton.isSelected = !AIConversationState.instance.audioMuted.value
        AIConversationManager.instance.muteLocalAudio(audioStateButton.isSelected)
    }
    
    @objc func speakerSwitchClicked() {
        AIConversationManager.instance.openSpeaker(isOpen: speakerStateButton.isSelected)
        speakerStateButton.isSelected = !AIConversationState.instance.speakerIsOpen.value
      
    }
    
    @objc func playStateChangeClicked() {
        if (AIConversationState.instance.conversationState.value == .pause) {
            AIConversationManager.instance.resume()
            playStateButton.isSelected = false
        } else if  AIConversationState.instance.conversationState.value == .start  {
            AIConversationManager.instance.pause()
            playStateButton.isSelected = true
        }
        
    }
    
    @objc func hangupClicked() {
        AIConversationManager.instance.stop()
        hangupClosure()
    }
}
