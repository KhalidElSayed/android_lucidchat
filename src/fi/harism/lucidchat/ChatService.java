package fi.harism.lucidchat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Vector;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class ChatService extends Service implements ChatObserver {

	private final ServiceBinder mBinder = new ServiceBinder();
	private IServiceCallback mCallback;
	private ChatRunnable mChatRunnable;
	private Thread mChatThread;
	private final HashMap<String, Vector<ChatEvent>> mEventMap = new HashMap<String, Vector<ChatEvent>>();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onChatError(String host, String reason) {
		if (mCallback != null) {
			ChatEvent event = new ChatEvent();
			event.mMessage = reason;
			event.mCommand = ChatEvent.CMD_EXCEPTION;
			event.mFrom = host;
			postChatEvent(event);
			mChatThread = null;
		}
	}

	@Override
	public void onChatMessage(String host, String message) {
		Log.d("onChatMessage", message);
		ChatEvent event = ChatUtils.getEvent(message);
		if (event.mCommand.equals("PING")) {
			try {
				mChatRunnable.send("PONG " + event.mMessage);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (event.mFrom == null) {
				event.mFrom = host;
			}
			postChatEvent(event);
		}
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "Service onCreate.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Service onDestroy.", Toast.LENGTH_SHORT).show();
		if (mChatRunnable != null) {
			mChatRunnable.disconnect();
			mChatThread = null;
		}
		stopForeground(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mCallback = null;
		return super.onUnbind(intent);
	}

	private void postChatEvent(ChatEvent event) {
		try {
			if (mCallback != null) {
				mCallback.onChatEvent(event);
			}
			if (mEventMap.get(null) == null) {
				mEventMap.put(null, new Vector<ChatEvent>());
			}
			mEventMap.get(null).add(event);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private class ServiceBinder extends IService.Stub {

		@Override
		public void connect(String nick, String host, int port)
				throws RemoteException {
			disconnect();
			mChatRunnable = new ChatRunnable(ChatService.this);
			mChatRunnable.init(nick, host, port);
			mChatThread = new Thread(mChatRunnable);
			mChatThread.start();

			Intent i = new Intent(ChatService.this, ChatActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pi = PendingIntent.getActivity(ChatService.this, 0,
					i, 0);

			Notification note = new Notification(R.drawable.ic_notification,
					null, System.currentTimeMillis());
			note.setLatestEventInfo(ChatService.this, "Lucid Chat",
					"Touch to open Lucid Chat application.", pi);
			note.flags |= Notification.FLAG_NO_CLEAR;

			startForeground(1337, note);
		}

		@Override
		public void disconnect() throws RemoteException {
			if (mChatThread != null) {
				mChatRunnable.disconnect();
				mChatThread = null;
			}
			stopForeground(true);
		}

		@Override
		public ChatEventList getEvents(String id) throws RemoteException {
			Vector<ChatEvent> events = mEventMap.get(id);
			if (events != null) {
				return new ChatEventList(events);
			}
			return new ChatEventList(new Vector<ChatEvent>());
		}

		@Override
		public boolean isConnected() throws RemoteException {
			return mChatRunnable != null && mChatRunnable.isConnected();
		}

		@Override
		public void sendEvent(ChatEvent event) throws RemoteException {
			try {
				event.mCommand = event.mCommand.toUpperCase();
				StringWriter out = new StringWriter();
				out.write(event.mCommand);
				if (event.mTo != null) {
					out.write(' ');
					out.write(event.mTo);
				}
				if (event.mMessage != null) {
					out.write(' ');
					out.write(event.mMessage);
				}
				Log.d("sendEvent", out.toString());
				mChatRunnable.send(out.toString());

				if (event.mCommand.equals("PRIVMSG")) {
					postChatEvent(event);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void setCallback(IServiceCallback callback)
				throws RemoteException {
			mCallback = callback;
		}

	}

}
