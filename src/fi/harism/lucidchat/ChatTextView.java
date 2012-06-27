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

import java.util.Calendar;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Class for representing one line of text in a chat.
 */
public class ChatTextView extends TextView {

	private static final int COLOR_ACTION = 0xFF90B0A0;
	// Constant color values.
	private static final int COLOR_ERROR = 0xFFE26E9B;
	private static final int COLOR_LINK = 0xFFAACB63;
	private static final int COLOR_MESSAGE = 0xFFD0D0D0;
	private static final int COLOR_NICK = 0xFF7ba6d8;
	private static final int COLOR_NOTICE = 0xFFB0B0B0;
	private static final int COLOR_SERVER = 0xFF63CB63;
	private static final int COLOR_TIME = 0xFF3e97c1;

	// Spannable string builder instance.
	private SpannableStringBuilder mSpanBuilder = new SpannableStringBuilder();

	/**
	 * Default constructor.
	 */
	public ChatTextView(Context context) {
		super(context);
	}

	/**
	 * Default constructor.
	 */
	public ChatTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Default constructor.
	 */
	public ChatTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Parses nickname spannable from ChatMessage.
	 */
	private SpannableString getNick(ChatMessage event) {
		String from = "";
		// There's a nick only for PRIVMSG.
		if (event.mCommand == ChatMessage.CMD_PRIVMSG) {
			from = event.mFrom + ": ";
		}
		// In case of PRIVMSG_ACTION (/me).
		if (event.mCommand == ChatMessage.CMD_PRIVMSG_ACTION) {
			from = "* " + event.mFrom + " ";
		}
		// Create spannable with COLOR_NICK color.
		SpannableString span = new SpannableString(from);
		span.setSpan(new ForegroundColorSpan(COLOR_NICK), 0, span.length(), 0);
		return span;
	}

	/**
	 * Parses text part from ChatMessage.
	 */
	private SpannableString getText(ChatMessage event) {
		// Choose text color.
		int color = COLOR_SERVER;
		if (event.mCommand == ChatMessage.CMD_PRIVMSG) {
			color = COLOR_MESSAGE;
		}
		if (event.mCommand == ChatMessage.CMD_PRIVMSG_ACTION) {
			color = COLOR_ACTION;
		}
		if (event.mCommand == ChatMessage.CMD_EXCEPTION
				|| event.mCommand == ChatMessage.CMD_SERVERMSG_ERROR) {
			color = COLOR_ERROR;
		}

		// Create spannable with chosen color.
		SpannableString span = new SpannableString(event.mMessage);
		span.setSpan(new ForegroundColorSpan(color), 0, span.length(), 0);
		// Search for links.
		Linkify.addLinks(span, Linkify.ALL);
		// Change link span colors to COLOR_LINK.
		URLSpan spans[] = span.getSpans(0, span.length(), URLSpan.class);
		for (URLSpan s : spans) {
			int startIdx = span.getSpanStart(s);
			int endIdx = span.getSpanEnd(s);
			span.setSpan(new ForegroundColorSpan(COLOR_LINK), startIdx, endIdx,
					0);
		}
		return span;
	}

	/**
	 * Creates time spannable from time millis.
	 */
	private SpannableString getTime(long timeMillis) {
		// Generate time String.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeMillis);
		String time = String.format("%02d:%02d ",
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE));
		// Generate spannable from time string.
		SpannableString span = new SpannableString(time);
		span.setSpan(new ForegroundColorSpan(COLOR_TIME), 0, span.length(), 0);
		return span;
	}

	/**
	 * Sets formatted text from ChatMessage.
	 */
	public void setText(ChatMessage message) {
		// Generate new spannable text.
		mSpanBuilder.clear();
		mSpanBuilder.append(getTime(message.mTime));
		mSpanBuilder.append(getNick(message));
		mSpanBuilder.append(getText(message));
		// Enable links.
		setMovementMethod(LinkMovementMethod.getInstance());
		setText(mSpanBuilder, BufferType.SPANNABLE);
	}

}
