package cl.iplacex.reservalab;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaLabApplicationIT {
    private ReservaLabServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = new ReservaLabServer(0, "integration-test", "blue", "it-build");
        server.start();
        baseUrl = "http://localhost:" + server.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    @DisplayName("El health check informa ambiente, slot y version")
    void exposesHealthInformation() throws Exception {
        HttpResponse<String> response = get("/api/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertTrue(response.body().contains("\"environment\":\"integration-test\""));
        assertTrue(response.body().contains("\"color\":\"blue\""));
        assertTrue(response.body().contains("\"version\":\"it-build\""));
    }

    @Test
    @DisplayName("La pagina principal carga la aplicacion de reservas")
    void servesReservationPage() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ReservaLab"));
        assertTrue(response.body().contains("id=\"reservation-form\""));
    }

    @Test
    @DisplayName("La API crea una reserva valida")
    void createsReservationThroughApi() throws Exception {
        HttpResponse<String> response = post("customer=Edinson+Ahumada&email=edinson%40example.cl&workshop=CI%2FCD");

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"message\":\"Reserva confirmada\""));
        assertTrue(response.body().contains("\"id\":1"));
    }

    @Test
    @DisplayName("La API responde 400 ante datos invalidos")
    void rejectsInvalidReservationThroughApi() throws Exception {
        HttpResponse<String> response = post("customer=Edinson&email=correo-invalido&workshop=JUnit");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("formato valido"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/reservations"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

