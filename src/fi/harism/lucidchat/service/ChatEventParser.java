package fi.harism.lucidchat.service;

import java.io.StringWriter;

public final class ChatEventParser {

	public static String concat(String[] parts, int first, int last) {
		StringWriter out = new StringWriter();
		for (int i = first; i <= last; ++i) {
			if (i > first) {
				out.append(' ');
			}
			out.append(parts[i]);
		}
		return out.toString();
	}

	public static ChatEvent parse(String message) {
		ChatEvent event = new ChatEvent();

		int idx = 0;
		String parts[] = message.split(" ");
		if (parts[idx].startsWith(":")) {
			event.mFrom = parseFrom(parts[idx++]);
		}

		event.mCommand = parts[idx++].toUpperCase();
		if (event.mCommand.equals("PRIVMSG")) {
			event.mTo = parts[idx++];
		}

		while (idx < parts.length && !parts[idx].startsWith(":")) {
			++idx;
		}
		event.mMessage = concat(parts, idx, parts.length - 1);

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
