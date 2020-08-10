package neo4jSocket;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import neo4jDatabase.DBConnection;
import neo4jDatabase.DBUtility;
import neo4jDatabase.DBChecker;
import neo4jDatabase.DBUpdater;
import neo4jTraversal.MainTraversal;
import neo4jUtility.GeneralLogger;
import neo4jUtility.ThreadUtility;

public class SocketReader extends Thread
{

    private Socket socket;

    private InputStream inStream;

    private OutputStream outStream;

    private final static String mainSeparator = ",M,";

    private final static String singleSeparator = ",S,";

    private final static String partSeparator = ",P,";

    private final static String tgSeparator = ",T,";

    private Thread windowThread;

    SocketReader(Socket _socket)
    {
	socket = _socket;
	try
	{
	    inStream = socket.getInputStream();
	    outStream = socket.getOutputStream();
	}
	catch (IOException e)
	{
	    GeneralLogger.logError(e);
	    System.exit(0);
	}

    }

    @Override
    public void run()
    {
	boolean ready = false;
	boolean fullupdatemode = false;
	String neo4jpath = null;
	Set<Future<?>> futures = new HashSet<Future<?>>();
	while (true)
	{
	    try
	    {
		byte[] buffer = new byte[4096];
		GeneralLogger.log("Reading started!");
		inStream.read(buffer);
		final String[] separatedMessage = (new String(buffer, "UTF-8")).trim().split(mainSeparator);
		StringBuilder logMessage = new StringBuilder();
		for (int i = 0; i < separatedMessage.length; i++)
		{
		    logMessage.append(separatedMessage[i]);
		    if (i != separatedMessage.length)
		    {
			logMessage.append(" | ");
		    }
		}
		GeneralLogger.log(logMessage.toString());
		if (!ready)
		{
		    switch (separatedMessage[0])
		    {
			case "start neo4j path":
			    neo4jpath = separatedMessage[1];
			    final String dbpath = neo4jpath;
			    if (!ThreadUtility.getThreadUtility().noTasksRunning())
			    {
				ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
			    }
			    futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			    {
				@Override
				public void run()
				{
				    DBConnection.startDB(dbpath);
				}
			    }));
			    ready = true;
			    break;
			default:
			    GeneralLogger.log("Error Command while not ready: " + separatedMessage[0]);
			    System.exit(0);

		    }
		}
		else
		{
		    if (fullupdatemode)
		    {
			switch (separatedMessage[0])
			{
			    case "end full update":
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBConnection.shutdownBatchInserter();
					DBChecker.checkCFTModels();
					DBChecker.checkMCS();
				    }
				}));
				fullupdatemode = false;
				break;
			    case "add elements":
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBUpdater.addElementsBatch(separatedMessage, partSeparator, tgSeparator);
				    }
				}));
				break;
			    case "add connectors":
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBUpdater.addConnectorsBatch(separatedMessage, partSeparator);
				    }
				}));
				break;
			    default:
				GeneralLogger.log("Error Command while full update: " + separatedMessage[0]);
				System.exit(0);
			}
		    }
		    else
		    {
			switch (separatedMessage[0])
			{
			    case "start full update":
				fullupdatemode = true;
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBUtility.clearDB();
					DBConnection.startBatchInserter();
				    }
				}));
				break;
			    case "change neo4j path":
				ready = false;
				neo4jpath = null;
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBConnection.shutdownDB();
				    }
				}));
				break;
			    case "update":
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBUpdater.updateDB(separatedMessage, singleSeparator, partSeparator, tgSeparator);
				    }
				}));
				break;
			    case "exit":
				System.exit(0);
			    case "open analysis window":
				if (windowThread == null || !windowThread.isAlive())
				{
				    windowThread = new Thread(new Runnable()
				    {
					public void run()
					{
					    EventQueue.invokeLater(new Runnable()
					    {
						public void run()
						{
						    try
						    {
							new SocketAnalysisWindow();
						    }
						    catch (Exception e)
						    {
							GeneralLogger.logError(e);
						    }
						}
					    });
					}
				    });
				    windowThread.start();
				}
				break;
			    case "analyze and store results":
				if (!ThreadUtility.getThreadUtility().noTasksRunning())
				{
				    ThreadUtility.getThreadUtility().sleepUntilNoThreadsAreRunning();
				}
				futures.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					MainTraversal.performFullAnalysisAndStoreResults();
				    }
				}));
				break;
			    case "set developer mode":
				if (separatedMessage[1].equals("True"))
				{
				    SocketConnection.showSocketConnectionWindow(true);
				}
				else
				{
				    SocketConnection.showSocketConnectionWindow(false);
				}
				break;
			    default:
				GeneralLogger.log("Error Command while ready: " + separatedMessage[0]);
				System.exit(0);
			}
		    }
		}
		ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futures);
		futures.clear();
		String str = "received";
		byte[] data = str.getBytes("UTF-8");
		outStream.write(data);
		GeneralLogger.log("Task completed!");
	    }
	    catch (IOException e)
	    {
		GeneralLogger.logError(e);
		System.exit(0);
	    }
	}
    }

}
