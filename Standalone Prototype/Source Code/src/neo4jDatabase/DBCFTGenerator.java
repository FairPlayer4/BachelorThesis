package neo4jDatabase;

import neo4jEnum.NodeLabels;
import neo4jEnum.RelTypes;
import neo4jUtility.PrintUtility;

/**
 * This class is used for CFT generation.
 * @author Kevin Bartik
 *
 */
public final class DBCFTGenerator
{
    
    /**
     * The number of the generated CFT.
     * Used to connect all generated nodes to the CFT.
     */
    private static long cftNumber;
    
    /**
     * Used to add an unique ElementID to all generated nodes. 
     */
    private static int runningElementID = 1;
    
    /**
     * Private constructor so this class cannot be instantiated.
     */
    private DBCFTGenerator() {
	
    }
    
    /**
     * This method creates the CFT.
     * 
     * @param levels The number of levels in the CFT.
     * @param startinglinks The number of links after the starting gate.
     * @param followlinks The number of links for following gates.
     * @param startingGateType The type of the starting gate.
     * @param moonnumber The MoonNumber used if the starting gate is a MOON gate.
     * @param switchingGatesNumber Defines how the gates are switched.
     */
    public static void createRandomCFTBatch(int levels, int startinglinks, int followlinks, int startingGateType, int moonnumber, int switchingGatesNumber)
    {
	    PrintUtility.printInfo("Starting CFT Generation...");
	    DBConnection.batchproperties.clear();
	    DBConnection.batchproperties.put("Name", "Generated CFT");
	    DBConnection.batchproperties.put("ElementID", runningElementID);
	    cftNumber = DBConnection.getBatchInserter().createNode(DBConnection.batchproperties, NodeLabels.Neo4J_Generated_Element, NodeLabels.CFT);
	    runningElementID++;
	    long outport = createNodeBatch(NodeLabels.CFT_Outport, "Generated Outport", -1, 0);
	    if (startingGateType == 5)
	    {
		DBConnection.batchproperties.put("MOONNumber", moonnumber);
	    }
	    long gate = createNodeBatch(NodeLabels.getGateTypeLabel(startingGateType), "Generated Gate", outport, 0);
	    if (startingGateType == 5)
	    {
		DBConnection.batchproperties.remove("MOONNumber", moonnumber);
	    }
	    for (int i = 1; i <= startinglinks; i++)
	    {
		if (switchingGatesNumber == 0)
		{
		    addCFTLevelBatch(gate, (levels - 1), followlinks, 1, 0);
		}
		else
		{
		    addCFTLevelBatch(gate, (levels - 1), followlinks, ((i - 1) % 2) + 1, 1);
		}
	    }
	    PrintUtility.printInfo("CFT was successfully generated!");
    }

    /**
     * Adds another CFT level to the generated CFT.
     * 
     * @param rgate The number of the previous node for the failure propagation.
     * @param levels Number of levels that have to be created after this level.
     * @param links Number of links after the gate.
     * @param gateType The gate type.
     * @param switchingGatesNumber Defines how the gates are switched.
     */
    private static void addCFTLevelBatch(long rgate, int levels, int links, int gateType, int switchingGatesNumber)
    {
	if (levels == 0)
	{
	    createNodeBatch(NodeLabels.CFT_Basic_Event, "Generated Basic Event", rgate, 1);
	}
	else
	{
	    long ngate = createNodeBatch(NodeLabels.getGateTypeLabel(gateType), "Generated Gate", rgate, 0);
	    for (int i = 1; i <= links; i++)
	    {
		if (switchingGatesNumber == 0)
		{
		    if (gateType == 1)
		    {
			addCFTLevelBatch(ngate, (levels - 1), links, 2, 0);
		    }
		    else
		    {
			addCFTLevelBatch(ngate, (levels - 1), links, 1, 0);
		    }
		}
		else
		{
		    addCFTLevelBatch(ngate, (levels - 1), links, ((i - 1) % 2) + 1, 1);
		}
	    }
	}
    }

    /**
     * Adds a node to the generated CFT.
     * 
     * @param label The label of the node.
     * @param name The name of the node.
     * @param prior The number of the previous node for the failure propagation.
     * @param basicProbability The basic failure probability for the node.
     * @return The number of the created node.
     */
    private static long createNodeBatch(NodeLabels label, String name, long prior, double basicProbability)
    {
	DBConnection.batchproperties.put("Name", name);
	DBConnection.batchproperties.put("ElementID", runningElementID);
	if (label.equals(NodeLabels.CFT_Basic_Event) || label.equals(NodeLabels.CFT_Inport))
	{
	    DBConnection.batchproperties.put("Basic Failure Probability", basicProbability);
	}
	else
	{
	    DBConnection.batchproperties.remove("Basic Failure Probability");
	}
	long node = DBConnection.getBatchInserter().createNode(DBConnection.batchproperties, NodeLabels.Neo4J_Generated_Element, label);
	if (prior != -1)
	{
	    DBConnection.getBatchInserter().createRelationship(node, prior, RelTypes.Failure_Propagation, null);
	}
	DBConnection.getBatchInserter().createRelationship(node, cftNumber, RelTypes.Is_Child_Of, null);
	runningElementID++;
	return node;
    }
}
