//
//  EvalutionViewController.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/25.
//

import UIKit
import TUICore

class EvaluationViewController: UIViewController {

    private lazy var evaluationView: EvaluationView = {
        let view = EvaluationView(frame: .zero)
        view.submitHandler = { [weak self] satify, callLatency, noise, aiResponse, interactive, feedbackContent in
            guard let self = self else { return }
            let params = ["entirety": "\(satify)",
                          "callDelay": "\(callLatency)",
                          "noiseReduce": "\(noise)",
                          "ai": "\(aiResponse)",
                          "tone":"\(5)",
                          "interaction":"\(interactive)",
                          "feedback":"\(feedbackContent)",]
            TUICore.callService("TUICore_AIConversationSevice",
                                method: "TUICore_AIConversationSevice_Add_Feedback",
                                param: params)
            { errCode , errMsg , resultParams in
                if errCode == 0 {
                    
                } else {
                    
                }
            }
        }
        
        view.skipHandler = {
        }
        return view
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        constructViewHierarchy()
        activateConstraints()
        view.backgroundColor = .black
        // Do any additional setup after loading the view.
    }
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
