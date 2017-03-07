package com.myshopify.owcr.riologger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



public class RIOLogger {

    public static void main(String[] args) {
        RIOLogger logger = new RIOLogger();
        logger.startListening();
        while (!logger.cleanup);
        logger.stopListening();
    }

    public static byte[] getPacket(DatagramSocket socket, DatagramPacket buf) {
        try {
            socket.receive(buf);
        } catch (IOException e) {
            return null;
        }
        byte[] ret = new byte[buf.getLength()];
        System.arraycopy(buf.getData(), 0, ret, 0, ret.length);
        return ret;
    }

    public static DatagramSocket makeRecvSocket() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(6666));
        } catch (SocketException e) {
            e.printStackTrace();
            socket.close();
            return null;
        }
        return socket;
    }

    public static Thread startDaemonThread(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        t.start();
        return t;
    }

    Thread listener;
    Thread transferer;

    volatile DatagramSocket socket_hook = null;
    volatile boolean cleanup = false;

    /**
     * The constructor.
     */
    public RIOLogger() {}

    public static String drainToString(ArrayList<byte[]> arr) {
        int netlength = 0;
        for (byte[] b : arr) {
            netlength += b.length;
        }

        byte[] sum = new byte[netlength];
        int mark = 0;
        for (int i = 0; i < arr.size(); i++) {
            byte[] b = arr.get(i);
            System.arraycopy(b, 0, sum, mark, b.length);
            arr.set(i, null);
            mark += b.length;
        }
        arr.clear();
        return new String(sum);

    }

    void startListening() {
        final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        listener = startDaemonThread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = makeRecvSocket();
                if (socket == null)
                    return;
                socket_hook = socket;
                byte[] buf = new byte[4096];
                DatagramPacket datagram = new DatagramPacket(buf, buf.length);
                while (!Thread.interrupted()) {
                    byte[] s = getPacket(socket, datagram);
                    if (s != null) {
                        try {
                            queue.put(s);
                        } catch (InterruptedException e) {
                            socket.close();
                            return;
                        }
                    }
                }
                socket.close();
            }
        }, "Riolog-Listener");
        transferer = startDaemonThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<byte[]> temp = new ArrayList<>();
                while (!cleanup) {
                    try {
                        temp.add(queue.take());
                    } catch (InterruptedException e) {
                        if (cleanup) {
                            return;
                        }
                    }
                    queue.drainTo(temp);
                    startDaemonThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.print(drainToString(temp));
                        }
                    }, "Printer");
                }
            }
        }, "Riolog-Transfer");
    }

    void stopListening() {
        cleanup = true;
        if (socket_hook != null) {
            socket_hook.close();
        }
        listener.interrupt();
        transferer.interrupt();
    }
}
