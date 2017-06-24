package org.simple;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.Thread.UncaughtExceptionHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by OutOfMemory on 2017/06/23.
 */
public class SimpleServer {
    private static Logger log = LogManager.getLogger(SimpleServer.class);
    private final int NUM_WORKER_THREADS = 10;
    private final WorkerThread[] workerThreads = new WorkerThread[NUM_WORKER_THREADS];

    public SimpleServer() {
       for (int i = 0; i < NUM_WORKER_THREADS; i++) {
           workerThreads[i] = new WorkerThread(String.format("WorkerThread-%d", i));
           workerThreads[i].setUncaughtExceptionHandler(new WorkerUncaughtExceptionHandler());
       }
    }

    private void processRequests(Socket socket) {
        log.info("Connection accepted {}", socket.getInetAddress().toString());
        int index = (socket.hashCode() & 0x7FFFFFFF) % NUM_WORKER_THREADS;
        workerThreads[index].putTask(socket);
    }

    public void start() throws IOException {
        log.info("SimpleHttpServer starting...");

        Thread.currentThread().setName("ServerMainThread");

        for (int i = 0; i < NUM_WORKER_THREADS; i++) {
            workerThreads[i].start();
        }

        log.info("Create server socket and binding");
        final ServerSocket serverSocket = new ServerSocket(8080);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            processRequests(clientSocket);
        }
    }

    public void stop() {
        //running = false; // Signal worker threads to stop
        for (int i = 0; i < NUM_WORKER_THREADS; i++) {
            try {
                log.info("Waiting worker thread {} to stop", workerThreads[i].getName());
                workerThreads[i].shutdown();
                workerThreads[i].join();
            } catch (InterruptedException e) {
                log.error("Failed to shutdown gracefully", e);
            }
        }
    }

    /**
     *  设置线程池里线程的未捕获异常handler，未知异常，logger出错误，然后让程序退出。
     * @author OutOfMemory
     */
    public static class WorkerUncaughtExceptionHandler implements UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            log.error("[FATAL] Uncaught exception occurred, thread: {}", t.getName(), e);
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        SimpleServer server = new SimpleServer();

        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to create server socket", e);
            throw new RuntimeException(e);
        }

        server.stop();
    }
}
