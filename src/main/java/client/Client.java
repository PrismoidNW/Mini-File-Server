package client;

import info.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Client extends Thread {
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static Scanner scanner;
    private final Path path = Constants.clientDataDir;

    public static Socket getSocket() {
        return socket;
    }

    public static DataInputStream getIn() {
        return in;
    }

    public static DataOutputStream getOut() {
        return out;
    }

    public static Scanner getScanner() {
        return scanner;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(Constants.address, Constants.port)) {
            Client.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to websocket successfully");

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Username:");
                String user = scanner.nextLine();
                System.out.print("Password: ");
                String pass = scanner.nextLine();
                out.writeUTF("POST LOGIN " + user + " " + pass);
                if (in.readInt() == 505) {
                    System.out.println("Incorrect login!");
                    closeConnection();
                    return;
                }
                Client.scanner = scanner;
                loop:
                while (true) {
                    System.out.print("Please type a command. (1 - Download file, 2 - Save file, 3 - Delete File, 4 " +
                            "- " +
                            "Update file name, exit -" +
                            " Close connection): ");
                    String input = scanner.nextLine();
                    switch (input.toLowerCase()) {
                        case "1" -> {
                            System.out.print("Enter the file's name to download: ");
                            this.determineStatus(this.get(scanner.nextLine()));
                        }
                        case "2" -> {
                            System.out.print("Enter the file's name to save: ");
                            File fileToSave = new File(this.path.toFile(), scanner.nextLine());
                            if (!fileToSave.exists()) {
                                System.out.println("Sorry, that file doesn't exist.");
                                break;
                            }
                            this.determineStatus(this.post(fileToSave));
                        }
                        case "3" -> {
                            System.out.print("Enter the file's name to delete: ");
                            this.determineStatus(this.delete(scanner.nextLine()));
                        }
                        case "4" -> {
                            System.out.print("Enter file's name to update: ");
                            String name = scanner.nextLine();
                            System.out.print("Enter the new file name: ");
                            this.determineStatus(this.update(name, scanner.nextLine()));
                        }
                        case "exit" -> {
                            break loop;
                        }
                        default -> {
                            System.out.println("Unknown command.");
                        }
                    }
                }
                out.writeUTF("EXIT");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.out.println("Could not connect. Reason: " + e.getMessage());
        }
    }

    private void closeConnection() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public int post(File file) throws IOException {
        out.writeUTF("POST " + file.getName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        out.writeInt(bytes.length);
        out.write(bytes);
        return in.readInt();
    }

    private void determineStatus(int status) {
        switch (status) {
            case 200 -> {
                System.out.println("OK");
            }
            case 404 -> {
                System.out.println("CLIENT ERROR");
            }
            case 505 -> {
                System.out.println("SERVER ERROR");
            }
            case 500 -> {
                System.out.println("Already Exists");
            }
        }
    }

    private int delete(String name) throws IOException {
        if (!this.fileExistsOnServer(name)) {
            System.out.println("This file doesn't exist.");
            return 404;
        }
        out.writeUTF("DELETE " + name);
        return in.readInt();
    }

    private int get(String name) throws IOException {
        out.writeUTF("GET " + name);
        if (in.readInt() == 505) {
            System.out.println("That file doesn't exist");
            return 0;
        }
        File file = new File(path.toFile(), name);
        if (file.exists()) {
            System.out.println("This file already exists. Do you want to overwrite it? Y/n");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no")) {
                this.determineStatus(404);
                return 404;
            }
        }
        int length = in.readInt();
        byte[] bytes = in.readNBytes(length);
        Files.write(file.toPath(), bytes);
        return in.readInt();
    }

    private int update(String name, String newName) throws IOException {
        if (!this.fileExistsOnServer(name)) {
            return 505;
        }
        out.writeUTF("UPDATE " + name);
        out.writeUTF(newName);
        return in.readInt();
    }

    private boolean fileExistsOnServer(String name) throws IOException {
        out.writeUTF("GET " + name);
        int status = in.readInt();
        return status >= 200 && status < 300;
    }
}
