package fi.harism.lucidchat;

import fi.harism.lucidchat.IServiceCallback;
import fi.harism.lucidchat.ChatEvent;
import fi.harism.lucidchat.ChatEventList;

interface IService {
	void connect(in String nick, in String host, in int port);
	void disconnect();
	boolean isConnected();
	void sendEvent(in ChatEvent event);
	void setCallback(in IServiceCallback callback);
	ChatEventList getEvents(in String id);
}
