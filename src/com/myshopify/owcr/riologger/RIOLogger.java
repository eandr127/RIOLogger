package com.myshopify.owcr.riologger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class RIOLogger {

    public static final String VERSION = "1.2";

    private static RIOLogger logger;
    
    public static void main(String[] args) throws InterruptedException {
        if (isAppActive())
            return;

        // default value is returned if the preference does not exist
        String defaultValue = new Integer(Level.OFF.intValue()).toString();
        String propertyValue = LoggerLevelChooser.prefs.get(LoggerLevelChooser.PREF_NAME, defaultValue);
        Level level = Level.parse(propertyValue);
        
        LoggerLevelChooser.setUpTray(level);
        logger = new RIOLogger(ClientLogger.getPrintStream());
        RobotLoggerLevelSetter.setUpRobotLoggingLevelSetter();
        ClientLogger.setUpLogging(level);
        logger.startListening();
        while (!logger.cleanup) Thread.sleep(1000);
        logger.exit();
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

    public static RIOLogger getLogger() {
        return logger;
    }
    
    Thread listener;
    Thread transferer;
    Thread consoleExitListener;

    volatile DatagramSocket socket_hook = null;
    volatile boolean cleanup = false;
    private final PrintStream out;


    /**
     * The constructor.
     * 
     * @param out Where the data from the RIOLog should be printed
     */
    public RIOLogger(OutputStream out) {
        this(new PrintStream(out));
    }

    /**
     * The constructor.
     * 
     * @param out Where the data from the RIOLog should be printed
     */
    public RIOLogger(final PrintStream out) {
        this.out = out;
    }

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
                            out.print(drainToString(temp));
                        }
                    }, "Printer");
                }
            }
        }, "Riolog-Transfer");
        consoleExitListener = startDaemonThread(new Runnable() {
            @Override
            public void run() {
                try (Scanner sc = new Scanner(System.in)) {
                    while (!cleanup) {
                        if (sc.nextLine().trim().equalsIgnoreCase("exit")) {
                            cleanup = true;
                        }
                    }
                }
            }
        }, "Console Exit Listener");
    }

    void stopListening() {
        cleanup = true;
        if (socket_hook != null) {
            socket_hook.close();
        }
        listener.interrupt();
        transferer.interrupt();
    }

    private static final int PORT = 9999;

    private static boolean isAppActive() {
        ServerSocket s = null;
        try {
            // Bind to localhost adapter with a zero connection queue
            s = new ServerSocket(PORT, 0, InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
        } catch (BindException e) {
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        final ServerSocket finalS = s;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (finalS != null)
                    try {
                        finalS.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        });

        return false;
    }

    public void exit() {
        RobotLoggerLevelSetter.exit();
        stopListening();
        LoggerLevelChooser.exit();
        System.exit(0);
    }
    
}
