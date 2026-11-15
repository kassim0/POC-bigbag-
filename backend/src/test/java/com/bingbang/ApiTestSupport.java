package com.bingbang;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Base of the API tests: full application, in-memory database, helpers to act as a given wallet. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String newGuestToken() throws Exception {
        return read(mvc.perform(post("/api/session/guest")).andExpect(status().isCreated()).andReturn())
                .get("token").asText();
    }

    protected static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, String content) {
        return request.contentType(MediaType.APPLICATION_JSON).content(content);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected static String game(long rawgId, String name) {
        return "{\"rawgId\": %d, \"name\": \"%s\", \"released\": \"2020-01-01\", \"backgroundImage\": \"https://img/%d.jpg\"}"
                .formatted(rawgId, name, rawgId);
    }

    /** Creates a list and returns its id. */
    protected long createList(String token, String name, String... games) throws Exception {
        String content = "{\"name\": \"%s\", \"games\": [%s]}".formatted(name, String.join(",", games));
        return read(mvc.perform(as(token, body(post("/api/lists"), content)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }
}
