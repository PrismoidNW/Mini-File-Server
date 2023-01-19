package server;

import info.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Session {
    public static boolean isShutDown = false;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Path path = Constants.serverDataDir;

    public Session(Socket socket) {
        this.socket = socket;
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startClient() {
        System.out.println("Connected to web socket client ");

        try (socket) {
            String request = in.readUTF();
            String[] splitRequest = request.split(" ");
            if ((splitRequest[0] + " " + splitRequest[1]).equals("POST LOGIN")) {
                String username = splitRequest[2];
                String password = splitRequest[3];
                if (!(username.equals(Constants.username) || password.equals(Constants.password))) {
                    out.writeInt(505);
                    System.out.print("\nFailed login attempt!");
                    return;
                }
                out.writeInt(200);
            }
            while (!isShutDown && !socket.isClosed()) {
                handleRequest(in.readUTF());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleRequest(String request) throws IOException {
        String[] splitRequest = request.split(" ");
        String requestType = splitRequest[0];
        if (requestType.equals("EXIT")) {
            CompletableFuture.runAsync(() -> {
                try {
                    Main.socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            isShutDown = true;
            return;
        }
        String param = splitRequest[1];

        switch (requestType) {
            case "GET" -> {
                this.get(param);
            }
            case "POST" -> {
                this.post(param);
            }
            case "DELETE" -> {
                this.delete(param);
            }
            case "UPDATE" -> {
                this.update(param, in.readUTF());
            }
            default -> {
                System.out.println("Unknown command");
            }
        }
    }

    private int get(String name) throws IOException {
        File file = new File(path.toFile(), name);
        if (!file.exists()) {
            out.writeInt(505);
            return 505;
        }
        if (path.toFile().listFiles() == null) {
            out.writeInt(505);
            return 505;
        }
        for (File f : path.toFile().listFiles()) {
            if (f.getName().equals(name)) {
                out.writeInt(200);
                byte[] bytes = Files.readAllBytes(file.toPath());
                out.writeInt(bytes.length);
                out.write(bytes);
                break;
            }
        }
        int status = 200;
        out.writeInt(status);
        return status;
    }

    public int delete(String name) throws IOException {
        File file = new File(path.toFile(), name);
        if (!file.exists()) {
            return 505;
        }
        file.delete();
        out.writeInt(200);
        return 200;
    }

    public int update(String name, String newName) throws IOException {
        File file = new File(this.path.toFile(), name);
        File newFile = new File(this.path.toFile(), newName);
        if (!file.exists()) {
            out.writeInt(505);
            return 505;
        }
        file.renameTo(newFile);
        return 200;
    }

    private int post(String name) throws IOException {
        int length = in.readInt();
        byte[] bytes = in.readNBytes(length);
        File file = new File(path.toFile(), name);
        if (file.exists()) {
            out.writeInt(500);
            return 500;
        }
        file.createNewFile();
        Files.write(file.toPath(), bytes);
        out.writeInt(200);
        return 200;
    }
}
