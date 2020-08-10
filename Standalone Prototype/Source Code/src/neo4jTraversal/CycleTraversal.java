package neo4jTraversal;

import java.util.HashSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import neo4jDatabase.DBConnection;
import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;

/**
 * This class is used to check for cycles and errors in a CFT.
 * @author Kevin Bartik
 *
 */
public final class CycleTraversal
{
    /**
     * The inports of the CFT.
     */
    private static HashSet<Node> inports;

    /**
     * Is true if a cycle was found.
     */
    private static boolean cycleFound = false;

    /**
     * Is true if an error was found.
     */
    private static boolean errorFound = false;

    private CycleTraversal()
    {

    }

    /**
     * Checks if the CFT of an outport has cycles or errors.
     * @param outport The outport.
     * @return true if the CFT of the outport has cycles or errors. Otherwise false.
     */
    static boolean hasCycleOrErrors(Node outport)
    {
	cycleFound = false;
	errorFound = false;
	inports = DBUtility.getElementsbyLabelandParent(NodeLabels.CFT_Inport, DBUtility.getCFTofNode(outport));
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    InitialBranchState.State<TraversalState> ibs;
	    ibs = new InitialBranchState.State<TraversalState>(null, null);
	    TraversalDescription cycleTD = DBConnection.getGraphDB().traversalDescription().uniqueness(Uniqueness.NONE).depthFirst().expand(new CyclePathExpander(), ibs);
	    Traverser cycleTraverser = cycleTD.traverse(outport);
	    cycleTraverser.iterator().forEachRemaining(path ->
	    {

	    });
	    tx.success();
	}
	return cycleFound || errorFound;
    }

    /**
     * Sets cycleFound to true if the parameter is true.
     * @param cycle The parameter.
     */
    static void cycleFound(boolean cycle)
    {
	if (cycle)
	{
	    cycleFound = true;
	}
    }

    /**
     * Sets errorFound to true if the parameter is true.
     * @param cycle The parameter.
     */
    static void errorFound(boolean error)
    {
	if (error)
	{
	    errorFound = true;
	}
    }

    /**
     * Returns cycleFound.
     * @return cycleFound.
     */
    static boolean cycleFound()
    {
	return cycleFound;
    }

    /**
     * Returns errorFound.
     * @return errorFound.
     */
    static boolean errorFound()
    {
	return errorFound;
    }

    /**
     * Returns inports.
     * @return inports.
     */
    static HashSet<Node> getInports()
    {
	return inports;
    }

}
