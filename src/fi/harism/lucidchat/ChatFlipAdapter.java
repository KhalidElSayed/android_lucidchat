package fi.harism.lucidchat;

import android.view.View;
import android.view.ViewGroup;

public abstract class ChatFlipAdapter {

	private Observer mObserver;

	/**
	 * Getter for individual Views. Container is the parent View and position is
	 * value between [0, getCount()].
	 */
	public abstract View createView(ViewGroup container, int position);

	/**
	 * Return number of Views this adapter can provide.
	 * 
	 * @return Number of Views.
	 */
	public abstract int getCount();

	public void notifyDataSetChanged() {
		if (mObserver != null) {
			mObserver.onDataSetChanged(this);
		}
	}

	public void setObserver(Observer observer) {
		mObserver = observer;
	}

	public interface Observer {
		public void onDataSetChanged(ChatFlipAdapter adapter);
	}

}
