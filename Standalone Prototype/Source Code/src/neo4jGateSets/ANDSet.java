package neo4jGateSets;

import java.util.ArrayList;

import org.neo4j.graphdb.Node;

import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSUtility;
import neo4jUtility.SettingsUtility;

/**
 * The ANDSet is a GateSet for a Node that represents an AND Gate in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class ANDSet extends AbstractGateSet
{

    /**
     * Constructor for ANDSet.<br>
     * 
     * @param startElement
     *            The Node to which the ANDSet belongs to.
     */
    public ANDSet(Node startElement)
    {
	super(startElement);
    }

    private ANDSet(ANDSet andSet)
    {
	super(andSet);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ANDSet Description:<br>
     * Creates the HashSet of MCS.
     * 
     * @see MCS
     */
    @Override
    public void createMCSSet()
    {
	if (getMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    if (!nodeSet.isEmpty())
	    {
		getMCSSet().add(new MCS(MCSUtility.getMCSNodeSet(nodeSet)));
	    }
	    for (GateSet gs : lowerGateSets)
	    {
		if (getMCSSet().isEmpty())
		{
		    getMCSSet().addAll(gs.getMCSSetCopy());
		}
		else
		{
		    for (MCS mcs : getMCSSet())
		    {
			mcs.mergeMCSSet(gs.getMCSSetCopy());
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
     * ANDSet Description:<br>
     * Creates the HashSet of negated MCS.<br>
     * 
     * @see MCS
     */
    @Override
    public void createNegatedMCSSet()
    {
	if (getNegatedMCSSet().isEmpty())
	{
	    createNegatedMCSSetInLowerGateSets();
	    for (Node node : nodeSet)
	    {
		MCS negatedMCS = new MCS(new MCSNode(node, true));
		getNegatedMCSSet().add(negatedMCS);
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		getNegatedMCSSet().addAll(gateSet.getNegatedMCSSetCopy());
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * ANDSet Description:<br>
     * Adds the Probability of all Nodes and GateSets to an ArrayList.<br>
     * Each member of the ArrayList is multiplied with {@code result} which starts with the initial value of {@code 1}.<br>
     * If the ArrayList is empty then {@code 0} is returned.<br>
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
    public GateSet getGateSetCopy()
    {
	return new ANDSet(this);
    }
    
    /**
     * {@inheritDoc}<br>
     * <br>
     * ANDSet Description:<br>
     * Returns {@code 1}.<br>
     */
    @Override
    public int getGateType()
    {
	return 1;
    }

}
