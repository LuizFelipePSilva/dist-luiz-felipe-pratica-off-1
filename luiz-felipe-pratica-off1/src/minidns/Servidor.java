package minidns;


import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    private final Map<String, String> map = new ConcurrentHashMap<>();
    private final Map<String, Set<InetSocketAddress>> assinantes = new ConcurrentHashMap<>();
    private int porta;
    DatagramSocket socket;

    public Servidor(int porta) throws Exception {
        this.porta = porta;
        for(int i = 1; i <= 10; i++){
            map.put("servidor" + i, "192.168.0." + i*10);
        }
    }

    public void rodar() throws Exception{
        socket = new DatagramSocket(porta);
        System.out.println("Servidor " + porta + " iniciado");

        while(true){
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            socket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
            InetSocketAddress receivedFrom = new InetSocketAddress(packet.getAddress(), packet.getPort());
            String[] parts = msg.split("\\s+");

            if(parts[0].equals("QUERY") && parts.length == 2 && map.containsKey(parts[1])){
                assinantes.computeIfAbsent(parts[1], k -> ConcurrentHashMap.newKeySet()).add(receivedFrom);
                enviar(socket, receivedFrom, map.get(parts[1]));
            }
            else if(parts[0].equals("UPDATE")  && parts.length == 3 && map.containsKey(parts[1])){
                map.put(parts[1], parts[2]);
                for (InetSocketAddress a : assinantes.getOrDefault(parts[1], Set.of())) {
                    enviar(socket, a, "UPDATE " + parts[1] + " " + map.get(parts[1]));
                }
            }
            else {
                enviar(socket, receivedFrom, "ERRO");
            }
        }
    }

    public void enviar(DatagramSocket s, InetSocketAddress dest, String msg) throws Exception{
        byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, dest);
        socket.send(packet);
    }
}
