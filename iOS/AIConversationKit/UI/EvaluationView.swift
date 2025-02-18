//
//  EvaluationView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/25.
//

import UIKit
import RTCCommon

class EmojiView:UIView {
    var identifier: String?
    var imageView: UIImageView = {
        let view = UIImageView(frame: .zero)
        return view
    }()
    
    private let titlelabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.textColor = UIColor(red: 255.0/255.0, green: 255.0/255.0, blue: 255.0/255.0, alpha: 0.3)
        label.font = .systemFont(ofSize: 12)
        label.textAlignment = .center
        label.numberOfLines = 0
        return label
    }()
    
    init(withTitle title: String, imageName: String) {
        super.init(frame: .zero)
        setupView(withTitle: title, imageName: imageName)
    }
    
    func setupView(withTitle title: String, imageName: String) {
        imageView.image = UIImage(inAIBundleNamed: imageName)
        titlelabel.text = title
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
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
    }
    
    func constructViewHierarchy() {
        addSubview(imageView)
        addSubview(titlelabel)
    }
    
    func activateConstraints() {
        imageView.snp.makeConstraints { make in
            make.top.equalToSuperview()
            make.height.width.equalTo(42.scale375())
            make.centerX.equalToSuperview()
        }
        titlelabel.snp.makeConstraints { make in
            make.top.equalTo(imageView.snp.bottom).offset(8.scale375Height())
            make.leading.trailing.equalToSuperview()
        }
    }
}

class QualityView: UIView {
    var emojiViews: [EmojiView] = []
    
    let emojiImageNames = ["worse", "bad", "average", "nice", "very_nice",]
    let emojiTitles = [
        AIConversationLocalize("AIConversation.Evaluation.VeryDissatisfied"),
        AIConversationLocalize("AIConversation.Evaluation.Dissatisfied"),
        AIConversationLocalize("AIConversation.Evaluation.Neutral"),
        AIConversationLocalize("AIConversation.Evaluation.AINice"),
        AIConversationLocalize("AIConversation.Evaluation.AIVeryNice"),
    ]
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
        registerObserveState()
    }
    
    func configEmojiView() {
       
        for i in 0...emojiImageNames.count - 1 {
            let emojiView = EmojiView(withTitle: emojiTitles[i], imageName: "\(emojiImageNames[i])_unselected")
            emojiView.isUserInteractionEnabled = true
            emojiView.identifier = emojiImageNames[i]
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
    
    func constructViewHierarchy() {
        configEmojiView()
    }
    
    func activateConstraints() {
        let viewWidth = screenWidth - 72.scale375()
        let intervalWidth =  (viewWidth - (5 * 52.scale375())) / 4
        for (index, emojiView) in emojiViews.enumerated() {
            emojiView.snp.makeConstraints { make in
                if index == 0 {
                    make.left.equalToSuperview()
                } else {
                    make.left.equalTo(emojiViews[index - 1].snp.right).offset(intervalWidth)
                }
                
            }
        }
        
    }
    
    func bindInteraction() {
        
    }
    
    func registerObserveState() {
        
    }
}

extension QualityView {
    @objc func emojiTapped(_ sender: UITapGestureRecognizer) {
        guard let tappedView = sender.view as? EmojiView,
              let baseImageName = tappedView.identifier else {
            return
        }
        for emojiView in emojiViews {
            if let imageName = emojiView.identifier {
                let unselectedImageName = "\(imageName)_unselected"
                if let unselectedImage = UIImage(inAIBundleNamed: unselectedImageName) {
                    unselectedImage.accessibilityIdentifier = unselectedImageName
                    emojiView.imageView.image = unselectedImage
                }
            }
        }
        let selectedImageName = "\(baseImageName)_selected"
        if let selectedImage = UIImage(inAIBundleNamed: selectedImageName) {
            selectedImage.accessibilityIdentifier = selectedImageName
            tappedView.imageView.image = selectedImage
        }
        if let index = emojiImageNames.firstIndex(of: baseImageName) {
            AIConversationManager.instance.setEntireMark(mark: index + 1)
        }
    }
}

class StarRatingView: UIView {

    var selectRatingHandler: (_ index: Int) -> Void = {index in }
    private var starImageViews: [UIImageView] = []
    private let totalStars = 5
    private let starSize: CGFloat = 28.scale375()
    private let spacing: CGFloat = 6.scale375()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupStars()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupStars()
    }

    private func setupStars() {
        for i in 0..<totalStars {
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
                make.width.height.equalTo(starSize)
                if index == 0 {
                    make.left.equalToSuperview()
                } else {
                    make.left.equalTo(starImageViews[index - 1].snp.right).offset(spacing)
                }

                if index == starImageViews.count - 1 {
                    make.right.equalToSuperview()
                }
            }
        }
    }

    @objc private func starTapped(_ sender: UITapGestureRecognizer) {
        guard let tappedStar = sender.view else { return }
        let rating = tappedStar.tag
        updateStarSelection(rating: rating)
        selectRatingHandler(rating)
    }

    private func updateStarSelection(rating: Int) {
        for (index, starImageView) in starImageViews.enumerated() {
            if index < rating {
                starImageView.image = UIImage(inAIBundleNamed: "star_selected")
            } else {
                starImageView.image = UIImage(inAIBundleNamed: "star_unselected")
            }
        }
    }
}

class StarRatingWithLabelView: UIView {
    var selectRatingHandler: (_ index: Int, _ title: String?) -> Void = {index, title in }
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .white
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    
    private let starRatingView = StarRatingView()

    init(title: String) {
        super.init(frame: .zero)
        setupView(title: title)
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView(title: "")
    }

    private func setupView(title: String) {
        titleLabel.text = title
        addSubview(titleLabel)
        addSubview(starRatingView)
        starRatingView.selectRatingHandler = { [weak self] rating in
            guard let self = self else { return }
            self.selectRatingHandler(rating, titleLabel.text)
        }
        titleLabel.snp.makeConstraints { make in
            make.left.bottom.top.equalToSuperview()
        }
        starRatingView.snp.makeConstraints { make in
            make.centerY.equalTo(titleLabel)
            make.right.bottom.equalToSuperview()
        }
    }
}

class RatingsContainerView: UIView {
    
    private var ratingViews: [StarRatingWithLabelView] = []

    init(titles: [String]) {
        super.init(frame: .zero)
        setupRatingViews(titles: titles)
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupRatingViews(titles: [])
    }

    private func setupRatingViews(titles: [String]) {
        var previousView: UIView? = nil
        for title in titles {
            let ratingView = StarRatingWithLabelView(title: title)
            ratingView.selectRatingHandler = { rating, title in
                if title == AIConversationLocalize("AIConversation.Evaluation.callLatency") {
                    AIConversationManager.instance.setCallDelayMark(mark: rating)
                }
                if title == AIConversationLocalize("AIConversation.Evaluation.NoiseSuppression") {
                    AIConversationManager.instance.setNoiseReduceMark(mark: rating)
                }
                if title == AIConversationLocalize("AIConversation.Evaluation.AIResponse") {
                    AIConversationManager.instance.setAiMark(mark: rating)
                }
                if title == AIConversationLocalize("AIConversation.Evaluation.InteractiveExperience") {
                    AIConversationManager.instance.setInteractionMark(mark: rating)
                }
            }
            addSubview(ratingView)
            ratingViews.append(ratingView)
            ratingView.snp.makeConstraints { make in
                make.left.right.equalToSuperview()
                make.height.equalTo(28.scale375())
                if let previous = previousView {
                    make.top.equalTo(previous.snp.bottom).offset(20.scale375Height())
                } else {
                    make.top.equalToSuperview()
                }
            }
            previousView = ratingView
        }
        if let lastView = ratingViews.last {
            lastView.snp.makeConstraints { make in
                make.bottom.equalToSuperview()
            }
        }
    }
}


class EvaluationView: UIView {
    var submitHandler: (_ satify: Int,
                        _ callLatency: Int,
                        _ noise: Int,
                        _ aiResponse: Int,
                        _ interactive: Int,
                        _ feedbackContent: String)->Void = {satify,
         callLatency,
         noise,
         aiResponse,
         interactive,
         feedbackContent in }
    var skipHandler: ()->Void = {}
    private let evaluaTitle: UILabel = {
        let label = UILabel(frame: .zero)
        label.text = AIConversationLocalize("AIConversation.Evaluation.Title")
        label.font = .systemFont(ofSize: 20)
        label.textAlignment = .center
        label.textColor = .white
        return label
    }()
    
    private let qualityView: QualityView = {
        let view = QualityView(frame: .zero)
        return view
    }()
    
    private let starRatingView: RatingsContainerView = {
        let view = RatingsContainerView(titles: [AIConversationLocalize("AIConversation.Evaluation.callLatency"),
                                                 AIConversationLocalize("AIConversation.Evaluation.NoiseSuppression"),
                                                 AIConversationLocalize("AIConversation.Evaluation.AIResponse"),
                                                 AIConversationLocalize("AIConversation.Evaluation.InteractiveExperience"),])
        return view
    }()
    private let placeHolderView: UIView = {
        let view  = UIView(frame: .zero)
        return view
    }()
    
    private let placeHolderIconView: UIImageView = {
        let imageView  = UIImageView(image: UIImage(inAIBundleNamed: "comment_placeholder_icon"))
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    
    private let placeHolderLabel: UILabel = {
        let label  = UILabel(frame: .zero)
        label.text = AIConversationLocalize("AIConversation.Evaluation.CommentPlaceHolder")
        label.textColor = UIColor(0xFFFFFF, alpha: 0.3)
        label.font = .systemFont(ofSize: 14)
        return label
    }()
    
    private lazy var textView: UITextView = {
        let textView = UITextView(frame: .zero)
        textView.backgroundColor = UIColor(hex: "1F2024")
        textView.textColor = .white
        textView.delegate = self
        textView.font = .systemFont(ofSize: 14)
        textView.layer.cornerRadius = 8
        textView.layer.masksToBounds = true
        return textView
    }()
    
    let skipButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIColor(hex: "1F2024")?.trans2Image(), for: .normal)
        button.layer.shadowColor = UIColor(hex: "1F2024")?.cgColor ?? UIColor.blue.cgColor
        button.layer.cornerRadius = 24.scale375Height()
        button.layer.masksToBounds = true
        button.setTitle(AIConversationLocalize("AIConversation.Evaluation.Skip"), for: .normal)
        return button
    }()
    
    let submitButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIColor(hex: "4086FF")?.trans2Image(), for: .normal)
        button.layer.shadowColor = UIColor(hex: "4086FF")?.cgColor ?? UIColor.blue.cgColor
        button.layer.cornerRadius = 24.scale375Height()
        button.layer.masksToBounds = true
        button.setTitle(AIConversationLocalize("AIConversation.Evaluation.Submit"), for: .normal)
        button.isEnabled = false
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
        registerObserveState()
        AIConversationManager.instance.setEntireMark(mark: 0)
        AIConversationManager.instance.setCallDelayMark(mark: 0)
        AIConversationManager.instance.setNoiseReduceMark(mark: 0)
        AIConversationManager.instance.setAiMark(mark: 0)
        AIConversationManager.instance.setInteractionMark(mark: 0)
        AIConversationManager.instance.setCommentText(text: "")
    }
    
    func constructViewHierarchy() {
        addSubview(evaluaTitle)
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
        evaluaTitle.snp.makeConstraints { make in
            make.top.equalToSuperview()
            make.centerX.equalToSuperview()
        }
        qualityView.snp.makeConstraints { make in
            make.top.equalTo(evaluaTitle.snp.bottom).offset(24.scale375Height())
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
            make.left.equalTo(placeHolderIconView.snp.right)
        }
        if AIConversationState.instance.isFirstTimeComment.value == true {
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
                make.left.equalTo(skipButton.snp.right).offset(12.scale375())
                make.height.equalTo(48.scale375Height())
            }
            
        }
    }
    
    func bindInteraction() {
        submitButton.addTarget(self, action: #selector(submitClicked), for: .touchUpInside)
        skipButton.addTarget(self, action: #selector(skipClicked), for: .touchUpInside)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        endEditing(true)
    }
    
    deinit {
        unregisterObserveState()
    }
}

extension EvaluationView {
    @objc func submitClicked() {
        submitHandler(AIConversationState.instance.entiretyMark.value,
                      AIConversationState.instance.callDelayMark.value,
                      AIConversationState.instance.noiseReduceMark.value,
                      AIConversationState.instance.noiseReduceMark.value,
                      AIConversationState.instance.interactionMark.value,
                      AIConversationState.instance.commentText.value)
    }
    
    @objc func skipClicked() {
        skipHandler()
    }
}

extension EvaluationView: UITextViewDelegate {
    
    func textViewDidBeginEditing(_ textView: UITextView) {
          placeHolderView.isHidden = true
    }
    func textViewDidEndEditing(_ textView: UITextView) {
        if textView.text.count <= 0 {
            placeHolderView.isHidden = false
        } else {
            placeHolderView.isHidden = true
            AIConversationManager.instance.setCommentText(text: "")
        }
    }
}

extension EvaluationView {
    func registerObserveState() {
        AIConversationState.instance.entiretyMark.addObserver(self) { [weak self] _, _ in
            guard let self = self else { return }
            self.updateSubmitButtonState()
        }
        AIConversationState.instance.callDelayMark.addObserver(self) { [weak self] _, _ in
            guard let self = self else { return }
            self.updateSubmitButtonState()
        }
        AIConversationState.instance.noiseReduceMark.addObserver(self) { [weak self] _, _ in
            guard let self = self else { return }
            self.updateSubmitButtonState()
        }
        AIConversationState.instance.aiMark.addObserver(self) { [weak self] _, _ in
            guard let self = self else { return }
            self.updateSubmitButtonState()
        }
        AIConversationState.instance.interactionMark.addObserver(self) { [weak self] _, _ in
            guard let self = self else { return }
            self.updateSubmitButtonState()
        }
        AIConversationState.instance.commentText.addObserver(self) { [weak self] _, _ in
            guard let self = self else { return }
            self.updateSubmitButtonState()
        }
        
        
    }
    
    func unregisterObserveState() {
        AIConversationState.instance.entiretyMark.removeObserver(self)
        AIConversationState.instance.callDelayMark.removeObserver(self)
        AIConversationState.instance.noiseReduceMark.removeObserver(self)
        AIConversationState.instance.aiMark.removeObserver(self)
        AIConversationState.instance.interactionMark.removeObserver(self)
        AIConversationState.instance.commentText.removeObserver(self)
    }
    
    func updateSubmitButtonState() {
        let entiretyMark = AIConversationState.instance.entiretyMark.value
        let callDelayMark = AIConversationState.instance.callDelayMark.value
        let noiseReduceMark = AIConversationState.instance.noiseReduceMark.value
        let aiMark = AIConversationState.instance.aiMark.value
        let interactionMark = AIConversationState.instance.interactionMark.value
        if entiretyMark > 0 &&
            callDelayMark > 0 &&
            noiseReduceMark > 0 &&
            aiMark > 0 &&
            interactionMark > 0
        {
            submitButton.isEnabled = true
        }
    }
}
