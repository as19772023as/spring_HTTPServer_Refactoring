package ru.strebkov;

import org.apache.http.HttpStatus;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RequestProcessor {
    public static void processTheRequest(Socket socket, ConcurrentHashMap<String,
            ConcurrentHashMap<String, Handler>> handlers) {

        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            Request request = Request.parse(in);

            // Check for bad requests and drop connection
            if (request == null || !handlers.containsKey(request.getMethod())) {
                // 400 => HttpStatus.SC_BAD_REQUEST
                responseWithoutContent(out, 400, "Bad Request");
                return;
            }
            // Get PATH, HANDLER Map
            ConcurrentHashMap<String, Handler> handlerMap = handlers.get(request.getMethod());

            String requestPath = request.getPath();
            if (handlerMap.containsKey(requestPath)) {
                Handler handler = handlerMap.get(requestPath);
                handler.handle(request, out);
            } else {
                responseWithoutContent(out, HttpStatus.SC_NOT_FOUND, "Not Found");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void responseWithoutContent(BufferedOutputStream out, int responseCode,
                                              String responseStatus) throws IOException {
        out.write((
                "HTTP/1.1 " + responseCode + " " + responseStatus + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}
