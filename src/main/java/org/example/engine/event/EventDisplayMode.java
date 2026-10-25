package org.example.engine.event;

public enum EventDisplayMode {

    ALL {
        @Override
        public boolean shouldDisplay(MatchEvent event) {
            return true;
        }
    },

    KEY {
        @Override
        public boolean shouldDisplay(MatchEvent event) {
            return event.isKeyEvent();
        }
    };

    public abstract boolean shouldDisplay(MatchEvent event);
}