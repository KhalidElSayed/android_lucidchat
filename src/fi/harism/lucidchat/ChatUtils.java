package fi.harism.lucidchat;

import java.io.StringWriter;
import java.util.Calendar;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;

public final class ChatUtils {

	private static final int COLOR_ERROR = 0xFFE26E9B;
	private static final int COLOR_LINK = 0xFFAACB63;
	private static final int COLOR_MESSAGE = 0xFFD0D0D0;
	private static final int COLOR_NICK = 0xFFaecaea;
	private static final int COLOR_NOTICE = 0xFFB0B0B0;
	private static final int COLOR_SERVER = 0xFF63CB63;
	private static final int COLOR_TIME = 0xFF4CBAED;

	private static final Calendar mCalendar = Calendar.getInstance();

	public static String concat(String[] parts, int first, int last,
			boolean stripColon) {
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

	public static SpannableStringBuilder createSpannable(ChatEvent event) {

		int color = COLOR_SERVER;
		if (event.mCommand.equals("PRIVMSG")) {
			color = COLOR_MESSAGE;
		}
		if (event.mCommand.equals("NOTICE")) {
			color = COLOR_NOTICE;
		}
		if (event.mCommand.equals(ChatEvent.CMD_EXCEPTION)) {
			color = COLOR_ERROR;
		}
		if (getCommand(event) >= 400 && getCommand(event) < 600) {
			color = COLOR_ERROR;
		}

		SpannableStringBuilder output = new SpannableStringBuilder();
		SpannableString span;

		mCalendar.setTimeInMillis(event.mTime);
		String time = String.format("%02d:%02d ",
				mCalendar.get(Calendar.HOUR_OF_DAY),
				mCalendar.get(Calendar.MINUTE));
		span = new SpannableString(time);
		span.setSpan(new ForegroundColorSpan(COLOR_TIME), 0, span.length(), 0);
		output.append(span);

		if (getCommand(event) < 0) {
			if (event.mCommand.equals("PRIVMSG")) {
				String text = event.mFrom;
				if (event.mMessage.startsWith((char) 0x01 + "ACTION ")) {
					text = "* " + text + " ";
				} else {
					text += ": ";
				}
				span = new SpannableString(text);
				span.setSpan(new ForegroundColorSpan(COLOR_NICK), 0,
						span.length(), 0);
			} else {
				span = new SpannableString(event.mCommand + " ");
				span.setSpan(new ForegroundColorSpan(color), 0, span.length(),
						0);
			}
			output.append(span);
		}

		if (event.mMessage.startsWith((char) 0x01 + "ACTION ")) {
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
		output.append(span);

		return output;
	}

	public static int getCommand(ChatEvent event) {
		if (Character.isDigit(event.mCommand.charAt(0))) {
			return Integer.parseInt(event.mCommand);
		}
		return -1;
	}

	public static ChatEvent getEvent(String message) {
		ChatEvent event = new ChatEvent();

		int idx = 0;
		String parts[] = message.split(" ");
		if (parts[idx].startsWith(":")) {
			event.mFrom = getFrom(parts[idx++]);
		}

		event.mCommand = parts[idx++].toUpperCase();

		int cmd = ChatUtils.getCommand(event);
		if (cmd >= 0) {
			event.mTo = parts[idx++];
		}

		if (event.mCommand.equals("PRIVMSG")) {
			event.mTo = parts[idx++];
		}
		if (cmd == 353) {
			event.mTo = parts[idx += 2];
		}
		if (cmd == 432 || cmd == 433 || cmd == 436 || cmd == 437) {
			++idx;
		}

		event.mMessage = concat(parts, idx, parts.length - 1, true);

		return event;
	}

	public static String getFrom(String from) {
		if (from.charAt(0) != ':') {
			return from;
		}

		int endIdx = from.indexOf('!');
		if (endIdx == -1) {
			endIdx = from.length();
		}

		return from.substring(1, endIdx);
	}

	public static boolean setEvent(ChatEvent event, String text) {
		event.mFrom = event.mCommand = event.mTo = event.mMessage = null;

		text = text.trim();
		String parts[] = text.split(" ");
		if (parts.length >= 1) {
			parts[0] = parts[0].toUpperCase();
		}
		if (parts.length >= 2) {
			if (parts[0].equals("/JOIN")) {
				event.mCommand = "JOIN";
				event.mTo = parts[1];
			}
		}
		if (parts.length >= 3) {
			if (parts[0].equals("/MSG")) {
				event.mCommand = "PRIVMSG";
				event.mTo = parts[1];
				event.mMessage = concat(parts, 2, parts.length - 1, false);
			}
			if (parts[0].equals("/ME")) {
				event.mCommand = "PRIVMSG";
				event.mTo = parts[1];
				event.mMessage = ":" + (char) 0x01 + "ACTION "
						+ concat(parts, 2, parts.length - 1, false);
			}
		}

		return event.mCommand != null;
	}

}
