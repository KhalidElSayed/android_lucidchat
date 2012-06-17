package fi.harism.lucidchat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ChatRunnable implements Runnable {

	private String mHost;
	private int mHostPort;
	private boolean mKeepRunning;
	private String mNick;
	private ChatObserver mObserver;
	private Socket mSocket;
	private BufferedWriter mWriter;

	public ChatRunnable(ChatObserver observer) {
		mObserver = observer;
	}

	public void disconnect() {
		mKeepRunning = false;
		try {
			if (mWriter != null) {
				send("QUIT");
			} else if (mSocket != null) {
				mSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void init(String nick, String host, int port) {
		mNick = nick;
		mHost = host;
		mHostPort = port;
	}

	@Override
	public void run() {
		mKeepRunning = true;
		while (mKeepRunning) {
			try {
				mSocket = new Socket();
				mSocket.connect(new InetSocketAddress(mHost, mHostPort), 5000);

				InputStream is = mSocket.getInputStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				OutputStream os = mSocket.getOutputStream();
				mWriter = new BufferedWriter(new OutputStreamWriter(os));

				send("PASS " + System.currentTimeMillis());
				send("NICK " + mNick);
				send("USER " + mNick + " 0 * " + mNick);

				mObserver.onChatConnected(true);

				String msg;
				while (mKeepRunning && (msg = reader.readLine()) != null) {
					mObserver.onChatMessage(mHost, msg);
				}

				mWriter = null;
				mSocket.close();

			} catch (IOException ex) {
				ex.printStackTrace();
				mWriter = null;
				mObserver.onChatError(mHost, ex.getMessage());
			}

			mObserver.onChatConnected(false);

			if (mKeepRunning) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public void send(String message) throws IOException {
		if (mWriter != null) {
			mWriter.append(message);
			mWriter.append("\r\n");
			mWriter.flush();
		}
	}
}
