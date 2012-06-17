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

	public static int getCommandInt(String command) {
		if (Character.isDigit(command.charAt(0))) {
			int cmd = Integer.parseInt(command);
			if (cmd >= 400 && cmd < 600) {
				return ChatMessage.CMD_SERVERMSG_ERROR;
			}
			return ChatMessage.CMD_SERVERMSG;
		} else {
			if (command.equalsIgnoreCase("PRIVMSG")) {
				return ChatMessage.CMD_PRIVMSG;
			}
			if (command.equalsIgnoreCase("ERROR")) {
				return ChatMessage.CMD_SERVERMSG_ERROR;
			}
			if (command.equals("JOIN")) {
				return ChatMessage.CMD_JOIN;
			}
			if (command.equals("PART")) {
				return ChatMessage.CMD_PART;
			}
			if (command.equals("PING")) {
				return ChatMessage.CMD_PING;
			}
		}
		return ChatMessage.CMD_UNKNOWN;
	}

	public static String getFrom(String from) {
		if (from.charAt(0) != ':') {
			return "";
		}
		int endIdx = from.indexOf('!');
		if (endIdx == -1) {
			return "";
		}
		return from.substring(1, endIdx);
	}

	public static ChatMessage parseMessage(String message) {
		ChatMessage msg = new ChatMessage();

		int fromIdx = -1;
		int commandIdx = 0;
		int conversationIdx = -1;
		int messageIdx = 1;

		String parts[] = message.split(" ");
		if (parts[0].startsWith(":")) {
			fromIdx = 0;
			commandIdx = 1;
			messageIdx = 2;
		}

		int cmd = -1;
		msg.mCommand = getCommandInt(parts[commandIdx]);
		if (Character.isDigit(parts[commandIdx].charAt(0))) {
			cmd = Integer.parseInt(parts[commandIdx]);
		}

		if (cmd >= 0 || msg.mCommand == ChatMessage.CMD_PRIVMSG) {
			conversationIdx = commandIdx + 1;
			messageIdx = conversationIdx + 1;
		}
		if (cmd == 353) {
			conversationIdx = commandIdx + 3;
			messageIdx = conversationIdx + 1;
		}
		if (msg.mCommand == ChatMessage.CMD_SERVERMSG_ERROR) {
			while (!parts[messageIdx].startsWith(":")) {
				++messageIdx;
			}
		}

		if (fromIdx >= 0) {
			msg.mFrom = getFrom(parts[fromIdx]);
		}
		if (conversationIdx >= 0) {
			msg.mConversation = parts[conversationIdx];
		}
		msg.mMessage = concat(parts, messageIdx, parts.length - 1, true);

		if (msg.mCommand == ChatMessage.CMD_PRIVMSG
				&& msg.mMessage.charAt(0) == '\u0001') {
			if (msg.mMessage.startsWith("\u0001ACTION ")) {
				msg.mMessage = msg.mMessage.substring(8);
				msg.mCommand = ChatMessage.CMD_PRIVMSG_ACTION;
			}
		}

		return msg;
	}

}
