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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import fi.harism.lucidchat.IChatService.Stub;

public class ChatActivity extends Activity {

	private static final String KEY_DLGERROR = "mDlgError";
	private static final String KEY_DLGERROR_MESSAGE = "mDlgError.message";

	private static final String KEY_DLGLOGIN = "mDlgLogin";
	private static final String KEY_DLGLOGIN_HOST = "mDlgLogin.host";
	private static final String KEY_DLGLOGIN_NICK = "mDlgLogin.nick";
	private static final String KEY_DLGLOGIN_PORT = "mDlgLogin.port";

	private ChatService mChatService;
	private ChatService.Observer mChatServiceObserver = new ChatServiceObserver();
	private ChatDlgError mDlgError;
	private ChatDlgLogin mDlgLogin;
	private OnClickListenerImpl mOnClickListener = new OnClickListenerImpl();
	private ViewPager.OnPageChangeListener mOnPageChangeListener = new OnPageChangeListenerImpl();
	private PagerAdapterImpl mPagerAdapter = new PagerAdapterImpl();
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

		ViewPager viewPager = (ViewPager) findViewById(R.id.root_viewpager);
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setOnPageChangeListener(mOnPageChangeListener);

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

	@Override
	public void onRestoreInstanceState(Bundle bundle) {
		if (bundle.getBoolean(KEY_DLGLOGIN)) {
			mDlgLogin = new ChatDlgLogin(this);
			mDlgLogin.setOnClickListener(mOnClickListener);
			mDlgLogin.setNick(bundle.getString(KEY_DLGLOGIN_NICK));
			mDlgLogin.setHost(bundle.getString(KEY_DLGLOGIN_HOST));
			mDlgLogin.setPort(bundle.getInt(KEY_DLGLOGIN_PORT));
			mDlgLogin.show();
		}
		if (bundle.getBoolean(KEY_DLGERROR)) {
			mDlgError = new ChatDlgError(this);
			mDlgError.setOnClickListener(mOnClickListener);
			mDlgError.setMessage(bundle.getString(KEY_DLGERROR_MESSAGE));
			mDlgError.show();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		if (mDlgLogin != null) {
			bundle.putBoolean(KEY_DLGLOGIN, true);
			bundle.putString(KEY_DLGLOGIN_NICK, mDlgLogin.getNick());
			bundle.putString(KEY_DLGLOGIN_HOST, mDlgLogin.getHost());
			bundle.putInt(KEY_DLGLOGIN_PORT, mDlgLogin.getPort());
			mDlgLogin.dismiss();
		}
		if (mDlgError != null) {
			bundle.putBoolean(KEY_DLGERROR, true);
			bundle.putString(KEY_DLGERROR_MESSAGE, mDlgError.getMessage());
			mDlgError.dismiss();
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
			ChatScrollView cScrollView = mPagerAdapter
					.getScrollView(msg.mConversationId);

			if (cScrollView == null) {
				cScrollView = (ChatScrollView) getLayoutInflater().inflate(
						R.layout.chat_scrollview, null);
				cScrollView.setConversationId(msg.mConversationId);
				mPagerAdapter.addScrollView(cScrollView);
				mPagerAdapter.notifyDataSetChanged();
			}

			ChatTextView cTextView = (ChatTextView) getLayoutInflater()
					.inflate(R.layout.chat_textview, null);
			cTextView.setText(msg);
			cScrollView.addView(cTextView);
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

	private class OnClickListenerImpl implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			switch (view.getId()) {
			case R.id.root_footer_send: {
				EditText edit = (EditText) findViewById(R.id.root_footer_edit);
				String txt = edit.getText().toString().trim();
				if (txt.length() > 0) {
					String txtUpper = txt.toUpperCase();
					if (txtUpper.startsWith("/JOIN ")) {
						txt = "JOIN " + txt.substring(5).trim();
						if (txt.trim().length() == 4) {
							txt = "";
						}
					}
					if (txtUpper.startsWith("/MSG ")) {
						String cmd = "PRIVMSG ";
						String to = txt.substring(4).trim();
						int spaceIdx = to.indexOf(' ');
						if (spaceIdx > 0) {
							String msg = to.substring(spaceIdx + 1);
							if (msg.length() > 0) {
								to = to.substring(0, spaceIdx + 1);
								txt = cmd + to + ":" + msg;
							} else {
								txt = "";
							}
						} else {
							txt = "";
						}
					}
					if (txtUpper.startsWith("/ME ")) {
						txt = "PRIVMSG harism :" + "\u0001ACTION "
								+ txt.substring(4);
					}

					if (txt.length() > 0) {
						mChatService.sendMessage(txt);
					}
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
	}

	private class OnPageChangeListenerImpl implements
			ViewPager.OnPageChangeListener {

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onPageSelected(int position) {
			TextView tv = (TextView) findViewById(R.id.root_conversation);
			String txt = mPagerAdapter.getScrollView(position)
					.getConversationId();
			if (txt.length() > 0) {
				tv.setVisibility(View.VISIBLE);
				tv.setText(txt);
			} else {
				tv.setVisibility(View.GONE);
			}
		}

	}

	private class PagerAdapterImpl extends PagerAdapter {

		private Comparator<ChatScrollView> mComparator = new Comparator<ChatScrollView>() {
			@Override
			public int compare(ChatScrollView c1, ChatScrollView c2) {
				return c1.getConversationId().compareToIgnoreCase(
						c2.getConversationId());
			}
		};
		private Vector<ChatScrollView> mViews = new Vector<ChatScrollView>();

		public void addScrollView(ChatScrollView view) {
			mViews.add(view);
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, Object obj) {
			((ViewPager) collection).removeView((View) obj);
		}

		@Override
		public int getCount() {
			return mViews.size();
		}

		public ChatScrollView getScrollView(int position) {
			return mViews.get(position);
		}

		public ChatScrollView getScrollView(String conversationId) {
			for (ChatScrollView csv : mViews) {
				if (csv.getConversationId().equals(conversationId)) {
					return csv;
				}
			}
			return null;
		}

		@Override
		public Object instantiateItem(ViewGroup collection, int position) {
			((ViewPager) collection).addView(mViews.get(position));
			return mViews.get(position);
		}

		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == obj;
		}

		public void removeScrollView(ChatScrollView view) {
			mViews.remove(view);
		}

		@Override
		public void startUpdate(ViewGroup collection) {
			super.startUpdate(collection);
			Collections.sort(mViews, mComparator);
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
					ChatScrollView cScrollView = (ChatScrollView) getLayoutInflater()
							.inflate(R.layout.chat_scrollview, null);
					if (conversation != null) {
						for (ChatMessage message : conversation.getMessages()) {
							ChatTextView cTextView = (ChatTextView) getLayoutInflater()
									.inflate(R.layout.chat_textview, null);
							cTextView.setText(message);
							cScrollView.addView(cTextView);
						}
					}
					cScrollView.setConversationId(id);
					mPagerAdapter.addScrollView(cScrollView);
				}
				mPagerAdapter.notifyDataSetChanged();
				setSendEnabled(true);
				Button connect = (Button) findViewById(R.id.root_header_connect);
				connect.setText(R.string.root_header_disconnect);
				connect.setTag(true);
			} else {
				setSendEnabled(false);
				Button connect = (Button) findViewById(R.id.root_header_connect);
				connect.setText(R.string.root_header_connect);
				connect.setTag(false);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mChatService = null;
		}

	}

}