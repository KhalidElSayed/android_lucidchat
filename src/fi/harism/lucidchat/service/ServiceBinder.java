package fi.harism.lucidchat.service;

import android.os.RemoteException;

public class ServiceBinder extends IService.Stub {

	private ChatService mService;

	public ServiceBinder(ChatService service) {
		mService = service;
	}

	@Override
	public void connect(String host, int port, IServiceCallback callback)
			throws RemoteException {

		mService.connect(host, port, callback);

	}

	@Override
	public void disconnect() throws RemoteException {
		mService.disconnect();
	}

	@Override
	public boolean isConnected() throws RemoteException {
		return mService.isConnected();
	}

	@Override
	public void send(String message) throws RemoteException {
		mService.send(message);
	}

}
