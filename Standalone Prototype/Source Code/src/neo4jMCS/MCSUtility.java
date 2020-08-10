package neo4jMCS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Node;

import neo4jUtility.GeneralUtility;
import neo4jUtility.ThreadUtility;

/**
 * This class provides utility methods for MCS and MCSNode.
 * 
 * @author Kevin Bartik
 *
 */
public final class MCSUtility
{

    /**
     * Variable that determines if messages about MCS should be sent to the GUI.<br>
     * These messages inform the user if a MCS is always true or always false which should rarely happen.<br>
     * Often errors in the CFT Model can cause MCS that are always true or always false.<br>
     * Is turned false if methods are used that produce and then discard MCS that are always true or always false.<br>
     */
    private static boolean printMessage = true;
    
    private MCSUtility() {
	
    }

    /**
     * Returns {@code printMessage}.<br>
     * 
     * @return {@code printMessage}.
     */
    static boolean isPrintMessage()
    {
	return printMessage;
    }

    /**
     * Copies a MCSNode HashSet and returns the copied MCSNode HashSet.<br>
     * 
     * @param mcsNodeSet
     *            The MCSNode HashSet that is copied.
     * @return A copy of the original MCSNode HashSet.
     */
    static HashSet<MCSNode> copyMCSNodeSet(HashSet<MCSNode> mcsNodeSet)
    {
	HashSet<MCSNode> returnSet = new HashSet<MCSNode>();
	for (MCSNode mcsNode : mcsNodeSet)
	{
	    returnSet.add(new MCSNode(mcsNode));
	}
	return returnSet;
    }
    
    /**
     * Adds the MCS to each MCS of a MCS HashSet.<br>
     * Returns a new MCS HashSet where every MCS contains the specified MCS.<br>
     * 
     * @param mcsSet
     *            The MCS HashSet that is combined with the specified MCS.
     * @param mcs
     *            The specified MCS that is added to a MCS of the MCS HashSet.
     * @return A new MCS HashSet where every MCS contains the specified MCS.
     */
    static HashSet<MCS> combineMCSSetWithMCS(HashSet<MCS> mcsSet, MCS mcs)
    {
	HashSet<MCS> newMCSSet = new HashSet<MCS>(GeneralUtility.setSize(mcsSet.size()));
	for (MCS _mcs : mcsSet)
	{
	    MCS newMCS = new MCS(_mcs);
	    newMCS.addMCS(new MCS(mcs));
	    if (!newMCS.isAlwaysFalse())
	    {
		newMCSSet.add(newMCS);
	    }
	}
	return newMCSSet;
    }

    /**
     * Returns a MCSNode HashSet that contains every Node of the specified Node HashSet as a MCSNode that is not negated.<br>
     * 
     * @param nodeSet
     *            The specified Node HashSet.
     * @return A MCSNode HashSet that contains every Node of the specified Node HashSet as a MCSNode that is not negated.
     */
    public static HashSet<MCSNode> getMCSNodeSet(HashSet<Node> nodeSet)
    {
	HashSet<MCSNode> mcsNodeSet = new HashSet<MCSNode>();
	for (Node node : nodeSet)
	{
	    mcsNodeSet.add(new MCSNode(node, false));
	}
	return mcsNodeSet;
    }

    /**
     * Returns a MCSNode HashSet that contains every Node of the specified Node HashSet as a negated MCSNode.<br>
     * 
     * @param nodeSet
     *            The specified Node HashSet.
     * @return A MCSNode HashSet that contains every Node of the specified Node HashSet as a negated MCSNode.
     */
    public static HashSet<MCSNode> getNegatedMCSNodeSet(HashSet<Node> nodeSet)
    {
	HashSet<MCSNode> mcsNodeSet = new HashSet<MCSNode>();
	for (Node node : nodeSet)
	{
	    mcsNodeSet.add(new MCSNode(node, true));
	}
	return mcsNodeSet;
    }

    /**
     * Combines all merged MCS sets of all MCS in the HashSet.<br>
     * 
     * @param mcsSet
     *            The MCS HashSet.
     * @return A MCS HashSet that does not have merged MCS.
     */
    public static HashSet<MCS> combineAllMCSInMCSSet(HashSet<MCS> mcsSet)
    {
	HashSet<MCS> fullyCombinedMCSSet = null;
	if (ThreadUtility.getThreadUtility().isMultiThreading()) // Multithreading
	{
	    Set<MCS> combinedMCSSet = ConcurrentHashMap.newKeySet(GeneralUtility.setSize(mcsSet.size()));
	    Set<Future<?>> futureSet = new HashSet<Future<?>>();
	    for (MCS mcs : mcsSet)
	    {
		if (!mcs.hasMergedMCSSets())
		{
		    combinedMCSSet.add(new MCS(mcs));
		}
		else
		{
		    if (mcsSet.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
		    {
			futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				combinedMCSSet.addAll(mcs.returnCombinedMCSSet());
			    }
			}));
		    }
		    else
		    {
			combinedMCSSet.addAll(mcs.returnCombinedMCSSet());
		    }
		}
	    }
	    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
	    fullyCombinedMCSSet = new HashSet<MCS>(combinedMCSSet);
	}
	else // No Multithreading
	{
	    HashSet<MCS> combinedMCSSet = new HashSet<MCS>(GeneralUtility.setSize(mcsSet.size()));
	    for (MCS mcs : mcsSet)
	    {
		if (!mcs.hasMergedMCSSets())
		{
		    combinedMCSSet.add(new MCS(mcs));
		}
		else
		{
		    combinedMCSSet.addAll(mcs.returnCombinedMCSSet());
		}
	    }
	    fullyCombinedMCSSet = combinedMCSSet;
	}
	return fullyCombinedMCSSet;
    }

    /**
     * Removes all MCS that are contain other MCS and are not equal to them.<br>
     * The returned MCS HashSet contains only MCS that are minimal.<br>
     * 
     * @param mcsSet
     *            A MCS HashSet
     * @return A MCS HashSet that contains only minimal MCS.
     */
    public static HashSet<MCS> minimizeMCSSet(HashSet<MCS> mcsSet)
    {
	if (!ThreadUtility.getThreadUtility().isMultiThreading()) // No Multithreading
	{
	    HashSet<MCS> minimalMCSSet = copyMCSSet(mcsSet);
	    for (MCS mcs1 : mcsSet)
		{
		    for (MCS mcs2 : mcsSet)
		    {
			if (!mcs1.equals(mcs2) && mcs1.containsMCSFully(mcs2))
			{
			    minimalMCSSet.remove(mcs1);
			    break;
			}
		    }
		}
		return minimalMCSSet;
	}
	else // Multithreading
	{
	    Set<MCS> minimalMCSSet = ConcurrentHashMap.newKeySet(GeneralUtility.setSize(mcsSet.size()));
	    minimalMCSSet.addAll(copyMCSSet(mcsSet));
	    Set<Future<?>> futureSet = new HashSet<Future<?>>();
	    for (MCS mcs1 : mcsSet)
	    {
		if (ThreadUtility.getThreadUtility().isThreadAvailable())
		{
		    futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    for (MCS mcs2 : mcsSet)
			    {
				if (!mcs1.equals(mcs2) && mcs1.containsMCSFully(mcs2))
				{
				    minimalMCSSet.remove(mcs1);
				    break;
				}
			    }
			}
		    }));
		}
		else
		{
		    for (MCS mcs2 : mcsSet)
		    {
			if (!mcs1.equals(mcs2) && mcs1.containsMCSFully(mcs2))
			{
			    minimalMCSSet.remove(mcs1);
			    break;
			}
		    }
		}
	    }
	    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
	    return new HashSet<MCS>(minimalMCSSet);
	}
	
    }

    /**
     * Copies a MCS HashSet so the copy can be used for modification.<br>
     * Removes any MCS that are always false and if the MCS HashSet only contains MCS that are always false then a MCS HashSet with a single MCS that is always
     * false is returned. <br>
     * 
     * @param mcsSet
     *            A MCS HashSet.
     * @return The copied MCS HashSet without MCS that are always false.
     */
    public static HashSet<MCS> copyMCSSet(HashSet<MCS> mcsSet)
    {
	HashSet<MCS> returnSet = new HashSet<MCS>(GeneralUtility.setSize(mcsSet.size()));
	if (!mcsSet.isEmpty())
	{
	    for (MCS mcs : mcsSet)
	    {
		if (!mcs.isAlwaysFalse())
		{
		    returnSet.add(new MCS(mcs));
		}
	    }
	    if (returnSet.isEmpty())
	    {
		returnSet.add(new MCS(false));
	    }
	}
	return returnSet;
    }

    /**
     * Returns false if any MCS in the MCS HashSet have merged MCS sets.<br>
     * Otherwise true.<br>
     * 
     * @param mcsSet
     *            A MCS HashSet.
     * @return False if any MCS in the MCS HashSet have merged MCS sets. Otherwise true.
     */
    public static boolean hasMergedMCSInMCSSet(HashSet<MCS> mcsSet)
    {
	for (MCS mcs : mcsSet)
	{
	    if (mcs.hasMergedMCSSets())
	    {
		return true;
	    }
	}
	return false;
    }


    /**
     * Returns the MCS HashSet that contains all possible Prime Implicants. <br>
     * Prime Implicants that are not minimal are not added.<br>
     * 
     * @param mcsSet
     *            A minimized MCS HashSet.
     * @return MCS HashSet that contains all Prime Implicants and is minimal.
     */
    public static HashSet<MCS> returnAllPrimeImplicants(HashSet<MCS> mcsSet)
    {
	printMessage = false;
	HashSet<MCS> newPrimeImplicants = new HashSet<MCS>();
	HashSet<MCS> saveMCSSet = copyMCSSet(mcsSet);
	do
	{
	    newPrimeImplicants.clear();
	    HashSet<MCSNode> finishedNegatedKeyNodes = new HashSet<MCSNode>();
	    HashMap<MCSNode, HashSet<MCS>> piMap = getPrimeImplicants(saveMCSSet);
	    for (MCSNode keyNode : piMap.keySet())
	    {
		if (!finishedNegatedKeyNodes.contains(keyNode))
		{
		    MCSNode negatedKeyNode = keyNode.getNegatedMCSNode();
		    finishedNegatedKeyNodes.add(negatedKeyNode);
		    HashSet<MCS> mcsSetNoKeyNode = removeMCSNodeInMCSSet(keyNode, piMap.get(keyNode));
		    HashSet<MCS> mcsSetNoNegatedKeyNode = removeMCSNodeInMCSSet(negatedKeyNode, piMap.get(negatedKeyNode));
		    for (MCS mcs : mcsSetNoKeyNode)
		    {
			HashSet<MCS> primeSet = combineMCSSetWithMCS(mcsSetNoNegatedKeyNode, mcs);
			for (MCS primeMCS : primeSet)
			{
			    if (!saveMCSSet.contains(primeMCS) && !containsMCSAlreadyMinimal(saveMCSSet, primeMCS))
			    {
				newPrimeImplicants.add(primeMCS);
				saveMCSSet.add(primeMCS);
			    }
			}
		    }
		}
	    }
	}
	while (!newPrimeImplicants.isEmpty());
	printMessage = true;
	return saveMCSSet;
    }
    
    /**
     * Replaces every occurrence of a Node in a MCS HashSet with a replacement MCS HashSet.<br>
     * 
     * @param fullMCSSet
     *            MCS HashSet where the Node is replaced.
     * @param node
     *            The Node that is replaced.
     * @param replacementMCSSet
     *            The replacement MCS HashSet for the Node.
     * @return A MCS HashSet that does not contain the Node.
     */
    public static HashSet<MCS> replaceNodeInMCSSetWithMCSSet(HashSet<MCS> fullMCSSet, Node node, MCSPairSet replacementMCSSet)
    {
	if (!ThreadUtility.getThreadUtility().isMultiThreading()) // No Multithreading
	{
	    HashSet<MCS> newMCSSet = new HashSet<MCS>(GeneralUtility.setSize(fullMCSSet.size()));
	    for (MCS mcs : fullMCSSet)
	    {
		newMCSSet.addAll(mcs.replaceNodeWithMCSSet(node, replacementMCSSet));
	    }
	    return newMCSSet;
	}
	else // Multithreading
	{
	    Set<MCS> newMCSSet = ConcurrentHashMap.newKeySet(GeneralUtility.setSize(fullMCSSet.size()));
	    Set<Future<?>> futureSet = new HashSet<Future<?>>();
	    for (MCS mcs : fullMCSSet)
	    {
		if (fullMCSSet.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
		{
		    futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    newMCSSet.addAll(mcs.replaceNodeWithMCSSet(node, replacementMCSSet));
			}
		    }));
		}
		else
		{
		    newMCSSet.addAll(mcs.replaceNodeWithMCSSet(node, replacementMCSSet));
		}
	    }
	    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
	    return new HashSet<MCS>(newMCSSet);
	}
    }

    /**
     * Returns a HashMap that shows which MCSNodes a relevant for the creation of Prime Implicant MCS.<br>
     * If the HashMap contains a MCSNode as a key then it also contains the otherwise equal negated MCSNode as a key.<br>
     * 
     * @param mcsSet
     *            A MCS HashSet.
     * @return A HashMap that shows which MCSNodes a relevant for the creation of Prime Implicant MCS.
     */
    private static HashMap<MCSNode, HashSet<MCS>> getPrimeImplicants(HashSet<MCS> mcsSet)
    {
	HashMap<MCSNode, HashSet<MCS>> piMap = new HashMap<MCSNode, HashSet<MCS>>();
	for (MCS mcs : mcsSet)
	{
	    for (MCSNode mcsNode : mcs.getMCSNodeSet())
	    {
		if (piMap.containsKey(mcsNode))
		{
		    piMap.get(mcsNode).add(new MCS(mcs));
		}
		else
		{
		    piMap.put(mcsNode, new HashSet<MCS>());
		    piMap.get(mcsNode).add(new MCS(mcs));
		}
	    }
	}
	HashSet<MCSNode> nodeSet = new HashSet<MCSNode>(piMap.keySet());
	for (MCSNode mcsNode : nodeSet)
	{
	    MCSNode negatedNode = mcsNode.getNegatedMCSNode();
	    if (!piMap.containsKey(negatedNode))
	    {
		piMap.remove(mcsNode);
	    }
	}
	return piMap;
    }

    /**
     * Return a new MCS HashSet that does not contain the specified MCSNode in any MCS.<br>
     * 
     * @param mcsNodeToRemove
     *            MCSNode that is removed from the MCS HashSet.
     * @param mcsSet
     *            A MCS HashSet.
     * @return A new MCS HashSet that does not contain the specified MCSNode in any MCS.
     */
    private static HashSet<MCS> removeMCSNodeInMCSSet(MCSNode mcsNodeToRemove, HashSet<MCS> mcsSet)
    {
	HashSet<MCS> newMCSSet = new HashSet<MCS>(GeneralUtility.setSize(mcsSet.size()));
	for (MCS mcs : mcsSet)
	{
	    MCS newMCS = new MCS(mcs);
	    newMCS.getMCSNodeSet().remove(mcsNodeToRemove);
	    newMCSSet.add(newMCS);
	}
	return newMCSSet;
    }

    /**
     * Returns true if the MCS is already in the MCS HashSet or if the MCS HashSet contains a MCS that is contained fully by the specified MCS.<br>
     * Otherwise false.<br>
     * 
     * @param mcsSet
     *            A MCS HashSet.
     * @param mcsToAdd
     *            A MCSNode.
     * @return True if the MCS is already in the MCS HashSet or if the MCS HashSet contains a MCS that is contained fully by the specified MCS. Otherwise false.
     */
    private static boolean containsMCSAlreadyMinimal(HashSet<MCS> mcsSet, MCS mcsToAdd)
    {
	for (MCS mcs : mcsSet)
	{
	    if (mcsToAdd.containsMCSFully(mcs))
	    {
		return true;
	    }
	}
	return false;
    }
    
}
