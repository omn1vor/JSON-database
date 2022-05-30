package client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.nio.file.Files;
import java.nio.file.Path;

public class Request {
    String type;
    JsonElement key;
    JsonElement value;

    public static Request parse(Parameters parameters, String clientPath) {
        String filename = parameters.getFileName();
        if (filename != null) {
            try {
                Path clientDataDir = Path.of(clientPath);
                if (!Files.exists(clientDataDir)) {
                    Files.createDirectories(clientDataDir);
                }
                return new Gson().fromJson(Files.readString(Path.of(clientPath, filename)), Request.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Request request = new Request();
        request.type = parameters.getType();
        request.key = parameters.getKey() == null ? null : new JsonPrimitive(parameters.getKey());
        request.value = parameters.getValue() == null ? null : new JsonPrimitive(parameters.getValue());
        return request;
    }

    public String getType() {
        return type;
    }

    public JsonElement getKey() {
        return key;
    }

    public JsonElement getValue() {
        return value;
    }

}
