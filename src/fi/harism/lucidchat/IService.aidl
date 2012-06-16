package fi.harism.lucidchat;

import fi.harism.lucidchat.IServiceCallback;
import fi.harism.lucidchat.ChatMessage;
import fi.harism.lucidchat.ChatMessageList;

interface IService {
	void connect(in String nick, in String host, in int port);
	void disconnect();
	boolean isConnected();
	void sendEvent(in ChatMessage message);
	void setCallback(in IServiceCallback callback);
	ChatMessageList getMessages(in String id);
}
