package neo4jGateSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;
import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSPairSet;
import neo4jMCS.MCSUtility;
import neo4jTraversal.MainTraversal;

/**
 * The ConnectorGateSet is a GateSet for a Node that represents an Outport Instance or an Inport in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * This class is used for the connection of results after a Traversal.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class ConnectorGateSet extends AbstractGateSet
{

    /**
     * This variable is used when a ConnectorGateSet is created from an Outport Instance during a split Traversal. Later when this GateSet is connected with a
     * GateSet of an Outport the MCS set will contain Inports that need to be mapped to Inport Instances.
     */
    private HashMap<Node, Node> inportMap;

    private boolean mcsConnected = false;

    public ConnectorGateSet(Node startElement)
    {
	super(startElement);
    }

    public ConnectorGateSet(Node startElement, HashMap<Node, Node> _inportMap)
    {
	super(startElement);
	inportMap = _inportMap;
    }

    private ConnectorGateSet(ConnectorGateSet cgs)
    {
	super(cgs);
	inportMap = cgs.inportMap;
    }

    @Override
    public void createMCSSet()
    {
	if (getMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    if (inportMap == null)
	    {
		if (!nodeSet.isEmpty())
		{
		    getMCSSet().add(new MCS(MCSUtility.getMCSNodeSet(nodeSet)));
		}
		else
		{
		    for (GateSet gateSet : lowerGateSets)
		    {
			getMCSSet().addAll(gateSet.getMCSSetCopy());
		    }
		}
	    }
	    else // For saving MCS in DB
	    {
		if (!nodeSet.isEmpty())
		{
		    getMCSSet().add(new MCS(new MCSNode(getStartNode(), false)));
		}
	    }
	}
    }

    @Override
    public void createNegatedMCSSet()
    {
	if (getNegatedMCSSet().isEmpty())
	{
	    createNegatedMCSSetInLowerGateSets();
	    if (inportMap == null)
	    {
		if (!nodeSet.isEmpty())
		{
		    MCS negatedMCS = new MCS(MCSUtility.getNegatedMCSNodeSet(nodeSet));
		    getNegatedMCSSet().add(negatedMCS);
		}
		else
		{
		    for (GateSet gateSet : lowerGateSets)
		    {
			getNegatedMCSSet().addAll(gateSet.getNegatedMCSSetCopy());
		    }
		}
	    }
	    else // For saving MCS in DB
	    {
		if (!nodeSet.isEmpty())
		{
		    MCS outinst = new MCS(new MCSNode(getStartNode(), true));
		    getNegatedMCSSet().add(outinst);
		}
	    }
	}
    }

    @Override
    public double calculateBasicFailureProbability()
    {
	double result = 1;
	ArrayList<Double> allprops = getNodeProbabilities();
	for (GateSet gateSet : lowerGateSets)
	{
	    allprops.add(gateSet.calculateBasicFailureProbability());
	}
	if (!allprops.isEmpty())
	{
	    for (int i = 0; i < allprops.size(); i++)
	    {
		result *= allprops.get(i);
	    }
	    return result;
	}
	else
	{
	    return 0;
	}
    }

    @Override
    public void connectGateSets(LinkedList<HashMap<Node, Node>> inportMapList)
    {
	// Can only be used when the Traversal was split.
	HashSet<Node> connectionNodes = new HashSet<Node>();
	for (Node node : nodeSet)
	{
	    if (DBUtility.hasLabel(node, NodeLabels.CFT_Outport))
	    {
		connectionNodes.add(node);
	    }
	}
	if (connectionNodes.size() == 1)
	{
	    LinkedList<HashMap<Node, Node>> newInportMapList;
	    if (inportMapList != null)
	    {
		newInportMapList = new LinkedList<HashMap<Node, Node>>(inportMapList);
	    }
	    else
	    {
		newInportMapList = new LinkedList<HashMap<Node, Node>>();
	    }
	    newInportMapList.addLast(inportMap);
	    for (Node node : connectionNodes)
	    {
		GateSet replacement = MainTraversal.getResultGateSet(node).getGateSetCopy();
		nodeSet.remove(node);
		addLowerGateSet(replacement);
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		gateSet.connectGateSets(newInportMapList);
	    }
	}
    }

    @Override
    public MCSPairSet connectMCS()
    {
	if (!mcsConnected)
	{
	    HashMap<Node, MCSPairSet> nextInportMap = new HashMap<Node, MCSPairSet>();
	    GateSet outport = lowerGateSets.iterator().next();
	    HashSet<GateSet> inportInstGS = outport.findGateSets(inportMap.values());
	    for (GateSet gs : inportInstGS)
	    {
		MCSPairSet mcsS = gs.connectMCS();
		nextInportMap.put(DBUtility.getInportFromInportInstance(gs.getStartNode()), mcsS);
	    }
	    getMCSSet().clear();
	    getNegatedMCSSet().clear();
	    getMCSSet().addAll(outport.connectMCS().getMCSSet());
	    getNegatedMCSSet().addAll(outport.connectMCS().getNegatedMCSSet());
	    for (Node inport : nextInportMap.keySet())
	    {
		HashSet<MCS> returnSet = MCSUtility.replaceNodeInMCSSetWithMCSSet(getMCSSet(), inport, nextInportMap.get(inport));
		HashSet<MCS> negatedReturnSet = MCSUtility.replaceNodeInMCSSetWithMCSSet(getNegatedMCSSet(), inport, nextInportMap.get(inport));
		getMCSSet().clear();
		getNegatedMCSSet().clear();
		getMCSSet().addAll(returnSet);
		getNegatedMCSSet().addAll(negatedReturnSet);
	    }
	    mcsConnected = true;
	}
	return new MCSPairSet(getMCSSetCopy(), getNegatedMCSSetCopy());
    }

    @Override
    public GateSet getGateSetCopy()
    {
	return new ConnectorGateSet(this);
    }

    @Override
    public int getGateType()
    {
	return 6;
    }

}
