package fi.harism.lucidchat.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.os.RemoteException;

public class ChatRunnable implements Runnable {

	private IServiceCallback mCallback;
	private String mHost;
	private int mHostPort;
	private boolean mKeepRunning;
	private BufferedWriter mWriter;

	public void disconnect() {
		mKeepRunning = false;
	}

	public void init(String host, int port, IServiceCallback callback) {
		mHost = host;
		mHostPort = port;
		mCallback = callback;
	}

	public boolean isConnected() {
		return mKeepRunning;
	}

	@Override
	public void run() {
		try {
			mKeepRunning = true;

			Socket s = new Socket(mHost, mHostPort);

			InputStream is = s.getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));

			OutputStream os = s.getOutputStream();
			mWriter = new BufferedWriter(new OutputStreamWriter(os));

			mCallback.onChatConnected();

			while (mKeepRunning) {
				String msg = reader.readLine();
				if (msg != null) {
					mCallback.onChatMessage(msg);
				} else {
					mKeepRunning = false;
				}
			}

			send("QUIT\r\n");
			reader.readLine();
			mWriter = null;
			s.close();

			mCallback.onChatDisconnected();
		} catch (Exception ex) {
			try {
				ex.printStackTrace();
				mCallback.onChatError(ex.getMessage());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void send(String message) throws IOException {
		if (mWriter != null) {
			mWriter.append(message);
			mWriter.flush();
		}
	}
}
