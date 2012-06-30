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

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import fi.harism.lucidchat.IChatService.Stub;

public class ChatActivity extends Activity {

	private static final String KEY_DLGLOGIN_HOST = "mDlgLogin.host";
	private static final String KEY_DLGLOGIN_NICK = "mDlgLogin.nick";
	private static final String KEY_DLGLOGIN_PORT = "mDlgLogin.port";

	private ChatService mChatService;
	private ChatService.Observer mChatServiceObserver = new ChatServiceObserver();
	private ChatDlgError mDlgError;
	private ChatDlgLogin mDlgLogin;
	private FlipAdapterImpl mFlipAdapter = new FlipAdapterImpl();
	private ChatFlipView mFlipView;
	private OnClickListenerImpl mOnClickListener = new OnClickListenerImpl();
	private ServiceConnectionImpl mServiceConnection = new ServiceConnectionImpl();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
		setContentView(R.layout.root);

		findViewById(R.id.root_header_connect).setOnClickListener(
				mOnClickListener);
		findViewById(R.id.root_footer_send)
				.setOnClickListener(mOnClickListener);

		mFlipView = (ChatFlipView) findViewById(R.id.root_flipview);
		mFlipView.setAdapter(mFlipAdapter);

		setSendEnabled(false);

		Intent intent = new Intent(this, ChatService.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mChatService != null) {
			unbindService(mServiceConnection);
		}
	}

	public void setSendEnabled(boolean enabled) {
		ImageButton send = (ImageButton) findViewById(R.id.root_footer_send);
		send.setEnabled(enabled);
		if (enabled) {
			send.setColorFilter(0xFFFFFFFF);
		} else {
			send.setColorFilter(0x80A06060);
		}
	}

	private final class ChatServiceObserver implements ChatService.Observer {

		@Override
		public void onChatMessage(final ChatMessage message) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onChatMessageImpl(message);
				}
			});
		}

		private void onChatMessageImpl(ChatMessage msg) {
			ChatView chatView = mFlipAdapter.getChatView(msg.mConversationId);

			if (chatView == null) {
				chatView = (ChatView) getLayoutInflater().inflate(
						R.layout.chat_view, null);
				chatView.setConversationId(msg.mConversationId);
				mFlipAdapter.addChatView(chatView);
			}

			ChatTextView cTextView = (ChatTextView) getLayoutInflater()
					.inflate(R.layout.chat_textview, null);
			cTextView.setText(msg);
			chatView.addView(cTextView);
		}

		@Override
		public void onConnected(final boolean connected) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onConnectedImpl(connected);
				}
			});
		}

		private void onConnectedImpl(boolean connected) {
			setSendEnabled(connected);
		}

	}

	private class FlipAdapterImpl extends ChatFlipAdapter {

		private Comparator<ChatView> mComparator = new Comparator<ChatView>() {
			@Override
			public int compare(ChatView c1, ChatView c2) {
				return c1.getConversationId().compareToIgnoreCase(
						c2.getConversationId());
			}
		};
		private Vector<ChatView> mViews = new Vector<ChatView>();

		public void addChatView(ChatView view) {
			mViews.add(view);
			Collections.sort(mViews, mComparator);

			mViews.get(0).setText("1/" + mViews.size() + " Status");
			for (int i = 1; i < mViews.size(); ++i) {
				ChatView cv = mViews.get(i);
				cv.setText((i + 1) + "/" + mViews.size() + " "
						+ cv.getConversationId());
			}

			notifyDataSetChanged();
		}

		@Override
		public View createView(ViewGroup container, int position) {
			container.removeView(mViews.get(position));
			return mViews.get(position);
		}

		public ChatView getChatView(int position) {
			return mViews.get(position);
		}

		public ChatView getChatView(String conversationId) {
			for (ChatView csv : mViews) {
				if (csv.getConversationId().equals(conversationId)) {
					return csv;
				}
			}
			return null;
		}

		@Override
		public int getCount() {
			return mViews.size();
		}

		public void removeChatView(ChatView view) {
			mViews.remove(view);

			mViews.get(0).setText("1/" + mViews.size() + " Status");
			for (int i = 1; i < mViews.size(); ++i) {
				ChatView cv = mViews.get(i);
				cv.setText((i + 1) + "/" + mViews.size() + " "
						+ cv.getConversationId());
			}

			notifyDataSetChanged();
		}

	}

	private class OnClickListenerImpl implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			switch (view.getId()) {
			case R.id.root_footer_send: {
				EditText edit = (EditText) findViewById(R.id.root_footer_edit);
				String txt = edit.getText().toString().trim();
				if (sendImpl(txt)) {
					edit.setText("");
				}
				break;
			}
			case R.id.root_header_connect: {
				View v = findViewById(R.id.root_header_connect);
				if (v.getTag() == null || (Boolean) v.getTag() == false) {
					mDlgLogin = new ChatDlgLogin(ChatActivity.this);
					mDlgLogin.setOnClickListener(this);

					SharedPreferences prefs = getPreferences(MODE_PRIVATE);
					mDlgLogin.setNick(prefs.getString(KEY_DLGLOGIN_NICK, ""));
					mDlgLogin.setHost(prefs.getString(KEY_DLGLOGIN_HOST, ""));
					mDlgLogin.setPort(prefs.getInt(KEY_DLGLOGIN_PORT, -1));

					mDlgLogin.show();
				} else {
					mChatService.disconnect();
					Button connect = (Button) findViewById(R.id.root_header_connect);
					connect.setText(R.string.root_header_connect);
					connect.setTag(false);
				}
				break;
			}
			case R.id.dlg_login_cancel: {
				mDlgLogin.dismiss();
				mDlgLogin = null;
				break;
			}
			case R.id.dlg_login_login: {
				Intent intent = new Intent(ChatActivity.this, ChatService.class);
				if (mChatService.isConnected()) {
					mChatService.disconnect();
				}
				startService(intent);
				mChatService.connect(mDlgLogin.getNick(), mDlgLogin.getHost(),
						mDlgLogin.getPort());

				SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE)
						.edit();
				prefs.putString(KEY_DLGLOGIN_NICK, mDlgLogin.getNick());
				prefs.putString(KEY_DLGLOGIN_HOST, mDlgLogin.getHost());
				prefs.putInt(KEY_DLGLOGIN_PORT, mDlgLogin.getPort());
				prefs.commit();

				mDlgLogin.dismiss();
				mDlgLogin = null;

				Button connect = (Button) findViewById(R.id.root_header_connect);
				connect.setText(R.string.root_header_disconnect);
				connect.setTag(true);
				break;
			}
			case R.id.dlg_error_ok: {
				mDlgError.dismiss();
				mDlgError = null;
				break;
			}
			}
		}

		private boolean sendImpl(String msg) {
			String conversation = mFlipAdapter.getChatView(
					mFlipView.getCurrentIndex()).getConversationId();
			String parts[] = msg.split(" ");
			if (parts.length >= 2 && parts[0].equalsIgnoreCase("/JOIN")) {
				mChatService.sendMessage("JOIN "
						+ ChatUtils.concat(parts, 1, parts.length - 1, false));
				return true;
			}
			if (parts.length >= 2 && parts[0].equalsIgnoreCase("/ME")
					&& conversation.length() > 0) {
				mChatService.sendMessage("PRIVMSG " + conversation
						+ " :\u0001ACTION "
						+ ChatUtils.concat(parts, 1, parts.length - 1, false));
				return true;
			}
			if (parts.length >= 3 && parts[0].equalsIgnoreCase("/MSG")) {
				mChatService.sendMessage("PRIVMSG " + parts[1] + " :"
						+ ChatUtils.concat(parts, 2, parts.length - 1, false));
				return true;
			}
			if (parts.length >= 1 && parts[0].equalsIgnoreCase("/PART")) {
				if (conversation.length() > 0) {
					if (conversation.startsWith("#")) {
						mChatService.sendMessage("PART " + conversation);
					}
					mFlipAdapter.removeChatView(mFlipAdapter
							.getChatView(conversation));
				}
				return true;
			}
			if (parts.length >= 2 && parts[0].equalsIgnoreCase("/NICK")) {
				mChatService.sendMessage("NICK " + parts[1]);
				return true;
			}
			if (parts.length > 0 && conversation.length() > 0) {
				mChatService.sendMessage("PRIVMSG " + conversation + " :"
						+ ChatUtils.concat(parts, 0, parts.length - 1, false));
				return true;
			}

			return false;
		}
	}

	private class ServiceConnectionImpl implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			IChatService srv = Stub.asInterface(binder);
			mChatService = ((ChatService.Binder) srv.asBinder())
					.getChatService();
			mChatService.setObserver(mChatServiceObserver);
			if (mChatService.isConnected()) {
				for (String id : mChatService.getConversationIds()) {
					ChatConversation conversation = mChatService
							.getConversation(id);
					ChatView chatView = (ChatView) getLayoutInflater().inflate(
							R.layout.chat_view, null);
					if (conversation != null) {
						for (ChatMessage message : conversation.getMessages()) {
							ChatTextView cTextView = (ChatTextView) getLayoutInflater()
									.inflate(R.layout.chat_textview, null);
							cTextView.setText(message);
							chatView.addView(cTextView);
						}
					}
					chatView.setConversationId(id);
					mFlipAdapter.addChatView(chatView);
				}
				setSendEnabled(true);
				Button connect = (Button) findViewById(R.id.root_header_connect);
				connect.setText(R.string.root_header_disconnect);
				connect.setTag(true);
			} else {
				setSendEnabled(false);
				Button connect = (Button) findViewById(R.id.root_header_connect);
				connect.setText(R.string.root_header_connect);
				connect.setTag(false);

				ChatView chatView = (ChatView) getLayoutInflater().inflate(
						R.layout.chat_view, null);
				chatView.setConversationId("");
				mFlipAdapter.addChatView(chatView);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mChatService = null;
		}

	}

}