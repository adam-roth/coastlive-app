package au.com.suncoastpc.coastlive.event;

public interface EventHandler {
	public void handleEvent(final EventType eventType, final Object eventData);
}
