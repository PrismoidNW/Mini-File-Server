package info;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public static final int port = 19422;
    public static final InetAddress address;
    //Change these file paths to absolute path
    public static final Path clientDataDir = Paths.get("CHANGE FILE PATH TO CLIENT DATA DIRECTORY");
    public static final Path serverDataDir = Paths.get("CHANGE FILE PATH TO SERVER DATA DIRECTORY");
    //Change username and password
    public static final String username = "Username";
    public static final String password = "Password1234";

    static {
        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
