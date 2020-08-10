package neo4jTraversal;

import java.util.ArrayList;
import java.util.HashMap;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jGateSets.ANDSet;
import neo4jGateSets.GateSet;
import neo4jGateSets.MOONSet;
import neo4jGateSets.NOTSet;
import neo4jGateSets.ORSet;
import neo4jGateSets.ResultGateSet;
import neo4jGateSets.ConnectorGateSet;
import neo4jGateSets.XORSet;
import neo4jUtility.PrintUtility;

public class CFTPathExpanderSplit implements PathExpander<TraversalState>
{
    @Override
    public Iterable<Relationship> expand(Path path, BranchState<TraversalState> branchState)
    {
	ArrayList<Relationship> rels = new ArrayList<>();
	if (!MainTraversal.cycleFound())
	{
	    Node endNode = path.endNode();
	    TraversalState ts = null;
	    GateSet nextGateSet = null;
	    if (branchState.getState() == null)
	    {
		if (endNode.hasLabel(NodeLabels.CFT_Outport) || endNode.hasLabel(NodeLabels.CFT_Inport_Instance))
		{
		    ResultGateSet resultSet = new ResultGateSet(endNode);
		    MainTraversal.addResultGateSet(endNode, resultSet);
		    nextGateSet = resultSet;
		    ts = new TraversalState(nextGateSet);
		    ts.addNodeToSimple(endNode);
		    for (Relationship rel : endNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
		    {
			rels.add(rel);
		    }
		}
		else
		{
		    PrintUtility.printError("Invalid Label! The Traversal cannot be started with this Node.");
		}
	    }
	    else
	    {
		ts = new TraversalState(branchState.getState());
		MainTraversal.cycleFound(ts.addNodeToSimple(endNode));
		if (endNode.hasLabel(NodeLabels.CFT_Outport))
		{
		    // Handled by the Evaluator.
		}
		else
		{
		    if (endNode.hasLabel(NodeLabels.CFT_Inport_Instance))
		    {
			PrintUtility.printError("Invalid Label! The Traversal cannot continue with this Node.");
		    }
		    else
		    {
			if (endNode.hasLabel(NodeLabels.CFT_Basic_Event))
			{
			    ts.addNodeToLastGateSet(endNode);
			}
			else
			{
			    if (endNode.hasLabel(NodeLabels.CFT_Outport_Instance))
			    {
				HashMap<Node, Node> nextInportMap = new HashMap<Node, Node>();
				Node outport = endNode.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode();
				rels.add(endNode.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING)); // Add Relationship to Outport
				for (Node inportInstance : DBUtility.getInportInstances(endNode))
				{ // Map Inports to InportInstances
				    nextInportMap.put(inportInstance.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode(), inportInstance);
				}
				nextGateSet = new ConnectorGateSet(endNode, nextInportMap);
				nextGateSet.addNode(outport);
				ts.addGateSetToLastGateSet(nextGateSet);
			    }
			    else
			    {
				if (endNode.hasLabel(NodeLabels.CFT_Inport))
				{
				    ts.addNodeToLastGateSet(endNode);
				}
				else
				{
				    if (endNode.hasLabel(NodeLabels.CFT_AND_Gate))
				    {
					nextGateSet = new ANDSet(endNode);
				    }
				    else
				    {
					if (endNode.hasLabel(NodeLabels.CFT_OR_Gate))
					{
					    nextGateSet = new ORSet(endNode);
					}
					else
					{
					    if (endNode.hasLabel(NodeLabels.CFT_MOON_Gate))
					    {
						nextGateSet = new MOONSet(endNode);
					    }
					    else
					    {
						if (endNode.hasLabel(NodeLabels.CFT_XOR_Gate))
						{
						    nextGateSet = new XORSet(endNode);
						}
						else
						{
						    if (endNode.hasLabel(NodeLabels.CFT_NOT_Gate))
						    {
							nextGateSet = new NOTSet(endNode);
						    }
						    else
						    {
							PrintUtility.printError("Invalid Label! The Traversal cannot continue with this Node.");
						    }
						}
					    }
					}
				    }
				    ts.addGateSetToLastGateSet(nextGateSet);
				    for (Relationship rel : endNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
				    {
					rels.add(rel);
				    }
				}
			    }
			}
		    }
		}
	    }
	    if (nextGateSet != null)
	    {
		ts.setLastGateSet(nextGateSet);
	    }
	    branchState.setState(ts);
	}
	return rels;
    }

    @Override
    public PathExpander<TraversalState> reverse()
    {
	return null;
    }

}
