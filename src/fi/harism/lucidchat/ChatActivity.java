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
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import fi.harism.lucidchat.service.ChatService;
import fi.harism.lucidchat.service.IService;

public class ChatActivity extends Activity implements ServiceConnection {

	private final ClientBinder mClient = new ClientBinder(this);
	private IService mService;

	public void onChatConnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				View v = findViewById(R.id.send);
				v.setEnabled(true);

				v = findViewById(R.id.edit);
				v.setEnabled(true);
				v.setFocusableInTouchMode(true);
			}
		});
	}

	public void onChatDisconnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				View v = findViewById(R.id.send);
				v.setEnabled(false);

				v = findViewById(R.id.edit);
				v.setEnabled(false);
				v.setFocusable(false);
			}
		});
	}

	public void onChatError(String cause) {
		final Context context = this;
		final String ex = cause;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, ex, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void onChatMessage(String message) {
		final String msg = message;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ViewGroup chat = (ViewGroup) findViewById(R.id.chat);

				TextView tv = (TextView) getLayoutInflater().inflate(
						R.layout.chat_textview, null);
				tv.setText(msg);
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
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		ViewGroup chat = (ViewGroup) findViewById(R.id.chat);
		for (int i = 0; i < 0; ++i) {
			TextView tv = (TextView) getLayoutInflater().inflate(
					R.layout.chat_textview, null);

			SpannableString span = new SpannableString(
					"01:03 TEST: ASKDSA ASDKSAOKD SADKSAODKO SADKOASDKO SDOSAKOKO SADOKASD SADKO");

			span.setSpan(new ForegroundColorSpan(0xFF4CBAED), 0, 5, 0);
			span.setSpan(new ForegroundColorSpan(0xFF17AEF4), 6, 11, 0);
			span.setSpan(new ForegroundColorSpan(0xFFD0D0D0), 12, 20, 0);
			span.setSpan(new ForegroundColorSpan(0xFF63CB63), 20, 28, 0);

			span.setSpan(new URLSpan("tel:+358-123 123 123"), 29, 35, 0);
			span.setSpan(new URLSpan("http://www.hs.fi"), 37, 45, 0);
			span.setSpan(new URLSpan("mailto:harism@gmail.com"), 47, 55, 0);
			span.setSpan(new ForegroundColorSpan(0xFFAACB63), 47, 55, 0);

			tv.setMovementMethod(LinkMovementMethod.getInstance());
			tv.setText(span, BufferType.SPANNABLE);

			chat.addView(tv);
		}

		Dialog dlg = new Dialog(this);
		dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dlg.setContentView(R.layout.dialog_login);
		// dlg.show();

		View v = findViewById(R.id.send);
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText edit = (EditText) findViewById(R.id.edit);
				String txt = edit.getText().toString().trim();
				if (txt.length() > 0) {
					try {
						mService.send(txt + "\r\n");
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
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
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			try {
				mService.disconnect();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		Toast.makeText(this, "Client onServiceConnected.", Toast.LENGTH_LONG)
				.show();

		mService = IService.Stub.asInterface(binder);
		try {
			if (!mService.isConnected()) {
				mService.connect("irc.nebula.fi", 6667, mClient);
			}
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Toast.makeText(this, "Client onServiceDisconnected.", Toast.LENGTH_LONG)
				.show();
	}

}