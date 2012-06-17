package fi.harism.lucidchat;

import fi.harism.lucidchat.IServiceCallback;
import fi.harism.lucidchat.ChatMessage;
import fi.harism.lucidchat.ChatConversation;

interface IService {
	void connect(in String nick, in String host, in int port);
	void disconnect();
	boolean isConnected();
	void sendMessage(in String message);
	void setCallback(in IServiceCallback callback);
	ChatConversation getConversation(in String id);
}
