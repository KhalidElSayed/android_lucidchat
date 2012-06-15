package fi.harism.lucidchat;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class ChatDlgError extends Dialog implements
		DialogInterface.OnCancelListener, View.OnClickListener {

	private TextView mMessage;
	private View.OnClickListener mOnClickListener;

	public ChatDlgError(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog_error);
		setOnCancelListener(this);

		findViewById(R.id.dlg_error_ok).setOnClickListener(this);
		mMessage = (TextView) findViewById(R.id.dlg_error_message);
	}

	public String getMessage() {
		return mMessage.getText().toString();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (mOnClickListener != null) {
			mOnClickListener.onClick(findViewById(R.id.dlg_error_ok));
		}
	}

	@Override
	public void onClick(View view) {
		if (mOnClickListener != null) {
			mOnClickListener.onClick(view);
		}
	}

	public void setMessage(String message) {
		mMessage.setText(message);
	}

	public void setOnClickListener(View.OnClickListener listener) {
		mOnClickListener = listener;
	}

}
