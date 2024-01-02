package ru.strebkov;

public class Main {

    public static void main(String[] args) {
        int port = 9999;
        int poolSizeThreads = 64;

        ServerHTTP serverHTTP = new ServerHTTP(port, poolSizeThreads);
            serverHTTP.startServer();
    }
}
