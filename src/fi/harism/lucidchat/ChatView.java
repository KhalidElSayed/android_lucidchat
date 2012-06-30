package fi.harism.lucidchat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

public class ChatView extends RelativeLayout {

	private String mConversationId;
	private ScrollDownRunnable mScrollDownRunnable = new ScrollDownRunnable();

	public ChatView(Context context) {
		super(context);
	}

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChatView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void addView(View view) {
		ViewGroup viewGroup = (ViewGroup) findViewById(R.id.chat_scrollcontent);
		viewGroup.addView(view);
		if (getScrollY() + getHeight() >= view.getBottom() - 2) {
			post(mScrollDownRunnable);
		}
	}

	/**
	 * Getter for conversation id.
	 */
	public String getConversationId() {
		return mConversationId == null ? "" : mConversationId;
	}

	@Override
	public void removeAllViews() {
		ViewGroup viewGroup = (ViewGroup) findViewById(R.id.chat_scrollcontent);
		viewGroup.removeAllViews();
	}

	/**
	 * Setter for conversation id.
	 */
	public void setConversationId(String conversationId) {
		mConversationId = conversationId;
	}

	/**
	 * Private runnable for handling scroll down events.
	 */
	private class ScrollDownRunnable implements Runnable {
		@Override
		public void run() {
			ScrollView sv = (ScrollView) findViewById(R.id.chat_scrollview);
			sv.fullScroll(View.FOCUS_DOWN);
		}
	}

}
