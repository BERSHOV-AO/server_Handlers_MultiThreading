import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            threadPool = Executors.newFixedThreadPool(64);
            System.out.println("Веб-сервер запущен на порту " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Подключение принято от клиента: " + clientSocket.getInetAddress().getHostAddress());

                threadPool.execute(() -> handleConnection(clientSocket));
//                threadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        handleConnection(clientSocket);
//                    }
//                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             final var out = new BufferedOutputStream(clientSocket.getOutputStream())) {

            final var requestLine = in.readLine();
            System.out.println("Получен HTTP-запрос: " + requestLine);

            final var parts = requestLine.split(" ");

            if (parts.length != 3) {

                System.out.println("not path");
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);

                final var content = template.replace(
                        "{time}", LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();

            clientSocket.close();
            System.out.println("Подключение закрыто для клиента: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}