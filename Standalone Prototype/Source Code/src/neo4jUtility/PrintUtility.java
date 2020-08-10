package neo4jUtility;

import javax.swing.JOptionPane;

import neo4jTraversal.MainTraversal;

/**
 * Utility that provides methods to print on the console or show Dialogs to inform the user.
 * @author Kevin Bartik
 *
 */
public final class PrintUtility
{

    private PrintUtility() {
	
    }
    
    /**
     * Stops the application and shows some information about the error that occurred. The application may not be in a valid state after such an error.
     * 
     * @param text
     *            The error message.
     */
    public static void printError(String text, String additionalInfo)
    {
	JOptionPane.showMessageDialog(null, "Error: " + text + "\nAdditional Error Information: " + additionalInfo, "Error Message", JOptionPane.ERROR_MESSAGE);
	printInfo(text, "Error");
	printInfo(additionalInfo, "Additional Error Information");
	MainTraversal.errorOccured();
    }

    /**
     * Stops the application and shows some information about the error that occurred. The application may not be in a valid state after such an error.
     * 
     * @param text
     *            The error message.
     */
    public static void printError(String text)
    {
	JOptionPane.showMessageDialog(null, "Error: " + text, "Error Message", JOptionPane.ERROR_MESSAGE);
	printInfo(text, "Error");
	MainTraversal.errorOccured();
    }

    /**
     * Prints information on the Console. Usually just some information that shows what the application is currently doing.
     * 
     * @param text
     *            The information text.
     */
    public static void printInfo(String text)
    {
	if (SettingsUtility.isPrintInfo())
	{
	    System.out.println("Information: " + text);
	}
    }
    
    /**
     * Prints information on the Console. Usually just some information that shows what the application is currently doing.
     * 
     * @param text
     *            The information text.
     */
    private static void printInfo(String text, String tag)
    {
	if (SettingsUtility.isPrintInfo())
	{
	    System.out.println(tag + ": " + text);
	}
    }

    /**
     * Prints warnings on the Console.
     * 
     * @param text
     *            The information text.
     */
    public static void printWarning(String text)
    {
	JOptionPane.showMessageDialog(null,"Warning: " + text, "Warning Message", JOptionPane.WARNING_MESSAGE);
	printInfo(text, "Warning");
    }

    /**
     * Prints results of a given task (e.g. analysis) on the Console.
     * 
     * @param formattedText
     */
    public static void printResults(String formattedText)
    {
	System.out.println(formattedText);
    }
}
