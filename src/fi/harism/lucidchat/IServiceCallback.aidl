package fi.harism.lucidchat;

import fi.harism.lucidchat.ChatMessage;

interface IServiceCallback {
	void onChatMessage(in ChatMessage message);
	void onConnected(in boolean connected);
}
