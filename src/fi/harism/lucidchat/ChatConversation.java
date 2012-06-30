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

import java.util.List;
import java.util.Vector;

public class ChatConversation {

	public static final String MODE_NORMAL = "";
	public static final String MODE_OPERATOR = "@";
	public static final String MODE_VOICE = "+";

	private String mId;
	private final Vector<ChatMessage> mMessages = new Vector<ChatMessage>();

	private final Vector<String> mParticipants = new Vector<String>();

	public ChatConversation(String id) {
		mId = id;
	}

	public void addMessage(ChatMessage message) {
		mMessages.add(message);
	}

	public void addNick(String nick) {
		if (nick.startsWith(MODE_OPERATOR) || nick.startsWith(MODE_VOICE)) {
			nick = nick.substring(1);
		}
		mParticipants.add(nick);
	}

	public boolean changeNick(String nickFrom, String nickTo) {
		if (mParticipants.remove(MODE_OPERATOR + nickFrom)) {
			mParticipants.add(MODE_OPERATOR + nickTo);
			return true;
		}
		if (mParticipants.remove(MODE_VOICE + nickFrom)) {
			mParticipants.add(MODE_VOICE + nickTo);
			return true;
		}
		if (mParticipants.remove(nickFrom)) {
			mParticipants.add(nickTo);
			return true;
		}
		return false;
	}

	public void changeNickMode(String nick, String mode) {
		removeNick(nick);
		mParticipants.add(mode + nick);
	}

	public List<ChatMessage> getMessages() {
		return mMessages;
	}

	public List<String> getNicks() {
		return mParticipants;
	}

	public boolean isChannel() {
		char c = mId.charAt(0);
		return c == '#' || c == '+' || c == '!' || c == '&';
	}

	public void removeNick(String nick) {
		mParticipants.remove(MODE_OPERATOR + nick);
		mParticipants.remove(MODE_VOICE + nick);
		mParticipants.remove(MODE_NORMAL + nick);
	}

}
