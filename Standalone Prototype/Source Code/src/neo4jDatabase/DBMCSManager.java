package neo4jDatabase;

import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jGateSets.ResultGateSet;
import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jMCS.MCSPairSet;
import neo4jUtility.PrintUtility;

/**
 * This class manages analysis results that are stored in the database.
 * @author Kevin Bartik
 *
 */
public final class DBMCSManager
{
    
    /**
     * Private constructor so this class cannot be instantiated.
     */
    private DBMCSManager() {
	
    }
    
    /**
     * Deletes MCS and Quantitative Results from a node and all nodes that can be reached with failure propagations.
     * @param node The node where the MCS and Quantitative Results are deleted.
     * @param onlyFullMCS Indicates if only full MCS shall be deleted.
     */
    static void deleteMCSAndResults(Node node, boolean onlyFullMCS)
    {
	if (DBUtility.hasMCS())
	{
	    PrintUtility.printInfo("Deleting Minimal Cut Sets and Quantitative Analysis Results from the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database...");
	    HashMap<Node, Boolean> nextHigherNodes = new HashMap<Node, Boolean>();
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		for (Relationship failRel : node.getRelationships(RelTypes.Failure_Propagation, Direction.OUTGOING))
		{
		    nextHigherNodes.put(failRel.getEndNode(), onlyFullMCS);
		}
		if (node.hasLabel(NodeLabels.CFT_Outport))
		{
		    for (Relationship instRel : node.getRelationships(RelTypes.Is_Instance_Of, Direction.INCOMING))
		    {
			nextHigherNodes.put(instRel.getStartNode(), true);
		    }
		}
		if (node.hasLabel(NodeLabels.CFT_Inport_Instance))
		{
		    Node cftInstance = node.getSingleRelationship(RelTypes.Is_Child_Of, Direction.OUTGOING).getEndNode();
		    for (Relationship outInstRel : cftInstance.getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
		    {
			Node outInst = outInstRel.getStartNode();
			if (outInst.hasLabel(NodeLabels.CFT_Outport_Instance))
			{
			    nextHigherNodes.put(outInst, true);
			}
		    }
		}
		if (node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport))
		{
		    if (!onlyFullMCS)
		    {
			for (Relationship rel : node.getRelationships(Direction.INCOMING, RelTypes.Is_MCS_Of, RelTypes.Is_Negated_MCS_Of))
			{
			    Node mcs = rel.getStartNode();
			    for (Relationship rel2 : mcs.getRelationships(RelTypes.Is_Inside_MCS, Direction.INCOMING))
			    {
				rel2.delete();
			    }
			    rel.delete();
			    mcs.delete();
			}
		    }
		    for (Relationship rel : node.getRelationships(Direction.INCOMING, RelTypes.Is_Full_MCS_Of, RelTypes.Is_Full_Negated_MCS_Of))
		    {
			Node mcs = rel.getStartNode();
			for (Relationship rel2 : mcs.getRelationships(RelTypes.Is_Inside_MCS, Direction.INCOMING))
			{
			    rel2.delete();
			}
			rel.delete();
			mcs.delete();
		    }
		    Relationship resultRel = node.getSingleRelationship(RelTypes.Is_Quant_Result_Of, Direction.INCOMING);
		    if (resultRel != null)
		    {
			Node result = resultRel.getStartNode();
			resultRel.delete();
			result.delete();
		    }
		}
		tx.success();
	    }
	    for (Node nextNode : nextHigherNodes.keySet())
	    {
		deleteMCSAndResults(nextNode, nextHigherNodes.get(nextNode)); // Maybe Multithreading
	    }
	    PrintUtility.printInfo("Successfully deleted Minimal Cut Sets and Quantitative Analysis Results from the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database!");
	}
    }
    
    /**
     * Deletes only the Qualitative Result of a node and all nodes that are reachable by failure propagation or are in higher level CFT.
     * @param node The node where Quantitative Results are deleted.
     */
    static void deleteOnlyResults(Node node)
    {
	if (DBUtility.hasMCS())
	{
	    PrintUtility.printInfo("Quantitative Analysis Results from the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database...");
	    HashSet<Node> nextHigherNodes = new HashSet<Node>();
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		for (Relationship failRel : node.getRelationships(RelTypes.Failure_Propagation, Direction.OUTGOING))
		{
		    nextHigherNodes.add(failRel.getEndNode());
		}
		if (node.hasLabel(NodeLabels.CFT_Outport))
		{
		    for (Relationship instRel : node.getRelationships(RelTypes.Is_Instance_Of, Direction.INCOMING))
		    {
			nextHigherNodes.add(instRel.getStartNode());
		    }
		}
		if (node.hasLabel(NodeLabels.CFT_Inport_Instance))
		{
		    Node cftInstance = node.getSingleRelationship(RelTypes.Is_Child_Of, Direction.OUTGOING).getEndNode();
		    for (Relationship outInstRel : cftInstance.getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
		    {
			Node outInst = outInstRel.getStartNode();
			if (outInst.hasLabel(NodeLabels.CFT_Outport_Instance))
			{
			    nextHigherNodes.add(outInst);
			}
		    }
		}
		if (node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport))
		{
		    Relationship resultRel = node.getSingleRelationship(RelTypes.Is_Quant_Result_Of, Direction.INCOMING);
		    if (resultRel != null)
		    {
			Node result = resultRel.getStartNode();
			resultRel.delete();
			result.delete();
		    }
		}
		tx.success();
	    }
	    for (Node nextNode : nextHigherNodes)
	    {
		deleteOnlyResults(nextNode);
	    }
	    PrintUtility.printInfo("Successfully deleted Quantitative Analysis Results from the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database!");
	}
    }

    /**
     * Adds the Full MCS Set that is in the ResultGateSet to the start node.
     * @param resultGateSet The ResultGateSet.
     */
    public static void addFullMCSSet(ResultGateSet resultGateSet)
    {
	Node node = resultGateSet.getStartNode();
	HashSet<MCS> mcsSet = resultGateSet.getMCSSet();
	HashSet<MCS> negatedMCSSet = resultGateSet.getNegatedMCSSet();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Outport) && !node.hasRelationship(Direction.INCOMING, RelTypes.Is_Full_MCS_Of, RelTypes.Is_Full_Negated_MCS_Of))
	    {
		PrintUtility.printInfo("Adding Minimal Cut Sets to the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database...");
		for (MCS set : mcsSet)
		{
		    Node mcs = DBConnection.getGraphDB().createNode(NodeLabels.Full_MCS);
		    mcs.createRelationshipTo(node, RelTypes.Is_Full_MCS_Of);
		    for (MCSNode tnode : set.getMCSNodeSet())
		    {
			Relationship mcsRel = tnode.getNode().createRelationshipTo(mcs, RelTypes.Is_Inside_MCS);
			if (tnode.isNegated())
			{
			    mcsRel.setProperty("Negated", "true");
			}
			else
			{
			    mcsRel.setProperty("Negated", "false");
			}
		    }
		}
		for (MCS set : negatedMCSSet)
		{
		    Node mcs = DBConnection.getGraphDB().createNode(NodeLabels.Full_Negated_MCS);
		    mcs.createRelationshipTo(node, RelTypes.Is_Full_Negated_MCS_Of);
		    for (MCSNode tnode : set.getMCSNodeSet())
		    {
			Relationship mcsRel = tnode.getNode().createRelationshipTo(mcs, RelTypes.Is_Inside_MCS);
			if (tnode.isNegated())
			{
			    mcsRel.setProperty("Negated", "true");
			}
			else
			{
			    mcsRel.setProperty("Negated", "false");
			}
		    }
		}
		PrintUtility.printInfo("Successfully added Minimal Cut Sets to the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database!");
	    }
	    tx.success();
	}
    }

    /**
     * Adds the MCS Set that is in the ResultGateSet to the start node.
     * @param resultGateSet The ResultGateSet.
     */
    public static void addMCSSet(ResultGateSet resultGateSet)
    {
	Node node = resultGateSet.getStartNode();
	HashSet<MCS> mcsSet = resultGateSet.getMCSSet();
	HashSet<MCS> negatedMCSSet = resultGateSet.getNegatedMCSSet();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Outport) || node.hasLabel(NodeLabels.CFT_Inport_Instance))
	    {
		PrintUtility.printInfo("Adding Partial Minimal Cut Sets to the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database...");
		for (MCS set : mcsSet)
		{
		    Node mcs = DBConnection.getGraphDB().createNode(NodeLabels.MCS);
		    mcs.createRelationshipTo(node, RelTypes.Is_MCS_Of);
		    for (MCSNode tnode : set.getMCSNodeSet())
		    {
			Relationship mcsRel = tnode.getNode().createRelationshipTo(mcs, RelTypes.Is_Inside_MCS);
			if (tnode.isNegated())
			{
			    mcsRel.setProperty("Negated", "true");
			}
			else
			{
			    mcsRel.setProperty("Negated", "false");
			}
		    }
		}
		for (MCS set : negatedMCSSet)
		{
		    Node mcs = DBConnection.getGraphDB().createNode(NodeLabels.Negated_MCS);
		    mcs.createRelationshipTo(node, RelTypes.Is_Negated_MCS_Of);
		    for (MCSNode tnode : set.getMCSNodeSet())
		    {
			Relationship mcsRel = tnode.getNode().createRelationshipTo(mcs, RelTypes.Is_Inside_MCS);
			if (tnode.isNegated())
			{
			    mcsRel.setProperty("Negated", "true");
			}
			else
			{
			    mcsRel.setProperty("Negated", "false");
			}
		    }
		}
		PrintUtility.printInfo("Successfully added Partial Minimal Cut Sets to the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database!");
	    }
	    tx.success();
	}
    }

    /**
     * Adds a Quantitative Result to a node.
     * @param node The node.
     * @param result The Quantitative Result.
     */
    public static void addQuantitativeResult(Node node, double result)
    {
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Outport))
	    {
		PrintUtility.printInfo("Adding Quantitative Analysis Results to the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database...");
		Node resultNode = DBConnection.getGraphDB().createNode(NodeLabels.Quant_Result);
		resultNode.setProperty("Result", result);
		resultNode.createRelationshipTo(node, RelTypes.Is_Quant_Result_Of);
		PrintUtility.printInfo("Successfully added Quantitative Analysis Results to the Node (" + DBUtility.getIDAndName(node) + ") in the Neo4j database!");
	    }
	    tx.success();
	}
    }

    /**
     * Returns the MCSPairSet of the partial MCS for that node.
     * @param node The node.
     * @return The MCSPairSet of the partial MCS.
     */
    public static MCSPairSet getMCSSet(Node node)
    {
	HashSet<MCS> mcsSet = new HashSet<MCS>();
	HashSet<MCS> negatedMCSSet = new HashSet<MCS>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Outport) || node.hasLabel(NodeLabels.CFT_Inport_Instance))
	    {
		for (Relationship mcsRel : node.getRelationships(RelTypes.Is_MCS_Of, Direction.INCOMING))
		{
		    Node nextMCS = mcsRel.getStartNode();
		    HashSet<MCSNode> mcsNodes = new HashSet<MCSNode>();
		    for (Relationship mcsNodeRel : nextMCS.getRelationships(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		    {
			Node mcsNode = mcsNodeRel.getStartNode();
			boolean negated = false;
			if (mcsNodeRel.getProperty("Negated").toString().equals("true"))
			{
			    negated = true;
			}
			mcsNodes.add(new MCSNode(mcsNode, negated));
		    }
		    mcsSet.add(new MCS(mcsNodes));
		}
		for (Relationship mcsRel : node.getRelationships(RelTypes.Is_Negated_MCS_Of, Direction.INCOMING))
		{
		    Node nextMCS = mcsRel.getStartNode();
		    HashSet<MCSNode> mcsNodes = new HashSet<MCSNode>();
		    for (Relationship mcsNodeRel : nextMCS.getRelationships(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		    {
			Node mcsNode = mcsNodeRel.getStartNode();
			boolean negated = false;
			if (mcsNodeRel.getProperty("Negated").toString().equals("true"))
			{
			    negated = true;
			}
			mcsNodes.add(new MCSNode(mcsNode, negated));
		    }
		    negatedMCSSet.add(new MCS(mcsNodes));
		}
	    }
	    tx.success();
	}
	return new MCSPairSet(mcsSet, negatedMCSSet);
    }

    /**
     * Returns the MCSPairSet of the full MCS for that node.
     * @param node The node.
     * @return The MCSPairSet of the full MCS.
     */
    public static MCSPairSet getFullMCSSet(Node node)
    {
	HashSet<MCS> mcsSet = new HashSet<MCS>();
	HashSet<MCS> negatedMCSSet = new HashSet<MCS>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Outport))
	    {
		for (Relationship mcsRel : node.getRelationships(RelTypes.Is_Full_MCS_Of, Direction.INCOMING))
		{
		    Node fullmcs = mcsRel.getStartNode();
		    HashSet<MCSNode> mcsNodes = new HashSet<MCSNode>();
		    for (Relationship mcsNodeRel : fullmcs.getRelationships(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		    {
			Node mcsNode = mcsNodeRel.getStartNode();
			boolean negated = false;
			if (mcsNodeRel.getProperty("Negated").toString().equals("true"))
			{
			    negated = true;
			}
			mcsNodes.add(new MCSNode(mcsNode, negated));
		    }
		    mcsSet.add(new MCS(mcsNodes));
		}
		for (Relationship mcsRel : node.getRelationships(RelTypes.Is_Full_Negated_MCS_Of, Direction.INCOMING))
		{
		    Node fullmcs = mcsRel.getStartNode();
		    HashSet<MCSNode> mcsNodes = new HashSet<MCSNode>();
		    for (Relationship mcsNodeRel : fullmcs.getRelationships(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		    {
			Node mcsNode = mcsNodeRel.getStartNode();
			boolean negated = false;
			if (mcsNodeRel.getProperty("Negated").toString().equals("true"))
			{
			    negated = true;
			}
			mcsNodes.add(new MCSNode(mcsNode, negated));
		    }
		    negatedMCSSet.add(new MCS(mcsNodes));
		}
	    }
	    tx.success();
	}
	return new MCSPairSet(mcsSet, negatedMCSSet);
    }

    /**
     * Returns the Quantitative Result from a node.
     * @param node The node.
     * @return The Quantitative Result.
     */
    public static double getQuantitativeResult(Node node)
    {
	double result = -1;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    Relationship resultRel = node.getSingleRelationship(RelTypes.Is_Quant_Result_Of, Direction.INCOMING);
	    if (resultRel != null)
	    {
		result = (double) resultRel.getStartNode().getProperty("Result");
	    }
	    tx.success();
	}
	return result;
    }

}
