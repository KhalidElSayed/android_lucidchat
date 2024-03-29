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
	private Observer mObserver;
	private Socket mSocket;
	private BufferedWriter mWriter;

	public ChatRunnable(Observer observer) {
		mObserver = observer;
	}

	public void disconnect() {
		mKeepRunning = false;
		try {
			send("QUIT");
			if (mSocket != null) {
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

				mObserver.onConnected(true);

				String msg;
				while (mKeepRunning && (msg = reader.readLine()) != null) {
					mObserver.onMessage(msg);
				}

				mWriter = null;
				mSocket.close();

			} catch (IOException ex) {
				ex.printStackTrace();
				mWriter = null;
				mObserver.onError(ex.getMessage());
			}

			mObserver.onConnected(false);

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

	public static interface Observer {

		public void onConnected(boolean connected);

		public void onError(String reason);

		public void onMessage(String message);

	}

}
