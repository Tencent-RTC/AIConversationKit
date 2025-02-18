//
//  AudioInputView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/15.
//

import UIKit
import RTCCommon
import TUICore
import Kingfisher

enum ADSpectraStyle {
    case rect
    case round
}


class AudioInputView: UIView {
    
    private lazy var spectrumView: AudioSpectrumView = {
        let view = AudioSpectrumView(withBarWidth: 2.0,
                                space: 4.0,
                                bottomSpace: 0,
                                topSpace: 0,
                                barCount: 48,
                                barMinHeight: 4.0,
                                colors: [UIColor.white, UIColor.white])
        return view
    }()
    
    private lazy var subTitle: UILabel = {
        let label = UILabel()
        label.textColor = .white
        label.numberOfLines = 0
        label.font = .systemFont(ofSize: 14)
        label.textAlignment = .left
        label.sizeToFit()
        return label
    }()

    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView()
        if AILocalized.isChineseLocale() {
            imageView.image = UIImage(inAIBundleNamed: "ai_human_avatar_defaut")
         
        } else {
            imageView.image = UIImage(inAIBundleNamed: "ai_human_avatar_defaut_eng")
        }
        imageView.layer.cornerRadius = 15.scale375()
        imageView.layer.masksToBounds = true
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
        spectrumView.updateSpectra(AIConversationState.instance.userSpectrumData.value, withStyle: .round)
    }
    
    private func constructViewHierarchy() {
        addSubview(avatarImageView)
        addSubview(spectrumView)
        addSubview(subTitle)
    }
    
    private func activateConstraints() {
        avatarImageView.snp.makeConstraints { make in
            make.top.leading.equalToSuperview()
            make.height.width.equalTo(30.scale375())
        }
        spectrumView.snp.makeConstraints { make in
            make.top.equalToSuperview().inset(4.scale375Height())
            make.centerY.equalTo(avatarImageView)
            make.left.equalTo(avatarImageView.snp.right).offset(12.scale375())
            make.trailing.equalToSuperview().inset(41.scale375())
        }
        subTitle.snp.makeConstraints { make in
            make.leading.trailing.equalToSuperview()
            make.top.equalTo(avatarImageView.snp.bottom).offset(16.scale375())
        
        }
    }
    
    deinit {
        unregisterObserveState()
    }
}

extension AudioInputView {
    
}

extension AudioInputView {
    // MARK: Register AI Observer && Update UI
    func registerObserveState() {
        AIConversationState.instance.userSpectrumData.addObserver(self) { [weak self] _, _ in
            self?.updateSpectra()
        }
        
        AIConversationState.instance.userSubtitle.addObserver(self) { [weak self] _, _ in
            self?.updateSubtitle()
        }
    }
    //MARK: Unregister AI Observer
    func unregisterObserveState() {
        AIConversationState.instance.userSpectrumData.removeObserver(self)
        AIConversationState.instance.userSubtitle.removeObserver(self)
    }
    
    // MARK: Update UI
    func updateSpectra() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            spectrumView.updateSpectra(AIConversationState.instance.userSpectrumData.value, withStyle: .round)
        }
    }
    
    func updateSubtitle() {
        subTitle.text = AIConversationState.instance.userSubtitle.value
    }
}
