package fi.harism.lucidchat.service;

import fi.harism.lucidchat.service.IServiceCallback;
import fi.harism.lucidchat.service.ChatEvent;
import fi.harism.lucidchat.service.ChatEventList;

interface IService {
	void connect(in String nick, in String host, in int port);
	void disconnect();
	boolean isConnected();
	void sendEvent(in ChatEvent event);
	void setCallback(in IServiceCallback callback);
	ChatEventList getEvents(in String id);
}
