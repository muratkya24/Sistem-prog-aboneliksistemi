// Server/Server3.java
import java.net.*;
import java.io.*;
import java.util.*;
import com.example.SubscriberOuterClass.Subscriber; // Protobuf tarafından oluşturulan Subscriber sınıfı

public class Server3 {
    private static final int PORT = 5003; // Sunucu 3'ün dinleyeceği port
    private static List<Subscriber> subscribers = new ArrayList<>(); // Abone listesi
    private static final Object lock = new Object(); // Thread-safe erişim için lock

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server3 started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected to Server3");

            // Her yeni bağlantı için yeni bir thread oluştur
            new Thread(new ClientHandler(clientSocket)).start();
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
                    System.out.println("New subscriber added to Server3: " + subscriber.getNameSurname());
                }

                // Yanıt gönder
                Subscriber response = Subscriber.newBuilder()
                        .setId(subscribers.size()) // Yeni abone ID'si
                        .setNameSurname(subscriber.getNameSurname())
                        .setLastAccessed(System.currentTimeMillis() / 1000) // UNIX zaman damgası
                        .build();
                response.writeTo(output);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}