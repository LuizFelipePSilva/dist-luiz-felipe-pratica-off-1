package minidns;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class Cliente {
    private final String nome;
    private final int portaServidor;
    private final DatagramSocket socket;
    private final InetAddress addr;

    public Cliente(String nome, int portaServidor) throws Exception {
        this.nome = nome;
        this.portaServidor = portaServidor;
        this.socket = new DatagramSocket();
        this.addr = InetAddress.getByName("127.0.0.1");
        Thread t = new Thread(this::escutar);
        t.setDaemon(true);
        t.start();
    }

    private void escutar() {
        try {
            while (!socket.isClosed()) {
                DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
                socket.receive(p);
                System.out.println("[" + nome + "] recebeu: " +
                        new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    public void enviar(String msg) throws Exception {
        byte[] b = msg.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(b, b.length, addr, portaServidor));
    }
}