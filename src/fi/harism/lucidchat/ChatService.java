/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.lucidchat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ChatService extends Service {

	private Binder mBinder = new Binder(this);
	private ChatRunnable.Observer mChatObserver = new ChatRunnableObserver();
	private ChatRunnable mChatRunnable;
	private Thread mChatThread;
	private HashMap<String, ChatConversation> mConversationMap = new HashMap<String, ChatConversation>();
	private String mNick, mHost;
	private Observer mObserver;

	public void connect(String nick, String host, int port) {
		disconnect();

		mNick = nick;
		mHost = host;

		mChatRunnable = new ChatRunnable(mChatObserver);
		mChatRunnable.init(nick, host, port);
		mChatThread = new Thread(mChatRunnable);
		mChatThread.start();

		Intent i = new Intent(ChatService.this, ChatActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(ChatService.this, 0, i, 0);

		Notification note = new Notification(R.drawable.ic_notification, null,
				System.currentTimeMillis());
		note.setLatestEventInfo(ChatService.this, "Lucid Chat",
				"Touch to open Lucid Chat application.", pi);
		note.flags |= Notification.FLAG_NO_CLEAR;

		startForeground(1337, note);
	}

	public void disconnect() {
		if (mChatRunnable != null) {
			mChatRunnable.disconnect();
			mChatRunnable = null;
		}
		mConversationMap.clear();
		stopForeground(true);
	}

	public ChatConversation getConversation(String id) {
		if (id == null) {
			id = "";
		}
		if (!mConversationMap.containsKey(id)) {
			mConversationMap.put(id, new ChatConversation(id));
		}
		return mConversationMap.get(id);
	}

	public Set<String> getConversationIds() {
		return mConversationMap.keySet();
	}

	public boolean isConnected() {
		return mChatThread != null && mChatThread.isAlive();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mChatRunnable != null) {
			mChatRunnable.disconnect();
			mChatRunnable = null;
		}
		stopForeground(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mObserver = null;
		return false;
	}

	private void postChatEvent(ChatMessage msg) {
		if (mObserver != null) {
			mObserver.onChatMessage(msg);
		}
		ChatConversation c = mConversationMap.get(msg.mConversationId);
		if (c == null) {
			c = new ChatConversation(msg.mConversationId);
			c.addNick(mNick);
			mConversationMap.put(msg.mConversationId, c);
		}
		c.addMessage(msg);
	}

	public void sendMessage(String message) {
		try {
			ChatMessage msg = ChatUtils.parseMessage(message);
			msg.mFrom = mNick;

			Log.d("sendMessage", message);
			mChatRunnable.send(message);

			if (msg.mCommand == ChatMessage.CMD_PRIVMSG
					|| msg.mCommand == ChatMessage.CMD_PRIVMSG_ACTION) {
				postChatEvent(msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setObserver(Observer observer) {
		mObserver = observer;
	}

	public static class Binder extends IChatService.Stub {
		private ChatService mChatService;

		public Binder(ChatService chatService) {
			mChatService = chatService;
		}

		public ChatService getChatService() {
			return mChatService;
		}
	}

	private class ChatRunnableObserver implements ChatRunnable.Observer {

		@Override
		public void onConnected(boolean connected) {
			if (mObserver != null) {
				mObserver.onConnected(connected);
			}
		}

		@Override
		public void onError(String reason) {
			if (mObserver != null) {
				ChatMessage msg = new ChatMessage();
				msg.mMessage = reason;
				msg.mCommand = ChatMessage.CMD_EXCEPTION;
				msg.mFrom = "";

				for (String key : mConversationMap.keySet()) {
					ChatMessage copy = (ChatMessage) msg.clone();
					copy.mConversationId = key;
					postChatEvent(copy);
				}
			}
		}

		@Override
		public void onMessage(String message) {
			Log.d("onChatMessage", message);
			ChatMessage msg = ChatUtils.parseMessage(message);
			if (msg.mCommand == ChatMessage.CMD_PING) {
				try {
					mChatRunnable.send("PONG " + msg.mMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				if (msg.mFrom == null) {
					msg.mFrom = "";
				}

				if (msg.mCommand == ChatMessage.CMD_NICK) {
					String from = msg.mFrom;
					String to = msg.mMessage;

					if (from.equals(mNick)) {
						mNick = to;
						String str = getString(R.string.chat_nick_you);
						msg.mMessage = String.format(str, to);
					} else {
						String str = getString(R.string.chat_nick);
						msg.mMessage = String.format(str, from, to);
					}

					for (String key : mConversationMap.keySet()) {
						ChatConversation c = mConversationMap.get(key);
						if (c.getNicks().contains(from)) {
							c.changeNick(from, to);
							ChatMessage copy = (ChatMessage) msg.clone();
							copy.mConversationId = key;
							postChatEvent(copy);
						}
					}
					return;
				}

				if (msg.mCommand == ChatMessage.CMD_NAMES) {
					ChatConversation c = mConversationMap
							.get(msg.mConversationId);
					String nicks[] = msg.mMessage.split(" ");
					for (String nick : nicks) {
						c.addNick(nick);
					}
					return;
				}
				if (msg.mCommand == ChatMessage.CMD_NAMES_END) {
					ChatConversation c = mConversationMap
							.get(msg.mConversationId);
					String str = getString(R.string.chat_join_users);
					msg.mMessage = String.format(str, c.getNicks().size());
				}

				if (msg.mCommand == ChatMessage.CMD_JOIN) {
					msg.mConversationId = msg.mMessage;
					ChatConversation c = mConversationMap
							.get(msg.mConversationId);
					if (c == null) {
						c = new ChatConversation(msg.mConversationId);
						mConversationMap.put(msg.mConversationId, c);
					}
					if (msg.mFrom.equals(mNick)) {
						msg.mMessage = getString(R.string.chat_join_you);
					} else {
						String str = getString(R.string.chat_join);
						msg.mMessage = String.format(str, msg.mFrom);
						c.addNick(msg.mFrom);
					}
				}
				if (msg.mCommand == ChatMessage.CMD_PART) {
					if (msg.mFrom.equals(mNick)) {
						mConversationMap.remove(msg.mConversationId);
						return;
					} else {
						String str = getString(R.string.chat_part);
						msg.mMessage = String.format(str, msg.mFrom,
								msg.mMessage);
						mConversationMap.get(msg.mConversationId).removeNick(
								msg.mFrom);
					}
				}

				if (!msg.mFrom.isEmpty() && msg.mConversationId.equals(mNick)) {
					msg.mConversationId = msg.mFrom;
				}
				if (msg.mConversationId.equals("*")
						|| msg.mConversationId.equals(mHost)
						|| msg.mConversationId.equals(mNick)) {
					msg.mConversationId = "";
				}

				postChatEvent(msg);
			}
		}

	}

	public static interface Observer {
		public void onChatMessage(ChatMessage message);

		public void onConnected(boolean connected);
	}

}
