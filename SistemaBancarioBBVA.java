package fundamentos.inicio.entender.nueve.ejercicios;
import java.util.*;
import java.text.*;

// ============ EXCEPCIONES PERSONALIZADAS ============
class SaldoInsuficienteException extends Exception {
    public SaldoInsuficienteException(double saldo, double monto) {
        super("Error: Saldo insuficiente. Tienes $" + saldo + " pero la operación requiere $" + monto);
    }
}

class PINInvalidoException extends Exception {
    private int intentos;
    public PINInvalidoException(int intentos) {
        super("PIN incorrecto. Intentos restantes: " + intentos);
        this.intentos = intentos;
    }
}

class CuentaBloqueadaException extends Exception {
    public CuentaBloqueadaException() { super("Cuenta bloqueada por seguridad tras 3 intentos fallidos."); }
}

class ClienteNoEncontradoException extends Exception {
    public ClienteNoEncontradoException(String id) { super("El cliente con identificación " + id + " no existe."); }
}

// ============ INTERFAZ Y CLASES DE TRANSACCIÓN ============
interface Transaccionable {
    boolean procesarTransaccion();
    double calcularComision();
    String obtenerDetalles();
}

abstract class Transaccion implements Transaccionable {
    protected String id;
    protected String fecha;
    protected String estado;

    public Transaccion() {
        this.id = "TRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.estado = "PENDIENTE";
    }
}

class Deposito extends Transaccion {
    private double monto;
    public Deposito(double monto) { this.monto = monto; }
    @Override public boolean procesarTransaccion() { this.estado = "EXITOSA"; return true; }
    @Override public double calcularComision() { return 0; }
    @Override public String obtenerDetalles() {
        return String.format("[%s] %s | Depósito | +$%.2f | %s", fecha, id, monto, estado);
    }
}

class Retiro extends Transaccion {
    private double monto;
    public Retiro(double monto) { this.monto = monto; }
    @Override public boolean procesarTransaccion() { this.estado = "EXITOSA"; return true; }
    @Override public double calcularComision() { return 3.0; }
    @Override public String obtenerDetalles() {
        return String.format("[%s] %s | Retiro   | -$%.2f | %s", fecha, id, monto, estado);
    }
}

class Transferencia extends Transaccion {
    private double monto;
    private String destino;
    public Transferencia(double monto, String destino) { this.monto = monto; this.destino = destino; }
    @Override public boolean procesarTransaccion() { this.estado = "EXITOSA"; return true; }
    @Override public double calcularComision() { return monto * 0.01; }
    @Override public String obtenerDetalles() {
        return String.format("[%s] %s | Transfer | -$%.2f a %s | %s", fecha, id, monto, destino, estado);
    }
}

// ============ CLASES DE NEGOCIO ============
class CuentaBancaria {
    private String numero, titular, pin;
    private double saldo;
    private boolean activa = true;
    private int intentosFallidos = 0;
    private List<Transaccion> historial = new ArrayList<>();

    public CuentaBancaria(String numero, String titular, String pin, double saldo) {
        this.numero = numero; this.titular = titular; this.pin = pin; this.saldo = saldo;
    }

    public void verificarPin(String pinIngresado) throws PINInvalidoException, CuentaBloqueadaException {
        if (!activa) throw new CuentaBloqueadaException();
        if (!this.pin.equals(pinIngresado)) {
            intentosFallidos++;
            if (intentosFallidos >= 3) { activa = false; throw new CuentaBloqueadaException(); }
            throw new PINInvalidoException(3 - intentosFallidos);
        }
        intentosFallidos = 0;
    }

    public void depositar(double cant) {
        saldo += cant;
        Deposito d = new Deposito(cant);
        d.procesarTransaccion();
        historial.add(d);
    }

    public void retirar(double cant, String pin) throws Exception {
        verificarPin(pin);
        Retiro r = new Retiro(cant);
        double total = cant + r.calcularComision();
        if (saldo < total) throw new SaldoInsuficienteException(saldo, total);
        if (saldo - total < 100) throw new Exception("Error: El saldo no puede ser menor a $100.");
        saldo -= total;
        r.procesarTransaccion();
        historial.add(r);
    }

    public void transferir(CuentaBancaria dest, double cant, String pin) throws Exception {
        verificarPin(pin);
        Transferencia t = new Transferencia(cant, dest.numero);
        double total = cant + t.calcularComision();
        if (saldo < total) throw new SaldoInsuficienteException(saldo, total);
        if (saldo - total < 100) throw new Exception("Error: Debe mantener al menos $100 tras la transferencia.");
        saldo -= total;
        dest.saldo += cant;
        t.procesarTransaccion();
        historial.add(t);
    }

    public String getNumero() { return numero; }
    public double getSaldo() { return saldo; }
    public void mostrarHistorial() {
        System.out.println("\nHistorial de " + numero + ":");
        if (historial.isEmpty()) System.out.println("No hay transacciones.");
        else historial.forEach(t -> System.out.println(t.obtenerDetalles()));
    }
}

class Cliente {
    private String nombre;
    private Map<String, CuentaBancaria> cuentas = new HashMap<>();
    public Cliente(String nombre) { this.nombre = nombre; }
    public void addCuenta(CuentaBancaria c) { cuentas.put(c.getNumero(), c); }
    public CuentaBancaria getCuenta(String n) { return cuentas.get(n); }
    public String getNombre() { return nombre; }
    public Collection<CuentaBancaria> getCuentas() { return cuentas.values(); }
}

// ============ CLASE PRINCIPAL CON MENÚ ============
public class SistemaBancarioBBVA {
    private static Map<String, Cliente> clientes = new HashMap<>();
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        int op;
        do {
            System.out.println("\n--- CAJERO BBVA INTERACTIVO ---");
            System.out.println("1. Registrar Cliente y Cuenta");
            System.out.println("2. Depositar");
            System.out.println("3. Retirar");
            System.out.println("4. Transferir");
            System.out.println("5. Consultar Saldo e Historial");
            System.out.println("0. Salir");
            System.out.print("Seleccione: ");
            op = sc.nextInt();
            try {
                switch(op) {
                    case 1: registro(); break;
                    case 2: deposito(); break;
                    case 3: retiro(); break;
                    case 4: transferencia(); break;
                    case 5: consulta(); break;
                }
            } catch (Exception e) { System.out.println("ALERTA: " + e.getMessage()); }
        } while (op != 0);
    }

    private static void registro() {
        System.out.print("Nombre: "); String n = sc.next();
        System.out.print("Cédula/ID: "); String id = sc.next();
        System.out.print("Número Cuenta (ej: CTA-01): "); String cta = sc.next();
        System.out.print("PIN (4 dígitos): "); String pin = sc.next();
        System.out.print("Saldo inicial ($): "); double s = sc.nextDouble();
        
        Cliente cli = clientes.getOrDefault(id, new Cliente(n));
        cli.addCuenta(new CuentaBancaria(cta, n, pin, s));
        clientes.put(id, cli);
        System.out.println("Registro exitoso.");
    }

    private static void deposito() throws Exception {
        System.out.print("ID Cliente: "); String id = sc.next();
        Cliente cli = clientes.get(id); if(cli == null) throw new Exception("No existe.");
        System.out.print("Num Cuenta: "); String n = sc.next();
        CuentaBancaria cta = cli.getCuenta(n);
        System.out.print("Monto: "); double m = sc.nextDouble();
        cta.depositar(m);
        System.out.println("Depósito realizado. Nuevo saldo: $" + cta.getSaldo());
    }

    private static void retiro() throws Exception {
        System.out.print("ID Cliente: "); String id = sc.next();
        System.out.print("Num Cuenta: "); String n = sc.next();
        System.out.print("Monto: "); double m = sc.nextDouble();
        System.out.print("Ingrese PIN: "); String p = sc.next();
        clientes.get(id).getCuenta(n).retirar(m, p);
        System.out.println("Retiro exitoso.");
    }

    private static void transferencia() throws Exception {
        System.out.print("Su ID: "); String idO = sc.next();
        System.out.print("Su Cuenta: "); String nO = sc.next();
        System.out.print("ID Destino: "); String idD = sc.next();
        System.out.print("Cuenta Destino: "); String nD = sc.next();
        System.out.print("Monto: "); double m = sc.nextDouble();
        System.out.print("Su PIN: "); String p = sc.next();
        
        CuentaBancaria origen = clientes.get(idO).getCuenta(nO);
        CuentaBancaria destino = clientes.get(idD).getCuenta(nD);
        origen.transferir(destino, m, p);
        System.out.println("Transferencia completada.");
    }

    private static void consulta() {
        System.out.print("ID Cliente: "); String id = sc.next();
        Cliente cli = clientes.get(id);
        if(cli != null) {
            System.out.println("Cliente: " + cli.getNombre());
            cli.getCuentas().forEach(c -> {
                System.out.println("Cuenta: " + c.getNumero() + " | Saldo: $" + c.getSaldo());
                c.mostrarHistorial();
            });
        }
    }
}
