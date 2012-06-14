package fi.harism.lucidchat.service;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;
import fi.harism.lucidchat.ChatActivity;
import fi.harism.lucidchat.R;

public class ChatService extends Service {

	private final ServiceBinder mBinder = new ServiceBinder(this);
	private IServiceCallback mCallback;
	private Thread mChatThread;
	private final ChatRunnable mConnection = new ChatRunnable();

	public void connect(String host, int port, IServiceCallback callback) {
		try {
			mCallback = callback;
			mConnection.init(host, port, callback);
			mChatThread = new Thread(mConnection);
			mChatThread.start();
		} catch (Exception ex) {
			try {
				mCallback.onChatError(ex.getMessage());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void disconnect() {
		if (mChatThread != null) {
			mConnection.disconnect();
			mChatThread = null;
		}
		stopSelf();
	}

	public boolean isConnected() {
		return mConnection.isConnected();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "Service onCreate.", Toast.LENGTH_LONG).show();

	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Service onDestroy.", Toast.LENGTH_LONG).show();
		mConnection.disconnect();
		stopForeground(true);
	}

	public void onServerError(IOException exception) {
		if (mCallback != null) {
			try {
				mCallback.onChatError(exception.getMessage());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void onServerMessage(String message) {
		if (mCallback != null) {
			try {
				mCallback.onChatMessage(message);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Intent i = new Intent(this, ChatActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		Notification note = new Notification(R.drawable.ic_notification, null,
				System.currentTimeMillis());
		note.setLatestEventInfo(this, "Lucid Chat", "Connected.", pi);
		note.flags |= Notification.FLAG_NO_CLEAR;

		startForeground(1337, note);

		return Service.START_STICKY;
	}

	public void send(String message) {
		try {
			mConnection.send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
