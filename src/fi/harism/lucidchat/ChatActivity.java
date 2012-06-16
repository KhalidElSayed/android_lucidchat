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
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

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

	private boolean isMenuVisible(int menuId) {
		View v = findViewById(menuId);
		if (v.getVisibility() == View.VISIBLE) {
			return true;
		}
		return false;
	}

	private void onChatEvent(ChatEvent event) {
		if (mDlgLogin != null) {
			int cmd = ChatUtils.getCommandInt(event);
			if (cmd == 1) {
				setSendEnabled(true);
				SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE)
						.edit();
				prefs.putString(KEY_DLGLOGIN_NICK, mDlgLogin.getNick());
				prefs.putString(KEY_DLGLOGIN_HOST, mDlgLogin.getHost());
				prefs.putInt(KEY_DLGLOGIN_PORT, mDlgLogin.getPort());
				prefs.commit();

				mDlgLogin.dismiss();
				mDlgLogin = null;

				setSendEnabled(true);
			} else if (cmd >= 400 && cmd < 600) {
				mDlgError = new ChatDlgError(this);
				mDlgError.setOnClickListener(this);
				mDlgError.setMessage(event.mMessage);
				mDlgError.show();
				try {
					mService.disconnect();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		ViewGroup chat = (ViewGroup) findViewById(R.id.chat);
		ChatTextView ctv = (ChatTextView) getLayoutInflater().inflate(
				R.layout.chat_textview, null);
		ctv.setChatEvent(event);
		chat.addView(ctv);

		final ScrollView sv = (ScrollView) findViewById(R.id.scroll);
		if (sv.getScrollY() + sv.getHeight() >= chat.getBottom() - 2) {
			sv.post(new Runnable() {
				@Override
				public void run() {
					sv.fullScroll(View.FOCUS_DOWN);
				}
			});
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.send: {
			EditText edit = (EditText) findViewById(R.id.edit);
			String txt = edit.getText().toString().trim();
			if (txt.length() > 0) {
				try {
					ChatEvent event = new ChatEvent();
					if (!ChatUtils.setEvent(event, txt)) {
						event.mCommand = txt;
					}
					mService.sendEvent(event);
					edit.setText("");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			break;
		}
		case R.id.menu: {
			setMenuVisible(R.id.menu_main, !isMenuVisible(R.id.menu_main));
			break;
		}
		case R.id.connect: {
			mDlgLogin = new ChatDlgLogin(this);
			mDlgLogin.setOnClickListener(this);

			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			mDlgLogin.setNick(prefs.getString(KEY_DLGLOGIN_NICK, ""));
			mDlgLogin.setHost(prefs.getString(KEY_DLGLOGIN_HOST, ""));
			mDlgLogin.setPort(prefs.getInt(KEY_DLGLOGIN_PORT, -1));

			mDlgLogin.show();

			setMenuVisible(R.id.menu_main, false);
			break;
		}
		case R.id.disconnect: {
			setSendEnabled(false);
			try {
				mService.disconnect();
				setMenuVisible(R.id.menu_main, false);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			break;
		}
		case R.id.dlg_login_cancel: {
			mDlgLogin.dismiss();
			mDlgLogin = null;
			break;
		}
		case R.id.dlg_login_login: {
			try {
				mService.connect(mDlgLogin.getNick(), mDlgLogin.getHost(),
						mDlgLogin.getPort());
			} catch (RemoteException e) {
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

		findViewById(R.id.menu).setOnClickListener(this);
		findViewById(R.id.connect).setOnClickListener(this);
		findViewById(R.id.disconnect).setOnClickListener(this);

		View v = findViewById(R.id.send);
		v.setOnClickListener(this);
		v.setEnabled(false);

		v = findViewById(R.id.edit);
		v.setEnabled(false);
		v.setFocusable(false);

		Intent intent = new Intent(this, ChatService.class);
		startService(intent);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isMenuVisible(R.id.menu_main)) {
				setMenuVisible(R.id.menu_main, false);
				return true;
			} else {
				Intent intent = new Intent(this, ChatService.class);
				stopService(intent);
			}
		}
		return super.onKeyDown(keyCode, event);
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
		Toast.makeText(this, "Client onServiceConnected.", Toast.LENGTH_SHORT)
				.show();

		mService = IService.Stub.asInterface(binder);
		try {
			if (mService.isConnected()) {
				ChatEventList events = mService.getEvents(null);
				for (int i = 0; i < events.getSize(); ++i) {
					onChatEvent(events.get(i));
				}
				setSendEnabled(true);
			} else {
				setSendEnabled(false);
			}
			mService.setCallback(mClient);
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Toast.makeText(this, "Client onServiceDisconnected.",
				Toast.LENGTH_SHORT).show();
		try {
			if (mService != null) {
				mService.setCallback(null);
			}
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}

	private void setMenuVisible(int menuId, boolean visible) {
		final View v = findViewById(menuId);
		if (visible) {
			AnimationSet anim = new AnimationSet(true);
			anim.addAnimation(new AlphaAnimation(0, 1));
			anim.addAnimation(new TranslateAnimation(
					Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF,
					0, Animation.RELATIVE_TO_SELF, 0,
					Animation.RELATIVE_TO_SELF, 0));
			anim.setDuration(500);
			v.setAnimation(anim);
			v.setVisibility(View.VISIBLE);
			anim.startNow();
			v.invalidate();
		} else {
			AnimationSet anim = new AnimationSet(true);
			anim.addAnimation(new AlphaAnimation(1, 0));
			anim.addAnimation(new TranslateAnimation(
					Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF,
					-1, Animation.RELATIVE_TO_SELF, 0,
					Animation.RELATIVE_TO_SELF, 0));
			anim.setDuration(500);
			anim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation anim) {
					v.setVisibility(View.GONE);
				}

				@Override
				public void onAnimationRepeat(Animation anim) {
				}

				@Override
				public void onAnimationStart(Animation anim) {
				}
			});
			v.setAnimation(anim);
			anim.startNow();
			v.invalidate();
		}
	}

	private void setSendEnabled(boolean enabled) {
		View v = findViewById(R.id.send);
		v.setEnabled(enabled);

		v = findViewById(R.id.edit);
		v.setEnabled(enabled);
		v.setFocusableInTouchMode(enabled);
	}

	private final class ClientBinder extends IServiceCallback.Stub {

		@Override
		public void onChatEvent(final ChatEvent event) throws RemoteException {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ChatActivity.this.onChatEvent(event);
				}
			});
		}

	}

}