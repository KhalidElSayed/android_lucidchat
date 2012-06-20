/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.lucidchat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public class ChatScrollView extends ScrollView {

	private String mConversation;
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
		ViewGroup listView = (ViewGroup) findViewById(R.id.root_scroll_list);
		listView.addView(view);
		if (getScrollY() + getHeight() >= view.getBottom() - 2) {
			post(mScrollDownRunnable);
		}
	}

	public String getConversation() {
		return mConversation;
	}

	@Override
	public void removeAllViews() {
		ViewGroup listView = (ViewGroup) findViewById(R.id.root_scroll_list);
		listView.removeAllViews();
	}

	public void setConversation(String conversation) {
		mConversation = conversation;
	}

	private class ScrollDownRunnable implements Runnable {
		@Override
		public void run() {
			fullScroll(View.FOCUS_DOWN);
		}
	}

}
