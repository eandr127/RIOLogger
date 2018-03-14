package com.myshopify.owcr.riologger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

public class RIOLogger {

    public static final String VERSION = "1.4.0";

    private static RIOLogger logger;

    public static void main(String[] args) throws InterruptedException {
        // default value is returned if the preference does not exist
        String defaultValue = new Integer(Level.OFF.intValue()).toString();
        String propertyValue =
                        LoggerLevelChooser.prefs.get(LoggerLevelChooser.PREF_NAME, defaultValue);
        Level level = Level.parse(propertyValue);

        LoggerLevelChooser.setUpTray(level);
        ClientLogger.setUpLogging(level);
        logger = new RIOLogger(ClientLogger.getPrintStream());
        RobotLoggerLevelSetter.setUpRobotLoggingLevelSetter();
        while (!logger.cleanup)
            Thread.sleep(1000);
        logger.exit();
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

    public static void write(byte[] b) throws IOException {
        logger.out.write(b);
    }

    public void exit() {
        RobotLoggerLevelSetter.exit();
        LoggerLevelChooser.exit();
        System.exit(0);
    }

}
