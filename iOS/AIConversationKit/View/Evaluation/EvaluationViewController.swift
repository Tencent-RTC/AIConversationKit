//
//  EvaluationViewController.swift
//  AIConversationKit
//
//  Created on 2026/2/11.
//

import UIKit
import TUICore

class EvaluationViewController: UIViewController {
    
    // MARK: - Subviews
    
    private lazy var evaluationView: EvaluationView = {
        let view = EvaluationView(isFirstTimeComment: isFirstTimeComment)
        view.submitHandler = { [weak self] result in
            self?.submitEvaluation(result)
        }
        view.skipHandler = { [weak self] in
            self?.exitPage()
        }
        return view
    }()
    
    // MARK: - Properties
    
    private let isFirstTimeComment: Bool
    private weak var sourceViewController: UIViewController?
    
    // MARK: - Init
    
    init(isFirstTimeComment: Bool = true, sourceViewController: UIViewController? = nil) {
        self.isFirstTimeComment = isFirstTimeComment
        self.sourceViewController = sourceViewController
        super.init(nibName: nil, bundle: nil)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
    
    // MARK: - Lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        if let sourceVC = sourceViewController {
            navigationController?.viewControllers.removeAll(where: { $0 === sourceVC })
        }
        constructViewHierarchy()
        activateConstraints()
    }
    
    override var prefersStatusBarHidden: Bool { true }
}

// MARK: - Layout

private extension EvaluationViewController {
    
    func constructViewHierarchy() {
        view.addSubview(evaluationView)
    }
    
    func activateConstraints() {
        evaluationView.snp.makeConstraints { make in
            make.topMargin.equalTo(56.scale375())
            make.leading.trailing.equalToSuperview().inset(36.scale375())
            make.bottom.equalToSuperview().inset(124.scale375Height())
        }
    }
}

// MARK: - Action

private extension EvaluationViewController {
    
    static let defaultToneRating = 5
    
    func submitEvaluation(_ result: EvaluationResult) {
        let params = [
            "entirety": "\(result.entiretyMark)",
            "callDelay": "\(result.callDelayMark)",
            "noiseReduce": "\(result.noiseReduceMark)",
            "ai": "\(result.aiMark)",
            "tone": "\(Self.defaultToneRating)",
            "interaction": "\(result.interactionMark)",
            "feedback": result.commentText,
        ]
        TUICore.callService("TUICore_AIConversationService",
                            method: "TUICore_AIConversationService_Add_Feedback",
                            param: params)
        { [weak self] _, _, _ in
            self?.exitPage()
        }
    }
    
    func exitPage() {
        if let navigationController, navigationController.viewControllers.count > 1 {
            navigationController.popViewController(animated: true)
        } else {
            dismiss(animated: true)
        }
    }
}
