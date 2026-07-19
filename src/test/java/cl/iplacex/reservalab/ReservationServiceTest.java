package cl.iplacex.reservalab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationServiceTest {
    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService();
    }

    @Test
    @DisplayName("Crea una reserva valida y asigna un identificador")
    void createsValidReservation() {
        Reservation reservation = service.create("Edinson Ahumada", "EDINSON@EJEMPLO.CL", "Selenium");

        assertEquals(1L, reservation.id());
        assertEquals("Edinson Ahumada", reservation.customer());
        assertEquals("edinson@ejemplo.cl", reservation.email());
        assertEquals("Selenium", reservation.workshop());
        assertEquals(1, service.count());
    }

    @Test
    @DisplayName("Incrementa el identificador sin compartir estado entre pruebas")
    void incrementsReservationIdentifier() {
        Reservation first = service.create("Ana Soto", "ana@example.cl", "JUnit");
        Reservation second = service.create("Luis Diaz", "luis@example.cl", "CI/CD");

        assertEquals(1L, first.id());
        assertEquals(2L, second.id());
        assertEquals(2, service.count());
    }

    @Test
    @DisplayName("Rechaza un nombre vacio")
    void rejectsBlankCustomer() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("  ", "persona@example.cl", "JUnit"));

        assertEquals("El nombre es obligatorio", exception.getMessage());
        assertEquals(0, service.count());
    }

    @Test
    @DisplayName("Rechaza un correo con formato invalido")
    void rejectsInvalidEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("Persona", "correo-invalido", "JUnit"));

        assertTrue(exception.getMessage().contains("formato valido"));
    }

    @Test
    @DisplayName("Rechaza un taller fuera del catalogo")
    void rejectsUnknownWorkshop() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("Persona", "persona@example.cl", "Docker"));

        assertEquals("El taller seleccionado no existe", exception.getMessage());
    }
}

