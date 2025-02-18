//
//  AIConversationViewController.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/15.
//

import UIKit
import SnapKit
import TXLiteAVSDK_Professional
import RTCCommon
import TUICore
import SafariServices

private class CustomView: UIView {
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        roundedRect(rect: self.bounds, byRoundingCorners: [.topLeft, .topRight], cornerRadii: CGSize(width: 32, height: 32))
    }
}

class AIExperienceView: UIView {
    private let topBorderView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor(hex: "131417")
        return view
    }()
    
    private let attentionIconView: UIImageView = {
        let imageView = UIImageView(image: UIImage(inAIBundleNamed: "attention"))
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    
    private let attentionTitleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.text = AIConversationLocalize("AIConversation.Main.experienceDesc")
        label.font = .systemFont(ofSize: 12)
        if !AILocalized.isChineseLocale() {
            label.font = .systemFont(ofSize: 10)
        }
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.numberOfLines = 2
        return label
    }()
    
    private let timeView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        view.isHidden = !AIConversationManager.instance.isAppStoreDemo()
        return view
    }()
    
    private let minutesLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.55)
        label.textAlignment = .center
        label.backgroundColor = UIColor.black
        label.layer.cornerRadius = 2
        label.layer.masksToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let secondsLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.55)
        label.textAlignment = .center
        label.backgroundColor = .black
        label.layer.cornerRadius = 2
        label.layer.masksToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let durationTitleLabel: UILabel = {
        let label = UILabel()
        label.text = AIConversationLocalize("AIConversation.Main.experienceDuration")
        label.font = .systemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.textAlignment = .center
        return label
    }()
    
    private let colonLabel: UILabel = {
        let label = UILabel()
        label.font = .boldSystemFont(ofSize: 12)
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.text = ":"
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let intervalLine: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor(0xFFFFFF, alpha: 0.3)
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
    }
    
    func update(withSeconds totalSeconds: Int) {
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        minutesLabel.text = String(format: "%02d", minutes)
        secondsLabel.text = String(format: "%02d", seconds)
        
    }
    
    func constructViewHierarchy() {
        addSubview(topBorderView)
        addSubview(attentionIconView)
        addSubview(attentionTitleLabel)
        addSubview(timeView)
        timeView.addSubview(intervalLine)
        timeView.addSubview(durationTitleLabel)
        timeView.addSubview(minutesLabel)
        timeView.addSubview(colonLabel)
        timeView.addSubview(secondsLabel)
    }
    
    func activateConstraints() {
        topBorderView.snp.makeConstraints { make in
            make.top.leading.trailing.equalToSuperview()
            make.height.equalTo(1)
        }
        
        attentionIconView.snp.makeConstraints { make in
            make.top.equalToSuperview().offset(20.scale375Height())
            make.leading.equalToSuperview().offset(12.scale375())
            
        }
        attentionTitleLabel.snp.makeConstraints { make in
            make.centerY.equalTo(attentionIconView)
            make.left.equalTo(attentionIconView.snp.right).offset(4.scale375())
            make.right.equalTo(timeView.snp.left).offset(-8.scale375())
        }
        
        timeView.snp.makeConstraints { make in
            make.trailing.top.bottom.equalToSuperview()
            make.width.equalTo(140.scale375())
        }
        
        intervalLine.snp.makeConstraints { make in
            make.leading.equalToSuperview()
            make.top.equalToSuperview().offset(20.scale375Height())
            make.height.equalTo(12.scale375Height())
            make.width.equalTo(1.scale375())
        }
        durationTitleLabel.snp.makeConstraints { make in
            make.centerY.equalTo(intervalLine)
            make.left.equalTo(intervalLine.snp.right).offset(8.scale375())
            make.width.equalTo(48.scale375())
        }
        minutesLabel.snp.makeConstraints { make in
            make.left.equalTo(durationTitleLabel.snp.right).offset(7.scale375())
            make.centerY.equalTo(durationTitleLabel)
            make.height.width.equalTo(20.scale375())
        }
        colonLabel.snp.makeConstraints { make in
            make.left.equalTo(minutesLabel.snp.right).offset(1.scale375())
            make.centerY.equalTo(minutesLabel)
        }
        secondsLabel.snp.makeConstraints { make in
            make.left.equalTo(colonLabel.snp.right).offset(1.scale375())
            make.centerY.equalTo(minutesLabel)
            make.height.width.equalTo(20.scale375())
        }

    }

    
}

public class AIConversationViewController: UIViewController {
    
    var roomId: Int?
    var botId: String?
    var aiParams: StartAIConversationParams?
    private let isRTCubeApp: Bool = {
        return Bundle.main.bundleIdentifier == "com.tencent.mrtc"
    }()
    
    private lazy var popupView: AIContactUsPopView = {
        let view = AIContactUsPopView()
        view.isHidden = true
        view.closeHandler  = { [weak self] in
            guard let self = self else { return }
            view.isHidden = true
            if Bundle.main.bundleIdentifier == RTCubeBDID ||
               Bundle.main.bundleIdentifier == TencentRTCBDID {
                let vc = EvaluationViewController()
                self.navigationController?.pushViewController(vc, animated: true)
            }
        }
        
        view.contactUsHandler = { [weak self] in
            guard let self = self else { return }
            view.isHidden = true
            if self.isRTCubeApp {
                guard let url = URL(string: "https://cloud.tencent.com/apply/p/dlr7v7lxbwf") else { return }
                TUITool.openLink(with: url)
                self.navigationController?.popViewController(animated: true)
            } else {
                self.navigationController?.popViewController(animated: false)
                TUICore.callService("TUICore_ContactUsService",
                                                method: "TUICore_ContactService_gotoContactUS",
                                                param: [:])
            }
           
          
        }
        return view
    }()
    
    private var audioInputView: AudioInputView = {
        let view = AudioInputView(frame: .zero)
        return view
    }()
    
    private var experienceView: AIExperienceView = {
        let view = AIExperienceView(frame: .zero)
        return view
    }()
    
    private let robotReplyView: AIReplyAreaView = {
        let view = AIReplyAreaView(frame: .zero)
        return view
    }()
    
    private lazy var functionView: FunctionAreaView = {
        let view = FunctionAreaView(frame: .zero)
        view.hangupClosure = { [weak self] in
            guard let self = self else { return }
            if Bundle.main.bundleIdentifier == RTCubeBDID ||
               Bundle.main.bundleIdentifier == TencentRTCBDID {
                let vc = EvaluationViewController()
                self.navigationController?.pushViewController(vc, animated: true)
            } else {
                self.navigationController?.popViewController(animated: true)
            }
        }
        return view
    }()
    
    private let bottomContainerView: CustomView = {
        let view = CustomView(frame: .zero)
        view.backgroundColor = UIColor(hex: "1F2024")
        return view
    }()
    
    private let animationView: AnimationAreaView = {
        let view = AnimationAreaView(frame: .zero)
        return view
    }()
    
    private let animationPlayer: TXVodPlayer  = {
        let player = TXVodPlayer()
        player.loop = true
        return player
    }()
    
    public init(aiParams: StartAIConversationParams? = nil) {
        self.aiParams = aiParams
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
        registerObserveState()
       
        AIConversationManager.instance.start(aiParams: aiParams)
        view.backgroundColor = .black
    }
    
    deinit {
        unregisterObserveState()
    }
}

extension AIConversationViewController {
    
 
    
    private func constructViewHierarchy() {
        view.addSubview(animationView)
        view.addSubview(robotReplyView)
        view.addSubview(bottomContainerView)
        view.addSubview(popupView)
        bottomContainerView.addSubview(audioInputView)
        bottomContainerView.addSubview(functionView)
        bottomContainerView.addSubview(experienceView)
    }
    
    private func activateConstraints() {
        animationView.snp.makeConstraints { make in
            make.top.equalTo(view.safeAreaLayoutGuide).offset(62.scale375Height())
            make.left.right.equalToSuperview()
            make.bottom.equalTo(bottomContainerView.snp.top)
        }
        robotReplyView.snp.makeConstraints { make in
            make.leading.trailing.equalToSuperview().inset(36.scale375())
            make.height.equalTo(52.scale375Height())
            make.topMargin.equalToSuperview().offset(32.scale375())
            
        }
        bottomContainerView.snp.makeConstraints { make in
            make.height.equalTo(304.scale375Height())
            make.leading.trailing.bottom.equalToSuperview()
        }
        audioInputView.snp.makeConstraints { make in
            make.top.equalToSuperview().offset(32.scale375Height())
            make.leading.trailing.equalToSuperview().inset(32.scale375())
            make.height.equalTo(94.scale375Height())
        }
        functionView.snp.makeConstraints { make in
            make.bottom.equalTo(experienceView.snp.top).offset(-20.scale375Height())
            make.trailing.leading.equalToSuperview()
            make.height.equalTo(78.scale375Height())
        }
        experienceView.snp.makeConstraints { make in
            make.bottom.leading.trailing.equalToSuperview()
            make.height.equalTo(65.scale375Height())
        }
        popupView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
    
    private func bindInteraction() {
   
    }
    
    
}


extension AIConversationViewController {
    func registerObserveState() {
        AIConversationState.instance.expireDuraSec.addObserver(self) { [weak self] seconds, _ in
            guard let self = self else { return }
            self.experienceView.update(withSeconds: seconds)
            if seconds <= 0 {
                AIConversationManager.instance.stop()
                self.popUpContactView()
            }
        }
    }
    
    func unregisterObserveState() {
        AIConversationState.instance.expireDuraSec.removeObserver(self)
    }
    
    // MARK: Update UI
    func updateConversationState() {
        if AIConversationState.instance.conversationState.value == .stop {
            if Bundle.main.bundleIdentifier == RTCubeBDID ||
               Bundle.main.bundleIdentifier == TencentRTCBDID {
                let vc = EvaluationViewController()
                self.navigationController?.pushViewController(vc, animated: true)
            } else {
                self.navigationController?.popViewController(animated: true)
            }
           
        }
    }
    
    func popUpContactView() {
        AIConversationManager.instance.stop()
        view.bringSubviewToFront(popupView)
        popupView.isHidden = false
    }
}
