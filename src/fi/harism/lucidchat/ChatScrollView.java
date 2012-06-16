package fi.harism.lucidchat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public class ChatScrollView extends ScrollView {

	private final ScrollDownRunnable mScrollDownRunnable = new ScrollDownRunnable();

	public ChatScrollView(Context context) {
		super(context);
	}

	public ChatScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChatScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void addView(View view) {
		ViewGroup listView = (ViewGroup) findViewById(R.id.scroll_list);
		listView.addView(view);
		if (getScrollY() + getHeight() >= view.getBottom() - 2) {
			post(mScrollDownRunnable);
		}
	}

	@Override
	public void removeAllViews() {
		ViewGroup listView = (ViewGroup) findViewById(R.id.scroll_list);
		listView.removeAllViews();
	}

	private class ScrollDownRunnable implements Runnable {

		@Override
		public void run() {
			fullScroll(View.FOCUS_DOWN);
		}

	}

}
