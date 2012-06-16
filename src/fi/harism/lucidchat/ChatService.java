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

public class ChatService extends Service implements ChatObserver {

	private final ServiceBinder mBinder = new ServiceBinder();
	private IServiceCallback mCallback;
	private ChatRunnable mChatRunnable;
	private Thread mChatThread;
	private final HashMap<String, Vector<ChatMessage>> mMessageMap = new HashMap<String, Vector<ChatMessage>>();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onChatConnected(boolean connected) {
		try {
			if (mCallback != null) {
				mCallback.onConnected(connected);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onChatError(String host, String reason) {
		if (mCallback != null) {
			ChatMessage event = new ChatMessage();
			event.mMessage = reason;
			event.mCommand = ChatMessage.CMD_EXCEPTION;
			event.mFrom = host;
			postChatEvent(event);
			mChatThread = null;
		}
	}

	@Override
	public void onChatMessage(String host, String message) {
		Log.d("onChatMessage", message);
		ChatMessage msg = ChatUtils.getEvent(message);
		if (msg.mCommand.equals("PING")) {
			try {
				mChatRunnable.send("PONG " + msg.mMessage);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (msg.mFrom == null) {
				msg.mFrom = host;
			}
			postChatEvent(msg);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
		return false;
	}

	private void postChatEvent(ChatMessage message) {
		try {
			if (mCallback != null) {
				mCallback.onChatMessage(message);
			}
			if (mMessageMap.get(null) == null) {
				mMessageMap.put(null, new Vector<ChatMessage>());
			}
			mMessageMap.get(null).add(message);
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
			Log.d("disconnect()", "disconnect()");
			if (mChatThread != null) {
				mChatRunnable.disconnect();
				mChatThread = null;
			}
			stopForeground(true);
		}

		@Override
		public ChatMessageList getMessages(String id) throws RemoteException {
			Vector<ChatMessage> events = mMessageMap.get(id);
			if (events != null) {
				return new ChatMessageList(events);
			}
			return new ChatMessageList(new Vector<ChatMessage>());
		}

		@Override
		public boolean isConnected() throws RemoteException {
			return mChatThread != null && mChatThread.isAlive();
		}

		@Override
		public void sendEvent(ChatMessage message) throws RemoteException {
			try {
				message.mCommand = message.mCommand.toUpperCase();
				StringWriter out = new StringWriter();
				out.write(message.mCommand);
				if (message.mTo != null) {
					out.write(' ');
					out.write(message.mTo);
				}
				if (message.mMessage != null) {
					out.write(' ');
					out.write(message.mMessage);
				}
				Log.d("sendEvent", out.toString());
				mChatRunnable.send(out.toString());

				if (message.mCommand.equals("PRIVMSG")) {
					postChatEvent(message);
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
