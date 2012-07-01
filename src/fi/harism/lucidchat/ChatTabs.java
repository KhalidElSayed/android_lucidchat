package fi.harism.lucidchat;

import android.content.Context;
import android.graphics.Rect;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView.BufferType;

public class ChatTabs extends HorizontalScrollView {

	public ChatTabs(Context context) {
		super(context);
	}

	public ChatTabs(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChatTabs(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setSelectedTab(int index) {
		ViewGroup views = (ViewGroup) findViewById(R.id.root_tabs_list);
		for (int i = 0; i < views.getChildCount(); ++i) {
			View v = views.getChildAt(i);
			if (i == index) {
				v.setEnabled(false);
				Rect r = new Rect();
				v.getHitRect(r);
				int x = computeScrollDeltaToGetChildRectOnScreen(r);
				smoothScrollTo(x, 0);
			} else {
				v.setEnabled(true);
			}
		}
	}

	public void setTabs(String tabs[], View.OnClickListener listener) {
		ViewGroup views = (ViewGroup) findViewById(R.id.root_tabs_list);
		views.removeAllViews();
		LayoutInflater inflater = LayoutInflater.from(getContext());
		for (int i = 0; i < tabs.length; ++i) {
			String tab = tabs[i];
			if (i == 0) {
				tab = "Status";
			}
			Button button = (Button) inflater.inflate(R.layout.chat_tab, null);
			SpannableString span = new SpannableString(tab);
			span.setSpan(new UnderlineSpan(), 0, span.length(), 0);
			button.setText(span, BufferType.SPANNABLE);
			button.setOnClickListener(listener);
			button.setTag(tabs[i]);
			views.addView(button);
		}
		invalidate();
	}

}
