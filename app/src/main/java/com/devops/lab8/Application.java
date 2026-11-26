package com.devops.lab8;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class Application {
    private static final Gson JSON = new Gson();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "3000"));
        String environment = System.getenv().getOrDefault("NODE_ENV", "development");
        HttpServer server = createServer(new InetSocketAddress(port), environment);
        server.start();
        System.out.println("App running on :" + server.getAddress().getPort());
    }

    static HttpServer createServer(InetSocketAddress address, String environment) throws IOException {
        String hostname = InetAddress.getLocalHost().getHostName();
        HttpServer server = HttpServer.create(address, 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String method = exchange.getRequestMethod();
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                    respond(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                switch (exchange.getRequestURI().getPath()) {
                    case "/" -> respond(exchange, 200, Map.of(
                            "message", " DevOps Lab 8 — CD Pipeline",
                            "version", "2.0.0",
                            "env", environment,
                            "timestamp", Instant.now().toString(),
                            "host", hostname));
                    case "/health", "/health/" -> respond(exchange, 200, Map.of(
                            "status", "OK",
                            "uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0));
                    default -> respond(exchange, 404, Map.of("error", "Not found"));
                }
            }
        });
        return server;
    }

    private static void respond(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
        byte[] bytes = JSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
