//
//  AIReplyAreaView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/16.
//

import UIKit

class AIReplyAreaView: UIView {
    private lazy var subTitleView: UITextView = {
        let textView = UITextView()
        textView.isEditable = false
        textView.isScrollEnabled = true
        textView.backgroundColor = .clear
        textView.showsVerticalScrollIndicator = true
        textView.textContainer.lineBreakMode = .byTruncatingTail 
        textView.font = .systemFont(ofSize: 14)
        textView.textColor = .white
        textView.isEditable  = false
        return textView
    }()

    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(inAIBundleNamed: "ai_default_avatar"))
        imageView.sizeToFit()
        return imageView
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
        registerObserveState()
    }
    
    private func constructViewHierarchy() {
        addSubview(avatarImageView)
        addSubview(subTitleView)
    }
    
    private func activateConstraints() {
        avatarImageView.snp.makeConstraints { make in
            make.top.leading.equalToSuperview()
            make.height.width.equalTo(30.scale375())
        }
        
        subTitleView.snp.makeConstraints { make in
            make.top.right.equalToSuperview()
            make.left.equalTo(avatarImageView.snp.right).offset(12.scale375())
            make.height.equalToSuperview()
        }
    }
    
    deinit {
        unregisterObserveState()
    }

}

extension AIReplyAreaView {
    func registerObserveState() {
        AIConversationState.instance.robotSubtitle.addObserver(self) { [weak self] _, _ in
            self?.updateSubtitle()
        }
    }
    
    func unregisterObserveState() {
        AIConversationState.instance.robotSubtitle.removeObserver(self)
    }
    
    func updateSubtitle() {
        subTitleView.text = AIConversationState.instance.robotSubtitle.value
        scrollToBottom()
        subTitleView.layoutIfNeeded()
    }
    
    func scrollToBottom() {
        let range = NSMakeRange(subTitleView.text.count - 1, 1)
        subTitleView.scrollRangeToVisible(range)
    }
}

