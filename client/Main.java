package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        JCommander.newBuilder()
                .addObject(parameters)
                .acceptUnknownOptions(true)
                .build()
                .parse(args);
        String clientPath = getClientPath(args);

        try (Socket socket = new Socket("localhost", 10000)) {
            System.out.println("Client started!");
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            String json = new Gson().toJson(Request.parse(parameters, clientPath));
            output.writeUTF(json);
            System.out.printf("Sent: %s%n", json);
            json = input.readUTF();
            System.out.printf("Received: %s%n", json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getClientPath(String[] args) {
        StringBuilder sb = new StringBuilder(System.getProperty("user.dir"));
        if (Arrays.stream(args).anyMatch("--local"::equalsIgnoreCase)) {
            sb.append("/JSON Database/task");
        }
        sb.append("/src/client/data");
        return sb.toString();
    }
}

class Parameters {
    @Parameter(names = {"--type", "-t"})
    String type;
    @Parameter(names = {"--key", "-k"})
    String key;
    @Parameter(names = {"--value", "-v"})
    String value;
    @Parameter(names = {"--input-file", "-in"})
    String fileName;

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getFileName() {
        return fileName;
    }
}

