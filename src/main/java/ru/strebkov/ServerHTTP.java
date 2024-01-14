package ru.strebkov;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerHTTP {
    private final int port;
    private final ExecutorService executorService;
    //    protected static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
//            "/styles.css", "/app.js", "/links.html", "/forms.html",
//            "/classic.html", "/events.html", "/events.js");
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();
    static Socket socket;

    public ServerHTTP(int port, int numberOfThreads) {
        executorService = Executors.newFixedThreadPool(numberOfThreads);
        this.port = port;
    }

    public void startServer() {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер работает");
            while (true) {
                socket = serverSocket.accept();
                executorService.submit(() -> RequestProcessor.processTheRequest(socket, handlers));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }
}


