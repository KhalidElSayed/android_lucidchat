package fi.harism.lucidchat.service;

import fi.harism.lucidchat.service.ChatEvent;

interface IServiceCallback {
	void onChatEvent(in ChatEvent event);
}
