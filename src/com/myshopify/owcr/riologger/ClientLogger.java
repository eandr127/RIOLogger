package com.myshopify.owcr.riologger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ClientLogger {

    public static final String LOGGER_NAME = "RIO-Logger";

    private static final Logger logger = Logger.getLogger(LOGGER_NAME);

    private static String queue = "";

    private static PrintStream printStream;

    public static final File LOG_FILE = new File("logs/latest.log");

    private static Thread shutdownThread;
    
    private static final Formatter FORMATTER = new Formatter() {

        @Override
        public String format(LogRecord record) {
            // String not formatted
            if(record.getLevel() == Level.OFF) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                Date dt = new Date(record.getMillis());
                String S = sdf.format(dt);
                
                return "INFO " + S + " " + record.getMessage();
            }
            else {
                return record.getMessage();
            }
        }

    };

    private static FileHandler fh;
    private static ConsoleHandler ch;

    /**
     * Configures the logger
     */
    public static void setUpLogging() {
        if (!new File("logs").exists())
            new File("logs").mkdir();

        try {
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            fh = new FileHandler(LOG_FILE.getAbsolutePath(), true);
            ch = new EConsoleHandler();

            ch.setFormatter(FORMATTER);
            fh.setFormatter(FORMATTER);

            logger.addHandler(fh);
            logger.addHandler(ch);

            ch.setLevel(Level.ALL);
            fh.setLevel(LoggerLevelChooser.getLogLevel());
            
            shutdownThread = new Thread() {
                @Override
                public void run() {
                    fh.flush();
                    fh.close();

                    ch.flush();
                    ch.close();

                    cleanupLogFilename();
                }
            };
            
            Runtime.getRuntime().addShutdownHook(shutdownThread);

            printStream = createLoggingProxy(System.out);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static PrintStream getPrintStream() {
        return printStream;
    }

    public static void setLevel(Level level) {
        System.out.println(level.getName());
        fh.setLevel(level);
    }

    private static void cleanupLogFilename() {

        if (LOG_FILE.length() == 0) {
            LOG_FILE.delete();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-");
        Date dt = new Date();
        String S = sdf.format(dt);

        File datedFile = null;

        int i = 1;
        boolean exists = true;;
        while (exists) {
            datedFile = new File("logs/" + S + i + ".log");

            if (datedFile.exists()) {
                i++;
            } else {
                exists = false;
            }
        }

        // Rename file (or directory)
        boolean success = LOG_FILE.renameTo(datedFile);
        if (!success) {
            System.err.println("Unknown Error");
        }
    }

    private static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            @Override
            public void write(byte[] b) throws IOException {
                String string = new String(b);
                if (!string.isEmpty())
                    printFullLines(string);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String string = new String(b, off, len);
                if (!string.isEmpty())
                    printFullLines(string);
            }

            @Override
            public void write(int b) {
                String string = String.valueOf((char) b);
                if (!string.isEmpty())
                    printFullLines(string);
            }
        };
    }

    private static Level getLogLevel(String outputLine) {
        try {
            return Level.parse(outputLine.split(" ")[0]);
        } catch (IllegalArgumentException e) {
            // Tell formatter to do formatting on its own
            return Level.OFF;
        }
    }

    private static void printFullLines(String s) {
        s = queue + s;

        String[] lines = s.split("(?<=\\R)");
        for (String line : lines) {
            if (line.matches(".*?\\R")) {
                logger.log(getLogLevel(line), line);
                queue = "";
            } else {
                queue = line;
            }
        }

    }

    public static void restart() {
        shutdownThread.run();
        setUpLogging();
    }
}
