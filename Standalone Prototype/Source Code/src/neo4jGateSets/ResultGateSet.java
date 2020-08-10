package neo4jGateSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBUtility;
import neo4jDatabase.DBMCSManager;
import neo4jEnum.NodeLabels;
import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSPairSet;
import neo4jMCS.MCSUtility;
import neo4jTraversal.MainTraversal;

/**
 * The ResultGateSet is a GateSet for a Node that represents an Outport or an Inport Instance in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * This class is used for the results of a Traversal.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class ResultGateSet extends AbstractGateSet
{

    private HashSet<MCS> fullMCSSet;

    private HashSet<MCS> fullNegatedMCSSet;

    private boolean mcsConnected = false;

    /**
     * Constructor for ResultGateSet.<br>
     * 
     * @param startElement
     *            The Node to which the ResultGateSet belongs to.
     */
    public ResultGateSet(Node startElement)
    {
	// The startElement must have Label CFT_Outport or CFT_Inport_Instance.
	super(startElement);
	fullMCSSet = new HashSet<MCS>();
	fullNegatedMCSSet = new HashSet<MCS>();
    }

    private ResultGateSet(ResultGateSet rgs)
    {
	super(rgs);
	fullMCSSet = new HashSet<MCS>();
	fullNegatedMCSSet = new HashSet<MCS>();
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ResultGateSet Description:<br>
     * First the HashSet of MCS is created in each lower GateSet.<br>
     * If the {@code nodeSet} is not empty all Nodes are put into a single MCS which is then added to the {@code mcsSet}.<br>
     * If the {@code nodeSet} was empty then all MCS from all GateSets in {@code gateSets} are added to the {@code mcsSet}.<br>
     * 
     * @see MCS
     */
    @Override
    public void createMCSSet()
    {
	if (super.getMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    if (!nodeSet.isEmpty())
	    {
		super.getMCSSet().add(new MCS(MCSUtility.getMCSNodeSet(nodeSet)));
	    }
	    else
	    {
		for (GateSet gateSet : lowerGateSets)
		{
		    super.getMCSSet().addAll(gateSet.getMCSSetCopy());
		}
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ResultGateSet Description:<br>
     * First the HashSet of negated MCS is created in each lower GateSet.<br>
     * If the {@code nodeSet} is not empty all Nodes are put into a single MCS which is then added to the {@code mcsSet}.<br>
     * If the {@code nodeSet} was empty then all MCS from all GateSets in {@code gateSets} are added to the {@code mcsSet}.<br>
     * 
     * @see MCS
     */
    @Override
    public void createNegatedMCSSet()
    {
	if (getNegatedMCSSet().isEmpty())
	{
	    createNegatedMCSSetInLowerGateSets();
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
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ResultGateSet Description:<br>
     * Adds the Probability of all Nodes and GateSets to an ArrayList.<br>
     * Each member of the ArrayList is multiplied with {@code result} which starts with the initial value of {@code 1}.<br>
     * If the ArrayList does not have the size {@code 1} then {@code 0} is returned.<br>
     */
    @Override
    public double calculateBasicFailureProbability()
    {
	double result = 1;
	ArrayList<Double> allprops = getNodeProbabilities();
	for (GateSet gateSet : lowerGateSets)
	{
	    allprops.add(gateSet.calculateBasicFailureProbability());
	}
	if (allprops.size() == 1)
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

    /**
     * Replaces the outport instances in every MCS with the respective MCS of the outport instance.
     */
    @Override
    public MCSPairSet connectMCS()
    {
	if (!mcsConnected)
	{
	    if (lowerGateSets.size() == 1)
	    {
		HashSet<Node> outInsts = new HashSet<Node>();
		for (MCS mcs : super.getMCSSet())
		{
		    for (MCSNode mcsNode : mcs.getMCSNodeSet())
		    {
			if (DBUtility.hasLabel(mcsNode.getNode(), NodeLabels.CFT_Outport_Instance))
			{
			    outInsts.add(mcsNode.getNode());
			}
		    }
		}
		GateSet nextGateSet = lowerGateSets.iterator().next();
		HashSet<GateSet> outInstGS = nextGateSet.findGateSets(outInsts);
		HashMap<Node, MCSPairSet> outInstMap = new HashMap<Node, MCSPairSet>();
		for (GateSet gs : outInstGS)
		{
		    MCSPairSet mcsS = gs.connectMCS();
		    outInstMap.put(gs.getStartNode(), mcsS);
		}
		for (Node outInst : outInstMap.keySet())
		{
		    HashSet<MCS> returnSet = MCSUtility.replaceNodeInMCSSetWithMCSSet(super.getMCSSet(), outInst, outInstMap.get(outInst));
		    HashSet<MCS> negatedReturnSet = MCSUtility.replaceNodeInMCSSetWithMCSSet(super.getNegatedMCSSet(), outInst, outInstMap.get(outInst));
		    super.getMCSSet().clear();
		    super.getNegatedMCSSet().clear();
		    super.getMCSSet().addAll(returnSet);
		    super.getNegatedMCSSet().addAll(negatedReturnSet);
		}
		mcsConnected = true;
		if (DBUtility.hasLabel(getStartNode(), NodeLabels.CFT_Outport) && !MainTraversal.errorFound())
		{
		    DBMCSManager.addFullMCSSet(this);
		}
	    }
	}
	return new MCSPairSet(getMCSSetCopy(), getNegatedMCSSetCopy());
    }

    /**
     * Sets the MCS HashSet.
     * Used when MCS are retrieved from the database.
     * @param _mcsSet The HashSet of MCS.
     */
    public void SetMCSSet(HashSet<MCS> _mcsSet)
    {
	super.getMCSSet().clear();
	super.getMCSSet().addAll(_mcsSet);
    }

    /**
     * Sets the full MCS HashSet.
     * Used when full MCS are retrieved from the database.
     * @param _fullMCSSet The HashSet of full MCS
     */
    public void SetFullMCSSet(HashSet<MCS> _fullMCSSet)
    {
	fullMCSSet = _fullMCSSet;
    }

    /**
     * Returns true if the ResultGateSet has full MCS. Otherwise false.
     * @return true if the ResultGateSet has full MCS. Otherwise false.
     */
    public boolean hasFullMCSSet()
    {
	return !fullMCSSet.isEmpty();
    }
    
    /**
     * Returns true if the ResultGateSet has full negated MCS. Otherwise false.
     * @return true if the ResultGateSet has full negated MCS. Otherwise false.
     */
    public boolean hasFullNegatedMCSSet()
    {
	return !fullNegatedMCSSet.isEmpty();
    }
    
    /**
     * Sets the negated MCS HashSet.
     * Used when negated MCS are retrieved from the database.
     * @param _negatedMCSSet The HashSet of negated MCS.
     */
    public void SetNegatedMCSSet(HashSet<MCS> _negatedMCSSet)
    {
	super.getNegatedMCSSet().clear();
	super.getNegatedMCSSet().addAll(_negatedMCSSet);
	
    }

    /**
     * Sets the full negated MCS HashSet.
     * Used when full negated MCS are retrieved from the database.
     * @param _fullNegatedMCSSet The HashSet of full negated MCS.
     */
    public void SetFullNegatedMCSSet(HashSet<MCS> _fullNegatedMCSSet)
    {
	fullNegatedMCSSet = _fullNegatedMCSSet;
	
    }

    @Override
    public HashSet<MCS> getMCSSet()
    {
	if (hasFullMCSSet())
	{
	    return fullMCSSet;
	}
	return super.getMCSSet();
    }
    
    @Override
    public HashSet<MCS> getNegatedMCSSet()
    {
	if (hasFullNegatedMCSSet())
	{
	    return fullNegatedMCSSet;
	}
	return super.getNegatedMCSSet();
    }

    @Override
    public GateSet getGateSetCopy()
    {
	return new ResultGateSet(this);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ResultGateSet Description:<br>
     * Returns {@code 7}.<br>
     */
    @Override
    public int getGateType()
    {
	return 7;
    }

    @Override
    public String getMCSString()
    {
	if (!getMCSSet().isEmpty() || !getNegatedMCSSet().isEmpty())
	{
	    StringBuilder mcsString = new StringBuilder();
	    if (!getMCSSet().isEmpty())
	    {
		mcsString.append("Minimal Cut Sets for the Element (" + DBUtility.getIDAndName(getStartNode()) + " | [" + DBUtility.getRelevantLabel(getStartNode()) + "])");
	    }
	    else
	    {
		if (!getNegatedMCSSet().isEmpty())
		{
		    mcsString.append("Negated Minimal Cut Sets for the Element (" + DBUtility.getIDAndName(getStartNode()) + " | [" + DBUtility.getRelevantLabel(getStartNode()) + "])");
		}
	    }
	    for (MCS mcs : getMCSSet())
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
	    return "The Element (" + DBUtility.getIDAndName(getStartNode()) + " | [" + DBUtility.getRelevantLabel(getStartNode())
		    + "]) has no Minimal Cut Sets.\nThis could be caused by an error in the CFT model.";
	}
    }

    

}
