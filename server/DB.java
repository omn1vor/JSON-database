package server;

import client.Request;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DB {
    File dbFile;
    Map<String, JsonElement> db;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();
    Lock writeLock = lock.writeLock();

    public DB(String path) {
        dbFile = new File(path);
        initDbFile();
        readDB();
    }

    public Response process(Request request) {
        Response response = new Response();
        Command command;
        try {
            command = Command.valueOf(request.getType().toUpperCase());
        } catch (Exception e) {
            response.setError(ERRORS.INCORRECT_COMMAND.text);
            return response;
        }
        if (!command.isValid(request.getKey(), request.getValue())) {
            response.setError(ERRORS.MALFORMED_REQUEST.text);
        }
        switch (command) {
            case GET -> {
                Optional<JsonElement> value = get(request.getKey());
                if (value.isEmpty()) {
                    response.setError(ERRORS.NO_SUCH_KEY.text);
                } else {
                    response.setOK(value.get());
                }
            }
            case SET -> {
                try {
                    set(request.getKey(), request.getValue());
                    response.setOK();
                } catch (Exception e) {
                    response.setError(ERRORS.NO_SUCH_KEY.text);
                }
            }
            case DELETE -> {
                try {
                    delete(request.getKey());
                    response.setOK();
                } catch (Exception e) {
                    response.setError(ERRORS.NO_SUCH_KEY.text);
                }
            }
        }
        return response;
    }

    public Optional<JsonElement> get(JsonElement keysPath) {
        readLock.lock();
        DataPath dataPath = DataPath.parse(keysPath);
        JsonElement current = db.get(dataPath.keys.get(0));
        for (int i = 1; i < dataPath.keys.size(); i++) {
            if (current == null) {
                break;
            }
            current = current.getAsJsonObject().get(dataPath.keys.get(i));
        }
        readLock.unlock();
        return Optional.ofNullable(current);
    }

    public void set(JsonElement keysPath, JsonElement value) {
        writeLock.lock();
        DataPath dataPath = DataPath.parse(keysPath);
        if (dataPath.key.isEmpty()) {
            db.put(dataPath.property, value);
        } else {
            JsonElement current = db.get(dataPath.key);
            if (current == null) {
                throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text);
            }
            for (String key : dataPath.subKeys) {
                if (!current.getAsJsonObject().has(key)) {
                    throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text);
                }
                current = current.getAsJsonObject().get(key);
            }
            if (current.getAsJsonObject().has(dataPath.property)) {
                current.getAsJsonObject().remove(dataPath.property);
            }
            current.getAsJsonObject().add(dataPath.property, value);
        }
        writeDB();
        writeLock.unlock();
    }

    public void delete(JsonElement keysPath) {
        writeLock.lock();
        DataPath dataPath = DataPath.parse(keysPath);
        if (dataPath.key.isEmpty()) {
            db.remove(dataPath.property);
        } else {
            JsonElement current = db.get(dataPath.key);
            if (current == null) {
                throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text);
            }
            for (String key : dataPath.subKeys) {
                if (!current.getAsJsonObject().has(key)) {
                    throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text);
                }
                current = current.getAsJsonObject().get(key);
            }
            if (!current.getAsJsonObject().has(dataPath.property)) {
                throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text);
            }
            current.getAsJsonObject().remove(dataPath.property);
        }
        writeDB();
        writeLock.unlock();
    }

    private void initDbFile() {
        try {
            if (!dbFile.exists()) {
                Path parentDir = dbFile.toPath().getParent();
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                db = new HashMap<>();
                writeDB();
            } else if (!dbFile.isFile()) {
                throw new IOException();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_DB.text, e);
        }
    }

    private void readDB() {
        try {
            String json = Files.readString(dbFile.toPath());
            Type mapType = new TypeToken<Map<String, JsonElement>>() {}.getType();
            db = new Gson().fromJson(json, mapType);
        } catch (Exception e) {
            throw new RuntimeException(ERRORS.DB_FILE_IO_ERROR.text, e);
        }
    }

    private void writeDB() {
        try {
            String json = new Gson().toJson(db);
            Files.writeString(dbFile.toPath(), json);
        } catch (Exception e) {
            throw new RuntimeException(ERRORS.DB_FILE_IO_ERROR.text, e);
        }
    }
}

enum Command {
    GET,
    SET,
    DELETE;

    public boolean isValid(JsonElement key, JsonElement value) {
        if (this == GET || this == DELETE) {
            return key != null;
        } else {
            return key != null && value != null;
        }
    }
}

class DataPath {
    String property = "";
    String key = "";
    List<String> subKeys = new ArrayList<>();
    List<String> keys = new ArrayList<>();

    static DataPath parse(JsonElement keys) {
        DataPath dataPath = new DataPath();
        try {
            if (keys.isJsonArray()) {
                JsonArray arr = keys.getAsJsonArray();
                int size = arr.size();
                for (int i = 0; i < size; i++) {
                    String value = arr.get(i).getAsString();
                    dataPath.keys.add(value);
                    if (i == size - 1) {
                        dataPath.property = value;
                    } else if (i == 0) {
                        dataPath.key = value;
                    } else {
                        dataPath.subKeys.add(value);
                    }
                }
            } else {
                dataPath.property = keys.getAsString();
            }
        } catch (JsonSyntaxException e) {
            dataPath.property = keys.getAsString(); // what are you?
        } catch (Exception e) {
            throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text, e);
        }
        if (dataPath.property == null || dataPath.property.isEmpty()) {
            throw new IllegalArgumentException(ERRORS.WRONG_PATH_TO_PROPERTY.text);
        }
        return dataPath;
    }

}