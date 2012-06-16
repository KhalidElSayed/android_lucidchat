package fi.harism.lucidchat;

import java.util.Vector;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatMessageList implements Parcelable {

	public static final Parcelable.Creator<ChatMessageList> CREATOR = new Parcelable.Creator<ChatMessageList>() {
		@Override
		public ChatMessageList createFromParcel(Parcel in) {
			return new ChatMessageList(in);
		}

		@Override
		public ChatMessageList[] newArray(int size) {
			return new ChatMessageList[size];
		}
	};

	private Vector<ChatMessage> mEvents;

	private ChatMessageList(Parcel in) {
		mEvents = new Vector<ChatMessage>();
		in.readTypedList(mEvents, ChatMessage.CREATOR);
	}

	public ChatMessageList(Vector<ChatMessage> events) {
		mEvents = events;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public ChatMessage get(int index) {
		return mEvents.get(index);
	}

	public int getSize() {
		return mEvents.size();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeTypedList(mEvents);
	}

}
