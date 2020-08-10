package neo4jDatabase;

import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.Element;
import org.sparx.Repository;
import org.sparx.TaggedValue;

import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jUtility.GeneralLogger;
import neo4jUtility.PrintUtility;

/**
 * This class handles all updates to the Neo4j database.
 * @author Kevin Bartik
 *
 */
public final class DBUpdater
{

    /**
     * Node that were modified by an update.
     */
    private static HashSet<Node> changedNodes = new HashSet<Node>();

    /**
     * Private constructor so this class cannot be instantiated.
     */
    private DBUpdater() {
	
    }
    
    /**
     * Updates the Neo4j database with elements and connectors from a EA repository.
     * @param eapfilepath The file path to the EA repository.
     */
    public static void updateFromEA(String eapfilepath)
    {
	Repository repository = new org.sparx.Repository();
	PrintUtility.printInfo("Connecting to EA repository...");
	Boolean a = repository.OpenFile(eapfilepath);
	if (!a)
	{
	    PrintUtility.printInfo("Error: Connection to EA repository unsuccessful!");
	    repository.CloseFile();
	}
	else
	{
	    PrintUtility.printInfo("Connection to EA repository successful!");
	    @SuppressWarnings("unchecked")
	    Collection<Element> eaElements = repository.GetElementSet(
		    "SELECT element.Object_ID FROM t_object AS element WHERE element.Stereotype IN (\"FT\", \"FTInstance\", \"CFT\", \"CFTInstance\", \"IESELogicalComponent\", \"IESELogicalComponentInstance\", \"IESELogicalInport\", \"IESELogicalInportInstance\", \"IESELogicalOutport\", \"IESELogicalOutportInstance\", \"FTAND\", \"FTOR\", \"FTM/N\", \"FTXOR\", \"FTBasicEvent\", \"FTNOT\", \"InputFailureMode\", \"OutputFailureMode\")",
		    2);
	    HashSet<Integer> idSet = new HashSet<Integer>();
	    HashMap<Node, HashSet<EARelationship>> connectorMap = new HashMap<Node, HashSet<EARelationship>>();
	    for (Element eaElement : eaElements)
	    {
		idSet.add(eaElement.GetElementID());
		updateEANode(eaElement, connectorMap);
	    }
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		DBConnection.getGraphDB().getAllNodes().iterator().forEachRemaining(node ->
		{
		    if (node.hasLabel(NodeLabels.Neo4J_EA_Element) && !idSet.contains(Integer.parseInt(node.getProperty("ElementID").toString())))
		    {
			DBMCSManager.deleteMCSAndResults(node, false);
			for (Relationship rel : node.getRelationships())
			{
			    rel.delete();
			}
			node.delete();
			PrintUtility.printInfo("Expired EA Elements in Neo4j database were deleted!");
		    }
		});
		tx.success();
	    }
	    for (Node node : connectorMap.keySet())
	    {
		updateEARelationships(node, connectorMap.get(node));
	    }
	    for (Node cNode : changedNodes)
	    {
		DBConnection.uncheckedNodes.add(cNode);
		DBMCSManager.deleteMCSAndResults(cNode, false);
	    }
	    changedNodes.clear();
	    PrintUtility.printInfo("Disconnecting from EA repository...");
	    repository.CloseFile();
	    PrintUtility.printInfo("Disconnection from EA repository successful!");
	    PrintUtility.printInfo("Neo4j database update complete!");
	}
    }

    /**
     * Updates an EA node in the Neo4j database.
     * @param eaElement The EA element of the node that is updated.
     * @param connectorMap The Map that stores all connectors of each node.
     */
    private static void updateEANode(Element eaElement, HashMap<Node, HashSet<EARelationship>> connectorMap)
    {
	int elementid = eaElement.GetElementID();
	int parentid = eaElement.GetParentID();
	int classifierid = eaElement.GetClassifierID();
	NodeLabels label = NodeLabels.getLabelForEA(eaElement.GetStereotype(), classifierid != 0);
	Node node;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    node = DBConnection.getGraphDB().findNode(NodeLabels.Neo4J_EA_Element, "ElementID", elementid);
	    if (node == null)
	    {
		node = DBConnection.getGraphDB().createNode(NodeLabels.Neo4J_EA_Element, label);
		node.setProperty("ElementID", elementid);
		node.setProperty("Name", eaElement.GetName());
		changedNodes.add(node);
		PrintUtility.printInfo("A new EA Element (" + eaElement.GetName() + " | " + label + ") was added to the Neo4j database!");
	    }
	    else
	    {
		if (!node.getProperty("Name").toString().equals(eaElement.GetName()))
		{
		    node.setProperty("Name", eaElement.GetName());
		}
		if (!node.hasLabel(label))
		{
		    for (Label l : node.getLabels())
		    {
			if (!l.name().equals("Neo4J_EA_Element"))
			{
			    node.removeLabel(l);
			}
		    }
		    node.addLabel(label);
		    changedNodes.add(node);
		    PrintUtility.printInfo("An EA Element (" + eaElement.GetName() + " | " + label + ") in the Neo4j database was updated!");
		}
	    }
	    tx.success();
	}
	connectorMap.put(node, new HashSet<EARelationship>());
	HashSet<EARelationship> eaRels = connectorMap.get(node);
	if (parentid != 0)
	{
	    eaRels.add(new EARelationship(node, parentid, RelTypes.Is_Child_Of));
	}
	if (classifierid != 0)
	{
	    eaRels.add(new EARelationship(node, classifierid, RelTypes.Is_Instance_Of));
	}
	Collection<Connector> eaConnectors = eaElement.GetConnectors();
	for (Connector eaConnector : eaConnectors)
	{
	    if (eaConnector.GetClientID() == elementid && RelTypes.isRelevantStereotype(eaConnector.GetStereotype()))
	    {// Only Connectors that start at this Element
		eaRels.add(new EARelationship(eaConnector));
	    }
	}
	if (NodeLabels.isLabelforTaggedValues(label))
	{
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		TaggedValue tv = eaElement.GetTaggedValues().GetByName("cValue");
		String property = null;
		if (tv != null)
		{
		    property = "Basic Failure Probability";
		}
		else
		{
		    tv = eaElement.GetTaggedValues().GetByName("m");
		    if (tv != null)
		    {
			property = "MOONNumber";
		    }
		}
		if (property != null)
		{
		    if (node.hasProperty(property))
		    {
			if (!node.getProperty(property).toString().equals(tv.GetValue().toString()))
			{
			    node.setProperty(property, tv.GetValue());
			    changedNodes.add(node);
			    PrintUtility.printInfo("An EA Element (" + eaElement.GetName() + " | " + label + ") in the Neo4j database was updated!");
			}
		    }
		    else
		    {
			node.setProperty(property, tv.GetValue());
			changedNodes.add(node);
			PrintUtility.printInfo("An EA Element (" + eaElement.GetName() + " | " + label + ") in the Neo4j database was updated!");
		    }
		}
		tx.success();
	    }
	}
    }

    /**
     * Updates the relationships in the Neo4j database.
     * @param node The node where the relationships are updated.
     * @param eaRelsSet The set of EARelationships.
     */
    private static void updateEARelationships(Node node, HashSet<EARelationship> eaRelsSet)
    {
	HashSet<EARelationship> existingRelsSet = new HashSet<EARelationship>();
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    for (Relationship rel : node.getRelationships(Direction.OUTGOING)) // Only outgoing EARelationship were saved during the update
	    {
		if (rel.hasProperty("ConnectorID")) // Only Relationship that were created from EA
		{
		    existingRelsSet.add(new EARelationship(rel));
		}
	    }
	    tx.success();
	}
	// EARelationships are comparable with equals
	HashSet<EARelationship> saveRelSet = new HashSet<EARelationship>(existingRelsSet);
	existingRelsSet.removeAll(eaRelsSet);
	eaRelsSet.removeAll(saveRelSet);
	for (EARelationship eaRel : existingRelsSet)
	{
	    if ((eaRel.relType.equals(RelTypes.Failure_Propagation) || eaRel.relType.equals(RelTypes.Is_Child_Of) || eaRel.relType.equals(RelTypes.Is_Instance_Of)) && !changedNodes.contains(node))
	    {
		changedNodes.add(node);
	    }
	    eaRel.deleteRelationship();
	}
	for (EARelationship eaRel : eaRelsSet)
	{
	    if ((eaRel.relType.equals(RelTypes.Failure_Propagation) || eaRel.relType.equals(RelTypes.Is_Child_Of) || eaRel.relType.equals(RelTypes.Is_Instance_Of)) && !changedNodes.contains(node))
	    {
		changedNodes.add(node);
	    }
	    eaRel.addRelationship();
	}
    }

    /**
     * Adds elements from the EA extension with a batch inserter.
     * @param elements The elements in a string array.
     * @param partSeparator The used part separator.
     * @param tgSeparator The used tagged value separator.
     */
    public static void addElementsBatch(String[] elements, String partSeparator, String tgSeparator)
    {
	for (int i = 2; i < elements.length; i++)
	{
	    DBConnection.batchproperties.clear();
	    String[] parts = elements[i].trim().split(partSeparator);
	    long elementid = Long.parseLong(parts[0].trim());
	    DBConnection.batchproperties.put("ElementID", elementid);
	    DBConnection.batchproperties.put("Name", parts[1].trim());
	    NodeLabels label = NodeLabels.getLabelForEA(parts[2].trim(), Long.parseLong(parts[3].trim()) != 0);
	    for (int j = 4; j < parts.length; j++)
	    {
		String[] tg = parts[j].trim().split(tgSeparator);
		DBConnection.batchproperties.put(tg[0].trim(), tg[1].trim());
	    }
	    DBConnection.getBatchInserter().createNode(elementid, DBConnection.batchproperties, NodeLabels.Neo4J_EA_Element, label);
	}
    }

    /**
     * Adds connectors from the EA extension with a batch inserter.
     * @param connectors The connectors in a string array.
     * @param partSeparator The used part separator.
     */
    public static void addConnectorsBatch(String[] connectors, String partSeparator)
    {
	DBConnection.batchproperties.clear();
	for (int i = 2; i < connectors.length; i++)
	{
	    String[] parts = connectors[i].trim().split(partSeparator);
	    long connectorid = Long.parseLong(parts[0].trim());
	    DBConnection.batchproperties.put("ConnectorID", connectorid);
	    RelTypes reltype = RelTypes.getRelTypeForEA(parts[1].trim());
	    DBConnection.getBatchInserter().createRelationship(Long.parseLong(parts[2].trim()), Long.parseLong(parts[3].trim()), reltype, DBConnection.batchproperties);
	}
    }

    /**
     * The regular update form the EA extension.
     * @param message The message that contains what needs to be updated.
     * @param singleSeparator The used single command separator.
     * @param partSeparator The used part separator.
     * @param tgSeparator The used tagged value separator.
     */
    public static void updateDB(String[] message, String singleSeparator, String partSeparator, String tgSeparator)
    {
	for (int i = 2; i < message.length; i++)
	{
	    String[] splitCommand = message[i].trim().split(singleSeparator);
	    String command = splitCommand[0].trim();
	    String[] parts = splitCommand[1].trim().split(partSeparator);
	    String error = "";
	    switch (command)
	    {
		case "add single element":
		    if (DBUtility.getNodebyID(Long.parseLong(parts[0].trim())) == null)
		    {
			try (Transaction tx = DBConnection.getGraphDB().beginTx())
			{
			    Node node = DBConnection.getGraphDB().createNode(NodeLabels.getLabelForEA(parts[2].trim(), Long.parseLong(parts[3].trim()) != 0));
			    node.addLabel(NodeLabels.Neo4J_EA_Element);
			    node.setProperty("ElementID", Long.parseLong(parts[0].trim()));
			    node.setProperty("Name", parts[1].trim());
			    for (int j = 4; j < parts.length; j++)
			    {
				String[] tg = parts[j].trim().split(tgSeparator);
				node.setProperty(tg[0].trim(), tg[1].trim());
			    }
			    DBConnection.uncheckedNodes.add(node);
			    tx.success();
			}
		    }
		    break;
		case "delete single element":
		    Node node = DBUtility.getNodebyID(Long.parseLong(parts[0]));
		    if (node != null)
		    {
			DBMCSManager.deleteMCSAndResults(node, false);
			DBConnection.uncheckedNodes.remove(node);
			try (Transaction tx = DBConnection.getGraphDB().beginTx())
			{
			    for (Relationship rel : node.getRelationships())
			    {
				rel.delete();
			    }
			    node.delete();
			    tx.success();
			}
		    }
		    else
		    {
			error = "An element that should be deleted was not found!";
		    }
		    break;
		case "update single element":
		    Node node2 = DBUtility.getNodebyID(Long.parseLong(parts[0]));
		    if (node2 != null)
		    {
			try (Transaction tx = DBConnection.getGraphDB().beginTx())
			{
			    NodeLabels label = NodeLabels.getLabelForEA(parts[2].trim(), Long.parseLong(parts[3].trim()) != 0);
			    if (!node2.hasLabel(label))
			    {
				for (Label oldlabel : node2.getLabels())
				{
				    if (!oldlabel.name().equals(NodeLabels.Neo4J_EA_Element.name()))
				    {
					node2.removeLabel(oldlabel);
				    }
				}
				node2.addLabel(label);
				DBMCSManager.deleteMCSAndResults(node2, false);
				DBConnection.uncheckedNodes.add(node2);
			    }
			    if (node2.getProperty("Name").toString().equals(parts[1].trim()))
			    {
				node2.setProperty("Name", parts[1].trim());
			    }
			    // Only two possible properties
			    HashMap<String, String> taggedvalues = new HashMap<String, String>();
			    for (int j = 4; j < parts.length; j++)
			    {
				String[] tg = parts[j].trim().split(tgSeparator);
				taggedvalues.put(tg[0], tg[1]);
			    }
			    if (!taggedvalues.isEmpty())
			    {
				if (node2.hasLabel(NodeLabels.CFT_Basic_Event) || node2.hasLabel(NodeLabels.CFT_Inport))
				{
				    String bfp = "Basic Failure Propagation";
				    if (node2.hasProperty(bfp) && taggedvalues.containsKey(bfp))
				    {
					if (!node2.getProperty(bfp).toString().equals(taggedvalues.get(bfp)))
					{
					    DBMCSManager.deleteOnlyResults(node2);
					    node2.setProperty(bfp, taggedvalues.get(bfp));
					}
				    }
				    else
				    {
					DBMCSManager.deleteOnlyResults(node2);
					node2.setProperty(bfp, 0.000001);
				    }
				}
				else
				{
				    if (node2.hasLabel(NodeLabels.CFT_MOON_Gate))
				    {
					String mn = "MOONNumber";
					if (node2.hasProperty(mn) && taggedvalues.containsKey(mn))
					{
					    if (!node2.getProperty(mn).toString().equals(taggedvalues.get(mn)))
					    {
						DBMCSManager.deleteOnlyResults(node2);
						node2.setProperty(mn, taggedvalues.get(mn));
					    }
					}
					else
					{
					    DBMCSManager.deleteMCSAndResults(node2, false);
					    node2.setProperty(mn, 1);
					}
				    }
				}
			    }
			    tx.success();
			}
		    }
		    else
		    {
			error = "An element that should be updated was not found!";
		    }
		    break;
		case "add single connector":
		    Node start = DBUtility.getNodebyID(Long.parseLong(parts[2].trim()));
		    Node end = DBUtility.getNodebyID(Long.parseLong(parts[3].trim()));
		    if (start != null && end != null)
		    {
			try (Transaction tx = DBConnection.getGraphDB().beginTx())
			{
			    RelTypes reltype = RelTypes.getRelTypeForEA(parts[1].trim());
			    long connectorID = Long.parseLong(parts[0].trim());
			    if (start.hasRelationship(reltype, Direction.OUTGOING))
			    {
				boolean relexistsalready = false;
				for (Relationship rel : start.getRelationships(reltype, Direction.OUTGOING))
				{
				    long conid = Long.parseLong(rel.getProperty("ConnectorID").toString());
				    if (conid == connectorID && rel.getEndNode().equals(end))
				    {
					relexistsalready = true;
				    }
				}
				if (!relexistsalready)
				{
				    Relationship rel = start.createRelationshipTo(end, reltype);
				    rel.setProperty("ConnectorID", connectorID);
				    if (reltype.equals(RelTypes.Failure_Propagation))
				    {
					DBMCSManager.deleteMCSAndResults(end, false);
				    }
				}
			    }
			    else
			    {
				Relationship rel = start.createRelationshipTo(end, reltype);
				rel.setProperty("ConnectorID", connectorID);
				if (reltype.equals(RelTypes.Failure_Propagation))
				{
				    DBMCSManager.deleteMCSAndResults(end, false);
				}
			    }
			    tx.success();
			}
		    }
		    else
		    {
			error = "An connector could not be added because one or both elements are missing!";
		    }
		    break;
		case "delete single connector":
		    Node start2 = DBUtility.getNodebyID(Long.parseLong(parts[2].trim()));
		    Node end2 = DBUtility.getNodebyID(Long.parseLong(parts[3].trim()));
		    if (start2 != null && end2 != null)
		    {
			try (Transaction tx = DBConnection.getGraphDB().beginTx())
			{
			    RelTypes reltype = RelTypes.getRelTypeForEA(parts[1].trim());
			    long connectorID = Long.parseLong(parts[0].trim());
			    if (start2.hasRelationship(reltype, Direction.OUTGOING))
			    {
				boolean wasdeleted = false;
				for (Relationship rel : start2.getRelationships(reltype, Direction.OUTGOING))
				{
				    long conid = Long.parseLong(rel.getProperty("ConnectorID").toString());
				    if (conid == connectorID && rel.getEndNode().equals(end2))
				    {
					if (reltype.equals(RelTypes.Failure_Propagation))
					{
					    DBMCSManager.deleteMCSAndResults(end2, false);
					}
					rel.delete();
					wasdeleted = true;
				    }
				}
				if (!wasdeleted)
				{
				    error = "An connector could not be deleted because it was not found!";
				}
			    }
			    else
			    {
				error = "An connector could not be deleted because it was not found!";
			    }
			    tx.success();
			}
		    }
		    else
		    {
			error = "An connector could not be deleted because it was not found!";
		    }
		    break;
		case "update single connector":
		    Node start3 = DBUtility.getNodebyID(Long.parseLong(parts[2].trim()));
		    Node end3 = DBUtility.getNodebyID(Long.parseLong(parts[3].trim()));
		    if (start3 != null && end3 != null)
		    {
			try (Transaction tx = DBConnection.getGraphDB().beginTx())
			{
			    RelTypes reltype = RelTypes.getRelTypeForEA(parts[1].trim());
			    String connectorID = parts[0].trim();
			    boolean noChange = false;
			    if (start3.hasRelationship(reltype, Direction.OUTGOING) && end3.hasRelationship(reltype, Direction.INCOMING))
			    {
				for (Relationship rel : start3.getRelationships(reltype, Direction.INCOMING))
				{
				    if (rel.getProperty("ConnectorID").toString().equals(connectorID))
				    {
					noChange = true;
					break;
				    }
				}
			    }
			    if (!noChange)
			    {
				try
				{
				    DBConnection.getGraphDB().execute("MATCH (n)-[rel:" + reltype.name() + "]->(m) WHERE rel.ConnectorID = " + connectorID + " DELETE rel");
				}
				catch (QueryExecutionException qe)
				{
				    error = "The connector was not in the database before it was updated!";
				}
				Relationship newrel = start3.createRelationshipTo(end3, reltype);
				newrel.setProperty("ConnectorID", Long.parseLong(parts[0].trim()));
				if (reltype.equals(RelTypes.Failure_Propagation))
				{
				    changedNodes.add(end3);
				}
			    }
			    tx.success();
			}
		    }
		    else
		    {
			error = "The connector cannot be update because nodes are missing!";
		    }
		    break;
		default:
		    break;
	    }
	    if (!error.equals(""))
	    {
		GeneralLogger.log("Error: " + error);
		PrintUtility.printError(error);
	    }
	    for (Node cNode : changedNodes) {
		DBConnection.uncheckedNodes.add(cNode);
		DBMCSManager.deleteMCSAndResults(cNode, false);
	    }
	    changedNodes.clear();
	}
    }

    /**
     * A special class that is used to store relationships before they are added to the database.
     * It allows comparison of connectors from EA and relationships stored in the database.
     * @author Kevin Bartik
     *
     */
    private static class EARelationship
    {

	private int connectorID;

	private Node startNode;

	private int endNodeID;

	private RelTypes relType;

	private Relationship relship;

	private EARelationship(Node startNode, int endNodeID, RelTypes relType)
	{
	    connectorID = endNodeID;
	    this.startNode = startNode;
	    this.endNodeID = endNodeID;
	    this.relType = relType;
	}

	private EARelationship(Connector eaconnector)
	{
	    this.connectorID = eaconnector.GetConnectorID();
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		startNode = DBConnection.getGraphDB().findNode(NodeLabels.Neo4J_EA_Element, "ElementID", eaconnector.GetClientID());
		tx.success();
	    }
	    if (startNode == null)
	    {
		PrintUtility.printError("The start node of the EA Relationship is not in the database!");
	    }
	    this.endNodeID = eaconnector.GetSupplierID();
	    this.relType = RelTypes.getRelTypeForEA(eaconnector.GetStereotype());
	}

	private EARelationship(Relationship rel)
	{
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		connectorID = Integer.parseInt(rel.getProperty("ConnectorID").toString());
		startNode = rel.getStartNode();
		endNodeID = Integer.parseInt(rel.getEndNode().getProperty("ElementID").toString());
		relType = RelTypes.transformType(rel.getType());
		relship = rel;
		tx.success();
	    }
	}

	private void deleteRelationship()
	{
	    if (relship == null)
	    {
		PrintUtility.printError("EA Relationship is not in the database and cannot be deleted!");
	    }
	    else
	    {
		try (Transaction tx = DBConnection.getGraphDB().beginTx())
		{
		    DBConnection.uncheckedNodes.add(startNode);
		    DBConnection.uncheckedNodes.add(relship.getOtherNode(startNode));
		    relship.delete();
		    tx.success();
		}
		PrintUtility.printInfo("EA Relationship was successfully deleted!");
	    }
	}

	private void addRelationship()
	{
	    if (relship != null)
	    {
		PrintUtility.printError("The EA Relationship is already in the database!");
	    }
	    else
	    {
		try (Transaction tx = DBConnection.getGraphDB().beginTx())
		{
		    Node endnode = DBConnection.getGraphDB().findNode(NodeLabels.Neo4J_EA_Element, "ElementID", endNodeID);
		    if (endnode == null)
		    {
			PrintUtility.printError("The end node of the EA Relationship was not found so no EA Relationship was created!");
		    }
		    else
		    {
			Relationship relship = startNode.createRelationshipTo(endnode, relType);
			relship.setProperty("ConnectorID", connectorID);
			DBConnection.uncheckedNodes.add(startNode);
			DBConnection.uncheckedNodes.add(endnode);
			PrintUtility.printInfo("EA Relationship was successfully added to the Neo4j database!");
		    }
		    tx.success();
		}
	    }
	}

	@Override
	public int hashCode()
	{
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + connectorID;
	    result = prime * result + endNodeID;
	    result = prime * result + ((relType == null) ? 0 : relType.hashCode());
	    result = prime * result + ((startNode == null) ? 0 : startNode.hashCode());
	    return result;
	}

	@Override
	public boolean equals(Object obj)
	{
	    if (this == obj)
	    {
		return true;
	    }
	    if (obj == null || getClass() != obj.getClass())
	    {
		return false;
	    }
	    EARelationship other = (EARelationship) obj;
	    if (connectorID != other.connectorID || endNodeID != other.endNodeID || relType != other.relType)
	    {
		return false;
	    }
	    if (startNode == null || other.startNode == null || !startNode.equals(other.startNode))
	    {
		return false;
	    }
	    return true;
	}

	@Override
	public String toString()
	{
	    return "EARelationship [connectorID=" + connectorID + ", startNode=" + startNode + ", endNodeID=" + endNodeID + ", relType=" + relType + ", relship=" + relship + "]";
	}
    }
}
