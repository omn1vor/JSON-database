package server;

import client.Request;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final DB db;
    private ServerSocket server;
    private ExecutorService executor;
    boolean interrupted;

    public Server(DB db) {
        this.db = db;
        start();
    }
    public void stop() {
        interrupted = true;
        try {
            server.close();
            executor.shutdown();
        } catch (Exception ignored) {
        }
    }

    private void start() {
        interrupted = false;
        try {
            server = new ServerSocket(10000);
            System.out.println("Server started!");
            executor = Executors.newCachedThreadPool();
            while (!interrupted) {
                Socket socket = server.accept();
                executor.submit(() -> processInput(socket));
            }
        } catch (IOException ignored) {
        }
    }

    private void processInput(Socket socket) {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            String json = input.readUTF();
            System.out.printf("Received: %s%n", json);
            Request request = new Gson().fromJson(json, Request.class);
            if ("exit".equalsIgnoreCase(request.getType())) {
                Response response = new Response();
                response.setOK();
                output.writeUTF(new Gson().toJson(response));
                stop();
                return;
            }
            Response response = db.process(request);
            json = new Gson().toJson(response);
            output.writeUTF(json);
            System.out.printf("Sent: %s%n", json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
