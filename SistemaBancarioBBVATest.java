import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SistemaBancarioBBVATest {

    @Test
    void testDeposito() throws Exception {
        CuentaBancaria cuenta = new CuentaBancaria("CTA-01", "Juan", "1234", 1000);
        cuenta.depositar(500);
        assertEquals(1500, cuenta.getSaldo());
    }

    @Test
    void testRetiro() throws Exception {
        CuentaBancaria cuenta = new CuentaBancaria("CTA-02", "Ana", "4321", 1000);
        cuenta.retirar(100, "4321");
        assertEquals(897, cuenta.getSaldo()); // 100 + comisión 3
    }
}
