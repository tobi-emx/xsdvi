package xsdvi.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;

/**
 * @author Václav Slavìtínský
 *
 */
public class XsdErrorHandler implements DOMErrorHandler {

    private static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    /* (non-Javadoc)
	 * @see org.w3c.dom.DOMErrorHandler#handleError(org.w3c.dom.DOMError)
     */
    @Override
    public boolean handleError(DOMError error) {
        int severity = error.getSeverity();
        switch (severity) {
            case DOMError.SEVERITY_FATAL_ERROR:
                logger.log(Level.SEVERE, "[xs-fatal-error]: {0}", errorMessage(error));
                System.exit(1);
            case DOMError.SEVERITY_ERROR:
                logger.log(Level.SEVERE, "[xs-error]: {0}", errorMessage(error));
                break;
            case DOMError.SEVERITY_WARNING:
                logger.log(Level.WARNING, "[xs-warning]: {0}", errorMessage(error));
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * @param error
     * @return
     */
    private String errorMessage(DOMError error) {
        return error.getMessage() + " (line: " + error.getLocation().getLineNumber() + ")";
    }
}
