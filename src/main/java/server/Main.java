package server;

import info.Constants;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

    public static ServerSocket socket;

    public static void main(String[] args) {
        try (ServerSocket socket = new ServerSocket(Constants.port, 50, Constants.address)) {
            ;
            Main.socket = socket;
            while (!Session.isShutDown || socket.isClosed()) {
                Session session = new Session(socket.accept());
                session.startClient();
            }
        } catch (IOException e) {
        }
    }
}
