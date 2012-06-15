package fi.harism.lucidchat;

import java.io.StringWriter;
import java.util.Calendar;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;

public final class ChatUtils {

	private static final int COLOR_ERROR = 0xFFE26E9B;
	private static final int COLOR_LINK = 0xFFAACB63;
	private static final int COLOR_MESSAGE = 0xFFD0D0D0;
	private static final int COLOR_SERVER = 0xFF63CB63;
	private static final int COLOR_TIME = 0xFF4CBAED;

	private static final Calendar mCalendar = Calendar.getInstance();

	public static String concatParams(String[] parts, int first, int last) {
		boolean stripColon = true;
		StringWriter out = new StringWriter();
		for (int i = first; i <= last; ++i) {
			if (i > first) {
				out.append(' ');
			}
			if (stripColon && parts[i].charAt(0) == ':') {
				out.append(parts[i].substring(1));
				stripColon = false;
			} else {
				out.append(parts[i]);
			}
		}
		return out.toString();
	}

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
		if (event.mCommand.equals(ChatEvent.CMD_EXCEPTION)) {
			color = COLOR_ERROR;
		}
		if (Character.isDigit(event.mCommand.charAt(0))) {
			int code = Integer.parseInt(event.mCommand);
			if (code >= 400 && code < 600) {
				color = COLOR_ERROR;
			}
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

	public static ChatEvent parseEvent(String message) {
		ChatEvent event = new ChatEvent();

		int idx = 0;
		String parts[] = message.split(" ");
		if (parts[idx].startsWith(":")) {
			event.mFrom = parseFrom(parts[idx++]);
		}

		event.mCommand = parts[idx++].toUpperCase();
		if (event.mFrom != null) {
			event.mTo = parts[idx++];
		}

		event.mMessage = concatParams(parts, idx, parts.length - 1);

		return event;
	}

	public static String parseFrom(String from) {
		if (from.charAt(0) != ':') {
			return from;
		}

		int endIdx = from.indexOf('!');
		if (endIdx == -1) {
			endIdx = from.length();
		}

		return from.substring(1, endIdx);
	}

}
