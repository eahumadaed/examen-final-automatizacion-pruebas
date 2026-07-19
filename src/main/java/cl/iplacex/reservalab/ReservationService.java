package cl.iplacex.reservalab;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class ReservationService {
    private static final List<String> WORKSHOPS = List.of("JUnit", "Selenium", "CI/CD");

    private final AtomicLong sequence = new AtomicLong();
    private final List<Reservation> reservations = new CopyOnWriteArrayList<>();

    public Reservation create(String customer, String email, String workshop) {
        String normalizedCustomer = requireText(customer, "El nombre es obligatorio");
        String normalizedEmail = requireText(email, "El correo es obligatorio").toLowerCase(Locale.ROOT);
        String normalizedWorkshop = requireText(workshop, "El taller es obligatorio");

        if (!normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato valido");
        }
        if (!WORKSHOPS.contains(normalizedWorkshop)) {
            throw new IllegalArgumentException("El taller seleccionado no existe");
        }

        Reservation reservation = new Reservation(
                sequence.incrementAndGet(), normalizedCustomer, normalizedEmail, normalizedWorkshop);
        reservations.add(reservation);
        return reservation;
    }

    public int count() {
        return reservations.size();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

