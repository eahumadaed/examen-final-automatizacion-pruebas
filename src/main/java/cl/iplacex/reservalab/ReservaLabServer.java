package cl.iplacex.reservalab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ReservaLabServer implements AutoCloseable {
    private final HttpServer server;
    private final ReservationService reservations;
    private final String environment;
    private final String color;
    private final String version;

    public ReservaLabServer(int port, String environment, String color, String version) throws IOException {
        this.reservations = new ReservationService();
        this.environment = environment;
        this.color = color;
        this.version = version;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        this.server.createContext("/", this::handleHome);
        this.server.createContext("/api/health", this::handleHealth);
        this.server.createContext("/api/reservations", this::handleReservations);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) || !"/".equals(exchange.getRequestURI().getPath())) {
            send(exchange, 404, "application/json", "{\"error\":\"Recurso no encontrado\"}");
            return;
        }

        try (InputStream input = ReservaLabServer.class.getResourceAsStream("/index.html")) {
            if (input == null) {
                send(exchange, 500, "text/plain", "No se encontro index.html");
                return;
            }
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{ENVIRONMENT}}", escapeHtml(environment))
                    .replace("{{COLOR}}", escapeHtml(color))
                    .replace("{{VERSION}}", escapeHtml(version));
            send(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        String body = "{\"status\":\"UP\",\"environment\":\"" + json(environment)
                + "\",\"color\":\"" + json(color) + "\",\"version\":\"" + json(version)
                + "\",\"reservations\":" + reservations.count() + "}";
        send(exchange, 200, "application/json", body);
    }

    private void handleReservations(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Metodo no permitido\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(body);
        try {
            Reservation reservation = reservations.create(
                    form.get("customer"), form.get("email"), form.get("workshop"));
            String response = "{\"id\":" + reservation.id() + ",\"customer\":\""
                    + json(reservation.customer()) + "\",\"workshop\":\""
                    + json(reservation.workshop()) + "\",\"message\":\"Reserva confirmada\"}";
            send(exchange, 201, "application/json", response);
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, "application/json", "{\"error\":\"" + json(exception.getMessage()) + "\"}");
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        if (body.isBlank()) {
            return values;
        }
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
