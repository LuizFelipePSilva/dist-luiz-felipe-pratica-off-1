package serviceDiscovery;

import java.net.*;
import java.nio.charset.StandardCharsets;

enum Operation {
    MULT, SUM, DIVI, SUB
}

public class SecondaryServer {
    private DatagramSocket socket = null;
    private final int porta, portaDiretorio;
    private final String nome, ipSimulado;
    private volatile boolean ativo = true;

    public SecondaryServer(int porta, int portaDiretorio, String nome, String ipSimulado) {
        this.porta = porta;
        this.portaDiretorio = portaDiretorio;
        this.nome = nome;
        this.ipSimulado = ipSimulado;
    }

    public void rodar() throws Exception {
        socket = new DatagramSocket(porta);
        inicializarRegistro();
        System.out.println(nome +  " iniciando na porta " + porta);
        while(ativo) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try{socket.receive(packet);} catch (SocketException e){break;}
            String req =  new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
            String res;
            try {
                String[] parts = req.split("\\s+");
                if (parts.length != 3) throw new Exception("Mensagem invalida");
                double resultado = calcular(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Operation.valueOf(parts[0].toUpperCase()));
                res = "" + resultado;
            }
            catch (Exception e) { res = "ERRO " + e.getMessage();}; // CORRIGIDO: espaço
            System.out.println("[" + nome + "] " + req + " -> " + res);
            byte[] b = res.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
            socket.send(sendPacket);
        }
    }
    private void inicializarRegistro() {
        Thread t = new Thread(() -> {
            try(DatagramSocket s = new DatagramSocket()) {
                InetAddress inet = InetAddress.getByName("127.0.0.1");
                byte[] buffer = ("REGISTER " + nome + " " + ipSimulado + " " + porta + " SUM,SUB,DIVI,MULT").getBytes(StandardCharsets.UTF_8);
                while(ativo) {
                    s.send(new DatagramPacket(buffer, buffer.length, inet, portaDiretorio)); // CORRIGIDO: destino
                    Thread.sleep(2000);
                }
            }catch (Exception ignored) {}
        });
        t.setDaemon(true); // CORRIGIDO: iniciar a thread
        t.start();
    }
    public void parar() {ativo = false; socket.close();}

    public double calcular(double a, double b, Operation operation) {
        switch (operation) {
            case MULT -> {return a * b;}
            case SUM -> { return a + b;}
            case DIVI -> {
                if(b == 0) throw new ArithmeticException("Divisão por zero");
                return a / b;}
            case SUB -> {return a - b;}
        }
        return -1;
    }
}