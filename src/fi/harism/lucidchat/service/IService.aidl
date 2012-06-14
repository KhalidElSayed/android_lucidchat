package fi.harism.lucidchat.service;

import fi.harism.lucidchat.service.IServiceCallback;

interface IService {
	boolean isConnected();
	void connect(String host, int port, IServiceCallback callback);
	void disconnect();
	void send(String message);
}
