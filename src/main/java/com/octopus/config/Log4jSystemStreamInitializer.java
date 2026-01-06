package com.octopus.config;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Automatically redirects System.out and System.err to Log4j on class load.
 * This ensures printStackTrace() and System.out/err calls are captured in log files.
 */
public class Log4jSystemStreamInitializer {
    private static final Logger log = LogManager.getLogger(Log4jSystemStreamInitializer.class);

    // Static initializer runs automatically when class is loaded
    static {
        Logger sysOutLogger = LogManager.getLogger("System.out");
        Logger sysErrLogger = LogManager.getLogger("System.err");

        System.setOut(new PrintStream(new LoggingOutputStream(sysOutLogger, false), true));
        System.setErr(new PrintStream(new LoggingOutputStream(sysErrLogger, true), true));

        log.info("System.out and System.err have been redirected to Log4j");
    }

    /**
     * Call this method to trigger the static initializer.
     */
    public static void initialize() {
        // Method body is empty - just calling it triggers the static block
    }

    /**
     * OutputStream that writes to a Log4j logger.
     */
    @RequiredArgsConstructor
    private static class LoggingOutputStream extends OutputStream {
        private final Logger logger;
        private final boolean isError;
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void write(int b) {
            if (b == '\n') flush();
            else if (b != '\r') buffer.append((char) b);
        }

        @Override
        public void flush() {
            if (!buffer.isEmpty()) {
                String message = buffer.toString();
                if (isError) logger.error(message);
                else logger.info(message);
                buffer.setLength(0);
            }
        }
    }
}
