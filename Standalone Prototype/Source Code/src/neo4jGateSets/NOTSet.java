package neo4jGateSets;

import java.util.ArrayList;

import org.neo4j.graphdb.Node;

import neo4jMCS.MCS;
import neo4jMCS.MCSNode;

/**
 * The NOTSet is a GateSet for a Node that represents a NOT Gate in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class NOTSet extends AbstractGateSet
{

    /**
     * Constructor for NOTSet.<br>
     * 
     * @param startElement
     *            The Node to which the NOTSet belongs to.
     */
    public NOTSet(Node startElement)
    {
	super(startElement);
    }

    private NOTSet(NOTSet notSet)
    {
	super(notSet);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * NOTSet Description:<br>
     * Creates the HashSet of MCS.
     * 
     * @see MCS
     */
    @Override
    public void createMCSSet()
    {
	if (getMCSSet().isEmpty())
	{
	    createNegatedMCSSetInLowerGateSets();
	    for (Node node : nodeSet)
	    {
		MCS newMCS = new MCS(new MCSNode(node, true));
		getMCSSet().add(newMCS);
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		getMCSSet().addAll(gateSet.getNegatedMCSSetCopy());
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * NOTSet Description:<br>
     * Creates the HashSet of negated MCS.
     * 
     * @see MCS
     */
    @Override
    public void createNegatedMCSSet()
    {
	if (getNegatedMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    for (Node node : nodeSet)
	    {
		getNegatedMCSSet().add(new MCS(new MCSNode(node, false)));
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		getNegatedMCSSet().addAll(gateSet.getMCSSetCopy());
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * NOTSet Description:<br>
     * Adds the Probability of all Nodes and GateSets to an ArrayList.<br>
     * All Probabilities multiplied with the {@code result} which initially has the value {@code 1}.<br>
     * Then {@code 1 - result} is returned.<br>
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
	for (int i = 0; i < allprops.size(); i++)
	{
	    result *= (1 - allprops.get(i));
	}
	return result;
    }

    @Override
    public GateSet getGateSetCopy()
    {
	return new NOTSet(this);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * NOTSet Description:<br>
     * Returns {@code 3}.<br>
     */
    @Override
    public int getGateType()
    {
	return 3;
    }
}
