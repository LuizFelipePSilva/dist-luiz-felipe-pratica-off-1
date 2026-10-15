package P2POverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainSearch {
    private List<Node> nodes = new ArrayList<>();

    public MainSearch() throws Exception {
        for(int i =0; i < Node.TOTAL; i++) nodes.add(new Node(i));
        for(Node n : nodes) {
            Thread t = new Thread(n, n.name);
            t.setDaemon(true);
            t.start();
        }
    }

    public void console() {
        Scanner input = new Scanner(System.in);
        System.out.println("Formato P<0-5> arquivo<N>.ext | sair");
        while(input.hasNextLine()) {
            String line = input.nextLine().trim();
            if(line.equalsIgnoreCase("sair")) break;
            if(line.isEmpty()) continue;
            String[] tokens = line.split(" ");
            if(tokens.length != 2 || !tokens[0].matches("P[0-5]")) {
                System.out.println("Erro na entrada de dados");
                continue;
            }
            nodes.get(Integer.parseInt(tokens[0].substring(1))).iniciarBuscar(tokens[1]);
        }
    }

    public static void main(String[] args) throws Exception {
        new MainSearch().console();
    }
}
