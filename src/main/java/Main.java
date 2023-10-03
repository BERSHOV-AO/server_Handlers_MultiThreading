import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Server server = new Server(9999);

        server.addHandler("GET", "/", ((request, outputStream) -> server.defaultHandler(outputStream, "index.html")));

        server.start();
    }
}
