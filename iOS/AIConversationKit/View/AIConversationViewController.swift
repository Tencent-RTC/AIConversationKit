//
//  AIConversationViewController.swift
//  AIConversationKit
//
//  Created on 2026/2/10.
//

import UIKit
import TUICore

public class AIConversationViewController: UIViewController {
    
    private let config: AIConversationConfig
    private lazy var conversationView = AIConversationView()
    private var hasStartedConversation = false
    private var hasEndedConversation = false
    
    private lazy var popupView: AIContactUsPopView = {
        let view = AIContactUsPopView()
        view.isHidden = true
        view.closeHandler = { [weak self, weak view] in
            guard let self else { return }
            view?.isHidden = true
            if PackageService.isInternalDemo {
                pushEvaluationPage()
            }
        }
        view.contactUsHandler = { [weak self, weak view] in
            guard let self else { return }
            view?.isHidden = true
            if PackageService.isRTCube {
                guard let url = URL(string: .contactUsURL) else { return }
                TUITool.openLink(with: url)
                exitViewController()
            } else {
                exitViewController(animated: false)
                TUICore.callService("TUICore_ContactUsService",
                                    method: "TUICore_ContactService_gotoContactUS",
                                    param: [:])
            }
        }
        return view
    }()
    
    public init(config: AIConversationConfig) {
        self.config = config
        super.init(nibName: nil, bundle: nil)
        modalPresentationStyle = .fullScreen
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        AIConversationState.instance.expireDuraSec.removeObserver(self)
        Logger.info("AIConversationViewController deinit")
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        navigationController?.setNavigationBarHidden(true, animated: false)
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        startConversationIfNeeded()
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    public override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        guard isMovingFromParent || isBeingDismissed else { return }
        conversationView.onHangUp = nil
        conversationView.onRoomDismissed = nil
        AIConversationState.instance.expireDuraSec.removeObserver(self)
    }
    
    public override var prefersStatusBarHidden: Bool { false }
    public override var preferredStatusBarStyle: UIStatusBarStyle { .lightContent }
}

// MARK: - Layout

private extension AIConversationViewController {
    func constructViewHierarchy() {
        view.backgroundColor = .black
        view.addSubview(conversationView)
    }
    
    func activateConstraints() {
        conversationView.frame = view.bounds
        conversationView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }
    
    func bindInteraction() {
        conversationView.onHangUp = { [weak self] in
            self?.onConversationEnded()
        }
        conversationView.onRoomDismissed = { [weak self] in
            self?.onConversationEnded()
        }
        if PackageService.isInternalDemo {
            registerExpireDurationObserver()
        }
    }
}

// MARK: - Experience Duration (AppStore Demo)

private extension AIConversationViewController {
    func registerExpireDurationObserver() {
        AIConversationState.instance.expireDuraSec.addObserver(self) { [weak self] seconds, _ in
            guard let self else { return }
            guard !AIConversationState.isUnlimitedTime(seconds), seconds <= 0 else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                AIConversationStore.shared.stopAIConversation(completion: nil)
                self.showContactUsPopup()
            }
        }
    }
    
    func startConversationIfNeeded() {
        guard !hasStartedConversation else { return }
        hasStartedConversation = true
        conversationView.startAIConversation(config: config) { code, message in
            if code != 0 {
                Logger.error("startAIConversation failed, code:\(code), message:\(message)")
            }
        }
    }
    
    func showContactUsPopup() {
        if popupView.superview == nil {
            view.addSubview(popupView)
            popupView.frame = view.bounds
            popupView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        }
        view.bringSubviewToFront(popupView)
        popupView.isHidden = false
    }
}

// MARK: - Navigation

private extension AIConversationViewController {
    func onConversationEnded() {
        guard !hasEndedConversation else { return }
        hasEndedConversation = true
        if PackageService.isInternalDemo {
            pushEvaluationPage()
        } else {
            exitViewController()
        }
    }
    
    func pushEvaluationPage() {
        let isFirstTime = AIConversationState.instance.isFirstTimeComment.value
        let evaluationVC = EvaluationViewController(isFirstTimeComment: isFirstTime,
                                                    sourceViewController: self)
        navigationController?.pushViewController(evaluationVC, animated: true)
    }
    
    func exitViewController(animated: Bool = true) {
        if let navigationController {
            navigationController.popViewController(animated: animated)
        } else {
            dismiss(animated: animated)
        }
    }
}

// MARK: - Constants

private extension String {
    static let contactUsURL = "https://cloud.tencent.com/apply/p/dlr7v7lxbwf"
}
