package cl.iplacex.reservalab;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationAcceptanceTest {
    private WebDriver driver;
    private String baseUrl;

    @BeforeEach
    void openBrowser() {
        baseUrl = System.getProperty("acceptance.baseUrl", "http://localhost:8080");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1440,1000");
        String chromeBinary = System.getenv("CHROME_BIN");
        if (chromeBinary != null && !chromeBinary.isBlank()) {
            options.setBinary(chromeBinary);
        }
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Una persona puede completar y confirmar una reserva")
    void confirmsReservationFromBrowser() throws Exception {
        driver.get(baseUrl);
        driver.findElement(By.id("customer")).sendKeys("Edinson Ahumada");
        driver.findElement(By.id("email")).sendKeys("edinson@example.cl");
        new Select(driver.findElement(By.id("workshop"))).selectByVisibleText("Calidad de software");
        driver.findElement(By.id("submit-reservation")).click();

        String message = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#result.success")))
                .getText();
        assertTrue(message.contains("Reserva confirmada"));
        assertTrue(message.contains("Número 1"));

        Path evidenceDirectory = Path.of("target", "acceptance-evidence");
        Files.createDirectories(evidenceDirectory);
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Files.write(evidenceDirectory.resolve("reserva-confirmada.png"), screenshot);
    }
}
