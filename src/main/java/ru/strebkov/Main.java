package ru.strebkov;

public class Main {

    public static void main(String[] args) {
        int port = 9999;
        int poolSizeThreads = 64;

        ExampleHttpServer httpServer = new ExampleHttpServer(port, poolSizeThreads);
            httpServer.startServer();
    }
}
