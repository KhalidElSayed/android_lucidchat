package fi.harism.lucidchat;

import java.io.StringWriter;

public final class ChatUtils {

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

	public static int getCommandInt(ChatEvent event) {
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

		int cmd = ChatUtils.getCommandInt(event);
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
