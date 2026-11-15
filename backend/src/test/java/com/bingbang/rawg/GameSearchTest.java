package com.bingbang.rawg;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import com.bingbang.common.ApiExceptionHandler;

class GameSearchTest {

    private MockRestServiceServer rawgServer;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mvcWithKey("test-key");
    }

    private MockMvc mvcWithKey(String key) {
        RestClient.Builder builder = RestClient.builder();
        rawgServer = MockRestServiceServer.bindTo(builder).build();
        RawgClient client = new RawgClient(builder, "https://rawg.test/api", key);
        return MockMvcBuilders.standaloneSetup(new GameSearchController(client))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void searchMapsRawgResults() throws Exception {
        rawgServer.expect(requestTo(containsString("https://rawg.test/api/games")))
                .andExpect(queryParam("key", "test-key"))
                .andExpect(queryParam("search", "zelda"))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("page_size", "12"))
                .andRespond(withSuccess("""
                        {"count": 57, "next": "https://rawg.test/api/games?page=3", "previous": null,
                         "results": [
                           {"id": 22511, "slug": "botw", "name": "The Legend of Zelda: Breath of the Wild",
                            "released": "2017-03-03", "background_image": "https://img/botw.jpg",
                            "rating": 4.47, "unknown_field": true},
                           {"id": 1, "name": "No picture", "released": null, "background_image": null}
                         ]}""", MediaType.APPLICATION_JSON));

        mvc.perform(get("/api/games/search").param("q", " zelda ").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(57))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].rawgId").value(22511))
                .andExpect(jsonPath("$.results[0].name").value("The Legend of Zelda: Breath of the Wild"))
                .andExpect(jsonPath("$.results[0].released").value("2017-03-03"))
                .andExpect(jsonPath("$.results[0].backgroundImage").value("https://img/botw.jpg"))
                .andExpect(jsonPath("$.results[1].backgroundImage").isEmpty());
        rawgServer.verify();
    }

    @Test
    void blankQueryIsRejected() throws Exception {
        mvc.perform(get("/api/games/search").param("q", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingQueryIsRejected() throws Exception {
        mvc.perform(get("/api/games/search")).andExpect(status().isBadRequest());
    }

    @Test
    void oversizedPageIsRejected() throws Exception {
        mvc.perform(get("/api/games/search").param("q", "zelda").param("pageSize", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rawgFailureBecomesBadGatewayWithoutLeakingTheKey() throws Exception {
        rawgServer.expect(requestTo(containsString("/games"))).andRespond(withServerError());

        mvc.perform(get("/api/games/search").param("q", "zelda"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("The RAWG catalogue is unavailable"));
    }

    @Test
    void missingApiKeyIsReported() throws Exception {
        mvcWithKey("").perform(get("/api/games/search").param("q", "zelda"))
                .andExpect(status().isServiceUnavailable());
    }
}
