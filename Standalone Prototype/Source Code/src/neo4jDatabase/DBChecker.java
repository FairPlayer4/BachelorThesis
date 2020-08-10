package neo4jDatabase;

import java.util.ArrayList;
import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jUtility.PrintUtility;

/**
 * This class is used to check the database for errors.
 * @author Kevin Bartik
 *
 */
public final class DBChecker
{
    /**
     * List that stores sets of CFT.
     * Used for finding deep cycles in CFT.
     */
    private static ArrayList<HashSet<Node>> cftSetList = new ArrayList<HashSet<Node>>();

    /**
     * Private constructor so this class cannot be instantiated.
     */
    private DBChecker() {
	
    }
    
    /**
     * Checks for deep cycles in all CFTs.
     * Is an alternative to the cycle traversal but does not find all cycles in CFTs.
     * @return true if a deep cycle was found. Otherwise false.
     */
    public static boolean hasCFTCycles()
    {
	if (DBConnection.cftCycleChecked)
	{
	    return false;
	}
	boolean hasCFTCycle = false;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    ResourceIterator<Node> cftNodesIterator = DBConnection.getGraphDB().findNodes(NodeLabels.CFT);
	    while (cftNodesIterator.hasNext())
	    {
		Node cft = cftNodesIterator.next();
		boolean alreadyChecked = false;
		for (HashSet<Node> cftSet : cftSetList)
		{
		    if (cftSet.contains(cft))
		    {
			alreadyChecked = true;
			break;
		    }
		}
		if (!alreadyChecked)
		{
		    cftSetList.add(new HashSet<Node>());
		    if (!hasCFTCycle)
		    {
			hasCFTCycle = hasCFTCycle(cft, cftSetList.size() - 1);
		    }
		}
		if (hasCFTCycle)
		{
		    break;
		}
	    }
	    tx.success();
	}
	DBConnection.cftCycleChecked = !hasCFTCycle;
	return hasCFTCycle;
    }

    /**
     * Checks the next CFT for Cycles.
     * @param cft The CFT that is checked for cycles.
     * @param index The current index in the cftSetList.
     * @return true if the CFT has a cycle. Otherwise false.
     */
    private static boolean hasCFTCycle(Node cft, int index)
    {
	boolean hasCFTCycle = !cftSetList.get(index).add(cft);
	if (!hasCFTCycle)
	{
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		for (Relationship cftInstanceRel : cft.getRelationships(RelTypes.Is_Child_Of, Direction.INCOMING))
		{
		    Node cftInstance = cftInstanceRel.getStartNode();
		    if (cftInstance.hasLabel(NodeLabels.CFT_Instance))
		    {
			Node cftFromCFTInstance = cftInstance.getSingleRelationship(RelTypes.Is_Instance_Of, Direction.OUTGOING).getEndNode();
			boolean alreadyChecked = false;
			for (HashSet<Node> cftSet : cftSetList)
			{
			    if (cftSet.contains(cftFromCFTInstance))
			    {
				alreadyChecked = true;
				if (!hasCFTCycle)
				{
				    hasCFTCycle = cftSet.equals(cftSetList.get(index)); // Check if the current CFT set contains the cftFromCFTInstance
				}
				break;
			    }
			}
			if (!alreadyChecked)
			{
			    cftSetList.add(new HashSet<Node>(cftSetList.get(index)));
			    if (!hasCFTCycle)
			    {
				hasCFTCycle = hasCFTCycle(cftFromCFTInstance, cftSetList.size() - 1);
			    }
			}
			if (hasCFTCycle)
			{
			    break;
			}
		    }
		}
		tx.success();
	    }
	}
	return hasCFTCycle;
    }

    /**
     * Checks if the MCS are stored correctly.
     * Clears all MCS if errors are found.
     * Helpful when MCS are affected by changes outside the prototype.
     */
    public static void checkMCS()
    {
	DBConnection.mcsChecked = true;
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    DBConnection.getGraphDB().findNodes(NodeLabels.Full_MCS).forEachRemaining(node ->
	    {
		int number = 0;
		for (@SuppressWarnings("unused")
		Relationship rel : node.getRelationships(RelTypes.Is_Full_MCS_Of, Direction.OUTGOING))
		{
		    number++;
		    if (number > 1)
		    {
			DBConnection.mcsChecked = false;
			break;
		    }
		}
		if (number == 0)
		{
		    DBConnection.mcsChecked = false;
		}
		if (!node.hasRelationship(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		{
		    DBConnection.mcsChecked = false;
		}
	    });
	    DBConnection.getGraphDB().findNodes(NodeLabels.Full_Negated_MCS).forEachRemaining(node ->
	    {
		int number = 0;
		for (@SuppressWarnings("unused")
		Relationship rel : node.getRelationships(RelTypes.Is_Full_Negated_MCS_Of, Direction.OUTGOING))
		{
		    number++;
		    if (number > 1)
		    {
			DBConnection.mcsChecked = false;
			break;
		    }
		}
		if (number == 0)
		{
		    DBConnection.mcsChecked = false;
		}
		if (!node.hasRelationship(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		{
		    DBConnection.mcsChecked = false;
		}
	    });
	    DBConnection.getGraphDB().findNodes(NodeLabels.MCS).forEachRemaining(node ->
	    {
		int number = 0;
		for (@SuppressWarnings("unused")
		Relationship rel : node.getRelationships(RelTypes.Is_MCS_Of, Direction.OUTGOING))
		{
		    number++;
		    if (number > 1)
		    {
			DBConnection.mcsChecked = false;
			break;
		    }
		}
		if (number == 0)
		{
		    DBConnection.mcsChecked = false;
		}
		if (!node.hasRelationship(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		{
		    DBConnection.mcsChecked = false;
		}
	    });
	    DBConnection.getGraphDB().findNodes(NodeLabels.Negated_MCS).forEachRemaining(node ->
	    {
		int number = 0;
		for (@SuppressWarnings("unused")
		Relationship rel : node.getRelationships(RelTypes.Is_Negated_MCS_Of, Direction.OUTGOING))
		{
		    number++;
		    if (number > 1)
		    {
			DBConnection.mcsChecked = false;
			break;
		    }
		}
		if (number == 0)
		{
		    DBConnection.mcsChecked = false;
		}
		if (!node.hasRelationship(RelTypes.Is_Inside_MCS, Direction.INCOMING))
		{
		    DBConnection.mcsChecked = false;
		}
	    });
	    tx.success();
	}
	if (!DBConnection.mcsChecked)
	{
	    PrintUtility.printError("Invalid MCS were found! All MCS are cleared to assure correct analysis results!");
	    DBUtility.clearMCS();
	}
	DBConnection.mcsChecked = true;
    }

    /**
     * Checks all CFT nodes in the database for errors.
     * Depending on the label the relationships are checked and if there are any inconsistencies then the node is put into a list of error nodes.
     * @return The list of error nodes.
     */
    public static HashSet<Node> checkCFTModels()
    {
	if (!DBConnection.dbChecked)
	{
	    DBConnection.errorNodes = new HashSet<Node>();
	    try (Transaction tx = DBConnection.getGraphDB().beginTx())
	    {
		DBConnection.getGraphDB().findNodes(NodeLabels.Neo4J_EA_Element).forEachRemaining(node ->
		{
		    if (node.hasLabel(NodeLabels.CFT_Basic_Event) || node.hasLabel(NodeLabels.CFT_AND_Gate) || node.hasLabel(NodeLabels.CFT_OR_Gate) || node.hasLabel(NodeLabels.CFT_MOON_Gate)
			    || node.hasLabel(NodeLabels.CFT_XOR_Gate) || node.hasLabel(NodeLabels.CFT_NOT_Gate) || node.hasLabel(NodeLabels.CFT_Inport) || node.hasLabel(NodeLabels.CFT_Inport_Instance)
			    || node.hasLabel(NodeLabels.CFT_Outport) || node.hasLabel(NodeLabels.CFT_Outport_Instance) || node.hasLabel(NodeLabels.CFT) || node.hasLabel(NodeLabels.CFT_Instance))
		    {
			int outfailprop = 0;
			int infailprop = 0;
			int child = 0;
			int instance = 0;
			for (Relationship rel : node.getRelationships())
			{
			    RelTypes type = RelTypes.transformType(rel.getType());
			    if (rel.getStartNode().equals(node))
			    {
				switch (type)
				{
				    case Failure_Propagation:
					if (node.hasLabel(NodeLabels.CFT) || node.hasLabel(NodeLabels.CFT_Instance) || node.hasLabel(NodeLabels.CFT_Outport)
						|| node.hasLabel(NodeLabels.CFT_Inport_Instance))
					{
					    DBConnection.errorNodes.add(node);
					}
					else
					{
					    outfailprop++;
					}
					break;
				    case Is_Child_Of:
					child++;
					if (child > 1)
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_CFT_Of:
					if (!node.hasLabel(NodeLabels.CFT))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_Inside_MCS:
					if (!node.hasLabel(NodeLabels.CFT_Basic_Event) && !node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport_Instance))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_Instance_Of:
					if (!node.hasLabel(NodeLabels.CFT_Outport_Instance) && !node.hasLabel(NodeLabels.CFT_Inport_Instance) && !node.hasLabel(NodeLabels.CFT_Instance))
					{
					    DBConnection.errorNodes.add(node);
					}
					else
					{
					    instance++;
					    if (instance > 1)
					    {
						DBConnection.errorNodes.add(node);
					    }
					}
					break;
				    case Port_Propagation:
					if (!node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    default: // All other Types!
					DBConnection.errorNodes.add(node);
					break;
				}
			    }
			    else
			    {
				switch (type)
				{
				    case Failure_Propagation:
					infailprop++;
					if (node.hasLabel(NodeLabels.CFT_Basic_Event) || node.hasLabel(NodeLabels.CFT_Inport) || node.hasLabel(NodeLabels.CFT_Outport_Instance)
						|| node.hasLabel(NodeLabels.CFT) || node.hasLabel(NodeLabels.CFT_Instance))
					{
					    DBConnection.errorNodes.add(node);
					}
					else
					{
					    if (infailprop > 1 && (node.hasLabel(NodeLabels.CFT_NOT_Gate) || node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport)))
					    {
						DBConnection.errorNodes.add(node);
					    }
					}
					break;
				    case Is_Child_Of:
					if (!node.hasLabel(NodeLabels.CFT) && !node.hasLabel(NodeLabels.CFT_Instance))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_Instance_Of:
					if (!node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport) && !node.hasLabel(NodeLabels.CFT))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_MCS_Of:
					if (!node.hasLabel(NodeLabels.CFT_Inport_Instance) && !node.hasLabel(NodeLabels.CFT_Outport))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_Full_MCS_Of:
					if (!node.hasLabel(NodeLabels.CFT_Outport))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Port_Propagation:
					if (!node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    case Is_Quant_Result_Of:
					if (!node.hasLabel(NodeLabels.CFT_Outport))
					{
					    DBConnection.errorNodes.add(node);
					}
					break;
				    default:
					DBConnection.errorNodes.add(node);
					break;

				}
			    }
			    if (DBConnection.errorNodes.contains(node))
			    {
				break;
			    }
			}
			if (!DBConnection.errorNodes.contains(node))
			{
			    if (child == 0 && !node.hasLabel(NodeLabels.CFT))
			    {
				DBConnection.errorNodes.add(node);
			    }
			    if (instance == 0 && (node.hasLabel(NodeLabels.CFT_Instance) || node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport_Instance)))
			    {
				DBConnection.errorNodes.add(node);
			    }
			    if (node.hasLabel(NodeLabels.CFT_AND_Gate) || node.hasLabel(NodeLabels.CFT_OR_Gate) || node.hasLabel(NodeLabels.CFT_MOON_Gate) || node.hasLabel(NodeLabels.CFT_XOR_Gate)
				    || node.hasLabel(NodeLabels.CFT_NOT_Gate))
			    {
				if (outfailprop > 0 && infailprop == 0)
				{
				    DBConnection.errorNodes.add(node);
				}
			    }
			}
		    }
		});
		tx.success();
	    }
	    DBConnection.dbChecked = DBConnection.errorNodes.isEmpty();
	}
	else
	{
	    checkUpdatedCFTModel();
	}
	return DBConnection.errorNodes;
    }

    /**
     * Checks all modified CFT nodes in the database for errors.
     * Depending on the label the relationships are checked and if there are any inconsistencies then the node is put into a list of error nodes.
     * @return The list of error nodes.
     */
    private static HashSet<Node> checkUpdatedCFTModel()
    {
	try (Transaction tx = DBConnection.getGraphDB().beginTx())
	{
	    for (Node node : DBConnection.uncheckedNodes)
	    {
		if (node.hasLabel(NodeLabels.CFT_Basic_Event) || node.hasLabel(NodeLabels.CFT_AND_Gate) || node.hasLabel(NodeLabels.CFT_OR_Gate) || node.hasLabel(NodeLabels.CFT_MOON_Gate)
			|| node.hasLabel(NodeLabels.CFT_XOR_Gate) || node.hasLabel(NodeLabels.CFT_NOT_Gate) || node.hasLabel(NodeLabels.CFT_Inport) || node.hasLabel(NodeLabels.CFT_Inport_Instance)
			|| node.hasLabel(NodeLabels.CFT_Outport) || node.hasLabel(NodeLabels.CFT_Outport_Instance) || node.hasLabel(NodeLabels.CFT) || node.hasLabel(NodeLabels.CFT_Instance))
		{
		    int outfailprop = 0;
		    int infailprop = 0;
		    int child = 0;
		    int instance = 0;
		    for (Relationship rel : node.getRelationships())
		    {
			RelTypes type = RelTypes.transformType(rel.getType());
			if (rel.getStartNode().equals(node))
			{
			    switch (type)
			    {
				case Failure_Propagation:
				    if (node.hasLabel(NodeLabels.CFT) || node.hasLabel(NodeLabels.CFT_Instance) || node.hasLabel(NodeLabels.CFT_Outport)
					    || node.hasLabel(NodeLabels.CFT_Inport_Instance))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    else
				    {
					outfailprop++;
				    }
				    break;
				case Is_Child_Of:
				    child++;
				    if (child > 1)
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_CFT_Of:
				    if (!node.hasLabel(NodeLabels.CFT))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_Inside_MCS:
				    if (!node.hasLabel(NodeLabels.CFT_Basic_Event) && !node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport_Instance))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_Instance_Of:
				    if (!node.hasLabel(NodeLabels.CFT_Outport_Instance) && !node.hasLabel(NodeLabels.CFT_Inport_Instance) && !node.hasLabel(NodeLabels.CFT_Instance))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    else
				    {
					instance++;
					if (instance > 1)
					{
					    DBConnection.errorNodes.add(node);
					}
				    }
				    break;
				case Port_Propagation:
				    if (!node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				default: // All other Types!
				    DBConnection.errorNodes.add(node);
				    break;
			    }
			}
			else
			{
			    switch (type)
			    {
				case Failure_Propagation:
				    infailprop++;
				    if (node.hasLabel(NodeLabels.CFT_Basic_Event) || node.hasLabel(NodeLabels.CFT_Inport) || node.hasLabel(NodeLabels.CFT_Outport_Instance)
					    || node.hasLabel(NodeLabels.CFT) || node.hasLabel(NodeLabels.CFT_Instance))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    else
				    {
					if (infailprop > 1 && (node.hasLabel(NodeLabels.CFT_NOT_Gate) || node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport)))
					{
					    DBConnection.errorNodes.add(node);
					}
				    }
				    break;
				case Is_Child_Of:
				    if (!node.hasLabel(NodeLabels.CFT) && !node.hasLabel(NodeLabels.CFT_Instance))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_Instance_Of:
				    if (!node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport) && !node.hasLabel(NodeLabels.CFT))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_MCS_Of:
				    if (!node.hasLabel(NodeLabels.CFT_Inport_Instance) && !node.hasLabel(NodeLabels.CFT_Outport))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_Full_MCS_Of:
				    if (!node.hasLabel(NodeLabels.CFT_Outport))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Port_Propagation:
				    if (!node.hasLabel(NodeLabels.CFT_Inport) && !node.hasLabel(NodeLabels.CFT_Outport))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				case Is_Quant_Result_Of:
				    if (!node.hasLabel(NodeLabels.CFT_Outport))
				    {
					DBConnection.errorNodes.add(node);
				    }
				    break;
				default:
				    DBConnection.errorNodes.add(node);
				    break;

			    }
			}
			if (DBConnection.errorNodes.contains(node))
			{
			    break;
			}
		    }
		    if (!DBConnection.errorNodes.contains(node))
		    {
			if (child == 0 && !node.hasLabel(NodeLabels.CFT))
			{
			    DBConnection.errorNodes.add(node);
			}
			if (instance == 0 && (node.hasLabel(NodeLabels.CFT_Instance) || node.hasLabel(NodeLabels.CFT_Inport_Instance) || node.hasLabel(NodeLabels.CFT_Outport_Instance)))
			{
			    DBConnection.errorNodes.add(node);
			}
			if (node.hasLabel(NodeLabels.CFT_AND_Gate) || node.hasLabel(NodeLabels.CFT_OR_Gate) || node.hasLabel(NodeLabels.CFT_MOON_Gate) || node.hasLabel(NodeLabels.CFT_XOR_Gate)
				|| node.hasLabel(NodeLabels.CFT_NOT_Gate))
			{
			    if (outfailprop > 0 && infailprop == 0)
			    {
				DBConnection.errorNodes.add(node);
			    }
			}
		    }
		}
	    }
	    tx.success();
	}
	DBConnection.uncheckedNodes.clear();
	DBConnection.dbChecked = DBConnection.errorNodes.isEmpty();
	return DBConnection.errorNodes;
    }
}
