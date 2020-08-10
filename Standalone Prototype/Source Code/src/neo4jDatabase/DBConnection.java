package neo4jDatabase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import neo4jUtility.PrintUtility;
import neo4jUtility.SettingsUtility;

/**
 * This class manages the direct connection to the database.
 * It provides the GraphDatabaseService to all other classes.
 * @author Kevin Bartik
 *
 */
public final class DBConnection
{

    /**
     * The current path of the Neo4j database.
     * Used to reconnect when the batch inserter is used.
     */
    private static String currentDBPath = "";

    /**
     * The instance of this class.
     * There can only be one instance of this class.
     */
    private static DBConnection dbinstance;

    /**
     * The shutdownhook for the database.
     * This ensures that the database does not get corrupted when the prototype is closed.
     */
    private static Thread shutdownhook;

    /**
     * The GraphDatabaseService.
     * Used to access the database.
     */
    private static GraphDatabaseService graphDB = null;

    /**
     * The BatchInserter.
     * Used to perform large insertions to the database with high performance.
     */
    private static BatchInserter batch = null;

    /**
     * The batch properties for the batch inserter.
     */
    static Map<String, Object> batchproperties = new HashMap<String, Object>();

    /**
     * The nodes that need to be checked.
     */
    static HashSet<Node> uncheckedNodes = new HashSet<Node>();

    /**
     * True if the database was checked.
     */
    static boolean dbChecked = false;

    /**
     * True if the CFTs in the database were checked for cycles.
     */
    static boolean cftCycleChecked = false;

    /**
     * True if the MCSs in database were checked for errors. 
     */
    static boolean mcsChecked = false;
    
    /**
     * Set of error nodes.
     */
    static HashSet<Node> errorNodes = new HashSet<Node>();

    /**
     * Starts a new GraphDatabaseService at the provided path.
     * @param dbpath The file path of the database.
     * @throws IOException
     */
    private DBConnection(String dbpath) throws IOException
    {
	uncheckedNodes = new HashSet<Node>();
	dbChecked = false;
	cftCycleChecked = false;
	mcsChecked = false;
	errorNodes = new HashSet<Node>();
	if (graphDB != null)
	{
	    shutdownDB();
	}
	if (batch != null)
	{
	    batch.shutdown();
	    batch = null;
	}
	if (!dbpath.equals(currentDBPath))
	{
	    batchproperties = new HashMap<String, Object>();
	    currentDBPath = dbpath;
	}
	final File dbpathfile = new File(currentDBPath);
	graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(dbpathfile);
	if (shutdownhook != null)
	{
	    Runtime.getRuntime().removeShutdownHook(shutdownhook);
	}
	shutdownhook = new Thread()
	{
	    @Override
	    public void run()
	    {
		if (batch != null)
		{
		    batch.shutdown();
		    batch = null;
		}
		if (graphDB != null)
		{
		    if (graphDB.isAvailable(0))
		    {
			graphDB.shutdown();
		    }
		    graphDB = null;
		}
	    }
	};
	Runtime.getRuntime().addShutdownHook(shutdownhook);
    }

    /**
     * Starts the database. If the database is running and the path is equal then nothing happens.
     * @param dbpath The file path of the database that is started.
     */
    public static synchronized void startDB(String dbpath)
    {
	if ((dbpath.length() != 0 && new File(dbpath).isDirectory()))
	{
	    if (DBConnection.dbinstance == null || !currentDBPath.equals(dbpath) || graphDB == null)
	    {
		PrintUtility.printInfo("Connecting to Neo4j database...");
		try
		{
		    DBConnection.dbinstance = new DBConnection(dbpath);
		    SettingsUtility.resetTraversal();
		}
		catch (IOException e)
		{
		    e.printStackTrace();
		}
		PrintUtility.printInfo("Connection to Neo4j database successful!");
	    }
	}
	else
	{
	    PrintUtility.printError("The Neo4j Database cannot be started with that path!", "Path: " + dbpath);
	}
    }

    /**
     * Returns the current file path of the database.
     * @return The current file path of the database.
     */
    public static String getCurrentDBPath()
    {
	return currentDBPath;
    }

    /**
     * Shuts down the database.
     */
    public static void shutdownDB()
    {
	PrintUtility.printInfo("Disconnecting from Neo4j database...");
	getGraphDB().shutdown();
	graphDB = null;
	PrintUtility.printInfo("Disconnection from Neo4j database successful!");
    }

    /**
     * Returns the currently active GraphDatabaseService.
     * @return The GraphDatabaseService.
     */
    public static GraphDatabaseService getGraphDB()
    {
	if (graphDB != null && batch == null)
	{
	    return graphDB;
	}
	PrintUtility.printError("Internal Error! Neo4j database was accessed while it was unavailable!");
	return null;
    }

    /**
     * Return a BatchInserter if the BatchInserter was started.
     * @return The BatchInserter of the database.
     */
    static BatchInserter getBatchInserter()
    {
	if (graphDB == null && batch != null)
	{
	    return batch;
	}
	PrintUtility.printError("Internal Error! A batch inserter was accessed while it was unavailable!");
	return null;
    }

    /**
     * Shuts down the database and starts a BatchInserter at the current file path of the database.
     */
    public static void startBatchInserter()
    {
	shutdownDB();
	PrintUtility.printInfo("Starting a batch inserter for the Neo4j database...");
	File file = new File(currentDBPath);
	try
	{
	    batch = BatchInserters.inserter(file);
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	PrintUtility.printInfo("Batch inserter for Neo4j database successfully started!");
    }

    /**
     * Starts a database at the given file path.
     * Clears that database and shuts it down.
     * Starts a BatchInserter at the given file path.
     * @param dbpath The file path to the database where the BatchInserter shall be started.
     */
    public static void startBatchInserter(String dbpath)
    {
	String saveDBPath = new String(currentDBPath);
	startDB(dbpath);
	DBUtility.clearDB();
	startDB(saveDBPath);
	shutdownDB();
	PrintUtility.printInfo("Starting a batch inserter for generating CFT...");
	File file = new File(dbpath);
	try
	{
	    batch = BatchInserters.inserter(file);
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	PrintUtility.printInfo("Batch inserter for generating CFT successfully started!");
    }

    /**
     * Shuts down the BatchInserter.
     */
    public static void shutdownBatchInserter()
    {
	PrintUtility.printInfo("Shutting down batch inserter...");
	getBatchInserter().shutdown();
	batch = null;
	PrintUtility.printInfo("Batch inserter was successfully shut down...");
	startDB(currentDBPath);
    }

    /**
     * Checks if the GraphDatabaseService is running.
     * @return true if the GraphDatabaseService is running. Otherwise false.
     */
    public static boolean isReady()
    {
	return graphDB != null;
    }

}
