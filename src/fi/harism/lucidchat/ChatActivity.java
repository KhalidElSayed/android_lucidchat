package fi.harism.lucidchat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class ChatActivity extends Activity implements ServiceConnection,
		View.OnClickListener {

	private static final String KEY_DLGERROR = "mDlgError";
	private static final String KEY_DLGERROR_MESSAGE = "mDlgError.message";

	private static final String KEY_DLGLOGIN = "mDlgLogin";
	private static final String KEY_DLGLOGIN_HOST = "mDlgLogin.host";
	private static final String KEY_DLGLOGIN_NICK = "mDlgLogin.nick";
	private static final String KEY_DLGLOGIN_PORT = "mDlgLogin.port";

	private final ClientBinder mClient = new ClientBinder();
	private ChatDlgError mDlgError;
	private ChatDlgLogin mDlgLogin;
	private IService mService;

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.send: {
			EditText edit = (EditText) findViewById(R.id.edit);
			String txt = edit.getText().toString().trim();
			if (txt.length() > 0) {
				try {
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
						mService.sendMessage(txt);
					}
					edit.setText("");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			break;
		}
		case R.id.root_header_connect: {
			View v = findViewById(R.id.root_header_connect);
			if (v.getTag() == null || (Boolean) v.getTag() == false) {
				mDlgLogin = new ChatDlgLogin(this);
				mDlgLogin.setOnClickListener(this);

				SharedPreferences prefs = getPreferences(MODE_PRIVATE);
				mDlgLogin.setNick(prefs.getString(KEY_DLGLOGIN_NICK, ""));
				mDlgLogin.setHost(prefs.getString(KEY_DLGLOGIN_HOST, ""));
				mDlgLogin.setPort(prefs.getInt(KEY_DLGLOGIN_PORT, -1));

				mDlgLogin.show();
			} else {
				try {
					mService.disconnect();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			break;
		}
		case R.id.dlg_login_cancel: {
			mDlgLogin.dismiss();
			mDlgLogin = null;
			break;
		}
		case R.id.dlg_login_login: {
			Intent intent = new Intent(this, ChatService.class);
			try {
				if (mService.isConnected()) {
					mService.disconnect();
				}
				startService(intent);
				mService.connect(mDlgLogin.getNick(), mDlgLogin.getHost(),
						mDlgLogin.getPort());

				SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE)
						.edit();
				prefs.putString(KEY_DLGLOGIN_NICK, mDlgLogin.getNick());
				prefs.putString(KEY_DLGLOGIN_HOST, mDlgLogin.getHost());
				prefs.putInt(KEY_DLGLOGIN_PORT, mDlgLogin.getPort());
				prefs.commit();

				mDlgLogin.dismiss();
				mDlgLogin = null;
			} catch (RemoteException e) {
				stopService(intent);
				e.printStackTrace();
			}
			break;
		}
		case R.id.dlg_error_ok: {
			mDlgError.dismiss();
			mDlgError = null;
			break;
		}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.root);

		findViewById(R.id.root_header_connect).setOnClickListener(this);

		View v = findViewById(R.id.send);
		v.setOnClickListener(this);
		v.setEnabled(false);

		v = findViewById(R.id.edit);
		v.setEnabled(false);
		v.setFocusable(false);

		Intent intent = new Intent(this, ChatService.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			unbindService(this);
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle bundle) {
		if (bundle.getBoolean(KEY_DLGLOGIN)) {
			mDlgLogin = new ChatDlgLogin(this);
			mDlgLogin.setOnClickListener(this);
			mDlgLogin.setNick(bundle.getString(KEY_DLGLOGIN_NICK));
			mDlgLogin.setHost(bundle.getString(KEY_DLGLOGIN_HOST));
			mDlgLogin.setPort(bundle.getInt(KEY_DLGLOGIN_PORT));
			mDlgLogin.show();
		}
		if (bundle.getBoolean(KEY_DLGERROR)) {
			mDlgError = new ChatDlgError(this);
			mDlgError.setOnClickListener(this);
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

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		mService = IService.Stub.asInterface(binder);
		try {
			mService.setCallback(mClient);
			if (mService.isConnected()) {
				ChatScrollView cScrollView = (ChatScrollView) findViewById(R.id.root_scroll);
				ChatConversation conversation = mService.getConversation(null);
				for (ChatMessage message : conversation.getMessages()) {
					ChatTextView cTextView = (ChatTextView) getLayoutInflater()
							.inflate(R.layout.chat_textview, null);
					cTextView.setChatEvent(message);
					cScrollView.addView(cTextView);
				}
				setConnected(true);
			} else {
				setConnected(false);
			}
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
	}

	private void setConnected(boolean connected) {
		View v = findViewById(R.id.send);
		v.setEnabled(connected);

		v = findViewById(R.id.edit);
		v.setEnabled(connected);
		v.setFocusableInTouchMode(connected);

		Button connect = (Button) findViewById(R.id.root_header_connect);
		if (connected) {
			connect.setText(R.string.root_header_disconnect);
			connect.setTag(true);
		} else {
			connect.setText(R.string.root_header_connect);
			connect.setTag(false);
		}
	}

	private final class ClientBinder extends IServiceCallback.Stub {

		@Override
		public void onChatMessage(final ChatMessage message)
				throws RemoteException {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onChatMessageImpl(message);
				}
			});
		}

		private void onChatMessageImpl(ChatMessage msg) {
			ChatTextView cTextView = (ChatTextView) getLayoutInflater()
					.inflate(R.layout.chat_textview, null);
			cTextView.setChatEvent(msg);

			ChatScrollView cScrollView = (ChatScrollView) findViewById(R.id.root_scroll);
			cScrollView.addView(cTextView);
		}

		@Override
		public void onConnected(final boolean connected) throws RemoteException {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onConnectedImpl(connected);
				}
			});
		}

		private void onConnectedImpl(boolean connected) {
			try {
				if (!mService.isConnected()) {
					connected = false;
					Intent intent = new Intent(ChatActivity.this,
							ChatService.class);
					stopService(intent);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			setConnected(connected);
		}

	}

}