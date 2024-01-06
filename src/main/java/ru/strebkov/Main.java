package ru.strebkov;



public class Main {

    public static int numberOfThreads = 64;
    protected static final int PORT = 9999;

    public static void main(String[] args) {
        final var serverHTTP = new ServerHTTP(PORT, numberOfThreads);

        serverHTTP.addHandler("GET", "/messages", ((request, responseStream) ->
                serverHTTP.responseWithoutContent(responseStream, "404", "Not found!")));

        serverHTTP.addHandler("POST", "/messages", ((request, responseStream) ->
                serverHTTP.responseWithoutContent(responseStream,  "503", "Service Unavailable")));

        serverHTTP.addHandler("GET", "/", ((request, responseStream) ->
                serverHTTP.defaultHandler(responseStream, "spring.svg")));

        serverHTTP.startServer();
    }
}

