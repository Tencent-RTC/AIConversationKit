//
//  AIDisclaimerView.swift
//  AIConversationKit
//
//  Created on 2026/2/12.
//

import UIKit
import SnapKit
import RTCCommon

class AIDisclaimerView: UIView {
    
    private let attentionIconView: UIImageView = {
        let imageView = UIImageView(image: UIImage(inAIBundleNamed: "attention"))
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    
    private let disclaimerLabel: UILabel = {
        let label = UILabel()
        label.text = AIConversationLocalize("AIConversation.Main.experienceDesc")
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.font = .systemFont(ofSize: 12)
        label.numberOfLines = 2
        return label
    }()
    
    private let timeView = UIView()
    
    private let intervalLine: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(0xFFFFFF, alpha: 0.3)
        return view
    }()
    
    private let durationTitleLabel: UILabel = {
        let label = UILabel()
        label.text = AIConversationLocalize("AIConversation.Main.experienceDuration")
        label.font = .systemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.textAlignment = .center
        return label
    }()
    
    private let minutesLabel = makeCountdownLabel()
    private let secondsLabel = makeCountdownLabel()
    
    private let colonLabel: UILabel = {
        let label = UILabel()
        label.font = .boldSystemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.text = ":"
        return label
    }()
    
    // MARK: - Lifecycle
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        isHidden = !PackageService.isInternalDemo
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        AIConversationState.instance.expireDuraSec.removeObserver(self)
    }
    
    private var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else { return }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
}

// MARK: - Binding

private extension AIDisclaimerView {
    func bindInteraction() {
        guard PackageService.isInternalDemo else {
            timeView.isHidden = true
            return
        }
        updateCountdown(seconds: AIConversationState.instance.expireDuraSec.value)
        
        AIConversationState.instance.expireDuraSec.addObserver(self) { [weak self] seconds, _ in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                if AIConversationState.isUnlimitedTime(seconds) {
                    self.timeView.isHidden = true
                } else {
                    self.timeView.isHidden = false
                    self.updateCountdown(seconds: seconds)
                }
            }
        }
    }
    
    func updateCountdown(seconds: Int) {
        minutesLabel.text = String(format: "%02d", seconds / 60)
        secondsLabel.text = String(format: "%02d", seconds % 60)
    }
}

// MARK: - Layout

private extension AIDisclaimerView {
    static func makeCountdownLabel() -> UILabel {
        let label = UILabel()
        label.font = .systemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.55)
        label.textAlignment = .center
        label.backgroundColor = UIColor(0x131417)
        label.layer.cornerRadius = 2
        label.layer.masksToBounds = true
        return label
    }
    
    func constructViewHierarchy() {
        addSubview(attentionIconView)
        addSubview(disclaimerLabel)
        addSubview(timeView)
        timeView.addSubview(intervalLine)
        timeView.addSubview(durationTitleLabel)
        timeView.addSubview(minutesLabel)
        timeView.addSubview(colonLabel)
        timeView.addSubview(secondsLabel)
    }
    
    func activateConstraints() {
        attentionIconView.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.leading.equalToSuperview().offset(12)
            make.width.height.equalTo(14)
        }
        disclaimerLabel.snp.makeConstraints { make in
            make.centerY.equalTo(attentionIconView)
            make.leading.equalTo(attentionIconView.snp.trailing).offset(4)
            make.trailing.equalTo(timeView.snp.leading).offset(-8)
        }
        timeView.snp.makeConstraints { make in
            make.trailing.top.bottom.equalToSuperview()
            make.width.equalTo(140)
        }
        intervalLine.snp.makeConstraints { make in
            make.leading.equalToSuperview()
            make.centerY.equalToSuperview()
            make.height.equalTo(12)
            make.width.equalTo(1)
        }
        durationTitleLabel.snp.makeConstraints { make in
            make.centerY.equalTo(intervalLine)
            make.leading.equalTo(intervalLine.snp.trailing).offset(8)
        }
        minutesLabel.snp.makeConstraints { make in
            make.leading.equalTo(durationTitleLabel.snp.trailing).offset(7)
            make.centerY.equalTo(durationTitleLabel)
            make.height.width.equalTo(20)
        }
        colonLabel.snp.makeConstraints { make in
            make.leading.equalTo(minutesLabel.snp.trailing).offset(1)
            make.centerY.equalTo(minutesLabel)
        }
        secondsLabel.snp.makeConstraints { make in
            make.leading.equalTo(colonLabel.snp.trailing).offset(1)
            make.centerY.equalTo(minutesLabel)
            make.height.width.equalTo(20)
        }
    }
}
