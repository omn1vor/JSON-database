package server;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        DB db = new DB(getServerPath(args) + "/db.json");
        new Server(db);
    }

    private static String getServerPath(String[] args) {
        StringBuilder sb = new StringBuilder(System.getProperty("user.dir"));
        if (Arrays.stream(args).anyMatch("--local"::equalsIgnoreCase)) {
            sb.append("/JSON Database/task");
        }
        sb.append("/src/server/data");
        return sb.toString();
    }
}

