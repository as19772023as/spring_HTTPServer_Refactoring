package ru.strebkov;

import java.io.IOException;

public class Main {

    public  static int numberOfThreads = 64;
    protected static final Integer PORT = 9999;

    public static void main(String[] args) {
        final var serverHTTP = new ServerHTTP(numberOfThreads);
       // serverHTTP.startServer(PORT);

        serverHTTP.addHandler("GET", "/messages", ((request, responseStream) -> {
            try {
                serverHTTP.responseWithoutContent(responseStream,"404", "Not found");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        serverHTTP.addHandler("POST", "/messages", (request, responseStream) ->
                serverHTTP.responseWithoutContent(responseStream, "404", "Not found"));

        serverHTTP.addHandler("GET", "/", ((request, responseStream)
                -> serverHTTP.defaultHandler(responseStream, "spring.png")));

            serverHTTP.startServer(PORT);
    }
}

