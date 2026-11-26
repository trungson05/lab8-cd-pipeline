package com.devops.lab8;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static HttpServer server;
    private static URI baseUri;

    @BeforeAll
    static void startServer() throws IOException {
        server = Application.createServer(new InetSocketAddress("127.0.0.1", 0), "test");
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(baseUri.resolve(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void homeReturnsDeploymentMetadata() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals(" DevOps Lab 8 — CD Pipeline", body.get("message").getAsString());
        assertEquals("2.0.0", body.get("version").getAsString());
        assertEquals("test", body.get("env").getAsString());
        assertFalse(body.get("host").getAsString().isBlank());
        assertDoesNotThrow(() -> Instant.parse(body.get("timestamp").getAsString()));
    }

    @Test
    void healthReturnsStatusAndUptime() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals("OK", body.get("status").getAsString());
        assertTrue(body.get("uptime").getAsDouble() >= 0);
    }

    @Test
    void healthSupportsHeadProbes() throws Exception {
        HttpRequest probe = HttpRequest.newBuilder(baseUri.resolve("/health"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = CLIENT.send(probe, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void unknownPathsReturnNotFound() throws Exception {
        assertEquals(404, get("/health/missing").statusCode());
    }
}
