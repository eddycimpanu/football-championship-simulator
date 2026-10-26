package org.example.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The same controllers, called through HTTP with MockMvc: URLs, JSON in and out, and status codes.
// No server and no database: the controllers get the in-memory stores from WebTestKit.
class HttpApiTest {

    private WebTestKit kit;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        kit = new WebTestKit();
        mvc = MockMvcBuilders.standaloneSetup(kit.seasons, kit.betting, kit.agents, kit.bettors, kit.reset).build();
    }

    private String ticketJson(String stake, int matchId, String... selections) {
        StringBuilder picks = new StringBuilder();
        for (String selection : selections) {
            if (!picks.isEmpty()) picks.append(", ");
            picks.append("{\"matchId\": ").append(matchId).append(", \"selection\": \"").append(selection).append("\"}");
        }
        return "{\"stake\": " + stake + ", \"selections\": [" + picks + "]}";
    }

    @Test
    void theSeasonIsSentAsJson() throws Exception {
        mvc.perform(get("/api/season"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test League"))
                .andExpect(jsonPath("$.nextRound").value(1))
                .andExpect(jsonPath("$.finished").value(false));
    }

    @Test
    void theOddsOfTheNextRound() throws Exception {
        mvc.perform(get("/api/rounds/1/odds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.matches.length()").value(10))
                .andExpect(jsonPath("$.matches[0].odds.HOME").isNumber());
    }

    @Test
    void anUnknownRoundIsNotFound() throws Exception {
        mvc.perform(get("/api/rounds/99")).andExpect(status().isNotFound());
    }

    @Test
    void placingATicket() throws Exception {
        int match = kit.seasons.odds(1).matches().get(0).matchId();

        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(ticketJson("25", match, "HOME")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("OPEN"))
                .andExpect(jsonPath("$.ticket.selections.length()").value(1))
                .andExpect(jsonPath("$.ticket.selections[0].selection").value("HOME"))
                .andExpect(jsonPath("$.bettor.balance").value(975.0));

        mvc.perform(get("/api/betting/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open.length()").value(1))
                .andExpect(jsonPath("$.summary.open").value(1));
    }

    @Test
    void aBrokenRuleIsABadRequestWithItsReason() throws Exception {
        int match = kit.seasons.odds(1).matches().get(0).matchId();

        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson("10", match, "BOTH_SCORE", "NOT_BOTH_SCORE")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("only one selection per match")));
    }

    @Test
    void playingARoundThenTheBettorsTable() throws Exception {
        mvc.perform(post("/api/rounds/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.played").value(true));

        mvc.perform(get("/api/bettors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundsPlayed").value(1))
                .andExpect(jsonPath("$.bettors.length()").value(6))
                .andExpect(jsonPath("$.bettors[0].history.length()").value(2));
    }

    @Test
    void resettingTheLeague() throws Exception {
        mvc.perform(post("/api/rounds/next")).andExpect(status().isOk());

        mvc.perform(post("/api/season/reset")).andExpect(status().isOk());

        mvc.perform(get("/api/season"))
                .andExpect(jsonPath("$.roundsPlayed").value(0))
                .andExpect(jsonPath("$.nextRound").value(1));
    }
}
