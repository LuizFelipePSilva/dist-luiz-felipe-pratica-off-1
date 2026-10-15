package P2POverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node implements Runnable {
    static final int BASE = 10000;
    static final int TOTAL = 6;
    private static final Pattern PADRAO = Pattern.compile("^arquivo(\\d+)\\.ext$");

    final int id;
    final String name;
    final int port, prevPort, nextPort;
    private final Set<String> files = ConcurrentHashMap.newKeySet();
    private final List<String> historico = Collections.synchronizedList(new ArrayList<>());
    private final ServerSocket serverSocket;


    public Node(int id) throws IOException {
        this.id = id;
        this.name = "P" + id;
        this.port = BASE + id;
        this.prevPort = BASE + (id + TOTAL - 1) % TOTAL;
        this.nextPort = BASE + (id + 1) % TOTAL;
        for (int i = id * 10; i < id * 10 + 10; i++) files.add("arquivo" + i + ".ext");
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket c = serverSocket.accept();
                new Thread(() -> atender(c)).start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) log("ERRO accept: " + e.getMessage());
            }
        }
    }

    private void atender(Socket c) {
        try (c; BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
            String line = in.readLine();
            if (line != null) tratar(line.trim());
        } catch (IOException e) {
            log("ERRO conexao: " + e.getMessage());
        }
    }

    private void tratar(String linha) {
        String[] p = linha.split(";");
        if (p.length != 5) { log("MENSAGEM INVALIDA: " + linha); return;}
        try {
            switch(p[0]) {
                case "BUSCA" -> {
                    log("RECEBIDO " + linha);
                    tratarBusca(Integer.parseInt(p[1]), p[2], p[3], Integer.parseInt(p[4]));
                }
                case "ACHOU" -> {
                    log("RECEBIDO " + linha);
                    tratarAchou(Integer.parseInt(p[1]), p[2], p[3], p[4]);
                }
                default -> {
                    log("MENSAGEM INVALIDA" + linha);
                }

            }
        }
        catch (NumberFormatException e) {
            log("ERRO INVALIDA: " + e.getMessage());
        }
    }

    public void iniciarBuscar(String entrada) {
        Matcher m = PADRAO.matcher(entrada);
        int n = -1;
        if(m.matches()) {
            try {n = Integer.parseInt(m.group(1));} catch (NumberFormatException ignored) {}
        }
        if(n < 1 || n > 60) {
            System.out.println("Erro na entrada de dados, tente novamente");
            return;
        }
        String arquivo = "arquivo" + n + ".ext";
        log("INICIOU busca de " + arquivo);
        tratarBusca(id, entrada, "", 0);
    }

    private void tratarBusca(int origem, String arquivo, String caminho, int saltos) {
        caminho = caminho.isEmpty() ? name : caminho + ">" + name;
        if(files.contains(arquivo)) {
            String achou = "ACHOU;" + origem + ";" + arquivo + ";" + name + ";" + caminho;
            if(origem == id) resultado(arquivo, name, caminho);
            else enviar(prevPort, achou);
            return;
        }
        if(saltos > TOTAL) {log("DESCARTADOS(limite de saltos)" + arquivo); return;}
        enviar(nextPort, "BUSCA;" + origem + ";" + arquivo + ";" + caminho + ";" + (saltos + 1));
    }

    private void tratarAchou(int origem, String arquivo, String dono, String caminho) {
    if(origem == id) resultado(arquivo, name, caminho);
    else enviar(prevPort, "ACHOU;" + origem + ";" + arquivo + ";" + dono + ";" + caminho);
    }

    private void resultado(String arquivo, String dono, String caminho) {
        log("RESULTADO: " + arquivo + " esta em " + dono + " | caminho " + caminho);
    }
    private void enviar(int destino, String entrada) {
        try(Socket a = new Socket("127.0.0.1", destino); PrintWriter out = new PrintWriter(a.getOutputStream(), true)) {
            out.println(entrada);
            log("Enviando para porta " + destino + ": " + entrada);
        } catch(IOException e) {
            log("ERRO ao enviar para porta " + destino + ": " + e.getMessage());
        }
    }
    private void log(String entrada) {
        String msg = "[" + name + "] " + entrada;
        historico.add(msg);
        synchronized (Node.class) {System.out.println(msg);}
    }

    public List<String> getHistorico() {
        return historico;
    }
}
