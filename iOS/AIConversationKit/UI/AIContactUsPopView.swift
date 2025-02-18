//
//  AIContactUsPopView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/29.
//

import UIKit
import RTCCommon

class AIContactUsPopView: UIView {
    var closeHandler: () -> Void = {}
    var contactUsHandler: () -> Void = {}
    private let containerView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor(hex: "1F2024")
        view.layer.cornerRadius = 12.scale375Height()
        view.layer.masksToBounds = true
        return view
    }()
    
    private let descLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.text = AIConversationLocalize("AIConversation.Main.experienceEndDesc")
        label.textColor = .white
        label.font = .systemFont(ofSize: 16)
        label.numberOfLines = 0
        label.textAlignment = .center
        return label
    }()
    
    private let contactUsButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIColor(hex: "2B2C30")?.trans2Image(), for: .normal)
        button.layer.shadowColor = UIColor(hex: "2B2C30")?.cgColor ?? UIColor.blue.cgColor
        button.layer.cornerRadius = 20.scale375Height()
        button.layer.masksToBounds = true
        button.setTitle(AIConversationLocalize("AIConversation.Main.contactus"), for: .normal)
        button.setTitleColor(.white, for: .normal)
        
        return button
    }()
    
    private let endButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIColor(hex: "4086FF")?.trans2Image(), for: .normal)
        button.layer.shadowColor = UIColor(hex: "4086FF")?.cgColor ?? UIColor.blue.cgColor
        button.layer.cornerRadius = 20.scale375Height()
        button.layer.masksToBounds = true
        button.setTitle(AIConversationLocalize("AIConversation.Main.close"), for: .normal)
        button.setTitleColor(.white, for: .normal)
        return button
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
        backgroundColor = UIColor(0x000000, alpha: 0.55)
    }
    
    func constructViewHierarchy() {
        addSubview(containerView)
        containerView.addSubview(descLabel)
        containerView.addSubview(endButton)
        containerView.addSubview(contactUsButton)
    }
    
    func activateConstraints() {
        containerView.snp.makeConstraints { make in
            make.center.equalToSuperview()
            make.height.greaterThanOrEqualTo(160.scale375Height())
            make.width.equalTo(300.scale375())
            make.bottom.equalTo(endButton).offset(24.scale375())
        }
        descLabel.snp.makeConstraints { make in
            make.leading.trailing.top.equalToSuperview().inset(24.scale375())
        }
        
        endButton.snp.makeConstraints { make in
            make.top.equalTo(descLabel.snp.bottom).offset(24.scale375Height())
            make.height.equalTo(40.scale375Height())
            make.width.equalTo(120.scale375())
            make.leading.equalToSuperview().inset(24.scale375())
        }
        
        contactUsButton.snp.makeConstraints { make in
            make.trailing.bottom.equalToSuperview().inset(24.scale375())
            make.height.equalTo(40.scale375Height())
            make.width.equalTo(120.scale375())
        }
        
    }
  
    func bindInteraction() {
        endButton.addTarget(self, action: #selector(close), for: .touchUpInside)
        contactUsButton.addTarget(self, action: #selector(contactUsClicked), for: .touchUpInside)
    }
}

extension AIContactUsPopView {
    @objc func close() {
        closeHandler()
    }
    
    @objc func contactUsClicked() {
        contactUsHandler()
    }
}

