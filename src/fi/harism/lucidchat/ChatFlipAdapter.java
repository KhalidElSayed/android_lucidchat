package fi.harism.lucidchat;

import java.util.Vector;

import android.view.View;
import android.view.ViewGroup;

public abstract class ChatFlipAdapter {

	private Vector<Observer> mObservers = new Vector<Observer>();

	public void addObserver(Observer observer) {
		mObservers.add(observer);
	}

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
		for (Observer observer : mObservers) {
			observer.onDataSetChanged(this);
		}
	}

	public void removeObserver(Observer observer) {
		mObservers.remove(observer);
	}

	public interface Observer {
		public void onDataSetChanged(ChatFlipAdapter adapter);
	}

}
