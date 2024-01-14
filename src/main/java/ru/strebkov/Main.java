package ru.strebkov;

import org.apache.http.HttpStatus;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {

    public static int numberOfThreads = 64;
    protected static final int PORT = 9999;

    public static void main(String[] args) {
        final var serverTest = new ExampleHttpServer(PORT, numberOfThreads);

        serverTest.addHandler("GET", "/messages", ((request, responseStream) ->
                RequestProcessor.responseWithoutContent(responseStream, HttpStatus.SC_NOT_FOUND, "Not found...")));

        serverTest.addHandler("POST", "/messages", ((request, responseStream) ->
                RequestProcessor.responseWithoutContent(responseStream, 503, "Service Unavailable")));

        //   Доступные файлы для примера "/index.html", "/spring.svg", "/spring.png",
        //   "/styles.css", "/app.js", "/links.html", "/forms.html",
        //  "/classic.html", "/events.html", "/events.js"
        serverTest.addHandler("GET", "/index.html", ((request, responseStream) ->
                defaultHandler(responseStream, "/index.html")));

        serverTest.startServer();
    }

    static void defaultHandler(BufferedOutputStream out, String path) throws IOException {
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        if (mimeType == null) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return;
        }

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
}

