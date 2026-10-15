package serviceDiscovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainServer {
    public static void main(String[] args) throws Exception{
        daemon(() -> new Diretorio(6009).rodar());
        SecondaryServer c1 = new SecondaryServer(7001, 6009, "calc1", "10.0.0.1");
        SecondaryServer c2 = new SecondaryServer(7002, 6009, "calc2", "10.0.0.2");
        SecondaryServer c3 = new SecondaryServer(7003, 6009, "calc3", "10.0.0.3");
        daemon(c1::rodar); daemon(c2::rodar); daemon(c3::rodar);
        Thread.sleep(1000);

        cliente("CLI1", "SUM", "SUM 1 2");
        cliente("CLI2", "SUM", "SUM 3 4");
        cliente("CLI3", "SUM", "SUM 5 6");
        cliente("CLI1", "MULT", "MULT 2 3");
        cliente("CLI2", "MULT", "MULT 4 5");
        cliente("CLI3", "MULT", "MULT 6 7");
        cliente("CLI1", "DIVI", "DIVI 1 0");

        System.out.println("--- derrubando calc2 ---");
        c2.parar();
        Thread.sleep(7000);
        cliente("CLI1", "SUM", "SUM 1 1");
        cliente("CLI2", "SUM", "SUM 2 2");
        cliente("CLI3", "SUM", "SUM 3 3");

    }
    interface Tarefa {void run() throws Exception;}

    static void daemon(Tarefa t){
        Thread th = new Thread(() ->{try {t.run();} catch (Exception e) {e.printStackTrace();}});
        th.setDaemon(true);
        th.start();
    }

    static String udp(int porta, String msg) throws Exception{
        try (DatagramSocket socket = new DatagramSocket()) { // CORRIGIDO: sem porta
            socket.setSoTimeout(2000);
            byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(buf, buf.length, InetAddress.getByName("127.0.0.1"), porta));
            DatagramPacket r = new DatagramPacket(new byte[1024], 1024); // CORRIGIDO: buffer próprio
            socket.receive(r);
            return new String(r.getData(),0, r.getLength(), StandardCharsets.UTF_8);
        }
    }

    static void cliente(String nome, String servico, String op) throws Exception{
        String d = udp(6009, "DISCOVER " + servico); // CORRIGIDO: espaço
        System.out.println("[" + nome + "] DIRETORIO" + d + " -> " + op);
        if(d.startsWith("ERRO")) return;
        System.out.println("[" + nome + "] Resultado " + udp(Integer.parseInt(d.split(" ")[2]), op));
    }
}

class Diretorio {
    static class Intancia {
        final String nome, ip;
        final int porta;
        final Set<String> servicos;
        volatile long ultimo = System.currentTimeMillis();

        Intancia(String nome, String ip, int porta, Set<String> servicos) {
            this.nome = nome;
            this.ip = ip;
            this.porta = porta;
            this.servicos = servicos;
        }
    }
    private static final long ms = 6000;
    private final Map<String, Intancia> instancias = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> rr =  new ConcurrentHashMap<>();
    private final int porta;

    public Diretorio(int porta) {this.porta = porta;}

    public void rodar() throws Exception {
        try(DatagramSocket socket = new DatagramSocket(porta)) {
            System.out.println("[DIRETORIO] iniciando na porta " + porta);
            while(true) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                String[] a = new String(packet.getData(), 0, packet.getLength()).split(" ");
                String resp = tratar(a);
                if(resp != null) {
                    byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket sendPacket = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    System.out.println("[DIRETORIO]" + String.join(" ", a) + " -> " + resp);
                }
            }
        }
    }

    public String tratar(String[] mensagem) {
        if(mensagem[0].equals("REGISTER") && mensagem.length == 5){
            Set<String> servicos = new HashSet<>(Arrays.asList(mensagem[4].toUpperCase().split(",")));
            Intancia intancia = new Intancia(mensagem[1], mensagem[2], Integer.parseInt(mensagem[3]), servicos);
            Intancia antiga = instancias.put(mensagem[1], intancia);
            if(antiga == null) System.out.println("[DIRETORIO] registrado :" + mensagem[1]);
            return null;
        };
        if(mensagem[0].equals("DISCOVER") && mensagem.length == 2){
            String svc = mensagem[1].toUpperCase();
            long now =  System.currentTimeMillis();
            List<Intancia> ativos = instancias.values().stream().filter(i -> i.servicos.contains(svc) && now - i.ultimo < ms).sorted(Comparator.comparing(i -> i.nome)).toList();
            if(ativos.isEmpty()) return "ERRO sem servidor ativo";
            int idx = Math.floorMod(rr.computeIfAbsent(svc, s -> new AtomicInteger()).getAndIncrement(), ativos.size());
            Intancia ativo = ativos.get(idx);
            return ativo.nome + " " + ativo.ip + " " + ativo.porta;
        }
        return "ERRO";
    }

}