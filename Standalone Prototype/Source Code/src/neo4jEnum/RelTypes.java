package neo4jEnum;

import org.neo4j.graphdb.RelationshipType;

import neo4jUtility.PrintUtility;

/**
 * This enum holds all used RelationshipTypes of this application.<br>
 * Also has some convenience methods for working with RelationshipTypes.<br>
 * 
 * @author Kevin Bartik
 *
 */
public enum RelTypes implements RelationshipType
{
    Is_Child_Of, // Relationship to Parent (from Enterprise Architect ParentID).
    Is_Instance_Of, // Relationship to Classifier (from Enterprise Architect ClassifierID).

    Is_CFT_Of, // Relationship from some Component to its Failure Model (FT, CFT).
    Failure_Propagation,
    Port_Propagation,

    Logical_Information_Flow,

    Is_Full_Negated_MCS_Of,
    Is_Negated_MCS_Of,
    Is_Full_MCS_Of,
    Is_MCS_Of, // Relationship that shows to which element a minimal cut set belongs to.
    Is_Inside_MCS, // Relationship that shows which elements are in a minimal cut set.
    Is_Quant_Result_Of,
    
    Error_Type; // Only used if errors appear.

    /**
     * Transforms a RelationshipType from the Neo4j database to RelTypes.<br>
     * 
     * @param reltype
     *            RelationshipType in the Neo4j database.
     * @return The corresponding RelationshipType in RelTypes.
     */
    public static RelTypes transformType(RelationshipType reltype)
    {
	switch (reltype.name())
	{
	    case "Is_Child_Of":
		return Is_Child_Of;
	    case "Is_Instance_Of":
		return Is_Instance_Of;
	    case "Is_CFT_Of":
		return Is_CFT_Of;
	    case "Failure_Propagation":
		return Failure_Propagation;
	    case "Port_Propagation":
		return Port_Propagation;

	    case "Logical_Information_Flow":
		return Logical_Information_Flow;

	    case "Is_MCS_Of":
		return Is_MCS_Of;
	    case "Is_Negated_MCS_Of":
		return Is_Negated_MCS_Of;
	    case "Is_Full_MCS_Of":
		return Is_Full_MCS_Of;
	    case "Is_Full_Negated_MCS_Of":
		return Is_Full_Negated_MCS_Of;
	    case "Is_Inside_MCS":
		return Is_Inside_MCS;
	    case "Is_Quant_Result_Of":
		return Is_Quant_Result_Of;

	    case "Error_Type":
		PrintUtility.printError("Error_Type found!");
		return Error_Type;

	    default:
		PrintUtility.printError("RelationshipType (" + reltype.name() + ") from Neo4j database is not in Enum RelTypes!");
		return Error_Type;
	}
    }

    /**
     * Transforms an EA Connector stereotype into the corresponding RelationshipType.<br>
     * 
     * @param stereotype
     *            The EA Connector stereotype.
     * @return The corresponding RelationshipType.
     */
    public static RelTypes getRelTypeForEA(String stereotype)
    {
	switch (stereotype)
	{
	    case "Is_Child_Of":
		return Is_Child_Of;
	    case "Is_Instance_Of":
		return Is_Instance_Of;
	    case "ComponentFailureModelTrace":
		return Is_CFT_Of;
	    case "FailurePropagation":
		return Failure_Propagation;
	    case "PortFailureModeTrace":
		return Port_Propagation;

	    case "Logical Information Flow":
		return Logical_Information_Flow;

	    default:
		PrintUtility.printError("Connector Stereotype (" + stereotype + ") from EA Repository can't be handled!");
		return Error_Type;
	}
    }

    /**
     * Returns true if the EA Connector stereotype is relevant for the application. Otherwise false. <br>
     * 
     * @param stereotype
     *            The stereotype of the EA Connector.
     * @return true if the EA Connector stereotype is relevant for the application. Otherwise false.
     */
    public static boolean isRelevantStereotype(String stereotype)
    {
	switch (stereotype)
	{
	    case "ComponentFailureModelTrace":
		return true;
	    case "FailurePropagation":
		return true;
	    case "PortFailureModeTrace":
		return true;
	    case "Logical Information Flow":
		return true;
	    default:
		return false;
	}
    }
}
