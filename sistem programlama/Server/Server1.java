// Server/Server1.java
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.example.SyncMessageOuterClass.*;

public class Server1 {
    private static final int PORT = 5001; // Sunucu 1'in dinleyeceği port
    private static List<Subscriber> subscribers = new ArrayList<>(); // Abone listesi
    private static final Object lock = new Object(); // Thread-safe erişim için lock

    // Diğer sunucuların bilgileri
    private static final String[] OTHER_SERVERS = {"localhost:5002", "localhost:5003"};

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server1 started on port " + PORT);

        // Sunucular arası senkronizasyon için thread başlat
        new Thread(Server1::syncWithOtherServers).start();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected to Server1");

            // Her yeni bağlantı için yeni bir thread oluştur
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    // Sunucular arası senkronizasyon
    private static void syncWithOtherServers() {
        while (true) {
            for (String server : OTHER_SERVERS) {
                String[] parts = server.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                try (Socket socket = new Socket(host, port)) {
                    // SyncMessage nesnesi oluştur
                    SyncMessage.Builder syncMessage = SyncMessage.newBuilder()
                            .setServerId(1)  // Sunucu ID'si
                            .addAllSubscribers(subscribers);  // Abone listesi

                    // SyncMessage nesnesini gönder
                    OutputStream output = socket.getOutputStream();
                    syncMessage.build().writeTo(output);
                    System.out.println("Abone bilgileri gönderildi: " + server);
                } catch (IOException e) {
                    System.out.println("Sunucuya bağlanılamadı: " + server);

                    // Sunucu çöktüyse, abone bilgilerini devral
                    if (port == 5002) {
                        System.out.println("Server2 çöktü. Abone bilgilerini devralıyorum...");
                        // Server2'nin abone bilgilerini yükle (örneğin, bir dosyadan)
                        loadSubscribersFromFile("server2_subscribers.txt");
                    } else if (port == 5003) {
                        System.out.println("Server3 çöktü. Abone bilgilerini devralıyorum...");
                        // Server3'nin abone bilgilerini yükle (örneğin, bir dosyadan)
                        loadSubscribersFromFile("server3_subscribers.txt");
                    }
                }
            }

            try {
                Thread.sleep(10000);  // 10 saniyede bir senkronizasyon yap
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Dosyadan abone bilgilerini yükle
    private static void loadSubscribersFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Abone bilgilerini oku ve listeye ekle
                String[] parts = line.split(",");
                Subscriber subscriber = Subscriber.newBuilder()
                        .setId(Integer.parseInt(parts[0]))
                        .setNameSurname(parts[1])
                        .setStart_date(Long.parseLong(parts[2]))
                        .setLast_accessed(Long.parseLong(parts[3]))
                        .addAllInterests(Arrays.asList(parts[4].split(";")))
                        .setIsOnline(Boolean.parseBoolean(parts[5]))
                        .setDemand(Subscriber.DemandType.valueOf(parts[6]))
                        .build();
                subscribers.add(subscriber);
            }
            System.out.println("Abone bilgileri yüklendi: " + filename);
        } catch (IOException e) {
            System.out.println("Dosya okunamadı: " + filename);
        }
    }

    // İstemci bağlantılarını işleyen sınıf
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (InputStream input = clientSocket.getInputStream();
                 OutputStream output = clientSocket.getOutputStream()) {

                // Protobuf ile gelen veriyi oku
                Subscriber subscriber = Subscriber.parseFrom(input);

                // Abone işlemlerini gerçekleştir
                synchronized (lock) {
                    subscribers.add(subscriber);
                    System.out.println("New subscriber added to Server1: " + subscriber.getNameSurname());
                }

                // Yanıt gönder
                Subscriber response = Subscriber.newBuilder()
                        .setId(subscribers.size())
                        .setNameSurname(subscriber.getNameSurname())
                        .setLastAccessed(System.currentTimeMillis() / 1000)
                        .build();
                response.writeTo(output);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}