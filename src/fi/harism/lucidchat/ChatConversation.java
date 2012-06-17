package fi.harism.lucidchat;

import java.util.List;
import java.util.Vector;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatConversation implements Parcelable {

	public static final Parcelable.Creator<ChatConversation> CREATOR = new Parcelable.Creator<ChatConversation>() {
		@Override
		public ChatConversation createFromParcel(Parcel in) {
			return new ChatConversation(in);
		}

		@Override
		public ChatConversation[] newArray(int size) {
			return new ChatConversation[size];
		}
	};
	public static final String MODE_NORMAL = "";
	public static final String MODE_OPERATOR = "@";

	public static final String MODE_VOICE = "+";
	private final Vector<ChatMessage> mMessages = new Vector<ChatMessage>();

	private final Vector<String> mParticipants = new Vector<String>();

	public ChatConversation() {
	}

	private ChatConversation(Parcel in) {
		in.readTypedList(mMessages, ChatMessage.CREATOR);
		in.readStringList(mParticipants);
	}

	public void addMessage(ChatMessage message) {
		mMessages.add(message);
	}

	public void addNick(String nick, String mode) {
		mParticipants.add(mode + nick);
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

	@Override
	public int describeContents() {
		return 0;
	}

	public List<ChatMessage> getMessages() {
		return mMessages;
	}

	public List<String> getParticipants() {
		return mParticipants;
	}

	public void removeNick(String nick) {
		mParticipants.remove(MODE_OPERATOR + nick);
		mParticipants.remove(MODE_VOICE + nick);
		mParticipants.remove(MODE_NORMAL + nick);
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeTypedList(mMessages);
		out.writeStringList(mParticipants);
	}

}
