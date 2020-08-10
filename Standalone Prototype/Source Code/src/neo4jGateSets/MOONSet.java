package neo4jGateSets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import neo4jDatabase.DBConnection;
import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jUtility.ThreadUtility;

/**
 * The MOONSet is a GateSet for a Node that represents a M out of N or Voter Gate in a CFT Model.<br>
 * It extends GateSetAbstract and implements the remaining methods from the Interface GateSet.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class MOONSet extends AbstractGateSet
{

    /**
     * The {@code moonNumber} is the M (from M out of N).<br>
     * The N is the number of Nodes and GateSet of the MOONSet. <br>
     */
    private int moonNumber;

    /**
     * Constructor for MOONSet.<br>
     * The {@code moonNumber} is retrieved from the Neo4j database. <br>
     * If the startElement does not have the corresponding property then the {@code moonNumber} is set to {@code 1}.<br>
     * 
     * @param startElement
     *            The Node to which the MOONSet belongs to.
     */
    public MOONSet(Node startElement)
    {
	super(startElement);
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (startElement.hasProperty("MOONNumber"))
	    {
		moonNumber = Integer.parseInt(startElement.getProperty("MOONNumber").toString());
	    }
	    else
	    {
		moonNumber = 1;
	    }
	    tx.success();
	}
    }

    private MOONSet(MOONSet moonSet)
    {
	super(moonSet);
	moonNumber = moonSet.moonNumber;
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * MOONSet Description:<br>
     * Here a combination is a set of combined MCS sets.<br>
     * These MCS sets are created from each Node and each GateSet of the MOONSet and saves in an ArrayList of MCS HashSets.<br>
     * The final mcsSet contains all possible combination that have the length {@code moonNumber}.<br>
     * To save computation time all previously built combinations are stored in a HashMap.<br>
     * The keys of this HashMap are Integer arrays that store the indexes of the ArrayList of MCS HashSets.<br>
     * All combinations with 2 or more members are stored in this HashMap.<br>
     * The implementation also makes use of an index HashMap that maps the index of a combination to the index of the ArrayList of MCS HashMap.<br>
     * To create all possible combinations the index HashMap is incremented by incrementing the values.<br>
     * There are moonNumber values in the index HashMap and they are ordered, cannot be equal and the highest value is the highest index of the ArrayList of MCS
     * HashSets (size - 1).<br>
     * The values are first incremented by incrementing the the last value until it is the highest value.<br>
     * Then the previous to last value is incremented and the last value is reduced to that value + 1.<br>
     * Then the last value is incremented again until it reaches the highest value.<br>
     * Example: moonNumber = 3; ArrayList of MCS HashSets has size = 5.<br>
     * The index Map starts with the values {0, 1, 2} and the next increments would be {0, 1, 3}, {0, 1, 4}, {0, 2, 3}, {0, 2, 4}, {0, 3, 4}, {1, 2, 3}, {1, 2,
     * 4}, {1, 3, 4}, {2, 3, 4}.<br>
     * The indexMap is used for creating combinations and each completed combination is added to the mcsSet. <br>
     * In case multithreading is used the index map are are all created in advance and then the combinations can be created by multiple threads.<br>
     * If the moonNumber is higher than the number of Nodes and GateSets then a special MCS is added to the MCS Set which essentially makes the evaluation
     * always false.<br>
     * If the moonNumber is 0 or smaller then the same is done except that the MCS always evaluates true.<br>
     * The last two cases are usually user error but the Qualitative Analysis can continue.<br>
     * The MCS class will print an information message if these special MCS are created.<br>
     * 
     * @see MCS
     */
    @Override
    public void createMCSSet()
    {
	if (getMCSSet().isEmpty())
	{
	    createMCSSetInLowerGateSets();
	    if (moonNumber <= (lowerGateSets.size() + nodeSet.size()) && moonNumber > 0)
	    {
		if (!ThreadUtility.getThreadUtility().isMultiThreading()) // No Multithreading
		{
		    ArrayList<HashSet<MCS>> mcsSetList = new ArrayList<HashSet<MCS>>();
		    HashMap<int[], HashSet<MCS>> previousCombinations = new HashMap<int[], HashSet<MCS>>(); // Stores previously built combinations of MCS
													    // HashSets.
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
		    int listSize = mcsSetList.size();
		    HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(); // Maps each index of a combination to the index of mcsSetList.
		    for (int i = 0; i < moonNumber; i++)
		    {
			indexMap.put(i, i);
		    }
		    boolean lastCombination = false;
		    while (!lastCombination)
		    {
			lastCombination = true;
			for (int i = 1; i <= moonNumber; i++) // Check if the current combination in the index map is the last combination.
			{
			    if (indexMap.get(moonNumber - i) != (listSize - i))
			    {
				lastCombination = false;
				break;
			    }
			}
			HashSet<MCS> combination = mcsSetList.get(indexMap.get(0));
			for (int i = 1; i < moonNumber; i++)
			{
			    int[] prev = new int[i + 1];
			    for (int j = 0; j <= i; j++)
			    {
				prev[j] = indexMap.get(j);
			    }
			    if (!previousCombinations.containsKey(prev))
			    {
				HashSet<MCS> newCombination = new HashSet<MCS>();
				HashSet<MCS> nextSet = mcsSetList.get(indexMap.get(i));
				for (MCS mcs1 : combination)
				{
				    for (MCS mcs2 : nextSet)
				    {
					MCS newMCS = new MCS(mcs1);
					newMCS.addMCS(new MCS(mcs2));
					newCombination.add(newMCS);
				    }
				}
				previousCombinations.put(prev, newCombination);
			    }
			    combination = previousCombinations.get(prev);
			}
			getMCSSet().addAll(combination);
			if (!lastCombination)
			{ // Incrementing the indexMap.
			    for (int i = 1; i <= moonNumber; i++)
			    {
				int k = indexMap.get(moonNumber - i);
				if (k < (listSize - i))
				{
				    indexMap.put(moonNumber - i, k + 1);
				    break;
				}
				else
				{
				    int j = indexMap.get(moonNumber - i - 1);
				    if (j + 2 < k)
				    {
					indexMap.put(moonNumber - i, j + 2);
					int u = 1;
					for (int y = moonNumber - i + 1; y < moonNumber; y++)
					{
					    indexMap.put(y, j + 2 + u);
					    u++;
					}
				    }
				}
			    }
			}
		    }
		}
		else // Multithreading
		{
		    ArrayList<HashSet<MCS>> mcsSetList = new ArrayList<HashSet<MCS>>();
		    ConcurrentHashMap<int[], HashSet<MCS>> previousCombinations = new ConcurrentHashMap<int[], HashSet<MCS>>(); // Stores previously built //
																// combinations of MCS HashSets.
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
		    int listSize = mcsSetList.size();
		    ArrayList<HashMap<Integer, Integer>> combinationsList = new ArrayList<HashMap<Integer, Integer>>();
		    HashMap<Integer, Integer> firstIndexMap = new HashMap<Integer, Integer>(); // Maps each index of a combination to the index of mcsSetList.
		    for (int i = 0; i < moonNumber; i++)
		    {
			firstIndexMap.put(i, i);
		    }
		    combinationsList.add(firstIndexMap);
		    boolean lastCombination = false;
		    while (!lastCombination)
		    {
			HashMap<Integer, Integer> nextIndexMap = new HashMap<Integer, Integer>(combinationsList.get(combinationsList.size() - 1));
			lastCombination = true;
			for (int i = 1; i <= moonNumber; i++) // Check if the current combination in the index map is the last combination.
			{
			    if (nextIndexMap.get(moonNumber - i) != (listSize - i))
			    {
				lastCombination = false;
				break;
			    }
			}
			if (!lastCombination)
			{ // Incrementing the indexMap.
			    for (int i = 1; i <= moonNumber; i++)
			    {
				int k = nextIndexMap.get(moonNumber - i);
				if (k < (listSize - i))
				{
				    nextIndexMap.put(moonNumber - i, k + 1);
				    break;
				}
				else
				{
				    int j = nextIndexMap.get(moonNumber - i - 1);
				    if (j + 2 < k)
				    {
					nextIndexMap.put(moonNumber - i, j + 2);
					int u = 1;
					for (int y = moonNumber - i + 1; y < moonNumber; y++)
					{
					    nextIndexMap.put(y, j + 2 + u);
					    u++;
					}
				    }
				}
			    }
			}
			combinationsList.add(nextIndexMap);
		    }
		    Set<MCS> finalSet = ConcurrentHashMap.newKeySet();
		    Set<Future<?>> futureSet = new HashSet<Future<?>>();
		    Collections.shuffle(combinationsList); // Improves performance because previous combinations are saved.
		    // If Threads start with random index maps the chance is higher that previous combinations are reused and Threads don't create equal
		    // combinations at the same time.
		    for (HashMap<Integer, Integer> indexMap : combinationsList)
		    {
			if (combinationsList.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
			{
			    futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			    {
				@Override
				public void run()
				{
				    HashSet<MCS> combination = mcsSetList.get(indexMap.get(0));
				    for (int i = 1; i < moonNumber; i++)
				    {
					int[] prev = new int[i + 1];
					for (int j = 0; j <= i; j++)
					{
					    prev[j] = indexMap.get(j);
					}
					if (!previousCombinations.containsKey(prev))
					{
					    HashSet<MCS> newCombination = new HashSet<MCS>();
					    HashSet<MCS> nextSet = mcsSetList.get(indexMap.get(i));
					    for (MCS mcs1 : combination)
					    {
						for (MCS mcs2 : nextSet)
						{
						    MCS newMCS = new MCS(mcs1);
						    newMCS.addMCS(new MCS(mcs2));
						    newCombination.add(newMCS);
						}
					    }
					    previousCombinations.put(prev, newCombination);
					}
					combination = previousCombinations.get(prev);
				    }
				    finalSet.addAll(combination);
				}
			    }));
			}
			else
			{
			    HashSet<MCS> combination = mcsSetList.get(indexMap.get(0));
			    for (int i = 1; i < moonNumber; i++)
			    {
				int[] prev = new int[i + 1];
				for (int j = 0; j <= i; j++)
				{
				    prev[j] = indexMap.get(j);
				}
				if (!previousCombinations.containsKey(prev))
				{
				    HashSet<MCS> newCombination = new HashSet<MCS>();
				    HashSet<MCS> nextSet = mcsSetList.get(indexMap.get(i));
				    for (MCS mcs1 : combination)
				    {
					for (MCS mcs2 : nextSet)
					{
					    MCS newMCS = new MCS(mcs1);
					    newMCS.addMCS(new MCS(mcs2));
					    newCombination.add(newMCS);
					}
				    }
				    previousCombinations.put(prev, newCombination);
				}
				combination = previousCombinations.get(prev);
			    }
			    finalSet.addAll(combination);
			}
		    }
		    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
		    getMCSSet().addAll(finalSet);
		}
	    }
	    else
	    {
		if (moonNumber > 0)
		{
		    getMCSSet().add(new MCS(false));
		}
		else
		{
		    getMCSSet().add(new MCS(true));
		}
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * MOONSet Description:<br>
     * Creates HashSet of negated MCS.
     */
    @Override
    public void createNegatedMCSSet()
    {
	if (getNegatedMCSSet().isEmpty())
	{
	    createNegatedMCSSetInLowerGateSets();
	    if (moonNumber <= (lowerGateSets.size() + nodeSet.size()) && moonNumber > 0)
	    {
		int negatedMoonNumber = lowerGateSets.size() + nodeSet.size() + 1 - moonNumber;
		if (!ThreadUtility.getThreadUtility().isMultiThreading()) // No Multithreading
		{
		    ArrayList<HashSet<MCS>> mcsSetList = new ArrayList<HashSet<MCS>>();
		    HashMap<int[], HashSet<MCS>> previousCombinations = new HashMap<int[], HashSet<MCS>>(); // Stores previously built combinations of MCS
													    // HashSets.
		    for (Node node : nodeSet)
		    {
			HashSet<MCS> newMCSSet = new HashSet<MCS>();
			MCS negatedMCS = new MCS(new MCSNode(node, true));
			newMCSSet.add(negatedMCS);
			mcsSetList.add(newMCSSet);
		    }
		    for (GateSet gateSet : lowerGateSets)
		    {
			mcsSetList.add(gateSet.getNegatedMCSSet());
		    }
		    int listSize = mcsSetList.size();
		    HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(); // Maps each index of a combination to the index of mcsSetList.
		    for (int i = 0; i < negatedMoonNumber; i++)
		    {
			indexMap.put(i, i);
		    }
		    boolean lastCombination = false;
		    while (!lastCombination)
		    {

			lastCombination = true;
			for (int i = 1; i <= negatedMoonNumber; i++) // Check if the current combination in the index map is the last combination.
			{
			    if (indexMap.get(negatedMoonNumber - i) != (listSize - i))
			    {
				lastCombination = false;
				break;
			    }
			}
			HashSet<MCS> combination = mcsSetList.get(indexMap.get(0));
			for (int i = 1; i < negatedMoonNumber; i++)
			{
			    int[] prev = new int[i + 1];
			    for (int j = 0; j <= i; j++)
			    {
				prev[j] = indexMap.get(j);
			    }
			    if (!previousCombinations.containsKey(prev))
			    {
				HashSet<MCS> newCombination = new HashSet<MCS>();
				HashSet<MCS> nextSet = mcsSetList.get(indexMap.get(i));
				for (MCS mcs1 : combination)
				{
				    for (MCS mcs2 : nextSet)
				    {
					MCS newMCS = new MCS(mcs1);
					newMCS.addMCS(new MCS(mcs2));
					newCombination.add(newMCS);
				    }
				}
				previousCombinations.put(prev, newCombination);
			    }
			    combination = previousCombinations.get(prev);
			}
			getNegatedMCSSet().addAll(combination);
			if (!lastCombination)
			{ // Incrementing the indexMap.
			    for (int i = 1; i <= negatedMoonNumber; i++)
			    {
				int k = indexMap.get(negatedMoonNumber - i);
				if (k < (listSize - i))
				{
				    indexMap.put(negatedMoonNumber - i, k + 1);
				    break;
				}
				else
				{
				    int j = indexMap.get(negatedMoonNumber - i - 1);
				    if (j + 2 < k)
				    {
					indexMap.put(negatedMoonNumber - i, j + 2);
					int u = 1;
					for (int y = negatedMoonNumber - i + 1; y < negatedMoonNumber; y++)
					{
					    indexMap.put(y, j + 2 + u);
					    u++;
					}
				    }
				}
			    }
			}
		    }
		}
		else // Multithreading
		{
		    ArrayList<HashSet<MCS>> mcsSetList = new ArrayList<HashSet<MCS>>();
		    ConcurrentHashMap<int[], HashSet<MCS>> previousCombinations = new ConcurrentHashMap<int[], HashSet<MCS>>(); // Stores previously built
																// combinations of MCS HashSets.
		    for (Node node : nodeSet)
		    {
			HashSet<MCS> newMCSSet = new HashSet<MCS>();
			MCS negatedMCS = new MCS(new MCSNode(node, true));
			newMCSSet.add(negatedMCS);
			mcsSetList.add(newMCSSet);
		    }
		    for (GateSet gateSet : lowerGateSets)
		    {
			mcsSetList.add(gateSet.getNegatedMCSSet());
		    }
		    int listSize = mcsSetList.size();
		    ArrayList<HashMap<Integer, Integer>> combinationsList = new ArrayList<HashMap<Integer, Integer>>();
		    HashMap<Integer, Integer> firstIndexMap = new HashMap<Integer, Integer>(); // Maps each index of a combination to the index of mcsSetList.
		    for (int i = 0; i < negatedMoonNumber; i++)
		    {
			firstIndexMap.put(i, i);
		    }
		    combinationsList.add(firstIndexMap);
		    boolean lastCombination = false;
		    while (!lastCombination)
		    {
			HashMap<Integer, Integer> nextIndexMap = new HashMap<Integer, Integer>(combinationsList.get(combinationsList.size() - 1));
			lastCombination = true;
			for (int i = 1; i <= negatedMoonNumber; i++) // Check if the current combination in the index map is the last combination.
			{
			    if (nextIndexMap.get(negatedMoonNumber - i) != (listSize - i))
			    {
				lastCombination = false;
				break;
			    }
			}
			if (!lastCombination)
			{ // Incrementing the indexMap.
			    for (int i = 1; i <= negatedMoonNumber; i++)
			    {
				int k = nextIndexMap.get(negatedMoonNumber - i);
				if (k < (listSize - i))
				{
				    nextIndexMap.put(negatedMoonNumber - i, k + 1);
				    break;
				}
				else
				{
				    int j = nextIndexMap.get(negatedMoonNumber - i - 1);
				    if (j + 2 < k)
				    {
					nextIndexMap.put(negatedMoonNumber - i, j + 2);
					int u = 1;
					for (int y = negatedMoonNumber - i + 1; y < negatedMoonNumber; y++)
					{
					    nextIndexMap.put(y, j + 2 + u);
					    u++;
					}
				    }
				}
			    }
			}
			combinationsList.add(nextIndexMap);
		    }
		    Set<MCS> finalSet = ConcurrentHashMap.newKeySet();
		    Set<Future<?>> futureSet = new HashSet<Future<?>>();
		    Collections.shuffle(combinationsList); // Improves performance because previous combinations are saved.
		    // If Threads start with random index maps the chance is higher that previous combinations are reused and Threads don't create equal
		    // combinations at the same time.
		    for (HashMap<Integer, Integer> indexMap : combinationsList)
		    {
			if (combinationsList.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
			{
			    futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			    {
				@Override
				public void run()
				{
				    HashSet<MCS> combination = mcsSetList.get(indexMap.get(0));
				    for (int i = 1; i < negatedMoonNumber; i++)
				    {
					int[] prev = new int[i + 1];
					for (int j = 0; j <= i; j++)
					{
					    prev[j] = indexMap.get(j);
					}
					if (!previousCombinations.containsKey(prev))
					{
					    HashSet<MCS> newCombination = new HashSet<MCS>();
					    HashSet<MCS> nextSet = mcsSetList.get(indexMap.get(i));
					    for (MCS mcs1 : combination)
					    {
						for (MCS mcs2 : nextSet)
						{
						    MCS newMCS = new MCS(mcs1);
						    newMCS.addMCS(new MCS(mcs2));
						    newCombination.add(newMCS);
						}
					    }
					    previousCombinations.put(prev, newCombination);
					}
					combination = previousCombinations.get(prev);
				    }
				    finalSet.addAll(combination);
				}
			    }));
			}
			else
			{
			    HashSet<MCS> combination = mcsSetList.get(indexMap.get(0));
			    for (int i = 1; i < negatedMoonNumber; i++)
			    {
				int[] prev = new int[i + 1];
				for (int j = 0; j <= i; j++)
				{
				    prev[j] = indexMap.get(j);
				}
				if (!previousCombinations.containsKey(prev))
				{
				    HashSet<MCS> newCombination = new HashSet<MCS>();
				    HashSet<MCS> nextSet = mcsSetList.get(indexMap.get(i));
				    for (MCS mcs1 : combination)
				    {
					for (MCS mcs2 : nextSet)
					{
					    MCS newMCS = new MCS(mcs1);
					    newMCS.addMCS(new MCS(mcs2));
					    newCombination.add(newMCS);
					}
				    }
				    previousCombinations.put(prev, newCombination);
				}
				combination = previousCombinations.get(prev);
			    }
			    finalSet.addAll(combination);
			}
		    }
		    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
		    getNegatedMCSSet().addAll(finalSet);
		}
	    }
	    else
	    {
		if (moonNumber > 0)
		{
		    getNegatedMCSSet().add(new MCS(true));
		}
		else
		{
		    getNegatedMCSSet().add(new MCS(false));
		}
	    }
	}
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * MOONSet Description:<br>
     * Adds the Probability of all Nodes and GateSets to an ArrayList.<br>
     * An index map is used to create combinations.<br>
     * However in this case a combination always contains all probabilities but some are negated.<br>
     * The number of negated probabilities is the size of the list of all probabilities minus the {@code moonNumber} which is here called
     * {@code inverseMoonNumber}.<br>
     * The index map stores {@code inverseMoonNumber} indexes and otherwise works the same way as in {@code refineMCSSet()}.<br>
     * The combinations are created by multiplying probabilities from the ArrayList and negating any probabilities whose index is in the index maps values
     * before multiplication.<br>
     * Each {@code result} from a combination is added to the {@code finalResult} which has an initial value of {@code 0}.<br>
     * If the ArrayList is empty then {@code 0} is returned.<br>
     * If the {@code moonNumber} is higher than the number of Nodes and GateSets then {@code 0} is returned.<br>
     * If the {@code moonNumber} is {@code 0} or smaller then {@code 1} is returned.<br>
     */
    @Override
    public double calculateBasicFailureProbability()
    {
	if (moonNumber > (lowerGateSets.size() + nodeSet.size()))
	{
	    return 0;
	}
	if (moonNumber <= 0)
	{
	    return 1;
	}
	ArrayList<Double> listOfProbs = getNodeProbabilities();
	for (GateSet gateSet : lowerGateSets)
	{
	    listOfProbs.add(gateSet.calculateBasicFailureProbability());
	}
	int listSize = listOfProbs.size();
	if (listSize == 0)
	{
	    return 0;
	}
	int inverseMoonNumber = listSize - moonNumber;
	HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
	for (int i = 0; i < inverseMoonNumber; i++)
	{
	    indexMap.put(i, i);
	}
	ArrayList<Double> listOfCombinationProbabilities = new ArrayList<Double>();
	boolean lastCombination = false;
	while (!lastCombination)
	{
	    double probabilityOfCombination = 1;
	    for (int i = 0; i < listSize; i++)
	    {
		if (indexMap.containsValue(i))
		{
		    probabilityOfCombination *= (1 - listOfProbs.get(i));
		}
		else
		{
		    probabilityOfCombination *= listOfProbs.get(i);
		}
	    }
	    listOfCombinationProbabilities.add(probabilityOfCombination);
	    lastCombination = true;
	    for (int i = 1; i <= inverseMoonNumber; i++)
	    {
		if (indexMap.get(inverseMoonNumber - i) != (listSize - i))
		{
		    lastCombination = false;
		    break;
		}
	    }
	    if (!lastCombination)
	    {
		for (int i = 1; i <= inverseMoonNumber; i++)
		{
		    int k = indexMap.get(inverseMoonNumber - i);
		    if (k < (listSize - i))
		    {
			indexMap.put(inverseMoonNumber - i, k + 1);
			break;
		    }
		    else
		    {
			int j = indexMap.get(inverseMoonNumber - i - 1);
			if (j + 2 < k)
			{
			    indexMap.put(inverseMoonNumber - i, j + 2);
			    int u = 1;
			    for (int y = inverseMoonNumber - i + 1; y < inverseMoonNumber; y++)
			    {
				indexMap.put(y, j + 2 + u);
				u++;
			    }
			}
		    }
		}
	    }
	}
	double finalResult = 0;
	for (double result : listOfCombinationProbabilities)
	{
	    finalResult += result;
	}
	return finalResult;
    }

    @Override
    public GateSet getGateSetCopy()
    {
	return new MOONSet(this);
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * MOONSet Description:<br>
     * Returns {@code 5}.<br>
     */
    @Override
    public int getGateType()
    {
	return 5;
    }

}
