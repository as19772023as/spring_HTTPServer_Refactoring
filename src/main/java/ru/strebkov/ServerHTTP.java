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
    protected static final Integer PORT = 9999;
    protected static ExecutorService executorService = Executors.newFixedThreadPool(64);
    protected static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/styles.css", "/app.js", "/links.html", "/forms.html",
            "/classic.html", "/events.html", "/events.js");

    static Socket socket;


    public void startServer() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                socket = serverSocket.accept();
                executorService.submit(() -> processTheRequest(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processTheRequest(Socket socket) {
        while (true) {
            try (
                 final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream());//
            ) {
                // must be in form GET /path HTTP/1.1
                final var requestLine = in.readLine();
                if (requestLine == null) continue;

                final var parts = requestLine.split(" ");
                if (parts.length != 3) {
                    continue;
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
                    continue;
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
                     continue;
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



