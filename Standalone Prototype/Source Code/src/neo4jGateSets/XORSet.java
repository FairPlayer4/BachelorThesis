package neo4jGateSets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Node;

import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSUtility;
import neo4jUtility.SettingsUtility;
import neo4jUtility.ThreadUtility;

/**
 * The XORSet is a GateSet for a Node that represents a XOR Gate in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class XORSet extends AbstractGateSet
{

    /**
     * Constructor for XORSet.<br>
     * 
     * @param startElement
     *            The Node to which the XORSet belongs to.
     */
    public XORSet(Node startElement)
    {
	super(startElement);
    }

    private XORSet(XORSet xorSet)
    {
	super(xorSet);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * XORSet Description:<br>
     * First the HashSet of MCS and the HashSet of negated MCS of all lower GateSets are created.<br>
     * An ArrayList of MCS HashSets is created which is used to store each individual MCS / MCS sets from each Node / GateSet.<br>
     * Then a second ArrayList of MCS HashSets is created which is used to store each individual negated MCS / negated MCS set from each Node / GateSet.<br>
     * The second list has the same order as the first list.<br>
     * After both lists are created the creation of the final MCS set begins.<br>
     * Each MCS set in the first list is merged with all MCS sets in the second list except if the MCS set is the equal and just negated.<br>
     * At the end the final MCS set contains every MCS so that the MCS only evaluates true if exactly one Node or GateSet evaluates true.<br>
     * The implementation makes use of multithreading because each set can be negated individually and also the combination of MCS sets can also be done
     * individually.<br>
     * 
     * @see MCS
     */
    @Override
    public void createMCSSet()
    {
	if (getMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    createNegatedMCSSetInLowerGateSets();
	    ArrayList<Node> orderedNodes = new ArrayList<Node>(nodeSet);
	    ArrayList<GateSet> orderedGateSets = new ArrayList<GateSet>(lowerGateSets);
	    ArrayList<HashSet<MCS>> mcsSetList = new ArrayList<HashSet<MCS>>();
	    for (Node node : orderedNodes)
	    {
		HashSet<MCS> newMCSSet = new HashSet<MCS>();
		newMCSSet.add(new MCS(new MCSNode(node, false)));
		mcsSetList.add(newMCSSet);
	    }
	    for (GateSet gateSet : orderedGateSets)
	    {
		mcsSetList.add(gateSet.getMCSSet());
	    }
	    ArrayList<HashSet<MCS>> negatedMCSSetList = new ArrayList<HashSet<MCS>>();
	    for (Node node : orderedNodes)
	    {
		HashSet<MCS> newMCSSet = new HashSet<MCS>();
		MCS negatedMCS = new MCS(new MCSNode(node, true));
		newMCSSet.add(negatedMCS);
		negatedMCSSetList.add(newMCSSet);
	    }
	    for (GateSet gateSet : orderedGateSets)
	    {
		negatedMCSSetList.add(gateSet.getNegatedMCSSet());
	    }
	    if (ThreadUtility.getThreadUtility().isMultiThreading()) // Multithreading
	    {
		Set<MCS> finalSet = ConcurrentHashMap.newKeySet();
		Set<Future<?>> futureSet = new HashSet<Future<?>>();
		for (int i = 0; i < mcsSetList.size(); i++)
		{
		    final int fi = i;
		    if (mcsSetList.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
		    {
			futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				HashSet<MCS> nextMCSSet = MCSUtility.copyMCSSet(mcsSetList.get(fi));
				for (MCS nextMCS : nextMCSSet)
				{
				    for (int j = 0; j < negatedMCSSetList.size(); j++)
				    {
					if (j != fi)
					{
					    nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(negatedMCSSetList.get(j)));
					}
				    }
				}
				finalSet.addAll(nextMCSSet);
			    }
			}));
		    }
		    else
		    {
			HashSet<MCS> nextMCSSet = MCSUtility.copyMCSSet(mcsSetList.get(fi));
			for (MCS nextMCS : nextMCSSet)
			{
			    for (int j = 0; j < negatedMCSSetList.size(); j++)
			    {
				if (j != fi)
				{
				    nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(negatedMCSSetList.get(j)));
				}
			    }
			}
			finalSet.addAll(nextMCSSet);
		    }
		    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
		    getMCSSet().addAll(finalSet);
		}
	    }
	    else // No Multithreading
	    {
		for (int i = 0; i < mcsSetList.size(); i++)
		{
		    HashSet<MCS> nextMCSSet = MCSUtility.copyMCSSet(mcsSetList.get(i));
		    for (MCS nextMCS : nextMCSSet)
		    {
			for (int j = 0; j < negatedMCSSetList.size(); j++)
			{
			    if (j != i)
			    {
				nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(negatedMCSSetList.get(j)));
			    }
			}
		    }
		    getMCSSet().addAll(nextMCSSet);
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
     * XORSet Description:<br>
     * First the HashSet of MCS and the HashSet of negated MCS of all lower GateSets are created.<br>
     * An ArrayList of MCS HashSets is created which is used to store each individual MCS / MCS sets from each Node / GateSet.<br>
     * Then a second ArrayList of MCS HashSets is created which is used to store each individual negated MCS / negated MCS set from each Node / GateSet.<br>
     * The second list has the same order as the first list.<br>
     * After both lists are created the creation of the final MCS set begins.<br>
     * Each pair of MCS sets in the first list is merged.<br>
     * All MCS sets in the second list are merged together to one MCS set.
     * At the end the final MCS set contains every MCS so that the MCS only evaluates true if a pair of Nodes or GateSets or a Node and GateSet evaluates true.<br>
     * Also evaluates to true if all GateSets and Node are false.
     * The implementation makes use of multithreading because each set can be negated individually and also the combination of MCS sets can also be done
     * individually.<br>
     * 
     * @see MCS
     */
    @Override
    public void createNegatedMCSSet()
    {
	if (getNegatedMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    createNegatedMCSSetInLowerGateSets();
	    ArrayList<HashSet<MCS>> mcsSetList = new ArrayList<HashSet<MCS>>();
	    for (Node node : nodeSet)
	    {
		HashSet<MCS> newMCSSet = new HashSet<MCS>();
		newMCSSet.add(new MCS(new MCSNode(node, false)));
		mcsSetList.add(newMCSSet);
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		mcsSetList.add(gateSet.getMCSSet());
	    }
	    ArrayList<HashSet<MCS>> negatedMCSSetList = new ArrayList<HashSet<MCS>>();
	    for (Node node : nodeSet)
	    {
		HashSet<MCS> newMCSSet = new HashSet<MCS>();
		MCS negatedMCS = new MCS(new MCSNode(node, true));
		newMCSSet.add(negatedMCS);
		negatedMCSSetList.add(newMCSSet);
	    }
	    for (GateSet gateSet : lowerGateSets)
	    {
		negatedMCSSetList.add(gateSet.getNegatedMCSSet());
	    }
	    if (ThreadUtility.getThreadUtility().isMultiThreading()) // Multithreading
	    {
		Set<MCS> finalSet = ConcurrentHashMap.newKeySet();
		Set<Future<?>> futureSet = new HashSet<Future<?>>();
		for (int i = 0; i < mcsSetList.size(); i++)
		{
		    final int fi = i;
		    if (mcsSetList.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
		    {
			futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				for (int j = fi + 1; j < mcsSetList.size(); j++) // all pairs
				{
				    HashSet<MCS> nextMCSSet = MCSUtility.copyMCSSet(mcsSetList.get(fi));
				    for (MCS nextMCS : nextMCSSet)
				    {
					nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(mcsSetList.get(j)));
				    }
				    finalSet.addAll(nextMCSSet);
				}
			    }
			}));
		    }
		    else
		    {
			for (int j = fi + 1; j < mcsSetList.size(); j++) // all pairs
			{
			    HashSet<MCS> nextMCSSet = MCSUtility.copyMCSSet(mcsSetList.get(fi));
			    for (MCS nextMCS : nextMCSSet)
			    {
				nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(mcsSetList.get(j)));
			    }
			    finalSet.addAll(nextMCSSet);
			}
		    }
		}
		ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
		getNegatedMCSSet().addAll(finalSet);
		HashSet<MCS> nextMCSSet = null;
		for (int i = 0; i < negatedMCSSetList.size(); i++)
		{
		    if (nextMCSSet == null)
		    {
			nextMCSSet = MCSUtility.copyMCSSet(negatedMCSSetList.get(i));
		    }
		    else
		    {
			for (MCS nextMCS : nextMCSSet)
			{
			    nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(negatedMCSSetList.get(i)));
			}
		    }
		}
		if (nextMCSSet != null)
		{
		    getNegatedMCSSet().addAll(nextMCSSet);
		}
	    }
	    else // No Multithreading
	    {
		for (int i = 0; i < mcsSetList.size(); i++)
		{
		    for (int j = i + 1; j < mcsSetList.size(); j++) // all pairs
		    {
			HashSet<MCS> nextMCSSet = MCSUtility.copyMCSSet(mcsSetList.get(i));
			for (MCS nextMCS : nextMCSSet)
			{
			    nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(mcsSetList.get(j)));
			}
			getNegatedMCSSet().addAll(nextMCSSet);
		    }
		}
		HashSet<MCS> nextMCSSet = null;
		for (int i = 0; i < negatedMCSSetList.size(); i++)
		{
		    if (nextMCSSet == null)
		    {
			nextMCSSet = MCSUtility.copyMCSSet(negatedMCSSetList.get(i));
		    }
		    else
		    {
			for (MCS nextMCS : nextMCSSet)
			{
			    nextMCS.mergeMCSSet(MCSUtility.copyMCSSet(negatedMCSSetList.get(i)));
			}
		    }
		}
		if (nextMCSSet != null)
		{
		    getNegatedMCSSet().addAll(nextMCSSet);
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
     * XORSet Description:<br>
     * Adds the probability of all Nodes and GateSets to an ArrayList.<br>
     * Each member of the ArrayList is multiplied with the negated probability of all other members in the ArrayList.<br>
     * Each result is added to the final {@code result} which has an initial value of {@code 1}.<br>
     * If the ArrayList is empty then {@code 0} is returned.<br>
     */
    @Override
    public double calculateBasicFailureProbability()
    {
	double result = 0;
	ArrayList<Double> allprops = getNodeProbabilities();
	for (GateSet gateSet : lowerGateSets)
	{
	    allprops.add(gateSet.calculateBasicFailureProbability());
	}
	for (int i = 0; i < allprops.size(); i++)
	{
	    double res = allprops.get(i);
	    for (int j = 0; j < allprops.size(); j++)
	    {
		if (i != j)
		{
		    res *= (1 - allprops.get(j));
		}
	    }
	    result += res;
	}
	return result;
    }

    @Override
    public GateSet getGateSetCopy()
    {
	return new XORSet(this);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * XORSet Description:<br>
     * Returns {@code 4}.<br>
     */
    @Override
    public int getGateType()
    {
	return 4;
    }

}
