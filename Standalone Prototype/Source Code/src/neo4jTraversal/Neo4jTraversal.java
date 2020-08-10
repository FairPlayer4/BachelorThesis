package neo4jTraversal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import neo4jDatabase.DBConnection;
import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;
import neo4jUtility.ThreadUtility;

public final class Neo4jTraversal
{

    private Neo4jTraversal()
    {

    }

    static void startTraversal(boolean splitAndReuse)
    {
	if (splitAndReuse)
	{
	    traverseOutportSplit(MainTraversal.getOutport());
	}
	else
	{
	    traverseOutportFull();
	}
    }

    /**
     * 
     */
    private static void traverseOutportFull()
    {
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    InitialBranchState.State<TraversalState> ibs;
	    ibs = new InitialBranchState.State<TraversalState>(null, null);
	    TraversalDescription ftTD = DBConnection.getGraphDB().traversalDescription().uniqueness(Uniqueness.NONE).depthFirst().expand(new CFTPathExpanderFull(), ibs);
	    Traverser ftTraverser = ftTD.traverse(MainTraversal.getOutport());
	    ftTraverser.iterator().forEachRemaining(path ->
	    {

	    });
	    tx.success();
	}
    }

    /**
     * This method requires a Node to have the Label CFT_Outport.
     * 
     * @param outport
     *            The CFT_Outport that is traversed.
     */
    private static void traverseOutportSplit(Node outport)
    {
	ArrayList<Node> nextNodesToTraverse = new ArrayList<Node>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    ArrayList<Node> nodesToTraverse = new ArrayList<Node>();
	    nodesToTraverse.add(outport);
	    if (outport.hasLabel(NodeLabels.CFT_Outport))
	    {
		for (Node inportInstance : DBUtility.getInportInstances(outport))
		{
		    if (!MainTraversal.resultGateSetNodeTraversed(inportInstance))
		    {
			nodesToTraverse.add(inportInstance);
		    }
		}
	    }
	    InitialBranchState.State<TraversalState> ibs = new InitialBranchState.State<TraversalState>(null, null);
	    TraversalDescription ftTD = DBConnection.getGraphDB().traversalDescription().uniqueness(Uniqueness.NONE).depthFirst().expand(new CFTPathExpanderSplit(), ibs)
		    .evaluator(new CFTEvaluatorSplit());
	    Traverser ftTraverser = ftTD.traverse(nodesToTraverse);
	    ftTraverser.iterator().forEachRemaining(path ->
	    {
		nextNodesToTraverse.add(path.endNode());
	    });
	    tx.success();
	}
	if (ThreadUtility.getThreadUtility().isMultiThreading())
	{
	    Set<Future<?>> futureSet = new HashSet<Future<?>>();
	    for (Node _node : nextNodesToTraverse)
	    {
		if (!MainTraversal.resultGateSetNodeTraversed(_node))
		{
		    if (nextNodesToTraverse.size() != 1 && ThreadUtility.getThreadUtility().isThreadAvailable())
		    {
			futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				traverseOutportSplit(_node);
			    }
			}));
		    }
		    else
		    {
			traverseOutportSplit(_node);
		    }
		}
	    }
	    ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
	}
	else
	{
	    for (Node _node : nextNodesToTraverse)
	    {
		if (!MainTraversal.resultGateSetNodeTraversed(_node))
		{
		    traverseOutportSplit(_node);
		}
	    }
	}
    }

}
