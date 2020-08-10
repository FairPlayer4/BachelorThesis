package neo4jTraversal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBUtility;
import neo4jDatabase.DBMCSManager;
import neo4jEnum.NodeLabels;
import neo4jGateSets.ResultGateSet;
import neo4jMCS.MCSPairSet;
import neo4jUtility.PrintUtility;
import neo4jUtility.SettingsUtility;

/**
 * 
 * @author Kevin Bartik
 *
 */
public final class MainTraversal
{

    /**
     * Is true if the prime implicants are calculated.<br>
     * Otherwise false.<br>
     */
    private static boolean calculatePrimeImplicants = false;

    /**
     * The final result of the qualitative analysis.<br>
     */
    private static ResultGateSet qualititativeAnalysisResults;

    /**
     * The final result of the quantitative analysis.<br>
     */
    private static double quantitativeAnalysisResults = -1;

    /**
     * Maps Nodes with ResultGateSets.<br>
     * A Node key is the startNode of the ResultGateSet.<br>
     */
    private static HashMap<Node, ResultGateSet> resultGateSets = new HashMap<Node, ResultGateSet>();

    /**
     * The outport that is analyzed and where traversal starts.<br>
     */
    private static Node outport;

    /**
     * The inports of the parent CFT of the outport.<br>
     */
    private static HashSet<Node> inports;

    /**
     * The amount of time that was needed to perform the traversal.<br>
     * Time in milliseconds. <br>
     */
    private static long timeTraversal = -1;

    /**
     * The amount of time that was needed to perform the minimal cut set calculation.<br>
     * Time in milliseconds. <br>
     */
    private static long timeRefineMCS = -1;

    /**
     * The amount of time that was needed to perform the calculation of the prime implicants.<br>
     * Time in milliseconds. <br>
     */
    private static long timePrimeImplicants = -1;

    /**
     * The amount of time that was needed to perform the quantitative analysis.<br>
     * Time in milliseconds. <br>
     */
    private static long timeQuantAnalysis = -1;

    /**
     * Is true is manual traversal is used.<br>
     * Otherwise false.<br>
     */
    private static boolean useManualTraversal = false;

    /**
     * Is true if the traversal is split and the analysis results are reused.<br>
     * Otherwise false.<br>
     */
    private static boolean splitTraversalAndReuseAnalysisResults = false;

    /**
     * Is true if a cycle found during traversal.<br>
     * Otherwise false.<br>
     */
    private static boolean cycleFound = false;

    /**
     * Is true if an error occurred during traversal or analysis.<br>
     * Otherwise false.<br>
     */
    private static boolean errorFound = false;

    /**
     * A lock to prevent multiple threads from adding ResultGateSets concurrently.
     */
    private static final Object resultGateSetsLock = new Object();

    private MainTraversal()
    {

    }

    public static void qualitativeAnalysis(Node _outport)
    {

	performTraversal(_outport);
	if (!errorFound)
	{
	    PrintUtility.printInfo("Minimal Cut Set Analysis started!");
	    if (qualititativeAnalysisResults == null)
	    {
		if (splitTraversalAndReuseAnalysisResults)
		{
		    // Always calculate Prime Implicants
		    long startRefineMCS = System.nanoTime();
		    MCSPairSet outportFullMCSSet = DBMCSManager.getFullMCSSet(outport);
		    if (!outportFullMCSSet.isEmpty())
		    {
			getResultGateSet(outport).SetFullMCSSet(outportFullMCSSet.getMCSSet());
			getResultGateSet(outport).SetFullNegatedMCSSet(outportFullMCSSet.getNegatedMCSSet());
			qualititativeAnalysisResults = resultGateSets.get(outport);
			long endRefineMCS = System.nanoTime();
			timeRefineMCS = (endRefineMCS - startRefineMCS) / 1000000;
		    }
		    else
		    { // calculation necessary
			for (ResultGateSet rgs : resultGateSets.values())
			{
			    MCSPairSet mcsSet = DBMCSManager.getMCSSet(rgs.getStartNode());
			    if (mcsSet.isEmpty())
			    {
				rgs.createMCSSet();
				rgs.createNegatedMCSSet();
				rgs.combineAllMergedMCS();
				rgs.minimizeMCS();
				rgs.addHiddenPrimeImplicants();
				rgs.minimizeMCS();
				if (!errorFound)
				{
				    DBMCSManager.addMCSSet(rgs);
				}
			    }
			    else
			    {
				rgs.SetMCSSet(mcsSet.getMCSSet());
				rgs.SetNegatedMCSSet(mcsSet.getNegatedMCSSet());
			    }
			    MCSPairSet fullMCSSet = DBMCSManager.getFullMCSSet(rgs.getStartNode());
			    if (!fullMCSSet.isEmpty())
			    {
				rgs.SetFullMCSSet(fullMCSSet.getMCSSet());
				rgs.SetFullNegatedMCSSet(fullMCSSet.getNegatedMCSSet());
			    }
			}
			qualititativeAnalysisResults = (ResultGateSet) resultGateSets.get(outport).getGateSetCopy();
			qualititativeAnalysisResults.connectGateSets(null);
			qualititativeAnalysisResults.connectMCS();
			qualititativeAnalysisResults.minimizeMCS();
			long endRefineMCS = System.nanoTime();
			timeRefineMCS = (endRefineMCS - startRefineMCS) / 1000000;
			long startPrimeImplicants = System.nanoTime();
			qualititativeAnalysisResults.addHiddenPrimeImplicants();
			qualititativeAnalysisResults.minimizeMCS();
			long endPrimeImplicants = System.nanoTime();
			timePrimeImplicants = (endPrimeImplicants - startPrimeImplicants) / 1000000;
		    }
		}
		else
		{
		    long startRefineMCS = System.nanoTime();
		    qualititativeAnalysisResults = (ResultGateSet) resultGateSets.get(outport).getGateSetCopy();
		    qualititativeAnalysisResults.createMCSSet();
		    qualititativeAnalysisResults.combineAllMergedMCS();
		    qualititativeAnalysisResults.minimizeMCS();
		    long endRefineMCS = System.nanoTime();
		    timeRefineMCS = (endRefineMCS - startRefineMCS) / 1000000;
		    if (calculatePrimeImplicants)
		    {
			long startPrimeImplicants = System.nanoTime();
			qualititativeAnalysisResults.addHiddenPrimeImplicants();
			qualititativeAnalysisResults.minimizeMCS();
			long endPrimeImplicants = System.nanoTime();
			timePrimeImplicants = (endPrimeImplicants - startPrimeImplicants) / 1000000;
		    }
		}
		PrintUtility.printInfo("Minimal Cut Set Analysis finished!");
	    }
	    printPerformanceStatistics();
	}
    }

    public static void quantitativeAnalysis(Node _outport)
    {
	performTraversal(_outport);
	if (!errorFound)
	{
	    if (quantitativeAnalysisResults == -1)
	    {
		long start = System.nanoTime();
		if (splitTraversalAndReuseAnalysisResults)
		{
		    quantitativeAnalysisResults = DBMCSManager.getQuantitativeResult(resultGateSets.get(outport).getStartNode());
		    if (quantitativeAnalysisResults == -1)
		    {
			ResultGateSet resultGateSet = (ResultGateSet) resultGateSets.get(outport).getGateSetCopy();
			resultGateSet.connectGateSets(null);
			quantitativeAnalysisResults = resultGateSet.calculateBasicFailureProbability();
			if (!errorFound)
			{
			    DBMCSManager.addQuantitativeResult(resultGateSet.getStartNode(), quantitativeAnalysisResults);
			}
		    }
		}
		else
		{
		    quantitativeAnalysisResults = resultGateSets.get(outport).calculateBasicFailureProbability();
		}
		long end = System.nanoTime();
		timeQuantAnalysis = (end - start) / 1000000;
	    }
	    PrintUtility.printResults("The Probability of the Occurence of an Event at the Element (" + DBUtility.getIDAndName(outport) + " [OUT]) is " + quantitativeAnalysisResults + ".\n");
	    printPerformanceStatistics();
	}
    }

    public static void performFullAnalysisAndStoreResults()
    {
	HashSet<Node> outports = DBUtility.getAllNodesWithLabel(NodeLabels.CFT_Outport);
	for (Node out : outports)
	{
	    if (!CycleTraversal.hasCycleOrErrors(out))
	    {
		fullAnalysis(out);
	    }
	}
    }

    private static void fullAnalysis(Node _outport)
    {
	boolean neo4j = SettingsUtility.isUseManualTraversal();
	boolean reuse = SettingsUtility.isReuseAnalysisResults();
	SettingsUtility.setUseManualTraversal(false);
	SettingsUtility.setReuseAnalysisResults(true);
	boolean recalc = false;
	performTraversal(_outport);
	long startRefineMCS = System.nanoTime();
	MCSPairSet outportFullMCSSet = DBMCSManager.getFullMCSSet(outport);
	if (!outportFullMCSSet.isEmpty())
	{
	    getResultGateSet(outport).SetFullMCSSet(outportFullMCSSet.getMCSSet());
	    getResultGateSet(outport).SetFullNegatedMCSSet(outportFullMCSSet.getNegatedMCSSet());
	    qualititativeAnalysisResults = resultGateSets.get(outport);
	    long endRefineMCS = System.nanoTime();
	    timeRefineMCS = (endRefineMCS - startRefineMCS) / 1000000;
	}
	else
	{ // calculation necessary
	    for (ResultGateSet rgs : resultGateSets.values())
	    {
		MCSPairSet mcsSet = DBMCSManager.getMCSSet(rgs.getStartNode());
		if (mcsSet.isEmpty())
		{
		    rgs.createMCSSet();
		    rgs.createNegatedMCSSet();
		    rgs.combineAllMergedMCS();
		    rgs.minimizeMCS();
		    rgs.addHiddenPrimeImplicants();
		    rgs.minimizeMCS();
		    if (!errorFound)
		    {
			DBMCSManager.addMCSSet(rgs);
		    }
		}
		else
		{
		    rgs.SetMCSSet(mcsSet.getMCSSet());
		    rgs.SetNegatedMCSSet(mcsSet.getNegatedMCSSet());
		}
		MCSPairSet fullMCSSet = DBMCSManager.getFullMCSSet(rgs.getStartNode());
		if (!fullMCSSet.isEmpty())
		{
		    rgs.SetFullMCSSet(fullMCSSet.getMCSSet());
		    rgs.SetFullNegatedMCSSet(fullMCSSet.getNegatedMCSSet());
		}
	    }
	    qualititativeAnalysisResults = (ResultGateSet) resultGateSets.get(outport).getGateSetCopy();
	    qualititativeAnalysisResults.connectGateSets(null);
	    PrintUtility.printInfo(qualititativeAnalysisResults.getGateSetString(""));
	    qualititativeAnalysisResults.connectMCS();
	    qualititativeAnalysisResults.minimizeMCS();
	    long endRefineMCS = System.nanoTime();
	    timeRefineMCS = (endRefineMCS - startRefineMCS) / 1000000;
	    long startPrimeImplicants = System.nanoTime();
	    qualititativeAnalysisResults.addHiddenPrimeImplicants();
	    qualititativeAnalysisResults.minimizeMCS();
	    long endPrimeImplicants = System.nanoTime();
	    timePrimeImplicants = (endPrimeImplicants - startPrimeImplicants) / 1000000;
	    recalc = true;
	}
	quantitativeAnalysisResults = DBMCSManager.getQuantitativeResult(resultGateSets.get(outport).getStartNode());
	if (quantitativeAnalysisResults == -1)
	{
	    if (recalc)
	    {
		quantitativeAnalysisResults = qualititativeAnalysisResults.calculateBasicFailureProbability();
		if (!errorFound)
		{
		    DBMCSManager.addQuantitativeResult(qualititativeAnalysisResults.getStartNode(), quantitativeAnalysisResults);
		}
	    }
	    else
	    {
		ResultGateSet resultGateSet = (ResultGateSet) resultGateSets.get(outport).getGateSetCopy();
		resultGateSet.connectGateSets(null);
		quantitativeAnalysisResults = resultGateSet.calculateBasicFailureProbability();
		if (!errorFound)
		{
		    DBMCSManager.addQuantitativeResult(resultGateSet.getStartNode(), quantitativeAnalysisResults);
		}
	    }
	}
	SettingsUtility.setUseManualTraversal(neo4j);
	SettingsUtility.setReuseAnalysisResults(reuse);
    }

    static void addResultGateSet(Node node, ResultGateSet rgs)
    {
	synchronized (resultGateSetsLock)
	{
	    resultGateSets.put(node, rgs);
	}
    }

    public static Collection<ResultGateSet> getResultGateSets()
    {
	return resultGateSets.values();
    }

    public static ResultGateSet getResultGateSet(Node node)
    {
	return resultGateSets.get(node);
    }

    static boolean resultGateSetNodeTraversed(Node node)
    {
	synchronized (resultGateSetsLock)
	{
	    return resultGateSets.containsKey(node);
	}
    }

    public static Node getOutport()
    {
	return outport;
    }

    public static HashSet<Node> getInports()
    {
	return inports;
    }

    public static long getTraversalTime()
    {
	return timeTraversal;
    }

    public static long getMCSCalculationTime()
    {
	return timeRefineMCS;
    }

    public static long getPrimeImplicantCalcTime()
    {
	return timePrimeImplicants;
    }

    public static long getQuantTime()
    {
	return timeQuantAnalysis;
    }

    public static ResultGateSet getQualititativeAnalysisResults()
    {
	return qualititativeAnalysisResults;
    }

    public static double getQuantitativeAnalysisResults()
    {
	return quantitativeAnalysisResults;
    }

    /**
     * 
     */
    public static void errorOccured()
    {
	errorFound = true;
    }

    public static boolean errorFound()
    {
	return errorFound;
    }

    static void cycleFound(boolean found)
    {
	if (found)
	{
	    cycleFound = found;
	    PrintUtility.printError("A Cycle was found and the Traversal cannot continue!");
	}
    }

    static boolean cycleFound()
    {
	return cycleFound;
    }

    private static void performTraversal(Node _outport)
    {
	if (DBUtility.hasLabel(_outport, NodeLabels.CFT_Outport))
	{
	    if (SettingsUtility.isUseManualTraversal() != useManualTraversal || splitTraversalAndReuseAnalysisResults != SettingsUtility.isReuseAnalysisResults() || outport == null || inports == null
		    || resultGateSets.isEmpty() || timeTraversal == -1 || SettingsUtility.isResetTraversal() || !outport.equals(_outport))
	    {
		outport = null;
		inports = null;
		cycleFound = false;
		errorFound = false;
		resultGateSets.clear();
		timeTraversal = -1;
		timeRefineMCS = -1;
		timePrimeImplicants = -1;
		timeQuantAnalysis = -1;
		useManualTraversal = SettingsUtility.isUseManualTraversal();
		splitTraversalAndReuseAnalysisResults = SettingsUtility.isReuseAnalysisResults();
		calculatePrimeImplicants = SettingsUtility.isCalculatePrimeImplicants();
		quantitativeAnalysisResults = -1;
		qualititativeAnalysisResults = null;
		outport = _outport;
		inports = DBUtility.getElementsbyLabelandParent(NodeLabels.CFT_Inport, DBUtility.getCFTofNode(outport));
		if (useManualTraversal)
		{
		    PrintUtility.printInfo("Manual Traversal started!");
		    long startTrav = System.nanoTime();
		    ManualTraversal.startManualTraversal();
		    long endTrav = System.nanoTime();
		    timeTraversal = (endTrav - startTrav) / 1000000;
		    PrintUtility.printInfo("Manual Traversal completed!");
		}
		else
		{
		    PrintUtility.printInfo("Neo4j Traversal started!");
		    long startTrav = System.nanoTime();
		    Neo4jTraversal.startTraversal(splitTraversalAndReuseAnalysisResults);
		    long endTrav = System.nanoTime();
		    timeTraversal = (endTrav - startTrav) / 1000000;
		    PrintUtility.printInfo("Neo4j Traversal completed!");
		}
	    }
	}
	else
	{
	    PrintUtility.printError("The Analysis Element is not a CFT Outport!");
	}
    }

    private static void printPerformanceStatistics()
    {
	PrintUtility.printResults("Traversal time: " + timeTraversal + "ms");
	if (qualititativeAnalysisResults != null)
	{
	    PrintUtility.printResults("Qualitative Analysis (Minimal Cut Set) Calculation time: " + timeRefineMCS + " ms");
	}
	if (timePrimeImplicants != -1)
	{
	    PrintUtility.printResults("Prime Implicants Calculation time: " + timePrimeImplicants + " ms");
	}
	if (quantitativeAnalysisResults != -1)
	{
	    PrintUtility.printResults("Quantitative Analysis (Probability) Calculation time: " + timeQuantAnalysis + " ms");
	}
    }

}
