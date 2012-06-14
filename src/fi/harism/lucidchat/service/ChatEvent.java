package fi.harism.lucidchat.service;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatEvent implements Parcelable {

	public static final String CMD_CONNECT = "SERVER_CONNECT";
	public static final String CMD_DISCONNECT = "SERVER_DISCONNECT";
	public static final String CMD_EXCEPTION = "SERVER_EXCEPTION";

	public static final Parcelable.Creator<ChatEvent> CREATOR = new Parcelable.Creator<ChatEvent>() {
		@Override
		public ChatEvent createFromParcel(Parcel in) {
			return new ChatEvent(in);
		}

		@Override
		public ChatEvent[] newArray(int size) {
			return new ChatEvent[size];
		}
	};

	public String mFrom, mTo, mCommand, mMessage;
	public long mTime;

	public ChatEvent() {
		mTime = System.currentTimeMillis();
	}

	private ChatEvent(Parcel in) {
		mFrom = in.readString();
		mCommand = in.readString();
		mTo = in.readString();
		mMessage = in.readString();
		mTime = in.readLong();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mFrom);
		out.writeString(mCommand);
		out.writeString(mTo);
		out.writeString(mMessage);
		out.writeLong(mTime);
	}

}
