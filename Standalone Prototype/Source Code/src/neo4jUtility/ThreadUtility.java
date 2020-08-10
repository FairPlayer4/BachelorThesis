package neo4jUtility;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the multithreading of the prototype.  
 * @author Kevin Bartik
 *
 */
public final class ThreadUtility
{
    /**
     * The instance of this class.
     */
    private static ThreadUtility threadUtility;

    /**
     * The number of available processors in the Runtime minus 1 for the main thread.
     */
    private final int cpus;

    /**
     * The ThreadPoolExecutor that is used for multithreading in the application.
     */
    private ThreadPoolExecutor availableThreads;

    /**
     * Boolean that indicates if multithreading is used in the application.
     */
    private boolean multiThreading = false;

    /**
     * Initializes this class so the ThreadPoolExecutor can be used.
     */
    private ThreadUtility()
    {
	cpus = Runtime.getRuntime().availableProcessors();
	availableThreads = new ThreadPoolExecutor(cpus, cpus * 4, 0, TimeUnit.NANOSECONDS, new SynchronousQueue<Runnable>());
	availableThreads.prestartAllCoreThreads();
	threadUtility = this;
    }

    /**
     * Returns the instance of this class and creates an instance if none was created.
     * 
     * @return The instance of this class.
     */
    public static ThreadUtility getThreadUtility()
    {
	if (threadUtility == null)
	{
	    new ThreadUtility();
	}
	return threadUtility;
    }

    /**
     * Returns true if no threads are active in the ThreadPoolExecutor. Otherwise false.
     * @return true if no threads are active in the ThreadPoolExecutor. Otherwise false.
     */
    public boolean noTasksRunning()
    {
	return availableThreads.getActiveCount() == 0;
    }

    /**
     * Returns true if a thread in the ThreadPoolExecutor is available for use. Otherwise false. Can be wrong and the ThreadPoolExecutor provides extra threads
     * to prevent errors.
     * 
     * @return true if a thread in the ThreadPoolExecutor is available for use. Otherwise false.
     */
    public boolean isThreadAvailable()
    {
	return availableThreads.getActiveCount() < availableThreads.getCorePoolSize();
    }

    /**
     * Submits a task to the ThreadPoolExecutor.
     * 
     * @param task
     *            The task that is submitted.
     */
    public Future<?> submitTask(Runnable task)
    {
	return availableThreads.submit(task);
    }

    /**
     * Waits for all task in a future set to finish.
     * @param futureSet The set of futures.
     */
    public void waitForSubmittedTasksToFinish(Set<Future<?>> futureSet)
    {
	addThread();
	HashSet<Future<?>> currentFutureSet = new HashSet<Future<?>>();
	while (currentFutureSet.size() < futureSet.size())
	{
	    currentFutureSet = new HashSet<Future<?>>(futureSet);
	    for (Future<?> future : currentFutureSet)
	    {
		try
		{
		    future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
		    PrintUtility.printError(e.toString());
		    GeneralLogger.logError(e);
		    e.printStackTrace();
		}
	    }
	}
	removeThread();
    }

    /**
     * Sets the current Thread to sleep until no threads are active in the ThreadPoolExecutor.
     */
    public void sleepUntilNoThreadsAreRunning()
    {
	while (!noTasksRunning())
	{
	    try
	    {
		Thread.sleep(GeneralUtility.waitTime2);
	    }
	    catch (InterruptedException e)
	    {
		// Not an issue since threads are recreated once the ThreadPoolExecuter was shutdown.
	    }
	}
    }

    /**
     * Returns true if multithreading is active. Otherwise false.
     * @return true if multithreading is active. Otherwise false.
     */
    public boolean isMultiThreading()
    {
	return multiThreading;
    }

    /**
     * Sets if multithreading is used or not. Modifies the ThreadPoolExecutor accordingly.
     * 
     * @param _multiThreading The new value for multithreading.
     */
    public void setMultiThreading(boolean _multiThreading)
    {
	multiThreading = _multiThreading;
	if (multiThreading)
	{
	    availableThreads.setMaximumPoolSize(cpus * 4);
	    availableThreads.setCorePoolSize(cpus);
	}
	else
	{
	    availableThreads.setCorePoolSize(1);
	    availableThreads.setMaximumPoolSize(4);
	}
    }

    /**
     * Adds a core thread to the ThreadPoolExecutor.
     * Is used when one thread is done and waiting. 
     */
    private synchronized void addThread()
    {
	if (multiThreading)
	{
	    availableThreads.setMaximumPoolSize(availableThreads.getMaximumPoolSize() + 1);
	    availableThreads.setCorePoolSize(availableThreads.getCorePoolSize() + 1);
	}
    }

    /**
     * Removes a core thread from the ThreadPoolExecutor.
     * Is used when a thread finishes waiting.
     */
    private synchronized void removeThread()
    {
	if (multiThreading && availableThreads.getCorePoolSize() > cpus && availableThreads.getMaximumPoolSize() > 4 * cpus)
	{
	    availableThreads.setCorePoolSize(availableThreads.getCorePoolSize() - 1);
	    availableThreads.setMaximumPoolSize(availableThreads.getMaximumPoolSize() - 1);
	}
    }

}
