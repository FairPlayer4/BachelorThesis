package neo4jTraversal;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

import neo4jDatabase.DBUtility;
import neo4jEnum.NodeLabels;
import neo4jUtility.PrintUtility;

/**
 * This class evaluates paths for a split traversal.<br>
 * The traversal will prune (stop) and include the Path if the last Node of the Path is an Outport and the path has at least one Relationship.<br>
 * The Evaluator also checks if the starting Nodes of a split traversal have the right label to prevent traversal of CFT that have errors.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class CFTEvaluatorSplit implements Evaluator
{
    @Override
    public Evaluation evaluate(Path path)
    {
	final Node node = path.endNode();
	if (path.length() != 0) // length is the number of relationships in the path if it is zero then the path only contains a single node
	{
	    if (node.hasLabel(NodeLabels.CFT_Basic_Event) || node.hasLabel(NodeLabels.CFT_Inport) || node.hasLabel(NodeLabels.CFT_Outport_Instance) || node.hasLabel(NodeLabels.CFT_AND_Gate)
		    || node.hasLabel(NodeLabels.CFT_OR_Gate) || node.hasLabel(NodeLabels.CFT_MOON_Gate) || node.hasLabel(NodeLabels.CFT_XOR_Gate) || node.hasLabel(NodeLabels.CFT_NOT_Gate))
	    {
		return Evaluation.EXCLUDE_AND_CONTINUE;
	    }
	    if (node.hasLabel(NodeLabels.CFT_Outport))
	    {
		return Evaluation.INCLUDE_AND_PRUNE;
	    }
	}
	else // if the path has no relationships then the end Node of the path must be an CFT_Inport_Instance or a CFT_Outport because a split traversal can
	     // only begin at Nodes with these labels.
	{
	    if (node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport))
	    {
		return Evaluation.EXCLUDE_AND_CONTINUE;
	    }
	}
	PrintUtility.printError("The Node does not have a valid Label for Evaluation!", "Labels found: " + DBUtility.getLabelsAsString(node));
	return Evaluation.EXCLUDE_AND_PRUNE;
    }
}
