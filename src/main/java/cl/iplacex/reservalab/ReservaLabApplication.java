package cl.iplacex.reservalab;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class ReservaLabApplication {
    private ReservaLabApplication() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, String> options = parseOptions(args);
        int port = Integer.parseInt(options.getOrDefault("port", "8080"));
        String environment = options.getOrDefault("environment", "local");
        String color = options.getOrDefault("color", "blue");
        String version = options.getOrDefault("version", "dev");

        ReservaLabServer server = new ReservaLabServer(port, environment, color, version);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.printf("ReservaLab %s activo en http://localhost:%d [%s/%s]%n",
                version, port, environment, color);
        new CountDownLatch(1).await();
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String argument : args) {
            if (argument.startsWith("--") && argument.contains("=")) {
                String[] parts = argument.substring(2).split("=", 2);
                options.put(parts[0], parts[1]);
            }
        }
        return options;
    }
}

