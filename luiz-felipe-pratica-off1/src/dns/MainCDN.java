package dns;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

public class MainCDN {
    static final int ORIGIN = 8000, GEODNS = 5353;
    static final long TTL = 10_000;

    public static void main(String[] args) throws Exception {
        resetOrigem();

        daemon(new DataStorage(ORIGIN));
        daemon(new EdgeServer(8001, "127.0.0.1", ORIGIN, TTL));
        daemon(new EdgeServer(8002, "127.0.0.1", ORIGIN, TTL));
        daemon(new EdgeServer(8003, "127.0.0.1", ORIGIN, TTL));
        daemon(new GeoDNS(GEODNS, Map.of(
                "NORTE", "127.0.0.1:8001",
                "SUL", "127.0.0.1:8002",
                "LESTE", "127.0.0.1:8003")));
        Thread.sleep(500); // tempo para os REGISTER chegarem na Origem

        Client c = new Client(GEODNS, "127.0.0.1");

        System.out.println("\n=== CASO 1: HIT vs MISS ===");
        c.get("arquivo20.ext", "NORTE"); // MISS
        c.get("arquivo20.ext", "NORTE"); // HIT, tempo menor

        System.out.println("\n=== CASO 2: TTL (10 s) ===");
        c.get("arquivo25.ext", "NORTE"); // MISS
        c.get("arquivo25.ext", "NORTE"); // HIT
        System.out.println("(aguardando 11 s)");
        Thread.sleep(11_000);
        c.get("arquivo25.ext", "NORTE"); // MISS (expirou)

        System.out.println("\n=== CASO 3: INVALIDATE ===");
        c.get("arquivo30.ext", "NORTE"); // MISS
        c.get("arquivo30.ext", "SUL");   // MISS
        c.get("arquivo30.ext", "NORTE"); // HIT
        c.get("arquivo30.ext", "SUL");   // HIT
        update("arquivo30.ext", "conteudo novo");
        Thread.sleep(300);
        c.get("arquivo30.ext", "NORTE"); // MISS, conteudo novo
        c.get("arquivo30.ext", "SUL");   // MISS, conteudo novo

        System.out.println("\n=== ERROS ===");
        c.get("arquivo1.ext", "XYZ");      // ERRO regiao desconhecida
        c.get("arquivo2000.ext", "LESTE"); // 404 NOT FOUND
    }

    static void daemon(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    static void update(String arquivo, String conteudo) {
        try (Socket s = new Socket("127.0.0.1", ORIGIN)) {
            s.setSoTimeout(3000);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out.write("UPDATE /" + arquivo + " " + conteudo);
            out.newLine();
            out.flush();
            System.out.println("[admin] UPDATE /" + arquivo + " -> " + in.readLine());
        } catch (IOException e) {
            System.out.println("[admin] erro: " + e.getMessage());
        }
    }

    static void resetOrigem() throws IOException {
        Path dir = Paths.get("origin_files");
        if (!Files.exists(dir)) return;
        try (Stream<Path> w = Files.walk(dir)) {
            w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}