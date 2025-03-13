//
//  MainViewController.swift
//  AIConversationKit
//
//  Created by adams on 2021/6/4.
//

import UIKit
import TUICore
import AIConversationKit

private class WelcomeView: UIView {
    private let contentTextLable: UILabel = {
        let label = UILabel()
        label.textColor = UIColor(0xFFFFFF, alpha: 0.9)
        label.font = .systemFont(ofSize: 17)
        label.numberOfLines = 0
        return label
    }()
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        roundedRect(rect: self.bounds, byRoundingCorners: [.topRight, .bottomRight, .bottomLeft], cornerRadii: CGSize(width: 8, height: 8))
    }
    
    var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        backgroundColor = UIColor(hex: "1F2024")
    }
    
    func updateText(_ text: String) {
        contentTextLable.text = text
         setNeedsLayout()
         layoutIfNeeded()
     }
    
    func constructViewHierarchy() {
        addSubview(contentTextLable)
    }
    
    func activateConstraints() {
        contentTextLable.snp.makeConstraints { make in
            make.top.bottom.equalToSuperview().inset(12.scale375Height())
            make.leading.trailing.equalToSuperview().inset(16.scale375())
        }
    }
    override var intrinsicContentSize: CGSize {
         let labelSize = contentTextLable.sizeThatFits(CGSize(width: bounds.width - 32, height: CGFloat.greatestFiniteMagnitude))
         return CGSize(width: labelSize.width + 32, height: labelSize.height + 24)
     }
}




class MainViewController: UIViewController {
    var roomId: Int?
    var robotId: String?
    
    private var welcomeViews: [WelcomeView] = []
    private var welcomeTexts = [AIConversationKitAppLocalize("AIConversationKit.Main.welcome"),
                                AIConversationKitAppLocalize("AIConversationKit.Main.policyDesc"),
                                AIConversationKitAppLocalize("AIConversationKit.Main.timeLimitDesc"),]
    
    private let aiIconView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "ai_icon"))
        imageView.sizeToFit()
        return imageView
    }()
    
    private let startButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIImage(named: "start_ai_btn"), for: .normal)
        button.setImage(UIImage(named: "ai_call_icon"), for: .normal)
        button.setTitle(AIConversationKitAppLocalize("AIConversationKit.Entr.start"), for: .normal)
        return button
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
        configWelcomeView()
        login()
        view.backgroundColor = .black
    }
    
    func login() {
        let sdkAppId = 1600000001
        let userId = "people"
        let secretKey = "xxx"
        let userSig = GenerateTestUserSig.genTestUserSig(sdkAppId: sdkAppId,
                                                         userId: userId,
                                                         secrectkey: secretKey)
        TUILogin.login(Int32(sdkAppId), userID: userId, userSig:userSig){
            print("login success")
        } fail: { code, message in
            print("login failed, code: \(code), error: \(message ?? "nil")")
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: true)
        configSelfNavigationBar()
      
        
    }
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        let appperance = UINavigationBarAppearance()
        appperance.backgroundColor = .white
        appperance.shadowImage = UIImage()
        appperance.shadowColor = nil
        appperance.titleTextAttributes = [NSAttributedString.Key.font: UIFont.systemFont(ofSize: 18),
                                          NSAttributedString.Key.foregroundColor: UIColor.black,]
        navigationController?.navigationBar.standardAppearance = appperance
        navigationController?.navigationBar.scrollEdgeAppearance = appperance
        navigationController?.setNavigationBarHidden(true, animated: false)
        
    }
}

extension MainViewController {
    func constructViewHierarchy() {
        view.addSubview(aiIconView)
        view.addSubview(startButton)
        
    }
    
    func activateConstraints() {
        aiIconView.snp.makeConstraints { make in
            make.height.width.equalTo(178.scale375())
            make.top.equalTo(view.safeAreaLayoutGuide)
            make.centerX.equalToSuperview()
        }
        startButton.snp.makeConstraints { make in
            make.leading.trailing.bottom.equalToSuperview().inset(20.scale375())
            make.height.equalTo(56.scale375Height())
        }
    }
    
    func bindInteraction() {
        startButton.addTarget(self, action: #selector(startAIClicked), for: .touchUpInside)
    }

}


extension MainViewController {
    private func configSelfNavigationBar() {
        let appperance = UINavigationBarAppearance()
        appperance.backgroundColor = .black
        appperance.shadowImage = UIImage()
        appperance.shadowColor = nil
        self.navigationController?.navigationBar.standardAppearance = appperance
        self.navigationController?.navigationBar.scrollEdgeAppearance = appperance
        guard let titleFont = UIFont(name: "PingFangSC-Semibold", size: 18.0) else { return }
        let titleLb = UILabel()
        titleLb.text = AIConversationKitAppLocalize("AIConversationKit.Entr.title")
        titleLb.font = titleFont
        titleLb.textColor = .white
        navigationItem.titleView = titleLb
        navigationController?.setNavigationBarHidden(false, animated: false)
    }
    
    func configWelcomeView() {
        var welcomeViewArr: [WelcomeView] = []
        for text in welcomeTexts {
            let welcomeView = WelcomeView()
            welcomeView.updateText(text)
            welcomeViewArr.append(welcomeView)
            view.addSubview(welcomeView)
        }
        welcomeViews = welcomeViewArr
        
        welcomeViews[0].snp.makeConstraints { make in
            make.top.equalTo(aiIconView.snp.bottom).offset(12.scale375Height())
            make.leading.equalToSuperview().inset(24.scale375())
            make.trailing.lessThanOrEqualToSuperview().inset(24.scale375())
        }
        
        for i in 1..<welcomeViews.count {
            welcomeViews[i].snp.makeConstraints { make in
                make.top.equalTo(welcomeViews[i - 1].snp.bottom).offset(16.scale375Height())
                make.leading.equalToSuperview().inset(24.scale375())
                make.trailing.lessThanOrEqualToSuperview().inset(24.scale375())
            }
        }
    }
}

extension MainViewController {

    
    @objc func backAction() {
        navigationController?.popViewController(animated: true)
    }
    
    @objc func startAIClicked() {
        startAIConversation()
       
    }
    
    func startAIConversation() {
        let sdkAppId = 1600000001
        let secretKey = "xxx"
        let aiRobotId = "robot_\(TUILogin.getUserID() ?? "")"
        let aiRobotSig = GenerateTestUserSig.genTestUserSig(sdkAppId: sdkAppId,
                                                            userId: aiRobotId,
                                                            secrectkey: secretKey)
        let startAIparams = StartAIConversationParams()
        startAIparams.agentConfig = AIConversationDefine.AgentConfig.generateDefaultConfig(aiRobotId: aiRobotId,
                                                                                           aiRobotSig: aiRobotSig)
        startAIparams.secretId = "xxx";
        startAIparams.secretKey = "xxx";
        startAIparams.llmConfig = "{\"LLMType\":\"openai\",\"Model\":\"hunyuan-turbo-latest\",\"SystemPrompt\":\"你是一个个人助手\",\"APIUrl\":\"https://hunyuan.cloud.tencent.com/openai/v1/chat/completions\",\"APIKey\":\"xxx\",\"History\":5,\"Streaming\":true}"
        startAIparams.ttsConfig = "{\"TTSType\":\"tencent\",\"AppId\":\"xxx\",\"SecretId\":\"xxx\",\"SecretKey\":\"xxx\",\"VoiceType\":\"502001\",\"Speed\":1.25,\"Volume\":5,\"PrimaryLanguage\":1,\"FastVoiceType\":\"\"}"
        let vc = AIConversationViewController(aiParams: startAIparams)
        navigationController?.pushViewController(vc, animated: true)
    }
}




private extension String {
    static let liveText = AIConversationKitAppLocalize("AIConversationKit.Main.Live")
    static let selfInfoText = AIConversationKitAppLocalize("AIConversationKit.Main.SelfInfo")
}
