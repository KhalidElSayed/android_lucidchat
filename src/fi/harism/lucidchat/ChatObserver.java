package fi.harism.lucidchat;

public interface ChatObserver {

	public void onChatError(String host, String reason);

	public void onChatMessage(String host, String message);

}
