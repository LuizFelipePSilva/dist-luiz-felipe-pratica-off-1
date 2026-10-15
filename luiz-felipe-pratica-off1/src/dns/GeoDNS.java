package dns;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeoDNS implements Runnable {
    int port;
    private final Map<String, String> edges = new ConcurrentHashMap<>();
    private final ServerSocket serverSocket;

    public GeoDNS(int port, Map<String, String> edges) throws IOException {
        this.port = port;
        this.edges.putAll(edges);
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        while(!serverSocket.isClosed()){
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratar(socket)).start();
            }
            catch (IOException e) {
                System.out.println("ERRO accept: " + e.getMessage());
            }
        }
    }

    private void tratar(Socket c) {
        try (c; BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()))) {
            String line = in.readLine();
            if (line == null) return;
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length != 2) { responder(out, "ERRO formato invalido"); return; }
            String ipAndPort = edges.get(tokens[1].toUpperCase());
            if (ipAndPort == null) { responder(out, "ERRO regiao desconhecida"); return; }
            responder(out, ipAndPort);
        } catch (IOException e) {
            System.out.println("ERRO conexao: " + e);
        }
    }

    private void responder(BufferedWriter out, String msg) throws IOException {
        out.write(msg); out.newLine(); out.flush();
    }

}
