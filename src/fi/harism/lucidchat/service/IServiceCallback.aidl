package fi.harism.lucidchat.service;

interface IServiceCallback {
	void onChatError(String cause);
	void onChatMessage(String message);
	void onChatConnected();
	void onChatDisconnected();
}
