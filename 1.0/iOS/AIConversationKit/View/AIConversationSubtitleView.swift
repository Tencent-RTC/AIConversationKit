//
//  AIConversationSubtitleView.swift
//  AIConversationKit
//
//  Created on 2026/2/9.
//

import UIKit
import SnapKit
import Combine
import RTCCommon

public class AIConversationSubtitleView: UIView {
    
    private let store = AIConversationStore.shared
    private var cancellableSet = Set<AnyCancellable>()
    private var flattenedItems: [MessageItem] = []
    private var isUserScrolling = false
    
    private lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.backgroundColor = .clear
        tableView.separatorStyle = .none
        tableView.showsVerticalScrollIndicator = false
        tableView.allowsSelection = false
        tableView.dataSource = self
        tableView.delegate = self
        tableView.estimatedRowHeight = 60
        tableView.rowHeight = UITableView.automaticDimension
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 10, right: 0)
        tableView.register(UserMessageCell.self, forCellReuseIdentifier: UserMessageCell.reuseId)
        tableView.register(AIMessageCell.self, forCellReuseIdentifier: AIMessageCell.reuseId)
        return tableView
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private var isViewReady = false
    public override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else { return }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        subscribeState()
    }
}

private extension AIConversationSubtitleView {
    func constructViewHierarchy() {
        addSubview(tableView)
    }
    
    func activateConstraints() {
        tableView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
}

// MARK: - State Binding

private extension AIConversationSubtitleView {
    func subscribeState() {
        store.state.subscribe(StateSelector(keyPath: \.conversationMessageList))
            .receive(on: RunLoop.main)
            .sink { [weak self] messageList in
                guard let self else { return }
                self.updateMessages(messageList)
            }
            .store(in: &cancellableSet)
    }
}

// MARK: - Data Update

private extension AIConversationSubtitleView {
    
    func updateMessages(_ newList: [ConversationMessage]) {
        let newItems = newList.flatMap { $0.toMessageItems() }
        let oldCount = flattenedItems.count
        let newCount = newItems.count
        flattenedItems = newItems
        
        if oldCount == 0 || newCount < oldCount {
            tableView.reloadData()
            adjustContentInset()
            pinToBottom()
            return
        }
        
        let shouldPin = !isUserScrolling
        
        if newCount == oldCount {
            refreshVisibleCells()
            invalidateHeightsAndPin(shouldPin)
            return
        }
        
        let indexPaths = (oldCount..<newCount).map { IndexPath(row: $0, section: 0) }
        UIView.performWithoutAnimation {
            tableView.beginUpdates()
            tableView.insertRows(at: indexPaths, with: .none)
            tableView.endUpdates()
        }
        refreshVisibleCells()
        invalidateHeightsAndPin(shouldPin)
    }
    
    func refreshVisibleCells() {
        for cell in tableView.visibleCells {
            guard let indexPath = tableView.indexPath(for: cell),
                  let bubbleCell = cell as? MessageBubbleCell else { continue }
            bubbleCell.configure(text: flattenedItems[indexPath.row].text)
        }
    }
}

// MARK: - Layout & Scroll

private extension AIConversationSubtitleView {
    
    var maxContentOffsetY: CGFloat {
        let insets = tableView.contentInset
        return tableView.contentSize.height + insets.bottom - tableView.bounds.height
    }
    
    var isNearBottom: Bool {
        maxContentOffsetY <= 0 || tableView.contentOffset.y >= maxContentOffsetY - 30
    }
    
    func adjustContentInset() {
        tableView.layoutIfNeeded()
        let gap = tableView.bounds.height - tableView.contentSize.height - tableView.contentInset.bottom
        tableView.contentInset.top = max(gap, 0)
    }
    
    func invalidateHeightsAndPin(_ pin: Bool) {
        UIView.performWithoutAnimation {
            tableView.beginUpdates()
            tableView.endUpdates()
        }
        adjustContentInset()
        if pin { pinToBottom() }
    }
    
    func pinToBottom() {
        guard maxContentOffsetY > 0 else { return }
        tableView.contentOffset = CGPoint(x: 0, y: maxContentOffsetY)
    }
}

// MARK: - UITableViewDelegate

extension AIConversationSubtitleView: UITableViewDelegate {
    public func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        isUserScrolling = true
    }
    
    public func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate {
            isUserScrolling = !isNearBottom
        }
    }
    
    public func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        isUserScrolling = !isNearBottom
    }
}

// MARK: - UITableViewDataSource

extension AIConversationSubtitleView: UITableViewDataSource {
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        flattenedItems.count
    }
    
    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let item = flattenedItems[indexPath.row]
        let cell = tableView.dequeueReusableCell(withIdentifier: item.reuseId, for: indexPath)
        (cell as? MessageBubbleCell)?.configure(text: item.text)
        return cell
    }
}

// MARK: - MessageItem

private enum MessageItem {
    case user(String)
    case ai(String)
    
    var text: String {
        switch self {
        case .user(let t), .ai(let t): return t
        }
    }
    
    var reuseId: String {
        switch self {
        case .user: return UserMessageCell.reuseId
        case .ai:   return AIMessageCell.reuseId
        }
    }
}

private extension ConversationMessage {
    func toMessageItems() -> [MessageItem] {
        var items: [MessageItem] = []
        if !userSpeechText.isEmpty { items.append(.user(userSpeechText)) }
        if !aiSpeechText.isEmpty   { items.append(.ai(aiSpeechText)) }
        return items
    }
}

// MARK: - MessageBubbleCell

private class MessageBubbleCell: UITableViewCell {
    
    static var reuseId: String { String(describing: self) }
    
    let bubbleView = UIView()
    
    let messageLabel: UILabel = {
        let label = UILabel()
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.setContentCompressionResistancePriority(.required, for: .vertical)
        return label
    }()
    
    class var messageTextColor: UIColor { .black }
    
    private static let textAttributes: [NSAttributedString.Key: Any] = {
        let font = UIFont(name: "PingFangSC-Regular", size: 17) ?? .systemFont(ofSize: 17)
        let style = NSMutableParagraphStyle()
        style.lineHeightMultiple = 1.05
        style.alignment = .justified
        return [.font: font, .paragraphStyle: style]
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        selectionStyle = .none
        constructViewHierarchy()
        activateConstraints()
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func constructViewHierarchy() {
        bubbleView.layer.cornerRadius = 16
        contentView.addSubview(bubbleView)
        bubbleView.addSubview(messageLabel)
    }
    
    func activateConstraints() {
        messageLabel.snp.makeConstraints { make in
            make.edges.equalToSuperview().inset(UIEdgeInsets(top: 10, left: 14, bottom: 10, right: 14))
        }
    }
    
    func configure(text: String) {
        var attributes = Self.textAttributes
        attributes[.foregroundColor] = Self.messageTextColor
        messageLabel.attributedText = NSAttributedString(string: text, attributes: attributes)
    }
}

// MARK: - UserMessageCell

private final class UserMessageCell: MessageBubbleCell {
    
    override class var messageTextColor: UIColor { UIColor(0xFFFFFF, alpha: 0.9) }
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        bubbleView.backgroundColor = UIColor(0x4086FF, alpha: 0.85)
        bubbleView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMinXMaxYCorner, .layerMaxXMaxYCorner]
    }
    
    override func activateConstraints() {
        super.activateConstraints()
        bubbleView.snp.makeConstraints { make in
            make.top.equalToSuperview().offset(6)
            make.bottom.equalToSuperview().offset(-6)
            make.trailing.equalToSuperview().offset(-16)
            make.leading.greaterThanOrEqualToSuperview().offset(80)
        }
    }
}

// MARK: - AIMessageCell

private final class AIMessageCell: MessageBubbleCell {
    
    override class var messageTextColor: UIColor { UIColor(0x000000, alpha: 0.72) }
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        bubbleView.backgroundColor = UIColor(0xFFFFFF, alpha: 0.92)
        bubbleView.layer.maskedCorners = [.layerMaxXMinYCorner, .layerMinXMaxYCorner, .layerMaxXMaxYCorner]
    }
    
    override func activateConstraints() {
        super.activateConstraints()
        bubbleView.snp.makeConstraints { make in
            make.top.equalToSuperview().offset(6)
            make.bottom.equalToSuperview().offset(-6)
            make.leading.equalToSuperview().offset(16)
            make.trailing.lessThanOrEqualToSuperview().offset(-60)
        }
    }
}
