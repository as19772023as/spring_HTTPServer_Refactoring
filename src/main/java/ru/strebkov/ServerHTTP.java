package ru.strebkov;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerHTTP {
    private int port;
    private static ExecutorService executorService; // = Executors.newFixedThreadPool(64);
    protected static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/resources.html",
            "/classic.html", "/events.html", "/events.js");
    static Socket socket;

    public ServerHTTP(int port, int poolSizeThreads) {
        this.port = port;
        executorService = Executors.newFixedThreadPool(poolSizeThreads);
    }
    
    public void startServer() {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер работает");
            while (!serverSocket.isClosed()) {
                socket = serverSocket.accept();
                executorService.execute(() -> {
                    try {
                        processTheRequest(socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }


    public void processTheRequest(Socket socket) throws IOException {
        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        final var out = new BufferedOutputStream(socket.getOutputStream());//

        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        if (requestLine == null) return;

        final var parts = requestLine.split(" ");
        if (parts.length != 3) {
            return;
        }

        final var path = parts[1]; // GET /path HTTP/1.1 = массив [ 0;1;2]
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
        final var mimeType = Files.probeContentType(filePath);// получаем тип файла= "text/plain";

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
        Files.copy(filePath, out); // копируем файл по байтам в выходной поток
        out.flush();
    }
}



