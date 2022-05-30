package server;

import com.google.gson.JsonElement;

public class Response {
    String response;
    JsonElement value;
    String reason;

    public void setError(String reason) {
        this.response = "ERROR";
        this.reason = reason;
    }

    public void setOK() {
        this.response = "OK";
    }

    public void setOK(JsonElement value) {
        setOK();
        this.value = value;
    }
}
