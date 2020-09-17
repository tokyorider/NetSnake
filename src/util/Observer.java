package util;

import events.Event;

public interface Observer {
    void handleEvent(Event event);
}
