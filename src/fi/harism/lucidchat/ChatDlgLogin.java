package fi.harism.lucidchat;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Window;
import android.view.View;
import android.widget.EditText;

public class ChatDlgLogin extends Dialog implements DialogInterface.OnCancelListener, View.OnClickListener {
	
	private View.OnClickListener mOnClickListener;
	private EditText mEditNick;
	private EditText mEditHost;
	private EditText mEditPort;

	public ChatDlgLogin(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog_login);
		setOnCancelListener(this);
		
		findViewById(R.id.dlg_login_cancel).setOnClickListener(this);
		findViewById(R.id.dlg_login_login).setOnClickListener(this);
		
		mEditNick = (EditText)findViewById(R.id.dlg_login_nick);
		mEditHost = (EditText)findViewById(R.id.dlg_login_host);
		mEditPort = (EditText)findViewById(R.id.dlg_login_port);		
	}
	
	public void setOnClickListener(View.OnClickListener listener) {
		mOnClickListener = listener;
	}

	@Override
	public void onClick(View view) {
		if (mOnClickListener != null) {
			mOnClickListener.onClick(view);
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (mOnClickListener != null) {
			mOnClickListener.onClick(findViewById(R.id.dlg_login_cancel));
		}		
	}
	
	public String getNick() {
		return mEditNick.getEditableText().toString();
	}
	
	public String getHost() {
		return mEditHost.getEditableText().toString();
	}
	
	public int getPort() {
		return Integer.parseInt(mEditPort.getEditableText().toString());
	}

}
