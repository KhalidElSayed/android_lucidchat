package fi.harism.lucidchat;

import android.os.RemoteException;
import fi.harism.lucidchat.service.IServiceCallback;

public class ClientBinder extends IServiceCallback.Stub {

	private ChatActivity mChat;

	public ClientBinder(ChatActivity chat) {
		mChat = chat;
	}

	@Override
	public void onChatConnected() throws RemoteException {
		mChat.onChatConnected();
	}

	@Override
	public void onChatDisconnected() throws RemoteException {
		mChat.onChatDisconnected();
	}

	@Override
	public void onChatError(String cause) throws RemoteException {
		mChat.onChatError(cause);
	}

	@Override
	public void onChatMessage(String message) throws RemoteException {
		mChat.onChatMessage(message);
	}

}
