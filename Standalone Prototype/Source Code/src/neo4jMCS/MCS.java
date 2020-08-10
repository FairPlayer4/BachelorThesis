package neo4jMCS;

import java.util.ArrayList;
import java.util.HashSet;

import org.neo4j.graphdb.Node;

import neo4jUtility.PrintUtility;

/**
 * This class represents a prime implicant or minimal cut set with negated events.<br>
 * A minimal cut set is a set of elements that triggers the event it belongs to if all element in the MCS are triggered.<br>
 * A MCS has a MCSNode HashSet which represents all members of the minimal cut set.<br>
 * These members are nodes with the label CFT_Basic_Event, CFT_Outport or CFT_Inport but there is no explicit check for these nodes.<br>
 * The Nodes in a MCS are stored as MCSNodes so they can be negated without interfering with the Neo4j database.<br>
 * A MCS HashSet is a set where all MCS are logically connected by OR.
 * 
 * @author Kevin Bartik
 *
 */
public class MCS
{

    /**
     * The MCSNode HashSet.<br>
     * Logically MCSNodes are connected by AND.<br>
     */
    private HashSet<MCSNode> mcsNodeSet;

    /**
     * The ArrayList of merged MCS HashSets.<br>
     * Logically each MCS HashSet is connected to this MCS by AND.<br>
     * Allows to postpone large set operations (namely combining sets of MCS with sets of MCS which leads to a large number of individual combinations).<br>
     * It also reduces the memory overhead.<br>
     */
    private ArrayList<HashSet<MCS>> mergedMCSSets;

    /**
     * True if the MCS is always false which means it represents a minimal cut set that cannot trigger then next event.<br>
     * Example: A MCS that contains a MCSNode that is negated and an otherwise equal MCSNode that is not negated.<br>
     * These MCS are discarded when possible and can sometimes be caused by errors in the CFT Model.<br>
     */
    private boolean alwaysFalse;

    /**
     * True if the MCS is always true which means it represents a minimal cut set that will always trigger the next event.<br>
     * Usually caused by errors in the CFT Model.<br>
     */
    private boolean alwaysTrue;

    /**
     * Constructor for error MCS that are directly caused by errors in the application or in the CFT Model.<br>
     * 
     * @param always
     *            If true the MCS is always true. Otherwise the MCS is always false.<br>
     */
    public MCS(boolean always)
    {
	mcsNodeSet = new HashSet<MCSNode>();
	mergedMCSSets = new ArrayList<HashSet<MCS>>();
	if (always)
	{
	    alwaysFalse = false;
	    alwaysTrue = true;
	    messageAlwaysTrue("Constructor MCS(boolean always)");
	}
	else
	{
	    alwaysFalse = true;
	    alwaysTrue = false;
	    messageAlwaysFalse("Constructor MCS(boolean always)");
	}
    }

    /**
     * Constructor for creating MCS from a single MCSNode. <br>
     * The resulting MCS contains only one MCSNode.<br>
     * 
     * @param mcsNode
     *            The MCSNode.
     */
    public MCS(MCSNode mcsNode)
    {
	mcsNodeSet = new HashSet<MCSNode>();
	mcsNodeSet.add(mcsNode);
	mergedMCSSets = new ArrayList<HashSet<MCS>>();
	alwaysFalse = false;
	alwaysTrue = false;
    }

    /**
     * Constructor for creating MCS from a MCSNode HashSet.<br>
     * Also checks if the created MCS is always false.<br>
     * 
     * @param nodeSet
     *            The MCSNode HashSet.
     */
    public MCS(HashSet<MCSNode> nodeSet)
    {
	mcsNodeSet = nodeSet;
	mergedMCSSets = new ArrayList<HashSet<MCS>>();
	alwaysFalse = false;
	alwaysTrue = false;
	for (MCSNode mcsNode : mcsNodeSet)
	{
	    MCSNode negatedMCSNode = mcsNode.getNegatedMCSNode();
	    if (mcsNodeSet.contains(negatedMCSNode))
	    {
		alwaysFalse = true;
		messageAlwaysFalse("Constructor MCS(HashSet<MCSNode> nodeSet)");
		break;
	    }
	}
	resetAlwaysTrue();
    }

    /**
     * Copy Constructor. Used for creating a MCS from a MCS.<br>
     * Everything is copied from the MCS.<br>
     * 
     * @param mcs
     *            The MCS that is copied.
     */
    public MCS(MCS mcs)
    {
	mcsNodeSet = MCSUtility.copyMCSNodeSet(mcs.mcsNodeSet);
	mergedMCSSets = mcs.copyMergedMCSSets();
	alwaysFalse = mcs.alwaysFalse;
	alwaysTrue = mcs.alwaysTrue;
    }



    /**
     * Adds a MCS to the MCS.<br>
     * Adds all MCSNodes to the MCSNode HashSet and adds all merged MCS HashSets to the MCS. <br>
     * 
     * @param mcs
     *            The MCS that is added.
     */
    public void addMCS(MCS mcs)
    {
	if (!mcs.alwaysTrue)
	{
	    for (MCSNode node : mcs.mcsNodeSet)
	    {
		addMCSNode(node);
	    }
	    mergedMCSSets.addAll(mcs.mergedMCSSets);
	}
	if (mcs.alwaysFalse)
	{
	    alwaysFalse = true;
	    messageAlwaysFalse("Method addMCS(MCS mcs)");
	}
	resetAlwaysTrue();
    }

    /**
     * Adds a MCS HashSet to the ArrayList of merged MCS HashSets.<br>
     * If the MCS HashSet only contains one MCS then that MCS is added to the MCS. <br>
     * 
     * @param mcsSet
     *            The MCS HashSet that is merged to the MCS.
     */
    public void mergeMCSSet(HashSet<MCS> mcsSet)
    {
	if (mcsSet.size() == 1)
	{
	    for (MCS mcs : mcsSet)
	    {
		addMCS(mcs);
	    }
	}
	else
	{
	    mergedMCSSets.add(mcsSet);
	}
	resetAlwaysTrue();
    }
    
    /**
     * Returns the MCSNode HashSet of this MCS.<br>
     * Returns an empty HashSet if the MCS is always false.<br>
     * 
     * @return The MCSNode HashSet of this MCS.
     */
    public HashSet<MCSNode> getMCSNodeSet()
    {
	if (!alwaysFalse)
	{
	    return mcsNodeSet;
	}
	else
	{
	    return new HashSet<MCSNode>();
	}
    }
    
    /**
     * Returns true if the MCS is always false. Otherwise false.<br>
     * 
     * @return True if the MCS is always false. Otherwise false.
     */
    public boolean isAlwaysFalse()
    {
	return alwaysFalse;
    }

    /**
     * Returns true if the MCS is always true. Otherwise false.<br>
     * 
     * @return True if the MCS is always true. Otherwise false.
     */
    public boolean isAlwaysTrue()
    {
	return alwaysTrue;
    }
    
    /**
     * Adds a MCSNode to the MCSNode HashSet of the MCS. Checks if the MCS is always false after adding the MCSNode.
     * 
     * @param mcsNode
     *            The MCSNode that is added.
     */
    void addMCSNode(MCSNode mcsNode)
    {
	MCSNode negatedMCSNode = mcsNode.getNegatedMCSNode();
	if (mcsNodeSet.contains(negatedMCSNode))
	{
	    alwaysFalse = true;
	    messageAlwaysFalse("Method addMCSNode(MCSNode mcsNode)");
	}
	mcsNodeSet.add(mcsNode);
	resetAlwaysTrue();
    }

    /**
     * Returns the combined MCS HashSet of this MCS.<br>
     * This method combines all MCS HashSets in the ArrayList of merged MCS HashSets with this MCS.<br>
     * The resulting MCS HashSet contains only MCS that have no merged MCS HashSets.<br>
     * This method only properly works with MCS that contain merged MCS HashSets and are not always false.<br>
     * 
     * @return MCS HashSet with MCS that have no merged MCS HashSets.
     */
    HashSet<MCS> returnCombinedMCSSet()
    {
	HashSet<MCS> superMCS = new HashSet<MCS>();
	superMCS.add(new MCS(MCSUtility.copyMCSNodeSet(mcsNodeSet)));
	ArrayList<HashSet<MCS>> refinedCombinedMCS = new ArrayList<HashSet<MCS>>();
	for (HashSet<MCS> mcsSet : mergedMCSSets)
	{
	    if (MCSUtility.hasMergedMCSInMCSSet(mcsSet))
	    {
		refinedCombinedMCS.add(MCSUtility.combineAllMCSInMCSSet(mcsSet));
	    }
	    else
	    {
		refinedCombinedMCS.add(MCSUtility.copyMCSSet(mcsSet));
	    }
	}
	for (int i = 0; i < refinedCombinedMCS.size(); i++)
	{
	    HashSet<MCS> superMCS2 = new HashSet<MCS>();
	    for (MCS mcs : superMCS)
	    {
		superMCS2.addAll(MCSUtility.combineMCSSetWithMCS(refinedCombinedMCS.get(i), mcs));
	    }
	    superMCS = superMCS2;
	}
	return superMCS;
    }
    

    /**
     * Returns a new MCS HashSet that represent this MCS after the specified Node was replaced with a MCS HashSet.<br>
     * If the corresponding MCSNode of the Node is negated then the MCSNode is replace with the negated MCS HashSet.<br>
     * Requires this MCS to have no merged MCS HashSets.<br>
     * 
     * @param node
     *            The specified Node that is replaced.
     * @param mcsSet
     *            The replacement MCS HashSet.
     * @return A new MCS HashSet that represent this MCS after the specified Node was replaced with a MCS HashSet.
     */
    HashSet<MCS> replaceNodeWithMCSSet(Node node, MCSPairSet mcsSet)
    {
	HashSet<MCS> newMCSSet = new HashSet<MCS>();
	if (!alwaysTrue && !alwaysFalse)
	{
	    MCSNode mcsNode = new MCSNode(node, false);
	    if (mcsNodeSet.contains(mcsNode))
	    {
		HashSet<MCSNode> newNodeSet = MCSUtility.copyMCSNodeSet(mcsNodeSet);
		newNodeSet.remove(mcsNode);
		for (MCS mcs : mcsSet.getMCSSet())
		{
		    MCS newMCS = new MCS(mcs);
		    for (MCSNode nextMCSNode : newNodeSet)
		    {
			newMCS.addMCSNode(nextMCSNode);
		    }
		    if (!newMCS.alwaysFalse)
		    {
			newMCSSet.add(newMCS);
		    }
		}
	    }
	    else
	    {
		MCSNode negatedMCSNode = mcsNode.getNegatedMCSNode();
		if (mcsNodeSet.contains(negatedMCSNode))
		{
		    HashSet<MCSNode> newNodeSet = MCSUtility.copyMCSNodeSet(mcsNodeSet);
		    newNodeSet.remove(negatedMCSNode);
		    for (MCS mcs : mcsSet.getNegatedMCSSet())
		    {
			MCS newMCS = new MCS(mcs);
			for (MCSNode nextMCSNode : newNodeSet)
			{
			    newMCS.addMCSNode(nextMCSNode);
			}
			if (!newMCS.alwaysFalse)
			{
			    newMCSSet.add(newMCS);
			}
		    }
		}
		else
		{
		    newMCSSet.add(new MCS(this));
		}
	    }
	}
	return newMCSSet;
    }

    /**
     * Checks if the MCSNode HashSet of this MCS contains all MCSNodes in the MCSNode HashSet of the other MCS.<br>
     * 
     * @param mcs
     *            The other MCS.
     * @return True if this MCS contains all MCSNodes of the other MCS.
     */
    boolean containsMCSFully(MCS mcs)
    {
	return mcsNodeSet.containsAll(mcs.mcsNodeSet);
    }

    /**
     * Checks if the MCS has merged MCS HashSets or if it is always false.<br>
     * 
     * @return true if this MCS is always false or if it contains any merged MCS HashSets. Otherwise false.
     */
    boolean hasMergedMCSSets()
    {
	return mergedMCSSets.isEmpty() || alwaysFalse;
    }

    /**
     * Copies the ArrayList of merged MCS HashSets of this MCS.<br>
     * Every MCS is individually copied so they can be modified with affecting the original merged MCS HashSets.<br>
     * 
     * @return A copy of the ArrayList of merged MCS HashSets of this MCS.
     */
    private ArrayList<HashSet<MCS>> copyMergedMCSSets()
    {
	ArrayList<HashSet<MCS>> returnSetList = new ArrayList<HashSet<MCS>>();
	for (HashSet<MCS> mcsSet : mergedMCSSets)
	{
	    returnSetList.add(MCSUtility.copyMCSSet(mcsSet));
	}
	return returnSetList;
    }

    /**
     * Prints a message on the GUI to inform the user that a MCS is always true.<br>
     * This may be caused by an error in the CFT Model.<br>
     * @param origin The origin of this message.
     */
    private void messageAlwaysTrue(String origin)
    {
	if (MCSUtility.isPrintMessage())
	{
	    PrintUtility.printInfo("There might be an error in the CFT Model since a minimal cut set is always true.\nOrigin: " + origin);
	}
    }

    /**
     * Prints a message on the GUI to inform the user that a MCS is always true.<br>
     * This may be caused by an error in the CFT Model.<br>
     * @param origin The origin of this message.
     */
    private void messageAlwaysFalse(String origin)
    {
	if (MCSUtility.isPrintMessage())
	{
	    PrintUtility.printInfo("There might be an error in the CFT Model since a minimal cut set is always false.\nOrigin: " + origin);
	}
    }

    /**
     * Resets the always true status of a MCS.<br>
     */
    private void resetAlwaysTrue()
    {
	if (alwaysFalse || (mcsNodeSet.size() + mergedMCSSets.size()) > 0)
	{
	    alwaysTrue = false;
	}
	else
	{
	    alwaysTrue = true;
	    messageAlwaysTrue("Method resetAlwaysTrue()");
	}
    }

    @Override
    public int hashCode()
    {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((mergedMCSSets == null) ? 0 : mergedMCSSets.hashCode());
	result = prime * result + ((mcsNodeSet == null) ? 0 : mcsNodeSet.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj)
    {
	if (this == obj)
	{
	    return true;
	}
	if (obj == null)
	{
	    return false;
	}
	if (getClass() != obj.getClass())
	{
	    return false;
	}
	MCS other = (MCS) obj;
	if (mergedMCSSets == null || other.mergedMCSSets == null)
	{
	    return false;
	}
	else if (!mergedMCSSets.equals(other.mergedMCSSets))
	{
	    return false;
	}
	if (mcsNodeSet == null || other.mcsNodeSet == null)
	{
	    return false;
	}
	else if (!mcsNodeSet.equals(other.mcsNodeSet))
	{
	    return false;
	}
	return true;
    }

    @Override
    public String toString()
    {
	if (alwaysFalse)
	{
	    return "MCS [ALWAYSFALSE]";
	}
	if (alwaysTrue)
	{
	    return "MCS [ALWAYSTRUE]";
	}
	if (mergedMCSSets.isEmpty())
	{
	    return "MCS [" + mcsNodeSet + "]";
	}
	return "MCS [nodeSet=" + mcsNodeSet + ", combinedMCS=" + mergedMCSSets + "]";
    }

}
