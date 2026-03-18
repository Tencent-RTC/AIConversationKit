//
//  MicView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import Combine
import RTCCommon

public class MicView: UIButton {
    
    private let store = AIConversationStore.shared
    private var cancellableSet = Set<AnyCancellable>()
    
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
        setupImages()
        bindInteraction()
    }
}

// MARK: - Setup

private extension MicView {
    func setupImages() {
        setImage(UIImage(inAIBundleNamed: "ai_audio_unmute"), for: .normal)
        setImage(UIImage(inAIBundleNamed: "ai_audio_mute"), for: .selected)
    }
    
    func bindInteraction() {
        addTarget(self, action: #selector(micButtonTapped), for: .touchUpInside)
        subscribeState()
    }
    
    func subscribeState() {
        store.state.subscribe(StateSelector(keyPath: \.isMicOpened))
            .receive(on: RunLoop.main)
            .sink { [weak self] isMicOpened in
                guard let self else { return }
                self.isSelected = !isMicOpened
            }
            .store(in: &cancellableSet)
    }
    
    @objc func micButtonTapped() {
        if store.state.state.isMicOpened {
            store.closeLocalMicrophone()
        } else {
            store.openLocalMicrophone(completion: nil)
        }
    }
}
