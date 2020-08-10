package neo4jDatabase;

import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jMCS.MCSNode;
import neo4jUtility.PrintUtility;

/**
 * Contains some utility methods for the database.
 * @author Kevin Bartik
 *
 */
public final class DBUtility
{

    /**
     * Private constructor so this class cannot be instantiated.
     */
    private DBUtility()
    {

    }

    /**
     * Returns the ElementID and Name in a String of a Node.
     * @param node The node.
     * @return The ElementID and Name in a String.
     */
    public static String getIDAndName(Node node)
    {
	StringBuilder idname = new StringBuilder();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasProperty("Name"))
	    {
		idname.append(node.getProperty("Name").toString() + " | ID: " + node.getProperty("ElementID").toString());
	    }
	    tx.success();
	}
	return idname.toString();
    }

    /**
     * Returns the ElementID and Name in a String of the node in the MCSNode.
     * Also returns negation.
     * @param node The MCSNode.
     * @return Negation, ElementID and Name in a String.
     */
    public static String getIDAndName(MCSNode node)
    {
	StringBuilder idname = new StringBuilder();
	if (node.isNegated())
	{
	    idname.append("<NOT> ");
	}
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.getNode().hasProperty("Name"))
	    {
		idname.append(node.getNode().getProperty("Name").toString() + " | ID: " + node.getNode().getProperty("ElementID").toString());
	    }
	    tx.success();
	}
	return idname.toString();
    }

    /**
     * Gets the relevant label of a node.
     * The relevant label is the label that does not start with Neo4j_.
     * @param node The node.
     * @return The relevant label.
     */
    public static NodeLabels getRelevantLabel(Node node)
    {
	NodeLabels nodeLabel = null;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    boolean stop = false;
	    for (Label label : node.getLabels())
	    {
		NodeLabels nlabel = NodeLabels.transformLabel(label);
		switch (nlabel)
		{
		    case Neo4J_EA_Element:
			break;
		    case Neo4J_Generated_Element:
			break;
		    default:
			if (nodeLabel == null)
			{
			    nodeLabel = nlabel;
			}
			else
			{
			    stop = true;
			    PrintUtility.printError("The Node (" + getIDAndName(node) + ") has multiple Main Labels which is not allowed!",
				    "The first Label found was " + nodeLabel + " and the second Label found was " + nlabel + ".");
			}
			break;
		}
		if (stop)
		{
		    break;
		}
	    }
	    tx.success();
	}
	return nodeLabel;
    }

    /**
     * Returns true if the node has the label. Otherwise false is returned.
     * @param node The Node.
     * @param label The Label.
     * @return true if the node has the label. Otherwise false.
     */
    public static boolean hasLabel(Node node, NodeLabels label)
    {
	boolean result = false;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(label))
	    {
		result = true;
	    }
	    tx.success();
	}
	return result;
    }

    /**
     * Returns true if the database contains any CFT. Otherwise false is returned.
     * @return true if the database contains any CFT. Otherwise false.
     */
    public static boolean containsCFT()
    {
	boolean result = false;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    ResourceIterator<Node> te = DBConnection.getGraphDB().findNodes(NodeLabels.CFT);
	    if (te.hasNext())
	    {
		result = true;
	    }
	    te.close();
	    tx.success();
	}
	return result;
    }

    /**
     * Gets all nodes with a specific label.
     * Their ElementID and Name are put into a String array and they are added to a set.
     * @param label The label.
     * @return The set that contains the string arrays.
     */
    public static HashSet<String[]> getElementsbyLabel(NodeLabels label)
    {
	HashSet<String[]> elements = new HashSet<String[]>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    DBConnection.getGraphDB().findNodes(label).forEachRemaining(node ->
	    {
		String[] element = new String[2];
		element[0] = node.getProperty("ElementID").toString();
		element[1] = node.getProperty("Name").toString();
		elements.add(element);
	    });
	    tx.success();
	}
	return elements;
    }

    /**
     * Returns all outports of a CFT.
     * Their ElementID and Name are put into a String array and they are added to a set.
     * @param cftid The ElementID of the CFT.
     * @return The set that contains the string arrays.
     */
    public static HashSet<String[]> getOutportsOfCFT(long cftid)
    {
	HashSet<String[]> elements = new HashSet<String[]>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    for (Relationship rel : DBConnection.getGraphDB().findNode(NodeLabels.CFT, "ElementID", cftid).getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
	    {
		Node startNode = rel.getStartNode();
		if (startNode.hasLabel(NodeLabels.CFT_Outport))
		{
		    String[] element = new String[2];
		    element[0] = startNode.getProperty("ElementID").toString();
		    element[1] = startNode.getProperty("Name").toString();
		    elements.add(element);
		}
	    }
	    tx.success();
	}
	return elements;
    }

    /**
     * Returns all nodes with the specified label and parent.
     * @param label The label.
     * @param parent The parent node.
     * @return The set with all the node that have the specified label and parent.
     */
    public static HashSet<Node> getElementsbyLabelandParent(NodeLabels label, Node parent)
    {
	HashSet<Node> elements = new HashSet<Node>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    for (Relationship rel : parent.getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
	    {
		Node startNode = rel.getStartNode();
		if (startNode.hasLabel(label))
		{
		    elements.add(startNode);
		}
	    }
	    tx.success();
	}
	return elements;
    }

    /**
     * Returns the CFT of a node.
     * @param node The node.
     * @return The CFT of the node.
     */
    public static Node getCFTofNode(Node node)
    {
	Node cft = null;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    try
	    {
		cft = node.getSingleRelationship(RelTypes.Is_Child_Of, Direction.OUTGOING).getEndNode();
	    }
	    catch (RuntimeException re)
	    {
		PrintUtility.printError("The Element (" + DBUtility.getIDAndName(node) + ") has multiple Parent Elements which is not allowed!");
	    }
	    tx.success();
	}
	return cft;
    }

    /**
     * Returns the labels of Node as a String.
     * @param node The node.
     * @return The labels of the Node as a String.
     */
    public static String getLabelsAsString(Node node)
    {
	StringBuilder str = new StringBuilder("Labels found for the Node (" + getIDAndName(node) + "): ");
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    for (Label label : node.getLabels())
	    {
		NodeLabels nodeLabel = NodeLabels.transformLabel(label);
		if (!nodeLabel.equals(NodeLabels.Neo4J_EA_Element) && !nodeLabel.equals(NodeLabels.Neo4J_Generated_Element))
		{
		    str.append(label + ", ");
		}
	    }
	    tx.success();
	}
	str.deleteCharAt(str.lastIndexOf(", "));
	str.append(".");
	return str.toString();
    }

    /**
     * Gets the inport instances of the node.
     * @param node The node.
     * @return The inport instances of the node.
     */
    public static HashSet<Node> getInportInstances(Node node)
    {
	HashSet<Node> inportInsts = new HashSet<Node>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Outport_Instance))
	    {
		for (Relationship inportInst : node.getSingleRelationship(RelTypes.Is_Child_Of, Direction.OUTGOING).getEndNode().getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
		{
		    Node inportInstance = inportInst.getStartNode();
		    if (inportInstance.hasLabel(NodeLabels.CFT_Inport_Instance))
		    {
			inportInsts.add(inportInstance);
		    }
		}
	    }
	    else
	    {
		if (node.hasLabel(NodeLabels.CFT_Outport))
		{
		    for (Relationship cftInstanceRel : node.getSingleRelationship(RelTypes.Is_Child_Of, Direction.OUTGOING).getEndNode().getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
		    {
			Node cftInstance = cftInstanceRel.getStartNode();
			if (cftInstance.hasLabel(NodeLabels.CFT_Instance))
			{
			    for (Relationship inportInstanceRel : cftInstance.getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
			    {
				Node inportInstance = inportInstanceRel.getStartNode();
				if (inportInstance.hasLabel(NodeLabels.CFT_Inport_Instance))
				{
				    inportInsts.add(inportInstance);
				}
			    }
			}
		    }
		}
	    }
	    tx.success();
	}
	return inportInsts;
    }

    /**
     * Returns the classifier inport of an inport instance.
     * @param node The inport instance.
     * @return The classifier inport.
     */
    public static Node getInportFromInportInstance(Node node)
    {
	Node inport = null;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    if (node.hasLabel(NodeLabels.CFT_Inport_Instance))
	    {
		inport = node.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode();
	    }
	    tx.success();
	}
	return inport;
    }

    /**
     * Returns all node with the specified label.
     * @param label The specified label.
     * @return The node with the specified label.
     */
    public static HashSet<Node> getAllNodesWithLabel(NodeLabels label)
    {
	HashSet<Node> nodes = new HashSet<Node>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    DBConnection.getGraphDB().findNodes(label).forEachRemaining(node ->
	    {
		nodes.add(node);
	    });
	    tx.success();
	}
	return nodes;
    }

    /**
     * Returns true if the database contains MCS. Otherwise false.
     * @return true if the database contains MCS. Otherwise false.
     */
    public static boolean hasMCS()
    {
	boolean hasMCS = false;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    for (Label mcs : DBConnection.getGraphDB().getAllLabelsInUse())
	    {
		if (NodeLabels.isMCSLabel(mcs))
		{
		    hasMCS = true;
		    break;
		}
	    }
	    tx.success();
	}
	return hasMCS;
    }

    /**
     * Returns true if the node is in the set of error nodes.
     * @param node The node.
     * @return true if the node is in the set of error nodes.
     */
    public static boolean isErrorNode(Node node)
    {
	return DBConnection.errorNodes.contains(node);
    }

    /**
     * Returns the basic failure probability of a node.
     * @param node The node.
     * @return the basic failure probability of a node.
     */
    public static double findProbability(Node node)
    {
	double result = -1;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    result = Double.parseDouble(node.getProperty("Basic Failure Probability").toString());
	    tx.success();
	}
	return result;
    }

    /**
     * Return the node with the specified ElementID.
     * @param id The specified ElementID.
     * @return The node with the specified ElementID.
     */
    public static Node getNodebyID(long id)
    {
	Node node;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    node = DBConnection.getGraphDB().findNode(NodeLabels.Neo4J_EA_Element, "ElementID", id);
	    if (node == null) {
		node = DBConnection.getGraphDB().findNode(NodeLabels.Neo4J_Generated_Element, "ElementID", id);
	    }
	    tx.success();
	}
	return node;
    }

    /**
     * Clears the database.
     * Deletes all nodes and relationships.
     */
    public static void clearDB()
    {
	PrintUtility.printInfo("Clearing Neo4j database...");
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    DBConnection.getGraphDB().execute("MATCH (n) DETACH DELETE n");
	    tx.success();
	}
	PrintUtility.printInfo("Neo4j database was successfully cleared!");
    }

    /**
     * Deletes all forms of MCS and Quantitative Results.
     */
    public static void clearMCS()
    {
	PrintUtility.printInfo("Clearing Minimal Cut Sets and Quantitative Analysis Results from the Neo4j database...");
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    DBConnection.getGraphDB().execute("MATCH (n:MCS) DETACH DELETE n");
	    DBConnection.getGraphDB().execute("MATCH (n:Negated_MCS) DETACH DELETE n");
	    DBConnection.getGraphDB().execute("MATCH (n:Full_MCS) DETACH DELETE n");
	    DBConnection.getGraphDB().execute("MATCH (n:Full_Negated_MCS) DETACH DELETE n");
	    DBConnection.getGraphDB().execute("MATCH (n:Quant_Result) DETACH DELETE n");
	    tx.success();
	}
	PrintUtility.printInfo("Minimal Cut Sets and Quantitative Analysis Results were successfully cleared from the Neo4j database!");
    }
}
