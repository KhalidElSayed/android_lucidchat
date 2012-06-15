package fi.harism.lucidchat;

import fi.harism.lucidchat.ChatEvent;

interface IServiceCallback {
	void onChatEvent(in ChatEvent event);
}
