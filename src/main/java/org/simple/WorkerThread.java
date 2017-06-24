package org.simple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by OutOfMemory on 2017/06/24.
 */
public class WorkerThread extends Thread {
    private static final Logger log = LogManager.getLogger(WorkerThread.class);

    private volatile boolean stop = false;

    protected BlockingQueue<Socket> taskQueue = new LinkedBlockingDeque<Socket>(50);

    private static String responseBody = "<html lang=\"zh-cn\">\n" +
            "<head>\n" +
            "<meta charset=\"utf-8\"/>\n" +
            "<title>HTTP协议详解</title>\n" +
            "<body>\n" +
            "<p><span>这是一个测试页面——王亮</span></p>" +
            "</body>\n" +
            "</html>";

    private static String response = String.format("HTTP/1.1 200 OK\r\n"+
            "SimpleServer: SimpleServer/1.0\n" +
            //"Connection: Keep-Alive\n" +
            "Connection: close\n" +
            "Content-Length: %d\n" +
            "Content-Type: text/html\r\n\r\n%s", responseBody.getBytes().length, responseBody);

    public WorkerThread(String threadName) {
        super.setName(threadName);
    }

    public void putTask(Socket socket) {
        try {
            taskQueue.put(socket);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void serveClient(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        log.debug("Begin to read socket...");
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int offset = 0;

        while (true) {
            int count = inputStream.read(buffer, offset, BUFFER_SIZE-offset);
            if (count == -1) {
                log.info("Connection closed");
                socket.close();
                return;
            }
            offset += count;

            // FIXME
            if (inputStream.available() == 0) {
                break;
            }
        }

        log.info("Succeed to read {} bytes: \n{}", offset, new String(buffer, 0, offset));

        outputStream.write(response.getBytes());
        outputStream.flush();
        socket.close();
    }

    @Override
    public void run() {
        // 未设置stop标志，或者任务队列中还有剩余任务，则继续循环
        while (!stop || !taskQueue.isEmpty()) {
            Socket socket = null;
            try {
                socket = taskQueue.poll(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (socket == null) {
                continue;
            }

            log.info("got a connection, serve client...");
            try {
                serveClient(socket);
            } catch (IOException e) {
                log.error("Exception occurred on read(), ", e);
            }
        }

        log.info("Stop worker thread {}", Thread.currentThread().getName());
    }

    public void shutdown() {
        stop = true;
    }
}
