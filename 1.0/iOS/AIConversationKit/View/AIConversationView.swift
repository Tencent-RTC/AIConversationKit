//
//  AIConversationView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import SnapKit

public class AIConversationView: UIView {
    
    public enum Feature: String {
        case mic
        case aiInteraction
        case subtitle
    }
    
    public var onHangUp: (() -> Void)?
    public var onRoomDismissed: (() -> Void)?
    
    private let coreView = AIConversationCoreView()
    private let subtitleView = AIConversationSubtitleView()
    private let controlView = AIConversationControlView()
    private let disclaimerView = AIDisclaimerView()
    
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
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
    
    public override func removeFromSuperview() {
        onHangUp = nil
        onRoomDismissed = nil
        controlView.onHangUp = nil
        coreView.onRoomDismissed = nil
        super.removeFromSuperview()
    }
}

// MARK: - Public

public extension AIConversationView {
    func startAIConversation(config: AIConversationConfig, completion: CompletionHandler?) {
        coreView.startAIConversation(config: config, completion: completion)
    }
    
    func stopAIConversation(completion: CompletionHandler?) {
        coreView.stopAIConversation(completion: completion)
    }
    
    func setBackgroundImage(_ image: UIImage?) {
        coreView.setBackgroundImage(image)
    }
    
    func disableFeatures(_ features: [Feature]) {
        for feature in features {
            switch feature {
            case .subtitle:
                subtitleView.isHidden = true
            case .mic, .aiInteraction:
                controlView.disableFeature(feature)
            }
        }
    }
}

// MARK: - Layout

private extension AIConversationView {
    enum Layout {
        static let disclaimerHeight: CGFloat = 40
        static let controlViewHeight: CGFloat = 80
        static let subtitleTopMultiplier: CGFloat = 0.25
    }
    
    func constructViewHierarchy() {
        addSubview(coreView)
        addSubview(subtitleView)
        addSubview(controlView)
        addSubview(disclaimerView)
    }
    
    func activateConstraints() {
        coreView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        disclaimerView.snp.makeConstraints { make in
            make.leading.trailing.equalToSuperview().inset(16)
            make.bottom.equalTo(safeAreaLayoutGuide.snp.bottom)
            make.height.equalTo(Layout.disclaimerHeight)
        }
        controlView.snp.makeConstraints { make in
            make.leading.trailing.equalToSuperview()
            make.bottom.equalTo(disclaimerView.snp.top)
            make.height.equalTo(Layout.controlViewHeight)
        }
        subtitleView.snp.makeConstraints { make in
            make.leading.trailing.equalToSuperview()
            make.bottom.equalTo(controlView.snp.top)
            make.top.equalTo(self.snp.bottom).multipliedBy(Layout.subtitleTopMultiplier)
        }
    }
}

// MARK: - Binding

private extension AIConversationView {
    func bindInteraction() {
        controlView.onHangUp = { [weak self] in
            self?.stopAIConversation(completion: nil)
            self?.onHangUp?()
        }
        coreView.onRoomDismissed = { [weak self] in
            self?.stopAIConversation(completion: nil)
            self?.onRoomDismissed?()
        }
    }
}
