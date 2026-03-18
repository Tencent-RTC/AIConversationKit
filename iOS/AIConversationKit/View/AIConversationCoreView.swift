//
//  AIConversationCoreView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import SnapKit
import RTCCommon
import Combine

public class AIConversationCoreView: UIView {
    
    var onRoomDismissed: (() -> Void)?
    
    private let store = AIConversationStore.shared
    private var cancellableSet = Set<AnyCancellable>()
    
    private lazy var backgroundImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFill
        imageView.clipsToBounds = true
        return imageView
    }()
    
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
        backgroundColor = .black
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
        loadDefaultBackgroundImage()
    }
    
    public override func removeFromSuperview() {
        cancellableSet.removeAll()
        onRoomDismissed = nil
        super.removeFromSuperview()
    }
}

// MARK: - Public

public extension AIConversationCoreView {
    func startAIConversation(config: AIConversationConfig, completion: CompletionHandler?) {
        store.startAIConversation(config: config, completion: completion)
    }
    
    func stopAIConversation(completion: CompletionHandler?) {
        store.stopAIConversation(completion: completion)
    }
    
    func setBackgroundImage(_ image: UIImage?) {
        backgroundImageView.image = image
    }
}

// MARK: - Layout

private extension AIConversationCoreView {
    func constructViewHierarchy() {
        addSubview(backgroundImageView)
    }
    
    func activateConstraints() {
        backgroundImageView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
}

// MARK: - Binding

private extension AIConversationCoreView {
    func bindInteraction() {
        subscribeState()
    }
    
    func subscribeState() {
        store.state.subscribe(StateSelector(keyPath: \.aiStatus))
            .removeDuplicates()
            .dropFirst()
            .filter { $0 == .offline }
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                self?.onRoomDismissed?()
            }
            .store(in: &cancellableSet)
    }
    
    func loadDefaultBackgroundImage() {
        guard backgroundImageView.image == nil else { return }
        DispatchQueue.global(qos: .userInitiated).async {
            let image = UIImage(inAIBundleNamed: "ai_background")
            DispatchQueue.main.async { [weak self] in
                guard let self, self.backgroundImageView.image == nil else { return }
                self.backgroundImageView.image = image
            }
        }
    }
}
