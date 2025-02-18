//
//  AnimationAreaView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/15.
//

import UIKit
import RTCCommon
import TXLiteAVSDK_Professional

class AnimationAreaView: UIView {
    var animationPlayer: TXVodPlayer = {
        let player = TXVodPlayer()
        player.loop = true
        return player
    }()
    
    private let interruptAIIcon: UIImageView = {
        let imageView = UIImageView(image: UIImage(inAIBundleNamed: "ai_interrupt"))
        imageView.contentMode = .scaleAspectFit
        imageView.isHidden = true
        return imageView
    }()
    
    private let interruptAILabel: UILabel = {
        let label = UILabel()
        label.text = AIConversationLocalize("AIConversation.Animation.interrupAIDesc")
        label.font = .systemFont(ofSize: 14)
        label.numberOfLines = 0
        label.textColor = UIColor(0xFFFFFF, alpha: 0.55)
        label.isHidden = true
        return label
    }()
    
    private let interruptHintView = UIView(frame: .zero)
    private let interruptButton = UIButton(type: .custom)
    private lazy var animationFilePathTuple: (listeningPath: String,
                                              listenedPath: String,
                                              thinkingPath: String,
                                              thoughtPath:String) = {
        let listeningPath = AILocalized.sharedBundle.path(forResource: "ai_listening", ofType: "mp4") ?? ""
        let listenedPath = AILocalized.sharedBundle.path(forResource: "ai_listened", ofType: "mp4") ?? ""
        let thinkingPath = AILocalized.sharedBundle.path(forResource: "ai_thinking", ofType: "mp4") ?? ""
        let thoughtPath = AILocalized.sharedBundle.path(forResource: "ai_thought", ofType: "mp4") ?? ""
        return (listeningPath: listeningPath,
                listenedPath: listenedPath,
                thinkingPath: thinkingPath,
                thoughtPath: thoughtPath)
    }()
    
    var animationView: UIView = {
        let view = UIView(frame: .zero)
        return view
    }()
    
    
    private lazy var spectrumView: AudioSpectrumView = {
        let edgeCGColor = UIColor(hex: "467EE7") ?? UIColor.blue
        
        let view = AudioSpectrumView(withBarWidth: 10.scale375(),
                                space: 5.scale375(),
                                bottomSpace: 0,
                                topSpace: 0,
                                barCount: 20,
                                barMinHeight: 10.scale375(),
                                colors: [edgeCGColor, UIColor.white, edgeCGColor],
                                colorLocations: [0.0, 0.3, 1.0])
        return view
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
        registerObserveState()
        startAnimation(WithStatus: AIConversationState.instance.aiState.value)
    }
    
    private func constructViewHierarchy() {
        addSubview(spectrumView)
        addSubview(animationView)
        addSubview(interruptButton)
        addSubview(interruptHintView)
        interruptHintView.addSubview(interruptAIIcon)
        interruptHintView.addSubview(interruptAILabel)
    }
    
    private func activateConstraints() {
        spectrumView.snp.makeConstraints { make in
            make.top.bottom.equalToSuperview()
            make.leading.trailing.equalToSuperview().inset(44.scale375())
        }
        animationView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        interruptButton.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        interruptHintView.snp.makeConstraints { make in
            make.height.equalTo(22.scale375Height())
            make.bottom.equalToSuperview().inset(19.scale375Height())
            make.centerX.equalToSuperview()
        }
        interruptAIIcon.snp.makeConstraints { make in
            make.leading.equalToSuperview()
            make.bottom.equalToSuperview()
        }
        interruptAILabel.snp.makeConstraints { make in
            make.centerY.equalTo(interruptAIIcon)
            make.left.equalTo(interruptAIIcon.snp.right).offset(4.scale375())
            make.trailing.equalToSuperview()
        }
    }
    
    private func bindInteraction() {
        interruptButton.addTarget(self, action: #selector(interruptAIClicked), for: .touchUpInside)
    }

    deinit {
        unregisterObserveState()
    }
}

extension AnimationAreaView {
    
    private func startAnimation(WithStatus status: RobotState) {
        stopReplying()
        animationPlayer.setupVideoWidget(animationView, insert: 0)
        animationPlayer.stopPlay()
        switch status {
        case .listening:
            animationPlayer.startVodPlay(animationFilePathTuple.listeningPath)
        case .listened:
            animationPlayer.startVodPlay(animationFilePathTuple.listenedPath)
        case .thinking:
            animationPlayer.startVodPlay(animationFilePathTuple.thinkingPath)
        case .replying:
            startReplying()
        case .undefined:
            animationPlayer.startVodPlay(animationFilePathTuple.listeningPath)
        case .interrupted:
            animationPlayer.startVodPlay(animationFilePathTuple.listeningPath)
        }
        
    }
    
}

extension AnimationAreaView {
    func registerObserveState() {
        AIConversationState.instance.aiState.addObserver(self) { [weak self] _, _ in
            self?.updateAIState()
        }
        
        AIConversationState.instance.aiSpectrumData.addObserver(self) { [weak self] _, _ in
            self?.updateSpectrum()
        }
 
    }
    
    func unregisterObserveState() {
        AIConversationState.instance.aiState.removeObserver(self)
        AIConversationState.instance.aiSpectrumData.removeObserver(self)
 
    }
    
    // MARK: Update UI
    func updateAIState() {
        if  AIConversationState.instance.aiState.value == .replying  {
            startReplying()
        } else {
            stopReplying()
            startAnimation(WithStatus: AIConversationState.instance.aiState.value)
        }
    }
    
    func updateSpectrum() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            spectrumView.updateSpectra(AIConversationState.instance.aiSpectrumData.value, withStyle: .round)
        }
        
    }
    
    func startReplying() {
        interruptButton.isEnabled = true
        interruptAIIcon.isHidden = false
        interruptAILabel.isHidden = false
     
        self.animationView.isHidden = true
        self.spectrumView.isHidden = false
        self.spectrumView.updateSpectra(AIConversationState.instance.aiSpectrumData.value, withStyle: .round)
        
    }
    
    func stopReplying() {
        interruptButton.isEnabled = false
        animationView.isHidden = false
        spectrumView.isHidden = true
        interruptAIIcon.isHidden = true
        interruptAILabel.isHidden = true
       
    }
}

extension AnimationAreaView {
    @objc func interruptAIClicked() {
        AIConversationManager.instance.interuptAI()
    }
}
