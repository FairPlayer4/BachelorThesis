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

/**
 * This class is the PathExpander for a full Neo4jTraversal.<br>
 * It handles the traversal almost entirely. The PathExpander decides which relationships are traversed.<br>
 * To continue a relationship in the traversal it must returned by the method expand.<br>
 * While deciding how the traversal continues this PathExpander creates GateSets depending on which Nodes are traversed.<br>
 * More details of the implementation are documented within the code.<br>
 * The traversal works similar to ManualTraversal and also makes use of the TraversalState.<br>
 * The PathExpander will find simple and deep cycles in CFT and will stop traversal if it does find cycles.<br>
 * This Traversal can only be done in a single Thread because it runs in a single TraversalDescription and never returns Paths.<br>
 * 
 * @see ManualTraversal
 * @see TraversalState
 * 
 * @author Kevin Bartik
 *
 */
public class CFTPathExpanderFull implements PathExpander<TraversalState>
{
    @Override
    public Iterable<Relationship> expand(Path path, BranchState<TraversalState> branchState)
    {
	ArrayList<Relationship> relationshipsToExpand = new ArrayList<>(); // These relationships will be traversed further
	if (!MainTraversal.cycleFound()) // If cycles are found then the traversal stops
	{
	    final Node endNode = path.endNode();
	    TraversalState ts = null;
	    GateSet nextGateSet = null;
	    if (branchState.getState() == null)
	    {
		// Must be the starting Node and an Outport (was checked by Neo4jTraversal)
		ResultGateSet resultSet = new ResultGateSet(endNode);
		MainTraversal.addResultGateSet(endNode, resultSet);
		nextGateSet = resultSet;
		ts = new TraversalState(nextGateSet); // Starting a new TraversalState
		ts.addNodeToSimple(endNode);
		for (Relationship rel : endNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
		{
		    relationshipsToExpand.add(rel);
		}
	    }
	    else
	    {// the nodes are handled differently depending on their label
		ts = new TraversalState(branchState.getState()); // copies the previous TraversalState
		MainTraversal.cycleFound(ts.addNodeToSimple(endNode)); // finds simple cycles
		if (endNode.hasLabel(NodeLabels.CFT_Basic_Event))
		{
		    ts.addNodeToLastGateSet(endNode);
		}
		else
		{
		    if (endNode.hasLabel(NodeLabels.CFT_Outport_Instance)) // the inports are mapped to inport instances and saved in the TraversalState
		    {
			MainTraversal.cycleFound(ts.addNodeToInstances(endNode)); // find deep cycles
			nextGateSet = new ConnectorGateSet(endNode);
			ts.addGateSetToLastGateSet(nextGateSet);
			HashMap<Node, Node> nextInportMap = new HashMap<Node, Node>();
			relationshipsToExpand.add(endNode.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING)); // Traversal continues with the
															       // outport
			for (Node inportInstance : DBUtility.getInportInstances(endNode))
			{ // Map Inports to InportInstances
			    nextInportMap.put(inportInstance.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode(), inportInstance);
			}
			ts.addInportMap(nextInportMap);
			ts.clearSimple(); // traversal enters a different CFT
		    }
		    else
		    {
			if (endNode.hasLabel(NodeLabels.CFT_Inport))
			{
			    if (MainTraversal.getInports().contains(endNode))
			    {
				ts.addNodeToLastGateSet(endNode);
			    }
			    else
			    {// now the mapping done at the previous outport instance is used to continue with the right inport instance
				nextGateSet = new ConnectorGateSet(endNode);
				ts.addGateSetToLastGateSet(nextGateSet);
				Node nextInportInstance = ts.removeLastInportMap().get(endNode); // the mapping was performed so the last inport map can be
												 // deleted this only affects this traversal branch as others
												 // have a copy with the TraversalState
				for (Relationship rel : endNode.getRelationships(RelTypes.Is_Instance_Of, Direction.INCOMING))
				{
				    Node relNode = rel.getStartNode();
				    if (relNode.equals(nextInportInstance)) // pick the mapped inport instance
				    {
					relationshipsToExpand.add(rel); 
				    }
				}
				ts.clearSimple();
			    }
			}
			else
			{ // the following Nodes all can have incoming Failure Propagations which will be traversed 
			    if (endNode.hasLabel(NodeLabels.CFT_Inport_Instance))
			    {
				MainTraversal.cycleFound(ts.addNodeToInstances(endNode)); // find deep cycles
				nextGateSet = new ResultGateSet(endNode);
			    }
			    else
			    {
				if (endNode.hasLabel(NodeLabels.CFT_Outport))
				{
				    nextGateSet = new ResultGateSet(endNode);
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
							// should not happen if the CFT was constructed correctly
							PrintUtility.printError("The Node does not have a valid Label for the PathExpander!", "Labels found: " + DBUtility.getLabelsAsString(endNode));
						    }
						}
					    }
					}
				    }
				}
			    }
			    ts.addGateSetToLastGateSet(nextGateSet);
			    for (Relationship rel : endNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
			    {
				relationshipsToExpand.add(rel);
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
	return relationshipsToExpand;
    }

    @Override
    public PathExpander<TraversalState> reverse()
    {
	return null;
    }

}
