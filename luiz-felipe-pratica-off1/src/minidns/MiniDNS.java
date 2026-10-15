package minidns;

public class MiniDNS {
    public static void main(String[] args) throws Exception {
        Thread srv = new Thread(() -> {
            try { new Servidor(5050).rodar(); } catch (Exception e) { e.printStackTrace(); }
        });
        srv.setDaemon(true);
        srv.start();
        Thread.sleep(500);

        Cliente c1 = new Cliente("REQ1", 5050);
        Cliente c2 = new Cliente("REQ2", 5050);
        Cliente c3 = new Cliente("REQ3", 5050);
        c1.enviar("QUERY servidor1");
        c2.enviar("QUERY servidor4");
        c3.enviar("QUERY servidor9");
        Thread.sleep(500);

        Cliente reg = new Cliente("REG", 5050);
        reg.enviar("UPDATE servidor1 192.168.0.111");
        Thread.sleep(300);
        reg.enviar("UPDATE servidor4 192.168.0.144");
        Thread.sleep(300);
        reg.enviar("UPDATE servidor9 192.168.0.299");
        Thread.sleep(1000);

        c1.enviar("QUERY servidor1");
        c2.enviar("QUERY inexistente");
        Thread.sleep(500);
    }
}