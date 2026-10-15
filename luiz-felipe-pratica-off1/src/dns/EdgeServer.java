package dns;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class EdgeServer implements Runnable {
    record Entrada(String conteudo, long expira) {}
    private final long ttlMs;
    private final Map<String, Entrada> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Entrada> e) { return size() > 100; }
    };
    ServerSocket serverSocket;
    String ipDataStorage;
    int portDataStorage;
    int port;

    public EdgeServer(int port, String ipDataStorage, int portDataStorage, long ttlMs) throws IOException {
        this.port = port;
        this.ttlMs = ttlMs;
        this.ipDataStorage = ipDataStorage;
        this.portDataStorage = portDataStorage;
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        register(port);
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratar(socket)).start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) System.out.println("ERRO accept: " + e.getMessage());
            }
        }
    }

    public void tratar(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String linha = in.readLine();
            if (linha == null) return;
            String[] line = linha.trim().split("\\s+");

            if(line.length == 2 && line[0].equals("INVALIDATE")) {
                synchronized (cache) {
                    Entrada entrada = cache.get(line[1].substring(1));
                    if(entrada != null) {
                        cache.remove(line[1].substring(1));
                        System.out.println("REMOVED: " + line[1]);
                        return;
                    }
                 return;
                }
            }

            if (line.length != 2 || !line[0].equals("GET")) {
                responder(out, "400 BAD REQUEST");
                return;
            }

            String nome = line[1].substring(1);
            String conteudo = null;
            synchronized (cache) {
                Entrada e = cache.get(nome);
                if (e != null && e.expira() < System.currentTimeMillis()) { cache.remove(nome); e = null; }
                if (e != null) conteudo = e.conteudo();
            }
            String estado = "HIT";
            if (conteudo == null) {
                estado = "MISS";
                try (Socket envio = new Socket(ipDataStorage, portDataStorage);
                     BufferedReader input = new BufferedReader(new InputStreamReader(envio.getInputStream()));
                     PrintWriter output = new PrintWriter(envio.getOutputStream(), true)) {
                    output.println("GET " + line[1]);
                    String status = input.readLine();
                    if (status == null || !status.startsWith("200")) {
                        responder(out, status == null ? "502 BAD GATEWAY" : status);
                        return;
                    }
                    input.readLine(); // Length da Origem
                    conteudo = input.readLine();
                    if (conteudo == null) { responder(out, "502 BAD GATEWAY"); return; }
                }
                synchronized (cache) { cache.put(nome, new Entrada(conteudo, System.currentTimeMillis() + ttlMs)); }
            }
            out.write("200 OK"); out.newLine();
            out.write("Cache: " + estado); out.newLine();
            out.write("Length: " + conteudo.getBytes(StandardCharsets.UTF_8).length); out.newLine();
            out.write(conteudo); out.newLine();
            out.flush();
        } catch (IOException e) {
            System.out.println("ERRO conexao: " + e);
        }
    }

    private void responder(BufferedWriter out, String msg) throws IOException {
        out.write(msg);
        out.newLine();
        out.flush();
    }

    private void register(int myPort) {
        try {
            Socket s = new Socket(ipDataStorage,portDataStorage);
            InputStream in = s.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(in));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            out.write("REGISTER " + myPort);
            out.newLine();
            out.flush();
            System.out.println("Resposta do servidor" + input.readLine());
            in.close();
            out.close();
            s.close();
        }
        catch (Exception e) {
            System.out.println("ERRO registrar registro: " + e.getMessage());
        }

    }
}
