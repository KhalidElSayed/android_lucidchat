package fi.harism.lucidchat;

import java.util.Calendar;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import fi.harism.lucidchat.service.ChatEvent;

public final class ChatUtils {

	private static final int COLOR_LINK = 0xFFAACB63;
	private static final int COLOR_MESSAGE = 0xFFD0D0D0;
	private static final int COLOR_SERVER = 0xFF63CB63;
	private static final int COLOR_TIME = 0xFF4CBAED;

	private static final Calendar mCalendar = Calendar.getInstance();

	public static SpannableString createSpannable(ChatEvent event) {
		mCalendar.setTimeInMillis(event.mTime);
		String time = String.format("%02d:%02d ",
				mCalendar.get(Calendar.HOUR_OF_DAY),
				mCalendar.get(Calendar.MINUTE));
		String text = time + event.mMessage;

		SpannableString span = new SpannableString(text);
		span.setSpan(new ForegroundColorSpan(COLOR_TIME), 0, time.length(), 0);

		int color = COLOR_SERVER;
		if (event.mCommand.equals("PRIVMSG")) {
			color = COLOR_MESSAGE;
		}
		span.setSpan(new ForegroundColorSpan(color), time.length(),
				text.length(), 0);

		Linkify.addLinks(span, Linkify.ALL);
		URLSpan spans[] = span.getSpans(0, text.length(), URLSpan.class);
		for (URLSpan s : spans) {
			int startIdx = span.getSpanStart(s);
			int endIdx = span.getSpanEnd(s);
			span.setSpan(new ForegroundColorSpan(COLOR_LINK), startIdx, endIdx,
					0);
		}

		return span;
	}

}
