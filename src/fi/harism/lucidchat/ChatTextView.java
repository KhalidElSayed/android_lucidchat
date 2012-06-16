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

public class ChatTextView extends TextView {

	private static final int COLOR_ERROR = 0xFFE26E9B;
	private static final int COLOR_LINK = 0xFFAACB63;
	private static final int COLOR_MESSAGE = 0xFFD0D0D0;
	private static final int COLOR_NICK = 0xFF7ba6d8;
	private static final int COLOR_NOTICE = 0xFFB0B0B0;
	private static final int COLOR_SERVER = 0xFF63CB63;
	private static final int COLOR_TIME = 0xFF3e97c1;

	private final SpannableStringBuilder mSpanBuilder = new SpannableStringBuilder();

	public ChatTextView(Context context) {
		super(context);
	}

	public ChatTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChatTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private SpannableString getNick(ChatMessage event) {
		String from = "";
		if (ChatUtils.getCommandInt(event) < 0
				&& event.mCommand.equals("PRIVMSG")) {
			if (event.mMessage.startsWith("\u0001ACTION ")) {
				from = "* " + event.mFrom + " ";
			} else {
				from = event.mFrom + ": ";
			}
		}

		SpannableString span = new SpannableString(from);
		span.setSpan(new ForegroundColorSpan(COLOR_NICK), 0, span.length(), 0);
		return span;
	}

	private SpannableString getText(ChatMessage event) {
		int color = COLOR_SERVER;
		if (event.mCommand.equals("PRIVMSG")) {
			color = COLOR_MESSAGE;
		}
		if (event.mCommand.equals("NOTICE")) {
			color = COLOR_NOTICE;
		}
		if (event.mCommand.equals(ChatMessage.CMD_EXCEPTION)) {
			color = COLOR_ERROR;
		}
		if (ChatUtils.getCommandInt(event) >= 400
				&& ChatUtils.getCommandInt(event) < 600) {
			color = COLOR_ERROR;
		}

		SpannableString span;
		if (event.mMessage.startsWith("\u0001ACTION ")) {
			span = new SpannableString(event.mMessage.substring(8));
			color = COLOR_NOTICE;
		} else {
			span = new SpannableString(event.mMessage);
		}
		span.setSpan(new ForegroundColorSpan(color), 0, span.length(), 0);
		Linkify.addLinks(span, Linkify.ALL);
		URLSpan spans[] = span.getSpans(0, span.length(), URLSpan.class);
		for (URLSpan s : spans) {
			int startIdx = span.getSpanStart(s);
			int endIdx = span.getSpanEnd(s);
			span.setSpan(new ForegroundColorSpan(COLOR_LINK), startIdx, endIdx,
					0);
		}
		return span;
	}

	private SpannableString getTime(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeMillis);
		String time = String.format("%02d:%02d ",
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE));

		SpannableString span = new SpannableString(time);
		span.setSpan(new ForegroundColorSpan(COLOR_TIME), 0, span.length(), 0);
		return span;
	}

	public void setChatEvent(ChatMessage event) {
		mSpanBuilder.clear();
		mSpanBuilder.append(getTime(event.mTime));
		mSpanBuilder.append(getNick(event));
		mSpanBuilder.append(getText(event));

		setMovementMethod(LinkMovementMethod.getInstance());
		setText(mSpanBuilder, BufferType.SPANNABLE);
	}

}
