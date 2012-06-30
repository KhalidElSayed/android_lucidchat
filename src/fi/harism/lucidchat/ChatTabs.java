package fi.harism.lucidchat;

import android.content.Context;
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

	public void setTabs(String tabs[], View.OnClickListener listener) {
		ViewGroup views = (ViewGroup) findViewById(R.id.root_tabs_list);
		views.removeAllViews();
		LayoutInflater inflater = LayoutInflater.from(getContext());
		for (String tab : tabs) {
			Button button = (Button) inflater.inflate(R.layout.chat_tab, null);
			SpannableString span = new SpannableString(tab);
			span.setSpan(new UnderlineSpan(), 0, span.length(), 0);
			button.setText(span, BufferType.SPANNABLE);
			button.setOnClickListener(listener);
			views.addView(button);
		}
		invalidate();
	}

}
