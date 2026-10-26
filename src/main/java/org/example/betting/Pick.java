package org.example.betting;

// What the user chose for one match: the match and the selection. The odds are added by the server.
public record Pick(int matchId, Selection selection) {
}
