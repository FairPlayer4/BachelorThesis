package neo4jTraversal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import neo4jDatabase.DBConnection;
import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jGateSets.ANDSet;
import neo4jGateSets.MOONSet;
import neo4jGateSets.NOTSet;
import neo4jGateSets.ORSet;
import neo4jGateSets.ResultGateSet;
import neo4jGateSets.ConnectorGateSet;
import neo4jGateSets.XORSet;
import neo4jUtility.PrintUtility;
import neo4jUtility.ThreadUtility;

public final class ManualTraversal
{

    private static Set<Future<?>> futureSet = ConcurrentHashMap.newKeySet();

    private ManualTraversal()
    {

    }

    static void startManualTraversal()
    {
	ResultGateSet outportSet = new ResultGateSet(MainTraversal.getOutport());
	MainTraversal.addResultGateSet(MainTraversal.getOutport(), outportSet);
	TraversalState ts = new TraversalState(outportSet);
	ts.addNodeToSimple(MainTraversal.getOutport());
	boolean stopTraversal = false;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    Node lowerNode = MainTraversal.getOutport().getSingleRelationship(RelTypes.Failure_Propagation, Direction.INCOMING).getStartNode();
	    MainTraversal.cycleFound(ts.addNodeToSimple(lowerNode));
	    if (lowerNode.hasLabel(NodeLabels.CFT_AND_Gate))
	    {
		ANDSet andSet = new ANDSet(lowerNode);
		ts.addGateSetToLastGateSet(andSet);
		ts.setLastGateSet(andSet);
	    }
	    else
	    {
		if (lowerNode.hasLabel(NodeLabels.CFT_OR_Gate))
		{
		    ORSet orSet = new ORSet(lowerNode);
		    ts.addGateSetToLastGateSet(orSet);
		    ts.setLastGateSet(orSet);
		}
		else
		{
		    if (lowerNode.hasLabel(NodeLabels.CFT_NOT_Gate))
		    {
			NOTSet notSet = new NOTSet(lowerNode);
			ts.addGateSetToLastGateSet(notSet);
			ts.setLastGateSet(notSet);
		    }
		    else
		    {
			if (lowerNode.hasLabel(NodeLabels.CFT_XOR_Gate))
			{
			    XORSet xorSet = new XORSet(lowerNode);
			    ts.addGateSetToLastGateSet(xorSet);
			    ts.setLastGateSet(xorSet);
			}
			else
			{
			    if (lowerNode.hasLabel(NodeLabels.CFT_MOON_Gate))
			    {
				MOONSet moonSet = new MOONSet(lowerNode);
				ts.addGateSetToLastGateSet(moonSet);
				ts.setLastGateSet(moonSet);
			    }
			    else
			    {
				if (lowerNode.hasLabel(NodeLabels.CFT_Basic_Event))
				{
				    ts.addNodeToLastGateSet(lowerNode);
				    stopTraversal = true;
				}
				else
				{
				    if (lowerNode.hasLabel(NodeLabels.CFT_Inport))
				    {
					if (MainTraversal.getInports().contains(lowerNode))
					{
					    ts.addNodeToLastGateSet(lowerNode);
					    stopTraversal = true;
					}
					else
					{
					    PrintUtility.printError("The starting Outport is connected to an Inport of another CFT! Traversal cannot continue!");
					}
				    }
				    else
				    {
					if (lowerNode.hasLabel(NodeLabels.CFT_Outport_Instance))
					{
					    MainTraversal.cycleFound(ts.addNodeToInstances(lowerNode));
					    ConnectorGateSet conSet = new ConnectorGateSet(lowerNode);
					    ts.addGateSetToLastGateSet(conSet);
					    ts.setLastGateSet(conSet);
					    Node nextOutport = lowerNode.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode();
					    ResultGateSet nextResultGateSet = new ResultGateSet(nextOutport);
					    ts.addGateSetToLastGateSet(nextResultGateSet);
					    ts.setLastGateSet(nextResultGateSet);
					    HashMap<Node, Node> nextInportMap = new HashMap<Node, Node>();
					    for (Node inportInstance : DBUtility.getInportInstances(lowerNode))
					    { // Map Inports to InportInstances
						nextInportMap.put(inportInstance.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode(), inportInstance);
					    }
					    ts.addInportMap(nextInportMap);
					    ts.clearSimple();
					}
					else
					{
					    PrintUtility.printError("An unknown label was traversed and the analysis!", DBUtility.getLabelsAsString(lowerNode));
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	    tx.success();
	}
	if (!stopTraversal)
	{
	    futureSet.clear();
	    traverseState(ts);
	}
	ThreadUtility.getThreadUtility().waitForSubmittedTasksToFinish(futureSet);
    }
    
    private static void traverseState(TraversalState traversalState)
    {
	if (!MainTraversal.cycleFound())
	{
	    ArrayList<TraversalState> nextTraversals = new ArrayList<TraversalState>();
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		Node upperNode = traversalState.getLastGateSet().getStartNode();
		for (Relationship rel : upperNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
		{
		    Node lowerNode = rel.getStartNode();
		    TraversalState nextTS = new TraversalState(traversalState);
		    boolean stopTraversal = false;
		    MainTraversal.cycleFound(nextTS.addNodeToSimple(lowerNode));
		    if (lowerNode.hasLabel(NodeLabels.CFT_AND_Gate))
		    {
			ANDSet andSet = new ANDSet(lowerNode);
			nextTS.addGateSetToLastGateSet(andSet);
			nextTS.setLastGateSet(andSet);
		    }
		    else
		    {
			if (lowerNode.hasLabel(NodeLabels.CFT_OR_Gate))
			{
			    ORSet orSet = new ORSet(lowerNode);
			    nextTS.addGateSetToLastGateSet(orSet);
			    nextTS.setLastGateSet(orSet);
			}
			else
			{
			    if (lowerNode.hasLabel(NodeLabels.CFT_NOT_Gate))
			    {
				NOTSet notSet = new NOTSet(lowerNode);
				nextTS.addGateSetToLastGateSet(notSet);
				nextTS.setLastGateSet(notSet);
			    }
			    else
			    {
				if (lowerNode.hasLabel(NodeLabels.CFT_XOR_Gate))
				{
				    XORSet xorSet = new XORSet(lowerNode);
				    nextTS.addGateSetToLastGateSet(xorSet);
				    nextTS.setLastGateSet(xorSet);
				}
				else
				{
				    if (lowerNode.hasLabel(NodeLabels.CFT_MOON_Gate))
				    {
					MOONSet moonSet = new MOONSet(lowerNode);
					nextTS.addGateSetToLastGateSet(moonSet);
					nextTS.setLastGateSet(moonSet);
				    }
				    else
				    {
					if (lowerNode.hasLabel(NodeLabels.CFT_Basic_Event))
					{
					    nextTS.addNodeToLastGateSet(lowerNode);
					    stopTraversal = true;
					}
					else
					{
					    if (lowerNode.hasLabel(NodeLabels.CFT_Outport))
					    {
						ResultGateSet outportSet = new ResultGateSet(lowerNode);
						nextTS.addGateSetToLastGateSet(outportSet);
						nextTS.setLastGateSet(outportSet);
					    }
					    else
					    {
						if (lowerNode.hasLabel(NodeLabels.CFT_Inport_Instance))
						{
						    MainTraversal.cycleFound(nextTS.addNodeToInstances(lowerNode));
						    ResultGateSet resultGateSet = new ResultGateSet(lowerNode);
						    nextTS.addGateSetToLastGateSet(resultGateSet);
						    nextTS.setLastGateSet(resultGateSet);
						}
						else
						{
						    if (lowerNode.hasLabel(NodeLabels.CFT_Inport))
						    {
							if (MainTraversal.getInports().contains(lowerNode))
							{
							    nextTS.addNodeToLastGateSet(lowerNode);
							    stopTraversal = true;
							}
							else
							{
							    if (nextTS.containsInport(lowerNode))
							    {
								ConnectorGateSet conSet = new ConnectorGateSet(lowerNode);
								nextTS.addGateSetToLastGateSet(conSet);
								nextTS.setLastGateSet(conSet);
								Node inportInstance = nextTS.removeLastInportMap().get(lowerNode);
								ResultGateSet nextResultGateSet = new ResultGateSet(inportInstance);
								nextTS.addGateSetToLastGateSet(nextResultGateSet);
								nextTS.setLastGateSet(nextResultGateSet);
								nextTS.clearSimple();
							    }
							    else
							    {
								PrintUtility.printError("Inport is not mapped to an Inport Instance!");
							    }
							}
						    }
						    else
						    {
							if (lowerNode.hasLabel(NodeLabels.CFT_Outport_Instance))
							{
							    MainTraversal.cycleFound(nextTS.addNodeToInstances(lowerNode));
							    ConnectorGateSet conSet = new ConnectorGateSet(lowerNode);
							    nextTS.addGateSetToLastGateSet(conSet);
							    nextTS.setLastGateSet(conSet);
							    Node nextOutport = lowerNode.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode();
							    ResultGateSet nextResultGateSet = new ResultGateSet(nextOutport);
							    nextTS.addGateSetToLastGateSet(nextResultGateSet);
							    nextTS.setLastGateSet(nextResultGateSet);
							    HashMap<Node, Node> nextInportMap = new HashMap<Node, Node>();
							    for (Node inportInstance : DBUtility.getInportInstances(lowerNode))
							    { // Map Inports to InportInstances
								nextInportMap.put(inportInstance.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode(), inportInstance);
							    }
							    nextTS.addInportMap(nextInportMap);
							    nextTS.clearSimple();
							}
							else
							{
							    PrintUtility.printError("An invalid node label was found in the Neo4j database so the traversal cannot continue.");
							}
						    }
						}
					    }
					}
				    }
				}
			    }
			}
		    }
		    if (!stopTraversal)
		    {
			nextTraversals.add(nextTS);
		    }
		}
		tx.success();
	    }
	    if (ThreadUtility.getThreadUtility().isMultiThreading())
	    {
		Iterator<TraversalState> iterator = nextTraversals.iterator();
		while (iterator.hasNext())
		{
		    TraversalState nextTS = iterator.next();
		    if (nextTraversals.size() == 1 || !ThreadUtility.getThreadUtility().isThreadAvailable() || !iterator.hasNext())
		    {
			traverseState(nextTS);
		    }
		    else
		    {
			futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				traverseState(nextTS);
			    }
			}));
		    }
		}
	    }
	    else
	    {
		for (TraversalState nextTS : nextTraversals)
		{
		    traverseState(nextTS);
		}
	    }
	}
    }
}
