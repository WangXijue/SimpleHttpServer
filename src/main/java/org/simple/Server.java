package org.simple;

import com.sun.javafx.runtime.SystemProperties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by OutOfMemory on 2017/06/23.
 */
public class Server {
    private static Logger log = LogManager.getLogger(Server.class);

    public static void main(String[] args) {
        log.info("SimpleHttpServer starting...");

        ServerSocket serverSocket = null;
        try {
            log.info("Create server socket and binding");
            serverSocket = new ServerSocket(8080);
        } catch (IOException e) {
            log.error("Failed to create server socket", e);
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.info("Connection accepted {}", clientSocket.getInetAddress().toString());
            } catch (IOException e) {
                log.error("Error occurred on accept()", e);
            }
        }
    }
}
