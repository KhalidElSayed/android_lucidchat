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
	private BufferedWriter mWriter;

	public ChatRunnable(ChatObserver observer) {
		mObserver = observer;
	}

	public void disconnect() {
		mKeepRunning = false;
		try {
			send("QUIT");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void init(String nick, String host, int port) {
		mNick = nick;
		mHost = host;
		mHostPort = port;
	}

	public boolean isConnected() {
		return mKeepRunning;
	}

	@Override
	public void run() {
		mKeepRunning = true;
		while (mKeepRunning) {
			try {
				Socket s = new Socket();
				s.connect(new InetSocketAddress(mHost, mHostPort), 5000);

				InputStream is = s.getInputStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				OutputStream os = s.getOutputStream();
				mWriter = new BufferedWriter(new OutputStreamWriter(os));

				send("PASS " + System.currentTimeMillis());
				send("NICK " + mNick);
				send("USER " + mNick + " 0 * " + mNick);

				String msg;
				while (mKeepRunning && (msg = reader.readLine()) != null) {
					mObserver.onChatMessage(mHost, msg);
				}

				mWriter = null;
				s.close();

			} catch (IOException ex) {
				ex.printStackTrace();
				mWriter = null;
				mObserver.onChatError(mHost, ex.getMessage());
			}

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
