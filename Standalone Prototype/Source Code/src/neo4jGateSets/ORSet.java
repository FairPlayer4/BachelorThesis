package neo4jGateSets;

import java.util.ArrayList;

import org.neo4j.graphdb.Node;

import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSUtility;
import neo4jUtility.SettingsUtility;

/**
 * The ORSet is a GateSet for a Node that represents an OR Gate in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class ORSet extends AbstractGateSet
{

    /**
     * Constructor for ORSet.<br>
     * 
     * @param startElement
     *            The Node to which the ORSet belongs to.
     */
    public ORSet(Node startElement)
    {
	super(startElement);
    }

    private ORSet(ORSet orSet)
    {
	super(orSet);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ORSet Description:<br>
     * First the HashSet of MCS is created in all lower GateSets.<br>
     * Each Node in the {@code nodeSet} is used to create a MCS which is added to the {@code mcsSet}.<br>
     * The MCS from the {@code gateSets} are all added to the {@code mcsSet}.<br>
     * 
     * @see MCS
     */
    @Override
    public void createMCSSet()
    {
	if (getMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    for (Node node : nodeSet)
	    {
		getMCSSet().add(new MCS(new MCSNode(node, false)));
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		getMCSSet().addAll(gateSet.getMCSSetCopy());
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ORSet Description:<br>
     * First the HashSet of negated MCS is created in all lower GateSets.<br>
     * All Nodes in the {@code nodeSet} are negated and put into a single MCS. 
     * This MCS is added to the HashSet of negated MCS.
     * The MCS from the {@code gateSets} are merged to all MCS in HashSet of negated MCS.<br>
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
	    for (GateSet gs : lowerGateSets)
	    {
		if (getNegatedMCSSet().isEmpty())
		{
		    getNegatedMCSSet().addAll(gs.getNegatedMCSSetCopy());
		}
		else
		{
		    for (MCS mcs : getNegatedMCSSet())
		    {
			mcs.mergeMCSSet(gs.getNegatedMCSSetCopy());
		    }
		}
	    }
	    if (SettingsUtility.isAlwaysRefineFully())
	    {
		combineAllMergedMCS();
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ORSet Description:<br>
     * Adds the Probability of all Nodes and GateSets to an ArrayList.<br>
     * All Probabilities are negated (1 - p) and multiplied with the {@code result} which initially has the value {@code 1}.<br>
     * Then {@code 1 - result} is returned.<br>
     * If the ArrayList is empty then {@code 0} is returned.<br>
     */
    @Override
    public double calculateBasicFailureProbability()
    {
	ArrayList<Double> allprops = getNodeProbabilities();
	for (GateSet gateSet : lowerGateSets)
	{
	    allprops.add(gateSet.calculateBasicFailureProbability());
	}
	double result = 1;
	for (int i = 0; i < allprops.size(); i++)
	{
	    result *= (1 - allprops.get(i));
	}
	return (1 - result);
    }

    @Override
    public GateSet getGateSetCopy()
    {
	return new ORSet(this);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ORSet Description:<br>
     * Returns {@code 2}.<br>
     */
    @Override
    public int getGateType()
    {
	return 2;
    }

}
