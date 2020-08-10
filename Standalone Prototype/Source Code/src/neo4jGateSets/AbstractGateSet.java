package neo4jGateSets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;
import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSPairSet;
import neo4jMCS.MCSUtility;
import neo4jTraversal.MainTraversal;
import neo4jUtility.PrintUtility;
import neo4jUtility.ThreadUtility;

/**
 * This class implements many methods of the GateSet and provides some additional methods.
 * All GateSet implementations extend this class.
 * @author Kevin Bartik
 *
 */
public abstract class AbstractGateSet implements GateSet
{

    /**
     * The set of MCS.
     */
    private HashSet<MCS> mcsSet;

    /**
     * The set of negated MCS.
     */
    private HashSet<MCS> negatedMCSSet;

    /**
     * The start node of the GateSet.
     */
    private final Node startElement;

    /**
     * Used for printing MCS.
     */
    final int numberOfElementsPerRow = 1;

    /**
     * The node set of the GateSet.
     */
    HashSet<Node> nodeSet;

    /**
     * The lower GateSets of the GateSet.
     */
    HashSet<GateSet> lowerGateSets;

    /**
     * The constructor of the AbstractGateSet.
     * Initializes all sets and set the start node.
     * @param _startElement The start node.
     */
    AbstractGateSet(Node _startElement)
    {
	nodeSet = new HashSet<Node>();
	lowerGateSets = new HashSet<GateSet>();
	mcsSet = new HashSet<MCS>();
	negatedMCSSet = new HashSet<MCS>();
	startElement = _startElement;
    }

    /**
     * Copy constructor for the AbstractGateSet.
     * Copies an existing AbstractGateSet so that all contents are different objects.
     * @param _gateSet The AbstractGateSet.
     */
    AbstractGateSet(AbstractGateSet _gateSet)
    {
	nodeSet = new HashSet<Node>(_gateSet.nodeSet);
	lowerGateSets = new HashSet<GateSet>();
	mcsSet = _gateSet.getMCSSetCopy();
	negatedMCSSet = _gateSet.getNegatedMCSSetCopy();
	for (GateSet gateSet : _gateSet.lowerGateSets)
	{
	    lowerGateSets.add(gateSet.getGateSetCopy());
	}
	startElement = _gateSet.startElement;
    }

    /**
     * Creates the set of MCS in all lower GateSets.
     */
    void createMCSSetInLowerGateSets()
    {
	if (ThreadUtility.getThreadUtility().isMultiThreading())
	{
	    Set<Future<?>> futureSet = new HashSet<Future<?>>();
	    Iterator<GateSet> iterator = lowerGateSets.iterator();
	    while (iterator.hasNext())
	    {
		GateSet gateSet = iterator.next();
		if (lowerGateSets.size() == 1 || !ThreadUtility.getThreadUtility().isThreadAvailable() || !iterator.hasNext())
		{
		    gateSet.createMCSSet();
		}
		else
		{
		    futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    gateSet.createMCSSet();
			}
		    }));
		}
	    }
	    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
	}
	else
	{
	    for (GateSet gateSet : lowerGateSets)
	    {
		gateSet.createMCSSet();
	    }
	}
    }

    /**
     * Creates the set of negated MCS in all lower GateSets.
     */
    void createNegatedMCSSetInLowerGateSets()
    {
	if (ThreadUtility.getThreadUtility().isMultiThreading())
	{
	    Set<Future<?>> futureSet = new HashSet<Future<?>>();
	    Iterator<GateSet> iterator = lowerGateSets.iterator();
	    while (iterator.hasNext())
	    {
		GateSet gateSet = iterator.next();
		if (lowerGateSets.size() == 1 || !ThreadUtility.getThreadUtility().isThreadAvailable() || !iterator.hasNext())
		{
		    gateSet.createNegatedMCSSet();
		}
		else
		{
		    futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    gateSet.createNegatedMCSSet();
			}
		    }));
		}
	    }
	    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
	}
	else
	{
	    for (GateSet gateSet : lowerGateSets)
	    {
		gateSet.createNegatedMCSSet();
	    }
	}
    }

    /**
     * Creates a list that contains the probabilities of all nodes in the GateSet.
     * @return a list that contains the probabilities of all nodes in the GateSet.
     */
    ArrayList<Double> getNodeProbabilities()
    {
	ArrayList<Double> probs = new ArrayList<Double>();
	for (Node node : nodeSet)
	{
	    probs.add(DBUtility.findProbability(node));
	}
	return probs;
    }

    @Override
    public synchronized void addNode(Node node)
    {
	nodeSet.add(node);
    }

    @Override
    public synchronized void addLowerGateSet(GateSet gateSet)
    {
	lowerGateSets.add(gateSet);
    }

    @Override
    public void combineAllMergedMCS()
    {
	if (!mcsSet.isEmpty() && MCSUtility.hasMergedMCSInMCSSet(mcsSet))
	{
	    mcsSet = MCSUtility.combineAllMCSInMCSSet(mcsSet);
	}
	if (!negatedMCSSet.isEmpty() && MCSUtility.hasMergedMCSInMCSSet(negatedMCSSet))
	{
	    negatedMCSSet = MCSUtility.combineAllMCSInMCSSet(negatedMCSSet);
	}
    }

    @Override
    public void minimizeMCS()
    {
	if (!mcsSet.isEmpty())
	{
	    mcsSet = MCSUtility.minimizeMCSSet(mcsSet);
	}
	if (!negatedMCSSet.isEmpty())
	{
	    negatedMCSSet = MCSUtility.minimizeMCSSet(negatedMCSSet);
	}
    }

    @Override
    public void addHiddenPrimeImplicants()
    {
	if (!mcsSet.isEmpty())
	{
	    mcsSet = MCSUtility.returnAllPrimeImplicants(getMCSSetCopy());
	}
	if (!negatedMCSSet.isEmpty())
	{
	    negatedMCSSet = MCSUtility.returnAllPrimeImplicants(getNegatedMCSSetCopy());
	}
    }

    @Override
    public void connectGateSets(LinkedList<HashMap<Node, Node>> inportMapList)
    {
	if (MainTraversal.getResultGateSets().size() > 1)
	{
	    for (GateSet gateSet : lowerGateSets)
	    {
		gateSet.connectGateSets(inportMapList);
	    }
	    if (!nodeSet.isEmpty() && inportMapList != null && !inportMapList.isEmpty())
	    {
		LinkedList<HashMap<Node, Node>> newInportMapList = new LinkedList<HashMap<Node, Node>>(inportMapList);
		HashSet<Node> connectionNodes = new HashSet<Node>();
		for (Node node : nodeSet)
		{
		    if (DBUtility.hasLabel(node, NodeLabels.CFT_Inport) && !MainTraversal.getInports().contains(node))
		    {
			connectionNodes.add(node);
		    }
		}
		HashSet<GateSet> newGateSets = new HashSet<GateSet>();
		for (Node node : connectionNodes)
		{
		    Node inportInst = newInportMapList.getLast().get(node);
		    GateSet replacement = MainTraversal.getResultGateSet(inportInst).getGateSetCopy();
		    nodeSet.remove(node);
		    addLowerGateSet(replacement);
		    newGateSets.add(replacement);
		}
		newInportMapList.removeLast();
		for (GateSet gateSet : newGateSets)
		{
		    gateSet.connectGateSets(newInportMapList);
		}
	    }
	}
    }

    @Override
    public MCSPairSet connectMCS()
    {
	PrintUtility.printError("The MCS cannot be connected because there is some error!");
	return null;
    }

    @Override
    public HashSet<GateSet> findGateSets(Collection<Node> nodesToFind)
    {
	HashSet<GateSet> gsSet = new HashSet<GateSet>();
	if (nodesToFind.contains(startElement))
	{
	    gsSet.add(this);
	}
	else
	{
	    for (GateSet gs : lowerGateSets)
	    {
		gsSet.addAll(gs.findGateSets(nodesToFind));
	    }
	}
	return gsSet;
    }

    @Override
    public Node getStartNode()
    {
	return startElement;
    }

    @Override
    public HashSet<MCS> getMCSSet()
    {
	return mcsSet;
    }

    @Override
    public HashSet<MCS> getNegatedMCSSet()
    {
	return negatedMCSSet;
    }

    @Override
    public HashSet<MCS> getMCSSetCopy()
    {
	return MCSUtility.copyMCSSet(mcsSet);
    }

    @Override
    public HashSet<MCS> getNegatedMCSSetCopy()
    {
	return MCSUtility.copyMCSSet(negatedMCSSet);
    }

    @Override
    public String getGateSetString(String tab)
    {
	StringBuilder gates = new StringBuilder();
	gates.append(tab + DBUtility.getRelevantLabel(this.getStartNode()) + " GateSet | " + DBUtility.getIDAndName(this.getStartNode()));
	gates.append("\n " + tab + "NodeSet:");
	for (Node node : nodeSet)
	{
	    gates.append("\n  " + tab + "Node: " + DBUtility.getIDAndName(node));
	}
	gates.append("\n " + tab + "GateSets:");
	for (GateSet gs : lowerGateSets)
	{
	    gates.append("\n" + gs.getGateSetString(tab + "  "));
	}
	return gates.toString();
    }

    @Override
    public String getMCSString()
    {
	if (!mcsSet.isEmpty() || !negatedMCSSet.isEmpty())
	{
	    StringBuilder mcsString = new StringBuilder();
	    if (!mcsSet.isEmpty())
	    {
		mcsString.append("Minimal Cut Sets for the Element (" + DBUtility.getIDAndName(startElement) + " | [" + DBUtility.getRelevantLabel(startElement) + "])");
	    }
	    else
	    {
		if (!negatedMCSSet.isEmpty())
		{
		    mcsString.append("Negated Minimal Cut Sets for the Element (" + DBUtility.getIDAndName(startElement) + " | [" + DBUtility.getRelevantLabel(startElement) + "])");
		}
	    }
	    for (MCS mcs : mcsSet)
	    {
		mcsString.append("\n--------------------\n  {");
		if (mcs.isAlwaysTrue())
		{
		    mcsString.append("\n    A Minimal Cut Set that is always true.");
		}
		else
		{
		    if (mcs.isAlwaysFalse())
		    {
			mcsString.append("\n    A Minimal Cut Set that is always false.");
		    }
		    else
		    {
			int number = 0;
			ArrayList<MCSNode> mcsList = new ArrayList<MCSNode>(mcs.getMCSNodeSet());
			for (int i = 0; i < mcsList.size(); i++)
			{
			    if (i == number)
			    {
				mcsString.append("\n    ");
				number += numberOfElementsPerRow;
			    }
			    MCSNode element = mcsList.get(i);
			    mcsString.append("(" + DBUtility.getIDAndName(element) + " | [" + DBUtility.getRelevantLabel(element.getNode()) + "])");
			    if (i != mcsList.size() - 1)
			    {
				mcsString.append(", ");
			    }
			}
		    }
		}
		mcsString.append("\n  }");
	    }
	    mcsString.append("\n--------------------\nEnd of minimal cut sets");
	    return mcsString.toString();
	}
	else
	{
	    return "The Element (" + DBUtility.getIDAndName(startElement) + " | [" + DBUtility.getRelevantLabel(startElement)
		    + "]) has no Minimal Cut Sets.\nThis could be caused by an error in the CFT model.";
	}
    }

    @Override
    public int hashCode()
    {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((lowerGateSets == null) ? 0 : lowerGateSets.hashCode());
	result = prime * result + ((nodeSet == null) ? 0 : nodeSet.hashCode());
	result = prime * result + ((startElement == null) ? 0 : startElement.hashCode());
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
	AbstractGateSet other = (AbstractGateSet) obj;
	if (lowerGateSets == null)
	{
	    if (other.lowerGateSets != null)
	    {
		return false;
	    }
	}
	else if (!lowerGateSets.equals(other.lowerGateSets))
	{
	    return false;
	}
	if (nodeSet == null)
	{
	    if (other.nodeSet != null)
	    {
		return false;
	    }
	}
	else if (!nodeSet.equals(other.nodeSet))
	{
	    return false;
	}
	if (startElement == null)
	{
	    if (other.startElement != null)
	    {
		return false;
	    }
	}
	else if (!startElement.equals(other.startElement))
	{
	    return false;
	}
	return true;
    }

}
