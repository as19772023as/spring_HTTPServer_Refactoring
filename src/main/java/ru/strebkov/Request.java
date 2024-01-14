package ru.strebkov;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class Request {
    private final String method;
    private final String path;
    private String body;
    private static final List<String> listMethod = List.of("GET", "POST", "PUT",
            "OPTIONS", "HEAD", "PATCH", " delete", "TRACE", "CONNECT");


    public Request(String requestMethod, String requestPath) {
        this.method = requestMethod;
        this.path = requestPath;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public static Request parse(BufferedReader in) throws IOException {
        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            return null;
        }

        String method = parts[0];
        if ((method == null || method.isBlank()) && !method.equals(listMethod)) {
            return null;
        }
        final var path = parts[1];

        return new Request(method, path);
    }
}
