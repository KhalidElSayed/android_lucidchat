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

import java.io.StringWriter;

/**
 * Utility methods for chat message parsing.
 */
public final class ChatUtils {

	/**
	 * Concats string from string array. Starting from index first until index
	 * last. If stripColon is set to true, first string starting with colon is
	 * replaced with substring(1).
	 */
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

	/**
	 * Parses IRC command into ChatMessage command.
	 */
	public static int getCommandInt(String command) {
		if (Character.isDigit(command.charAt(0))) {
			int cmd = Integer.parseInt(command);
			if (cmd >= 400 && cmd < 600) {
				return ChatMessage.CMD_SERVERMSG_ERROR;
			}
			if (cmd == 353) {
				return ChatMessage.CMD_NAMES;
			}
			if (cmd == 366) {
				return ChatMessage.CMD_NAMES_END;
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
			if (command.equals("NICK")) {
				return ChatMessage.CMD_NICK;
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

	/**
	 * Returns nickname for from string.
	 */
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

	/**
	 * Parses IRC message into ChatMessage.
	 */
	public static ChatMessage parseMessage(String message) {
		ChatMessage msg = new ChatMessage();

		// By default we have only command and message indices.
		int fromIdx = -1;
		int commandIdx = 0;
		int conversationIdx = -1;
		int messageIdx = 1;

		// Split message into parts.
		String parts[] = message.split(" ");

		// If first part starts with a colon it's a from string.
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

		if (cmd >= 0 || msg.mCommand == ChatMessage.CMD_PRIVMSG
				|| msg.mCommand == ChatMessage.CMD_PART) {
			conversationIdx = commandIdx + 1;
			messageIdx = conversationIdx + 1;
		}
		if (msg.mCommand == ChatMessage.CMD_NAMES) {
			conversationIdx = commandIdx + 3;
			messageIdx = conversationIdx + 1;
		}
		if (msg.mCommand == ChatMessage.CMD_NAMES_END) {
			conversationIdx = commandIdx + 2;
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
			msg.mConversationId = parts[conversationIdx];
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
