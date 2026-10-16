package P2POverlay;

import java.util.ArrayList;
import java.util.List;

public class TesteQ3 {
    public static void main(String[] args) throws Exception {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < Node.TOTAL; i++) nodes.add(new Node(i));
        for (Node n : nodes) {
            Thread t = new Thread(n, n.name);
            t.setDaemon(true);
            t.start();
        }
        Thread.sleep(500);

        System.out.println("\n=== CASO 1: P0 busca arquivo35.ext ===");
        nodes.get(0).iniciarBuscar("arquivo35.ext");
        Thread.sleep(1000);

        System.out.println("\n=== CASO 2: P1 busca arquivo99.ext ===");
        nodes.get(1).iniciarBuscar("arquivo99.ext");
        Thread.sleep(500);

        System.out.println("\n=== CASO 2: P4 busca arquivoABC.ext ===");
        nodes.get(4).iniciarBuscar("arquivoABC.ext");
        Thread.sleep(500);
    }
}