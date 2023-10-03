import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Server server = new Server(9999);

//        server.addHandler("GET", "/messages", new Handler() {
//            public void handle(Request request, BufferedOutputStream responseStream) {
//                // TODO: handlers code
//            }
//        });

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages", (request, responseStream) -> {
            try {
                server.outContentResponse(responseStream, "404", "Not Found");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> server.outContentResponse(responseStream, "503", "Service Unavailable"));

        server.addHandler("GET", "/", ((request, outputStream) -> server.defaultHandler(outputStream, "index.html")));
        server.start();
    }
}
