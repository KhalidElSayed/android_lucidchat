package fi.harism.lucidchat;

import java.util.Vector;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatEventList implements Parcelable {

	public static final Parcelable.Creator<ChatEventList> CREATOR = new Parcelable.Creator<ChatEventList>() {
		@Override
		public ChatEventList createFromParcel(Parcel in) {
			return new ChatEventList(in);
		}

		@Override
		public ChatEventList[] newArray(int size) {
			return new ChatEventList[size];
		}
	};

	private Vector<ChatEvent> mEvents;

	private ChatEventList(Parcel in) {
		mEvents = new Vector<ChatEvent>();
		in.readTypedList(mEvents, ChatEvent.CREATOR);
	}

	public ChatEventList(Vector<ChatEvent> events) {
		mEvents = events;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public ChatEvent get(int index) {
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
