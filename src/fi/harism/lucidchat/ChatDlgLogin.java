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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class ChatDlgLogin extends Dialog implements
		DialogInterface.OnCancelListener, View.OnClickListener {

	private EditText mEditHost;
	private EditText mEditNick;
	private EditText mEditPort;
	private View.OnClickListener mOnClickListener;

	public ChatDlgLogin(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog_login);
		setOnCancelListener(this);

		findViewById(R.id.dlg_login_cancel).setOnClickListener(this);
		findViewById(R.id.dlg_login_login).setOnClickListener(this);

		mEditNick = (EditText) findViewById(R.id.dlg_login_nick);
		mEditHost = (EditText) findViewById(R.id.dlg_login_host);
		mEditPort = (EditText) findViewById(R.id.dlg_login_port);
	}

	public String getHost() {
		return mEditHost.getText().toString();
	}

	public String getNick() {
		return mEditNick.getText().toString();
	}

	public int getPort() {
		String port = mEditPort.getText().toString();
		if (port.length() == 0) {
			return -1;
		}
		return Integer.parseInt(port);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (mOnClickListener != null) {
			mOnClickListener.onClick(findViewById(R.id.dlg_login_cancel));
		}
	}

	@Override
	public void onClick(View view) {
		if (mOnClickListener != null) {
			mOnClickListener.onClick(view);
		}
	}

	public void setHost(String host) {
		mEditHost.setText(host);
	}

	public void setNick(String nick) {
		mEditNick.setText(nick);
	}

	public void setOnClickListener(View.OnClickListener listener) {
		mOnClickListener = listener;
	}

	public void setPort(int port) {
		if (port >= 0) {
			mEditPort.setText(Integer.toString(port));
		}
	}

}
