import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final String NOT_FOUND_CODE = "404";
    private final String NOT_FOUND_TEXT = "Not Found";
    private final int NUMBER_THREADS = 64;
    private final int PORT_SERVER_SOCKET;
    List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private ExecutorService threadPool;

    private ConcurrentHashMap<String, Map<String, Handler>> handlersStorageMap;

    public Server(int port) {
        //  try {
        // serverSocket = new ServerSocket(port);
        PORT_SERVER_SOCKET = port;

        threadPool = Executors.newFixedThreadPool(NUMBER_THREADS);
        handlersStorageMap = new ConcurrentHashMap<>();

        System.out.println("Веб-сервер запущен на порту " + PORT_SERVER_SOCKET);
        //   } catch (IOException e) {
        //       e.printStackTrace();
        //    }
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(PORT_SERVER_SOCKET)) {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Подключение приято от клиента: " + clientSocket.getInetAddress().getHostAddress());
                threadPool.execute(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
//        try {
//            while (true) {
//                Socket clientSocket = serverSocket.accept();
//                System.out.println("Подключение принято от клиента: " + clientSocket.getInetAddress().getHostAddress());
//
//                threadPool.execute(() -> handleConnection(clientSocket));
//
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void handleConnection(Socket clientSocket) {
        try (final var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             final var out = new BufferedOutputStream(clientSocket.getOutputStream())) {

            final var requestLine = in.readLine();
            System.out.println("Получен HTTP-запрос: " + requestLine);

            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                System.out.println("not path");
                clientSocket.close();
                return;
            }

            String method = parts[0];
            final var path = parts[1];
            Request request = new Request(method, path);

            // Проверяем наличие плохих запросов и разрываем соединение
            if (request == null || !handlersStorageMap.contains(request.getMethod())) {
                outContentResponse(out, NOT_FOUND_CODE, "Bad Request");
                return;
            }

            // Получить ПУТЬ, MAP
            Map<String, Handler> handlerMap = handlersStorageMap.get(request.getMethod());
            String requestPath = request.getPath();
            if (handlerMap.containsKey(requestPath)) {
                Handler handler = handlerMap.get(requestPath);
                handler.handle(request, out);
            } else {
                // не найден
                if (!validPaths.contains(request.getPath())) {
                    outContentResponse(out, NOT_FOUND_CODE, NOT_FOUND_TEXT);
                } else {
                    System.out.println("default handler");
                    //defaultHandler(out, path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void defaultHandler(BufferedOutputStream out, String path) throws IOException {
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
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
    }

    private Request createRequest(String method, String path) {
        if (method != null && !method.isBlank()) {
            return new Request(method, path);
        } else {
            return null;
        }
    }

    void addHandler(String method, String path, Handler handler) {
        if (!handlersStorageMap.containsKey(method)) {
            handlersStorageMap.put(method, new HashMap<>());
        }
        handlersStorageMap.get(method).put(path, handler);
    }



//            if (!validPaths.contains(path)) {
//                out.write((
//                        "HTTP/1.1 404 Not Found\r\n" +
//                                "Content-Length: 0\r\n" +
//                                "Connection: close\r\n" +
//                                "\r\n"
//                ).getBytes());
//                out.flush();
//                return;
//            }
//
//            final var filePath = Path.of(".", "public", path);
//            final var mimeType = Files.probeContentType(filePath);
//
//            if (path.equals("/classic.html")) {
//                final var template = Files.readString(filePath);
//
//                final var content = template.replace(
//                        "{time}", LocalDateTime.now().toString()
//                ).getBytes();
//                out.write((
//                        "HTTP/1.1 200 OK\r\n" +
//                                "Content-Type: " + mimeType + "\r\n" +
//                                "Content-Length: " + content.length + "\r\n" +
//                                "Connection: close\r\n" +
//                                "\r\n"
//                ).getBytes());
//                out.write(content);
//                out.flush();
//                return;
//            }
//
//            final var length = Files.size(filePath);
//            out.write((
//                    "HTTP/1.1 200 OK\r\n" +
//                            "Content-Type: " + mimeType + "\r\n" +
//                            "Content-Length: " + length + "\r\n" +
//                            "Connection: close\r\n" +
//                            "\r\n"
//            ).getBytes());
//            Files.copy(filePath, out);
//            out.flush();
//
//            clientSocket.close();
//            System.out.println("Подключение закрыто для клиента: " + clientSocket.getInetAddress().getHostAddress());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void outContentResponse(BufferedOutputStream out, String code, String status) throws IOException {
        out.write((
                "HTTP/1.1 " + code + " " + status + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}