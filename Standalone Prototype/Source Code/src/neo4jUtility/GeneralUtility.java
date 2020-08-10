package neo4jUtility;

import java.io.File;

/**
 * Provides some general utility methods.
 * @author Kevin Bartik
 *
 */
public final class GeneralUtility
{

    private GeneralUtility()
    {

    }

    /**
     * Deletes a directory and all files inside it.
     * 
     * @param file
     *            The directory that shall be deleted.
     */
    public static void deleteDir(File file)
    {
	File[] contents = file.listFiles();
	if (contents != null)
	{
	    for (File f : contents)
	    {
		deleteDir(f);
	    }
	}
	file.delete();
    }

    /**
     * Convenience variable to control the behavior of the applications wait times.
     */
    public static final int waitTime1 = 1;

    /**
     * Convenience variable to control the behavior of the applications wait times.
     */
    public static final int waitTime2 = 10;

    /**
     * Convenience variable to control the behavior of the applications wait times.
     */
    public static final int waitTime3 = 50;

    /**
     * Convenience variable to control the behavior of the applications wait times.
     */
    public static final int waitTime4 = 100;
    
    public static int setSize(int size) {
	return (int) Math.round(size * 1.4);
    }

}
