//
//  AIConversationControlView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import SnapKit

class AIConversationControlView: UIView {
    
    var onHangUp: (() -> Void)?
    
    private let micView = MicView()
    private let aiStatusView = AIStatusView()
    private let hangUpView = HangUpView()
    
    private lazy var stackView: UIStackView = {
        let stack = UIStackView(arrangedSubviews: [micView, aiStatusView, hangUpView])
        stack.axis = .horizontal
        stack.alignment = .center
        return stack
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else { return }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        updateSpacing()
        bindInteraction()
    }
    
    func disableFeature(_ feature: AIConversationView.Feature) {
        switch feature {
        case .mic:
            micView.isHidden = true
        case .aiInteraction:
            aiStatusView.isHidden = true
        default:
            break
        }
        updateSpacing()
    }
}

// MARK: - Layout

private extension AIConversationControlView {
    
    enum Layout {
        static let buttonSize: CGFloat = 64
        static let statusMinWidth: CGFloat = 60
        static let statusHeight: CGFloat = 56
        static let itemSpacing: CGFloat = 50
        static let twoItemSpacing: CGFloat = 100
    }
    
    func constructViewHierarchy() {
        addSubview(stackView)
    }
    
    func activateConstraints() {
        let buttonSize = Layout.buttonSize.scale375Width()
        
        stackView.snp.makeConstraints { make in
            make.center.equalToSuperview()
        }
        
        micView.snp.makeConstraints { make in
            make.width.height.equalTo(buttonSize)
        }
        
        hangUpView.snp.makeConstraints { make in
            make.width.height.equalTo(buttonSize)
        }
        
        aiStatusView.snp.makeConstraints { make in
            make.width.greaterThanOrEqualTo(Layout.statusMinWidth)
            make.height.equalTo(Layout.statusHeight)
        }
    }
    
    func bindInteraction() {
        hangUpView.onHangUp = { [weak self] in
            self?.disableFeature(.mic)
            self?.disableFeature(.aiInteraction)
            self?.onHangUp?()
        }
    }
    
    func updateSpacing() {
        let visibleCount = stackView.arrangedSubviews.filter({ !$0.isHidden }).count
        let spacing: CGFloat
        switch visibleCount {
        case ...1:
            spacing = 0
        case 2:
            spacing = Layout.twoItemSpacing
        default:
            spacing = Layout.itemSpacing
        }
        stackView.spacing = spacing.scale375Width()
    }
}
