//
//  EvaluationView.swift
//  AIConversationKit
//
//  Created on 2026/2/11.
//

import UIKit
import RTCCommon

// MARK: - Evaluation Result

struct EvaluationResult {
    var entiretyMark: Int = 0
    var callDelayMark: Int = 0
    var noiseReduceMark: Int = 0
    var aiMark: Int = 0
    var interactionMark: Int = 0
    var commentText: String = ""
}

// MARK: - EvaluationView

class EvaluationView: UIView {
    
    var submitHandler: ((_ result: EvaluationResult) -> Void)?
    var skipHandler: (() -> Void)?
    
    private var evaluationResult = EvaluationResult()
    private let isFirstTimeComment: Bool
    
    // MARK: - Subviews
    
    private let evaluationTitle: UILabel = {
        let label = UILabel()
        label.text = AIConversationLocalize("AIConversation.Evaluation.Title")
        label.font = .systemFont(ofSize: 20)
        label.textAlignment = .center
        label.textColor = .white
        return label
    }()
    
    private let qualityView = AIEmojiQualityView()
    
    private let starRatingView: AIRatingsContainerView = {
        let view = AIRatingsContainerView(titles: [
            AIConversationLocalize("AIConversation.Evaluation.callLatency"),
            AIConversationLocalize("AIConversation.Evaluation.NoiseSuppression"),
            AIConversationLocalize("AIConversation.Evaluation.AIResponse"),
            AIConversationLocalize("AIConversation.Evaluation.InteractiveExperience"),
        ])
        view.isHidden = true
        return view
    }()
    
    private let placeHolderView = UIView()
    
    private let placeHolderIconView: UIImageView = {
        let imageView = UIImageView(image: UIImage(inAIBundleNamed: "comment_placeholder_icon"))
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    
    private let placeHolderLabel: UILabel = {
        let label = UILabel()
        label.text = AIConversationLocalize("AIConversation.Evaluation.CommentPlaceHolder")
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    
    private lazy var textView: UITextView = {
        let textView = UITextView()
        textView.backgroundColor = UIColor(hex: "1F2024")
        textView.textColor = .white
        textView.delegate = self
        textView.font = .systemFont(ofSize: 14)
        textView.layer.cornerRadius = 8
        textView.layer.masksToBounds = true
        textView.isHidden = true
        return textView
    }()
    
    private let skipButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIColor(hex: "1F2024")?.trans2Image(), for: .normal)
        button.layer.shadowColor = UIColor(hex: "1F2024")?.cgColor ?? UIColor.blue.cgColor
        button.layer.cornerRadius = 24.scale375Height()
        button.layer.masksToBounds = true
        button.setTitle(AIConversationLocalize("AIConversation.Evaluation.Skip"), for: .normal)
        return button
    }()
    
    private let submitButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIColor(hex: "4086FF")?.trans2Image(), for: .normal)
        button.layer.shadowColor = UIColor(hex: "4086FF")?.cgColor ?? UIColor.blue.cgColor
        button.layer.cornerRadius = 24.scale375Height()
        button.layer.masksToBounds = true
        button.setTitle(AIConversationLocalize("AIConversation.Evaluation.Submit"), for: .normal)
        button.isEnabled = false
        return button
    }()
    
    // MARK: - Init
    
    init(isFirstTimeComment: Bool = true) {
        self.isFirstTimeComment = isFirstTimeComment
        super.init(frame: .zero)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
    
    private var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard window != nil, !isViewReady else { return }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        endEditing(true)
    }
}

// MARK: - UITextViewDelegate

extension EvaluationView: UITextViewDelegate {
    func textViewDidBeginEditing(_ textView: UITextView) {
        placeHolderView.isHidden = true
    }
    
    func textViewDidEndEditing(_ textView: UITextView) {
        placeHolderView.isHidden = !textView.text.isEmpty
        evaluationResult.commentText = textView.text
    }
}

// MARK: - Layout

private extension EvaluationView {
    
    func constructViewHierarchy() {
        addSubview(evaluationTitle)
        addSubview(qualityView)
        addSubview(starRatingView)
        addSubview(textView)
        textView.addSubview(placeHolderView)
        placeHolderView.addSubview(placeHolderIconView)
        placeHolderView.addSubview(placeHolderLabel)
        addSubview(skipButton)
        addSubview(submitButton)
    }
    
    func activateConstraints() {
        evaluationTitle.snp.makeConstraints { make in
            make.top.equalToSuperview()
            make.centerX.equalToSuperview()
        }
        qualityView.snp.makeConstraints { make in
            make.top.equalTo(evaluationTitle.snp.bottom).offset(24.scale375Height())
            make.leading.trailing.equalToSuperview().inset(4.scale375())
            make.height.equalTo(70.scale375())
        }
        starRatingView.snp.makeConstraints { make in
            make.top.equalTo(qualityView.snp.bottom).offset(36.scale375())
            make.height.equalTo(172.scale375Height())
            make.trailing.leading.equalToSuperview()
        }
        textView.snp.makeConstraints { make in
            make.top.equalTo(starRatingView.snp.bottom).offset(36.scale375Height())
            make.leading.trailing.equalToSuperview()
            make.height.equalTo(146.scale375Height())
        }
        placeHolderView.snp.makeConstraints { make in
            make.top.leading.trailing.equalToSuperview().inset(16.scale375())
            make.height.equalTo(22.scale375Height())
        }
        placeHolderIconView.snp.makeConstraints { make in
            make.leading.equalToSuperview()
            make.centerY.equalToSuperview()
        }
        placeHolderLabel.snp.makeConstraints { make in
            make.top.bottom.trailing.equalToSuperview()
            make.leading.equalTo(placeHolderIconView.snp.trailing)
        }
        if isFirstTimeComment {
            submitButton.snp.remakeConstraints { make in
                make.bottom.leading.trailing.equalToSuperview()
                make.height.equalTo(48.scale375Height())
            }
        } else {
            skipButton.snp.remakeConstraints { make in
                make.leading.bottom.equalToSuperview()
                make.width.equalTo(108.scale375())
                make.height.equalTo(48.scale375Height())
            }
            submitButton.snp.remakeConstraints { make in
                make.bottom.trailing.equalToSuperview()
                make.leading.equalTo(skipButton.snp.trailing).offset(12.scale375())
                make.height.equalTo(48.scale375Height())
            }
        }
    }
}

// MARK: - Binding

private extension EvaluationView {
    
    static let ratingKeyPaths: [String: WritableKeyPath<EvaluationResult, Int>] = [
        AIConversationLocalize("AIConversation.Evaluation.callLatency"): \.callDelayMark,
        AIConversationLocalize("AIConversation.Evaluation.NoiseSuppression"): \.noiseReduceMark,
        AIConversationLocalize("AIConversation.Evaluation.AIResponse"): \.aiMark,
        AIConversationLocalize("AIConversation.Evaluation.InteractiveExperience"): \.interactionMark,
    ]
    
    func bindInteraction() {
        submitButton.addTarget(self, action: #selector(submitClicked), for: .touchUpInside)
        skipButton.addTarget(self, action: #selector(skipClicked), for: .touchUpInside)
        
        qualityView.onEmojiSelected = { [weak self] mark in
            guard let self else { return }
            evaluationResult.entiretyMark = mark
            onEntiretyMarkChanged()
        }
        
        starRatingView.onRatingChanged = { [weak self] title, rating in
            guard let self, let keyPath = Self.ratingKeyPaths[title] else { return }
            evaluationResult[keyPath: keyPath] = rating
        }
    }
    
    func onEntiretyMarkChanged() {
        guard evaluationResult.entiretyMark > 0 else { return }
        starRatingView.isHidden = false
        textView.isHidden = false
        submitButton.isEnabled = true
    }
    
    @objc func submitClicked() {
        submitHandler?(evaluationResult)
    }
    
    @objc func skipClicked() {
        skipHandler?()
    }
}

// MARK: - AIEmojiView

private final class AIEmojiView: UIView {
    
    var identifier: String?
    let emojiImageView = UIImageView()
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.font = .systemFont(ofSize: 12)
        label.textAlignment = .center
        label.numberOfLines = 0
        return label
    }()
    
    init(title: String, imageName: String) {
        super.init(frame: .zero)
        emojiImageView.image = UIImage(inAIBundleNamed: imageName)
        titleLabel.text = title
        isUserInteractionEnabled = true
        addSubview(emojiImageView)
        addSubview(titleLabel)
        activateConstraints()
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
    
    private func activateConstraints() {
        emojiImageView.snp.makeConstraints { make in
            make.top.equalToSuperview()
            make.height.width.equalTo(42.scale375())
            make.centerX.equalToSuperview()
        }
        titleLabel.snp.makeConstraints { make in
            make.top.equalTo(emojiImageView.snp.bottom).offset(8.scale375Height())
            make.leading.trailing.equalToSuperview()
        }
    }
}

// MARK: - AIEmojiQualityView

private final class AIEmojiQualityView: UIView {
    
    var onEmojiSelected: ((_ mark: Int) -> Void)?
    
    private var emojiViews: [AIEmojiView] = []
    private let emojiImageNames = ["worse", "bad", "average", "nice", "very_nice"]
    private let emojiTitles = [
        AIConversationLocalize("AIConversation.Evaluation.VeryDissatisfied"),
        AIConversationLocalize("AIConversation.Evaluation.Dissatisfied"),
        AIConversationLocalize("AIConversation.Evaluation.Neutral"),
        AIConversationLocalize("AIConversation.Evaluation.AINice"),
        AIConversationLocalize("AIConversation.Evaluation.AIVeryNice"),
    ]
    
    // MARK: - Lifecycle
    
    private var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard window != nil, !isViewReady else { return }
        isViewReady = true
        setupEmojiViews()
        layoutEmojiViews()
    }
}

// MARK: - Setup

private extension AIEmojiQualityView {
    
    func setupEmojiViews() {
        for (imageName, title) in zip(emojiImageNames, emojiTitles) {
            let emojiView = AIEmojiView(title: title,
                                        imageName: "\(imageName)_unselected")
            emojiView.identifier = imageName
            addSubview(emojiView)
            emojiViews.append(emojiView)
            emojiView.snp.makeConstraints { make in
                make.width.equalTo(52.scale375())
                make.height.equalTo(70.scale375Height())
            }
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(emojiTapped(_:)))
            emojiView.addGestureRecognizer(tapGesture)
        }
    }
    
    func layoutEmojiViews() {
        let viewWidth = screenWidth - 72.scale375()
        let intervalWidth = (viewWidth - (5 * 52.scale375())) / 4
        for (index, emojiView) in emojiViews.enumerated() {
            emojiView.snp.makeConstraints { make in
                if index == 0 {
                    make.leading.equalToSuperview()
                } else {
                    make.leading.equalTo(emojiViews[index - 1].snp.trailing).offset(intervalWidth)
                }
            }
        }
    }
    
    @objc func emojiTapped(_ sender: UITapGestureRecognizer) {
        guard let tappedView = sender.view as? AIEmojiView,
              let baseImageName = tappedView.identifier else { return }
        
        for emojiView in emojiViews {
            if let imageName = emojiView.identifier {
                emojiView.emojiImageView.image = UIImage(inAIBundleNamed: "\(imageName)_unselected")
            }
        }
        tappedView.emojiImageView.image = UIImage(inAIBundleNamed: "\(baseImageName)_selected")
        
        if let index = emojiImageNames.firstIndex(of: baseImageName) {
            onEmojiSelected?(index + 1)
        }
    }
}

// MARK: - AIStarRatingView

private final class AIStarRatingView: UIView {
    
    var onRatingSelected: ((_ rating: Int) -> Void)?
    
    private var starImageViews: [UIImageView] = []
    
    private enum Layout {
        static let totalStars = 5
        static let starSize: CGFloat = 28.scale375()
        static let spacing: CGFloat = 6.scale375()
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupStars()
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
    
    private func setupStars() {
        for i in 0..<Layout.totalStars {
            let starImageView = UIImageView()
            starImageView.contentMode = .scaleAspectFit
            starImageView.image = UIImage(inAIBundleNamed: "star_unselected")
            starImageView.isUserInteractionEnabled = true
            starImageView.tag = i + 1
            addSubview(starImageView)
            starImageViews.append(starImageView)
            
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(starTapped(_:)))
            starImageView.addGestureRecognizer(tapGesture)
        }
        
        for (index, starImageView) in starImageViews.enumerated() {
            starImageView.snp.makeConstraints { make in
                make.width.height.equalTo(Layout.starSize)
                make.centerY.equalToSuperview()
                if index == 0 {
                    make.leading.equalToSuperview()
                } else {
                    make.leading.equalTo(starImageViews[index - 1].snp.trailing).offset(Layout.spacing)
                }
                if index == starImageViews.count - 1 {
                    make.trailing.equalToSuperview()
                }
            }
        }
    }
    
    @objc private func starTapped(_ sender: UITapGestureRecognizer) {
        guard let tappedStar = sender.view else { return }
        let rating = tappedStar.tag
        for (index, starImageView) in starImageViews.enumerated() {
            starImageView.image = UIImage(inAIBundleNamed: index < rating ? "star_selected" : "star_unselected")
        }
        onRatingSelected?(rating)
    }
}

// MARK: - AIStarRatingWithLabelView

private final class AIStarRatingWithLabelView: UIView {
    
    var onRatingChanged: ((_ rating: Int, _ title: String) -> Void)?
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .white
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    
    private let starRatingView = AIStarRatingView()
    private let title: String
    
    init(title: String) {
        self.title = title
        super.init(frame: .zero)
        titleLabel.text = title
        addSubview(titleLabel)
        addSubview(starRatingView)
        starRatingView.onRatingSelected = { [weak self] rating in
            guard let self else { return }
            onRatingChanged?(rating, title)
        }
        activateConstraints()
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
    
    private func activateConstraints() {
        titleLabel.snp.makeConstraints { make in
            make.leading.bottom.top.equalToSuperview()
        }
        starRatingView.snp.makeConstraints { make in
            make.centerY.equalTo(titleLabel)
            make.trailing.bottom.equalToSuperview()
        }
    }
}

// MARK: - AIRatingsContainerView

private final class AIRatingsContainerView: UIView {
    
    var onRatingChanged: ((_ title: String, _ rating: Int) -> Void)?
    
    private var ratingViews: [AIStarRatingWithLabelView] = []
    
    init(titles: [String]) {
        super.init(frame: .zero)
        setupRatingViews(titles: titles)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }
    
    private func setupRatingViews(titles: [String]) {
        var previousView: UIView?
        for title in titles {
            let ratingView = AIStarRatingWithLabelView(title: title)
            ratingView.onRatingChanged = { [weak self] rating, title in
                self?.onRatingChanged?(title, rating)
            }
            addSubview(ratingView)
            ratingViews.append(ratingView)
            ratingView.snp.makeConstraints { make in
                make.leading.trailing.equalToSuperview()
                make.height.equalTo(28.scale375())
                if let previous = previousView {
                    make.top.equalTo(previous.snp.bottom).offset(20.scale375Height())
                } else {
                    make.top.equalToSuperview()
                }
            }
            previousView = ratingView
        }
        ratingViews.last?.snp.makeConstraints { make in
            make.bottom.equalToSuperview()
        }
    }
}
