package com.trtc.uikit.aiconversationkit.view

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.store.AIConversationStore
import com.trtc.uikit.aiconversationkit.store.ConversationMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AIConversationSubtitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val subtitleAdapter = SubtitleAdapter()
    private val dataObserver = DataObserver()
    private var observeJob: Job? = null
    private var isAutoScrollEnabled: Boolean = true

    init {
        initView()
    }

    private fun initView() {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.stackFromEnd = true
        layoutManager = linearLayoutManager
        adapter = subtitleAdapter
        itemAnimator = null
        setBackgroundColor(Color.TRANSPARENT)
        addItemDecoration(SpaceItemDecoration(context, 8))
        setupScrollListener()
    }

    private fun setupScrollListener() {
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    SCROLL_STATE_DRAGGING,
                    SCROLL_STATE_SETTLING -> {
                        isAutoScrollEnabled = false
                    }
                    SCROLL_STATE_IDLE -> {
                        isAutoScrollEnabled = isAtBottom()
                    }
                }
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observeJob = CoroutineScope(Dispatchers.Main).launch {
            AIConversationStore.shared.conversationState
                .conversationMessageList.collect { messages ->
                    subtitleAdapter.submitList(messages.toList())
                }
        }
        subtitleAdapter.registerAdapterDataObserver(dataObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        observeJob?.cancel()
        observeJob = null
        subtitleAdapter.unregisterAdapterDataObserver(dataObserver)
    }


    private fun isAtBottom(): Boolean {
        val lm = layoutManager as? LinearLayoutManager ?: return true
        val lastVisiblePosition = lm.findLastVisibleItemPosition()
        val itemCount = subtitleAdapter.itemCount
        return lastVisiblePosition >= itemCount - 2
    }

    private fun scrollToBottomIfNeeded() {
        if (isAutoScrollEnabled && subtitleAdapter.itemCount > 0) {
            scrollToPosition(subtitleAdapter.itemCount - 1)
        }
    }

    private inner class DataObserver: AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            scrollToBottomIfNeeded()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            scrollToBottomIfNeeded()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            scrollToBottomIfNeeded()
        }
    }

    private class SubtitleAdapter : ListAdapter<ConversationMessage, SubtitleViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtitleViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.conversation_item_subtitle, parent, false)
            return SubtitleViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubtitleViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class SubtitleViewHolder(itemView: View) : ViewHolder(itemView) {
        private val tvUserMessage: TextView = itemView.findViewById(R.id.tv_user_message)
        private val tvAiMessage: TextView = itemView.findViewById(R.id.tv_ai_message)

        fun bind(message: ConversationMessage) {
            if (message.userSpeechText.isNotEmpty()) {
                tvUserMessage.text = message.userSpeechText
                tvUserMessage.visibility = VISIBLE
            } else {
                tvUserMessage.visibility = GONE
            }

            if (message.aiSpeechText.isNotEmpty()) {
                tvAiMessage.text = message.aiSpeechText
                tvAiMessage.visibility = VISIBLE
            } else {
                tvAiMessage.visibility = GONE
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ConversationMessage>() {
        override fun areItemsTheSame(oldItem: ConversationMessage, newItem: ConversationMessage): Boolean {
            return oldItem.roundId == newItem.roundId
        }

        override fun areContentsTheSame(oldItem: ConversationMessage, newItem: ConversationMessage): Boolean {
            return oldItem.userSpeechText == newItem.userSpeechText
                    && oldItem.aiSpeechText == newItem.aiSpeechText
        }
    }

    private class SpaceItemDecoration(context: Context, spaceDp: Int) : ItemDecoration() {
        private val spacePx = (spaceDp * context.resources.displayMetrics.density).toInt()

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            outRect.bottom = spacePx
        }
    }
}
