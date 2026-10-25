package org.example.engine.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchEventLog {

    private final List<MatchEvent> events = new ArrayList<>();

    public void add(MatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        events.add(event);
    }

    public List<MatchEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<MatchEvent> getEvents(EventDisplayMode mode) {
        return events.stream()
                .filter(mode::shouldDisplay)
                .toList();
    }

    public <T extends MatchEvent> List<T> getEventsOfType(Class<T> type) {
        return events.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    public int size() {
        return events.size();
    }
}