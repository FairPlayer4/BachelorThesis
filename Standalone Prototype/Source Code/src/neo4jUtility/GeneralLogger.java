package neo4jUtility;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Used to log general information and errors of the application. Only used when the application is running over SocketConnection.
 * 
 * @author Kevin Bartik
 *
 */
public final class GeneralLogger
{

    /**
     * The String file path of the log files.
     */
    private static String location = "";

    /**
     * The Logger provided by java.util.logging.
     */
    private static Logger logger;

    /**
     * Constructor of the GeneralLogger. Starts a new Logger with the provided path.
     * 
     * @param path
     *            The file path to where the log file should be saved.
     */
    private GeneralLogger(String path)
    {
	location = path;
	logger = Logger.getLogger("JavaLog");
	FileHandler fh;
	try
	{
	    fh = new FileHandler(path + "JavaLogger.log");
	    logger.addHandler(fh);
	    SimpleFormatter formatter = new SimpleFormatter();
	    fh.setFormatter(formatter);
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

    /**
     * Initializes a new GeneralLogger if the path changed or if no GeneralLogger was previously initialized.
     * 
     * @param path
     *            The file path to where the log file should be saved.
     */
    public static void startLogger(String path)
    {
	if (!location.equals(path))
	{
	    new GeneralLogger(path);
	}
    }

    /**
     * Writes a message to the log.
     * 
     * @param message
     *            The message that shall be logged.
     */
    public static void log(String message)
    {
	if (logger != null)
	{
	    logger.info(message);
	}
    }

    /**
     * Logs errors of the application.
     * 
     * @param e
     *            An Exception.
     */
    public static void logError(Exception e)
    {
	if (logger != null)
	{
	    int stack = e.getStackTrace().length;
	    if (stack < 20 && stack > 0)
	    {
		StringBuilder str = new StringBuilder();
		str.append("   StackTrace (all) for " + e.getMessage() + "\n");
		for (int i = 0; i < stack; i++)
		{
		    str.append("      " + e.getStackTrace()[i] + "\n");
		}
		log(str.toString());
	    }
	    else
	    {
		if (stack > 20)
		{
		    StringBuilder str = new StringBuilder();
		    str.append("   StackTrace (first 5) for " + e.getMessage() + "\n");
		    for (int i = 0; i < 5; i++)
		    {
			str.append("      " + e.getStackTrace()[i] + "\n");
		    }
		    str.append("   StackTrace (last 15) for " + e.getMessage() + "\n");
		    for (int i = stack - 16; i < stack; i++)
		    {
			str.append("      " + e.getStackTrace()[i] + "\n");
		    }
		    log(str.toString());
		}
		else
		{
		    log("   " + e.getMessage());
		}
	    }
	}
    }
}
