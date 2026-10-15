package dns;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class DataStorage implements Runnable {
    private final int TAM = 1000;
    private static final Pattern PADRAO = Pattern.compile("^/arquivo(\\d+)\\.ext$");
    private final Path dir = Paths.get("origin_files/");
    private final Set<String> edges = ConcurrentHashMap.newKeySet();
    private final ServerSocket serverSocket;



    public DataStorage(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        Files.createDirectories(dir);
        for (int i = 1; i <= TAM; i++) {
            Path p = dir.resolve("arquivo" + i + ".ext");
            if (!Files.exists(p)) {
                Files.writeString(p, "Conteudo bruto do arquivo" + i + ".ext (v1)");
            }
        }
    }

    @Override
    public void run() {
        while(!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratar(socket)).start();
                }
            catch (IOException e) {
                if (!serverSocket.isClosed()) System.out.println("ERRO accept: " + e.getMessage());
            }
        }
    }

    public void tratar(Socket socket){
        try(socket;
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))){

            String linha = in.readLine();
            if(linha == null) return;
            String[] line = linha.trim().split("\\s+", 3);

            if(line.length == 2 && line[0].equals("REGISTER")){
                String edge = socket.getInetAddress().getHostAddress() + ":" + Integer.parseInt(line[1]);
                edges.add(edge);
                responder(out, "200 OK");
                return;
            }
            if(line.length == 3 && line[0].equals("UPDATE") && PADRAO.matcher(line[1]).matches()){
                try {
                    alterar(line[1].substring(1), line[2]);
                    responder(out, "200 OK");
                }
                catch (IllegalArgumentException e) {
                    responder(out, "400 Bad Request");
                }
                return;
            }
            if(line.length != 2 || !line[0].equals("GET") || !PADRAO.matcher(line[1]).matches()){
                responder(out, "400 BAD REQUEST");
                return;
            }
            String name = line[1].substring(1);
            Path p = dir.resolve(name);
            if(!Files.exists(p)) {
                responder(out, "404 NOT FOUND");
                return;
            }
            String conteudo = ler(p);
            out.write("200 OK");
            out.newLine();
            out.write("Length: " + conteudo.getBytes(StandardCharsets.UTF_8).length);
            out.newLine();
            out.write(conteudo);
            out.newLine();
            out.flush();
        }
        catch (IOException e){
            System.out.println("ERRO conexao: " + e);
        }
    }
    private void alterar(String nome, String conteudo) throws IOException {
        if(!PADRAO.matcher("/" + nome).matches() || !Files.exists(dir.resolve(nome))) throw new IllegalArgumentException("Arquivo inexistente: " + nome);
        escrever(dir.resolve(nome), conteudo.replace("\n", " "));
        for(String edge : edges){
            String[] hp = edge.split(":");
            try(Socket s = new Socket(hp[0], Integer.parseInt(hp[1]))) {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                out.write("INVALIDATE /" + nome);
                System.out.println("INVALIDATE /" + nome);
                out.newLine();
                out.flush();
            }
            catch(IOException e) {
                System.out.println("[ORIGIN] falha ao invalidar " + edge + ": " + e.getMessage());
            }
        }
    }
    private synchronized String ler(Path p) throws IOException {
        return Files.readString(p);
    }

    private synchronized void escrever(Path p, String conteudo) throws IOException {
        Files.writeString(p, conteudo);
    }

    private void responder(BufferedWriter out, String msg) throws IOException {
        out.write(msg);
        out.newLine();
        out.flush();
    }
}
