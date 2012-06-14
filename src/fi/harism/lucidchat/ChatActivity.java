package fi.harism.lucidchat;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
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
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import fi.harism.lucidchat.service.ChatEvent;
import fi.harism.lucidchat.service.ChatEventList;
import fi.harism.lucidchat.service.ChatService;
import fi.harism.lucidchat.service.IService;
import fi.harism.lucidchat.service.IServiceCallback;

public class ChatActivity extends Activity implements ServiceConnection,
		View.OnClickListener {

	private final ClientBinder mClient = new ClientBinder();
	private IService mService;

	private boolean menuIsVisible(int menuId) {
		View v = findViewById(menuId);
		if (v.getVisibility() == View.VISIBLE) {
			return true;
		}
		return false;
	}

	private void menuSetVisible(int menuId, boolean visible) {
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

	private void onChatEvent(ChatEvent event) {
		if (event.mCommand.equals(ChatEvent.CMD_CONNECT)) {
			View v = findViewById(R.id.send);
			v.setEnabled(true);

			v = findViewById(R.id.edit);
			v.setEnabled(true);
			v.setFocusableInTouchMode(true);
		}
		if (event.mCommand.equals(ChatEvent.CMD_DISCONNECT)) {
			View v = findViewById(R.id.send);
			v.setEnabled(false);

			v = findViewById(R.id.edit);
			v.setEnabled(false);
			v.setFocusable(false);
		}

		ViewGroup chat = (ViewGroup) findViewById(R.id.chat);

		TextView tv = (TextView) getLayoutInflater().inflate(
				R.layout.chat_textview, null);

		SpannableString span = ChatUtils.createSpannable(event);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(span, BufferType.SPANNABLE);

		chat.addView(tv);

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
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.send: {
			EditText edit = (EditText) findViewById(R.id.edit);
			String txt = edit.getText().toString().trim();
			if (txt.length() > 0) {
				try {
					ChatEvent event = new ChatEvent();
					event.mCommand = txt;
					mService.sendEvent(event);
					edit.setText("");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			break;
		}
		case R.id.menu: {
			menuSetVisible(R.id.menu_main, !menuIsVisible(R.id.menu_main));
			break;
		}
		case R.id.connect: {

			try {
				mService.connect("harismm", "irc.nebula.fi", 6667);
			} catch (RemoteException e) {
				e.printStackTrace();

				Dialog dlg = new Dialog(this);
				dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dlg.setContentView(R.layout.dialog_login);

				dlg.findViewById(R.id.button_dlg_login)
						.setOnClickListener(this);
				dlg.findViewById(R.id.button_dlg_cancel).setOnClickListener(
						this);

				// dlg.show();
			}
			break;
		}
		case R.id.disconnect: {
			try {
				mService.disconnect();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
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
			if (menuIsVisible(R.id.menu_main)) {
				menuSetVisible(R.id.menu_main, false);
				return true;
			} else {
				Intent intent = new Intent(this, ChatService.class);
				stopService(intent);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		Toast.makeText(this, "Client onServiceConnected.", Toast.LENGTH_SHORT)
				.show();

		mService = IService.Stub.asInterface(binder);
		try {
			ChatEventList events = mService.getEvents(null);
			for (int i = 0; i < events.getSize(); ++i) {
				onChatEvent(events.get(i));
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