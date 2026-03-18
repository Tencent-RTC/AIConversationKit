//
//  HangUpView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import RTCCommon

public class HangUpView: UIButton {
    
    var onHangUp: (() -> Void)?
    
    // MARK: - Lifecycle
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private var isViewReady = false
    public override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else { return }
        isViewReady = true
        setImage(UIImage(inAIBundleNamed: "ai_hangup"), for: .normal)
        addTarget(self, action: #selector(hangUpButtonTapped), for: .touchUpInside)
    }
}

// MARK: - Action

private extension HangUpView {
    @objc func hangUpButtonTapped() {
        onHangUp?()
    }
}
