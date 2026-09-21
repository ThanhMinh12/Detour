package com.detour.trip;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TripAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void keepsTripsPrivateUntilAnAccountAcceptsTheInvite() throws Exception {
        Cookie alice = register("Alice", "private-alice@example.com");
        String tripBody = mvc.perform(post("/api/trips")
                        .cookie(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Private trip","destination":"Seoul","startDate":"2026-10-02","endDate":"2026-10-05","currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode trip = json.readTree(tripBody);
        String activityBody = mvc.perform(post("/api/trips/{tripId}/activities", trip.get("id").asText())
                        .cookie(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-03","type":"ACTIVITY","status":"PROPOSED","title":"Private plan"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode activity = json.readTree(activityBody);

        Cookie bob = register("Bob", "private-bob@example.com");
        mvc.perform(get("/api/trips").cookie(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/trips/{tripId}", trip.get("id").asText()).cookie(bob))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/trips/{tripId}/activities/{activityId}",
                        trip.get("id").asText(), activity.get("id").asText())
                        .cookie(bob).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-03","type":"ACTIVITY","status":"CONFIRMED","title":"Changed plan"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/trips/{tripId}/activities/{activityId}",
                        trip.get("id").asText(), activity.get("id").asText())
                        .cookie(bob).with(csrf()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/trips/join/{inviteCode}", trip.get("inviteCode").asText())
                        .cookie(bob).with(csrf()))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/trips/{tripId}/balances", trip.get("id").asText()).cookie(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void letsAMemberRemoveTheirUpvote() throws Exception {
        Cookie alice = register("Alice", "vote-alice@example.com");
        String tripBody = mvc.perform(post("/api/trips")
                        .cookie(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Voting trip","destination":"Seoul","startDate":"2026-10-02","endDate":"2026-10-05","currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode trip = json.readTree(tripBody);
        String activityBody = mvc.perform(post("/api/trips/{tripId}/activities", trip.get("id").asText())
                        .cookie(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-03","type":"ACTIVITY","status":"PROPOSED","title":"Photo walk"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voteScore").value(0))
                .andExpect(jsonPath("$.currentUserVote").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode activity = json.readTree(activityBody);

        mvc.perform(post("/api/trips/{tripId}/activities/{activityId}/votes",
                        trip.get("id").asText(), activity.get("id").asText())
                        .cookie(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.currentUserVote").value(1));
        mvc.perform(get("/api/trips/{tripId}/activities", trip.get("id").asText()).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].voteScore").value(1))
                .andExpect(jsonPath("$[0].currentUserVote").value(1));

        mvc.perform(post("/api/trips/{tripId}/activities/{activityId}/votes",
                        trip.get("id").asText(), activity.get("id").asText())
                        .cookie(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.currentUserVote").value(0));
        mvc.perform(get("/api/trips/{tripId}/activities", trip.get("id").asText()).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].voteScore").value(0))
                .andExpect(jsonPath("$[0].currentUserVote").value(0));
    }

    private Cookie register(String displayName, String email) throws Exception {
        String body = json.createObjectNode()
                .put("displayName", displayName)
                .put("email", email)
                .put("password", "correct-horse-battery-staple")
                .toString();
        return mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getCookie("DETOUR_SESSION");
    }
}
