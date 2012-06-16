package fi.harism.lucidchat;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatMessage implements Parcelable {

	public static final String CMD_EXCEPTION = "SERVER_EXCEPTION";

	public static final Parcelable.Creator<ChatMessage> CREATOR = new Parcelable.Creator<ChatMessage>() {
		@Override
		public ChatMessage createFromParcel(Parcel in) {
			return new ChatMessage(in);
		}

		@Override
		public ChatMessage[] newArray(int size) {
			return new ChatMessage[size];
		}
	};

	public String mFrom, mTo, mCommand, mMessage;
	public long mTime;

	public ChatMessage() {
		mTime = System.currentTimeMillis();
	}

	private ChatMessage(Parcel in) {
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
