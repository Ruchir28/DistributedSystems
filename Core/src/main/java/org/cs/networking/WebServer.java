package org.cs.networking;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Each Executor Node will start a webserver , so that it can receive tasks
 * from the driver node through http requests
 */
public class WebServer {
    private static final String STATUS_ENDPOINT = "/status";
    private final int port;
    private HttpServer server;
    private final OnRequestCallback onRequestCallback;

    public WebServer(int port, OnRequestCallback onRequestCallback) {
        this.port = port;
        this.onRequestCallback = onRequestCallback;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(onRequestCallback.getEndpoint());

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }
        InputStream inputStream = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data,0,nRead);
        }
        buffer.flush();
        byte[] result = onRequestCallback.handleRequest(buffer.toByteArray());
        sendResponse(result, exchange);
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = "Server is alive\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
}

