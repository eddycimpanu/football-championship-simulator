package org.example.betting;

// A bet that breaks a rule (not enough credits, a match already played...). The message is shown to the user.
public class BettingException extends RuntimeException {

    public BettingException(String message) {
        super(message);
    }
}
