package com.myshopify.owcr.riologger;

import java.io.PrintStream;
import java.util.logging.ConsoleHandler;

public class EConsoleHandler extends ConsoleHandler {

    public EConsoleHandler(PrintStream out) {
        setOutputStream(out);
    }
}
