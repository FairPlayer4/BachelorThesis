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
import neo4jUtility.PrintUtility;

public class CyclePathExpander implements PathExpander<TraversalState>
{
    
    @Override
    public Iterable<Relationship> expand(Path path, BranchState<TraversalState> branchState)
    {
	ArrayList<Relationship> relationshipsToExpand = new ArrayList<>(); // These relationships will be traversed further
	if (!CycleTraversal.cycleFound() && !CycleTraversal.errorFound()) // If cycles are found then the traversal stops
	{
	    final Node endNode = path.endNode();
	    CycleTraversal.errorFound(DBUtility.isErrorNode(endNode));
	    TraversalState ts = null;
	    if (branchState.getState() == null)
	    {
		// Must be the starting Node and an Outport (was checked by CycleTraversal)
		ts = new TraversalState(); // Starting a new TraversalState
		ts.addNodeToSimple(endNode);
		for (Relationship rel : endNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
		{
		    relationshipsToExpand.add(rel);
		}
	    }
	    else
	    {// the nodes are handled differently depending on their label
		ts = new TraversalState(branchState.getState()); // copies the previous TraversalState
		CycleTraversal.cycleFound(ts.addNodeToSimple(endNode)); // finds simple cycles
		if (endNode.hasLabel(NodeLabels.CFT_Outport_Instance)) // the inports are mapped to inport instances and saved in the TraversalState
		{
		    CycleTraversal.cycleFound(ts.addNodeToInstances(endNode)); // find deep cycles
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
			if (!CycleTraversal.getInports().contains(endNode))
			{// now the mapping done at the previous outport instance is used to continue with the right inport instance
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
			    CycleTraversal.cycleFound(ts.addNodeToInstances(endNode)); // find deep cycles
			}
			else
			{
			    if (!(endNode.hasLabel(NodeLabels.CFT_Outport) || endNode.hasLabel(NodeLabels.CFT_AND_Gate) || endNode.hasLabel(NodeLabels.CFT_OR_Gate)
				    || endNode.hasLabel(NodeLabels.CFT_MOON_Gate) || endNode.hasLabel(NodeLabels.CFT_XOR_Gate) || endNode.hasLabel(NodeLabels.CFT_NOT_Gate)
				    || endNode.hasLabel(NodeLabels.CFT_Basic_Event)))
			    {
				// should not happen if the CFT was constructed correctly
				PrintUtility.printError("The Node does not have a valid Label for the PathExpander!", "Labels found: " + DBUtility.getLabelsAsString(endNode));
			    }
			}
			for (Relationship rel : endNode.getRelationships(RelTypes.Failure_Propagation, Direction.INCOMING))
			{
			    relationshipsToExpand.add(rel);
			}
		    }
		}
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
