package org.simple;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by OutOfMemory on 2017/06/23.
 */
public class Server {
    private static Logger log = LogManager.getLogger(Server.class);

    public static class Task implements Runnable {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public Task(Socket socket) throws IOException {
            this.socket = socket;
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }

        public void run() {
            log.info("Begin to read socket...");

            byte[] buffer = new byte[1024];

            while (!socket.isClosed()) {
                try {
                    int count = inputStream.read(buffer);
                    log.info("Succeed to read {} bytes: {}", count, new String(buffer, 0, count));
                    outputStream.write("SimpleHttpServer\n".getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    log.error("Exception occurred on read(), ", e);
                }
            }
        }
    }

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
                new Task(clientSocket).run();
            } catch (IOException e) {
                log.error("Error occurred on accept()", e);
            }
        }
    }
}
