package xsdvi.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import xsdvi.XsdVi;

/**
 * @author Václav Slavìtínský
 *
 */
public final class LoggerHelper {

    /**
     *
     */
    public static final String LOGGER_NAME = XsdVi.class.getPackage().getName() + "." + XsdVi.class;

    /**
     *
     */
    public static final String DEFAULT_URI = "xsdvi.log";

    private static final Logger logger = Logger.getLogger(LOGGER_NAME);

    /**
     *
     */
    private LoggerHelper() {
        // no instances
    }

    /**
     * @param uri
     */
    public static void setupLogger(String uri) {
        try {
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s%6$s%n");
            FileHandler fileHandler = new FileHandler(uri, true);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
        }
    }

    /**
     *
     */
    public static void setupLogger() {
        setupLogger(DEFAULT_URI);
    }
}
