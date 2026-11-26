//
//  AIStatusView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import SnapKit
import Combine
import RTCCommon

public class AIStatusView: UIView {
    
    private let store = AIConversationStore.shared
    private var cancellableSet = Set<AnyCancellable>()
    
    private let capsuleAnimationView = CapsuleWaveAnimationView()
    
    private lazy var pauseIconView: UIImageView = {
        let imageView = UIImageView(image: UIImage(inAIBundleNamed: "ai_pause"))
        imageView.contentMode = .scaleAspectFit
        imageView.isHidden = true
        return imageView
    }()
    
    private lazy var statusLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor(0xFFFFFF, alpha: 0.8)
        label.font = .systemFont(ofSize: 13)
        label.textAlignment = .center
        return label
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
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
}

private extension AIStatusView {
    func constructViewHierarchy() {
        addSubview(capsuleAnimationView)
        addSubview(pauseIconView)
        addSubview(statusLabel)
    }
    
    func activateConstraints() {
        capsuleAnimationView.snp.makeConstraints { make in
            make.top.equalToSuperview()
            make.centerX.equalToSuperview()
            make.width.equalTo(50)
            make.height.equalTo(28)
        }
        pauseIconView.snp.makeConstraints { make in
            make.center.equalTo(capsuleAnimationView)
            make.width.height.equalTo(20)
        }
        statusLabel.snp.makeConstraints { make in
            make.top.equalTo(capsuleAnimationView.snp.bottom).offset(5)
            make.centerX.equalToSuperview()
            make.leading.greaterThanOrEqualToSuperview()
            make.trailing.lessThanOrEqualToSuperview()
        }
    }
}

private extension AIStatusView {
    func bindInteraction() {
        isUserInteractionEnabled = true
        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(statusTapped)))
        subscribeState()
    }
    
    func subscribeState() {
        store.state.subscribe(StateSelector(keyPath: \.aiStatus))
            .receive(on: RunLoop.main)
            .sink { [weak self] aiStatus in
                guard let self else { return }
                self.updateStatus(aiStatus)
            }
            .store(in: &cancellableSet)
    }
    
    func updateStatus(_ status: AIStatus) {
        capsuleAnimationView.isHidden = false
        statusLabel.isHidden = false
        pauseIconView.isHidden = true
        
        switch status {
        case .initializing:
            statusLabel.text = .connectingText
            capsuleAnimationView.startAnimating()
        case .listening, .interrupted:
            statusLabel.text = .listeningText
            capsuleAnimationView.startAnimating()
        case .thinking, .speaking:
            statusLabel.text = .tapToInterruptText
            capsuleAnimationView.stopAnimating()
            capsuleAnimationView.isHidden = true
            pauseIconView.isHidden = false
        case .completed, .offline:
            capsuleAnimationView.stopAnimating()
            capsuleAnimationView.isHidden = true
            statusLabel.isHidden = true
        }
    }
    
    @objc func statusTapped() {
        let status = store.state.state.aiStatus
        guard status == .thinking || status == .speaking else { return }
        store.interruptSpeech()
    }
}

private extension String {
    static let connectingText = AIConversationLocalize("AIConversation.Status.Connecting")
    static let listeningText = AIConversationLocalize("AIConversation.Status.Listening")
    static let tapToInterruptText = AIConversationLocalize("AIConversation.Status.TapToInterrupt")
}

// MARK: - CapsuleWaveAnimationView

private class CapsuleWaveAnimationView: UIView {
    
    private struct StyleConfig {
        let capsuleWidth: CGFloat
        let minHeight: CGFloat
        let maxHeight: CGFloat
        let spacing: CGFloat
        
        var staticHeights: [CGFloat] {
            let side = minHeight + (maxHeight - minHeight) * 0.3
            let center = minHeight + (maxHeight - minHeight) * 0.7
            return [side, center, side]
        }
    }
    
    private enum AnimationConfig {
        static let duration: CFTimeInterval = 0.6
        static let staggerDelay: CFTimeInterval = 0.15
        static let animationKey = "capsuleWave"
    }
    
    private static let largeConfig = StyleConfig(capsuleWidth: 10, minHeight: 10, maxHeight: 20, spacing: 5)
    private let capsuleCount = 3
    private var capsuleLayers: [CAShapeLayer] = []
    private var isAnimating = false
    private var config: StyleConfig { Self.largeConfig }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        clipsToBounds = false
        capsuleLayers = (0..<capsuleCount).map { _ in
            let capsule = CAShapeLayer()
            capsule.fillColor = UIColor.white.cgColor
            layer.addSublayer(capsule)
            return capsule
        }
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        if isAnimating {
            rebuildAnimations()
        } else {
            layoutCapsules()
        }
    }
    
    func startAnimating() {
        guard !isAnimating else { return }
        isAnimating = true
        rebuildAnimations()
    }
    
    func stopAnimating() {
        isAnimating = false
        capsuleLayers.forEach { $0.removeAllAnimations() }
        layoutCapsules(heights: config.staticHeights)
    }
    
    private func capsuleCenterXPositions() -> [CGFloat] {
        let cfg = config
        let totalWidth = CGFloat(capsuleCount) * cfg.capsuleWidth + CGFloat(capsuleCount - 1) * cfg.spacing
        let startX = (bounds.width - totalWidth) / 2
        return (0..<capsuleCount).map { i in
            startX + CGFloat(i) * (cfg.capsuleWidth + cfg.spacing) + cfg.capsuleWidth / 2
        }
    }
    
    private func capsulePath(width: CGFloat, height: CGFloat) -> CGPath {
        UIBezierPath(roundedRect: CGRect(x: 0, y: 0, width: width, height: height),
                     cornerRadius: width / 2).cgPath
    }
    
    private func applyCapsuleFrame(_ capsule: CAShapeLayer, centerX: CGFloat, height: CGFloat) {
        let w = config.capsuleWidth
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        capsule.bounds = CGRect(x: 0, y: 0, width: w, height: height)
        capsule.position = CGPoint(x: centerX, y: bounds.height / 2)
        capsule.path = capsulePath(width: w, height: height)
        CATransaction.commit()
    }
    
    private func layoutCapsules(heights: [CGFloat]? = nil) {
        let centers = capsuleCenterXPositions()
        let defaultHeights = heights ?? Array(repeating: config.minHeight, count: capsuleCount)
        for (i, capsule) in capsuleLayers.enumerated() {
            applyCapsuleFrame(capsule, centerX: centers[i], height: defaultHeights[i])
        }
    }
    
    private func rebuildAnimations() {
        let cfg = config
        let centers = capsuleCenterXPositions()
        let w = cfg.capsuleWidth
        let easeInOut = CAMediaTimingFunction(name: .easeInEaseOut)
        let now = CACurrentMediaTime()
        
        for (i, capsule) in capsuleLayers.enumerated() {
            applyCapsuleFrame(capsule, centerX: centers[i], height: cfg.minHeight)
            
            let minBounds = CGRect(x: 0, y: 0, width: w, height: cfg.minHeight)
            let maxBounds = CGRect(x: 0, y: 0, width: w, height: cfg.maxHeight)
            
            let boundsAnim = CAKeyframeAnimation(keyPath: "bounds")
            boundsAnim.values = [minBounds, maxBounds, minBounds].map { NSValue(cgRect: $0) }
            
            let pathAnim = CAKeyframeAnimation(keyPath: "path")
            pathAnim.values = [
                capsulePath(width: w, height: cfg.minHeight),
                capsulePath(width: w, height: cfg.maxHeight),
                capsulePath(width: w, height: cfg.minHeight),
            ]
            
            let keyTimes: [NSNumber] = [0, 0.5, 1.0]
            let timingFunctions = [easeInOut, easeInOut]
            for anim in [boundsAnim, pathAnim] {
                anim.keyTimes = keyTimes
                anim.timingFunctions = timingFunctions
            }
            
            let group = CAAnimationGroup()
            group.animations = [boundsAnim, pathAnim]
            group.duration = AnimationConfig.duration
            group.beginTime = now + Double(i) * AnimationConfig.staggerDelay
            group.repeatCount = .infinity
            group.fillMode = .backwards
            capsule.add(group, forKey: AnimationConfig.animationKey)
        }
    }
}
