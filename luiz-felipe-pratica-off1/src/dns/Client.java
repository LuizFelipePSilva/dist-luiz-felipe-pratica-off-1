package dns;

import java.io.*;
import java.net.Socket;

public class Client {
    private final int portGeoDNS;
    private final String ipGeoDNS;

    public Client(int portGeoDNS, String ipGeoDNS) throws IOException {
        this.portGeoDNS = portGeoDNS;
        this.ipGeoDNS = ipGeoDNS;
    }

    public void get(String file, String region) throws IOException {
        String ip = ""; int port = 0;
        try(Socket socket = new Socket(ipGeoDNS, portGeoDNS)){
            socket.setSoTimeout(3000);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.write(file + " " + region);
            out.newLine();
            out.flush();
            String res = in.readLine();
            if(res == null || res.startsWith("ERRO")){ System.out.println("[cliente] GEODNS: " + res); return;};
            String[] parts = res.trim().split(":");
            ip = parts[0]; port = Integer.parseInt(parts[1]);
        }
        catch (IOException | RuntimeException e){
            System.out.println("[cliente] GEODNS: " + e.getMessage());
            return;
        }

        try(Socket socket = new Socket(ip, port)) {
            socket.setSoTimeout(3000);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            long t0 = System.nanoTime();
            out.write("GET /" + file);
            out.newLine();
            out.flush();
            String status = in.readLine();
            if(status == null || !status.startsWith("200")) {
                System.out.println("[cliente] " + file + " -> " + status);
                return;
            }
            String cache = in.readLine();
            String lenght = in.readLine();
            String conteudo =  in.readLine();
            long us = (System.nanoTime() - t0) / 1000;
            System.out.println("[cliente] " + file + " | " + cache + " | " + lenght + " | " + conteudo + " | " + us);
        }catch (IOException | RuntimeException e){
            System.out.println("[cliente] GEODNS: " + e.getMessage());
            return;
        }
    }
}
