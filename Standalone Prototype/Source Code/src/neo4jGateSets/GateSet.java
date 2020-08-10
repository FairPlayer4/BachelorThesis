package neo4jGateSets;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.neo4j.graphdb.Node;

import neo4jMCS.MCS;
import neo4jMCS.MCSPairSet;

/**
 * A GateSet is a set of Nodes and a set of lower GateSets. <br>
 * A GateSet is created from an Element of a CFT and contains other Elements of a CFT. <br>
 * GateSets are used to traverse the CFT and save all failure paths of the CFT. <br>
 * Later the GateSet can then be used for analysis of the CFT. <br>
 * 
 * @author Kevin Bartik
 *
 */
public interface GateSet
{

    /**
     * Adds a Node to the GateSet. <br>
     * 
     * @param node
     *            The node that is added.
     */
    public void addNode(Node node);

    /**
     * Adds a lower GateSet to the GateSet.<br>
     * 
     * @param gateSet
     *            The lower GateSet that is added.
     */
    public void addLowerGateSet(GateSet gateSet);

    /**
     * Creates the set of MCS in a GateSet.<br>
     */
    public void createMCSSet();

    /**
     * Creates the set of negated MCS in a GateSet.<br>
     */
    public void createNegatedMCSSet();

    /**
     * Combines all MCS that are merged with MCS sets.<br>
     */
    public void combineAllMergedMCS();
    
    /**
     * Minimizes the set of MCS and the set of negated MCS.
     */
    public void minimizeMCS();

    /**
     * Adds all hidden prime implicants to the set of MCS.
     */
    public void addHiddenPrimeImplicants();

    /**
     * Calculates the Probability of the GateSet and returns it as a double.<br>
     * 
     * @return Probability of the GateSet.
     */
    public double calculateBasicFailureProbability();

    /**
     * Connects GateSets.
     * @param inportMapList The list of inport maps.
     */
    public void connectGateSets(LinkedList<HashMap<Node, Node>> inportMapList);

    /**
     * Connects MCS of connected GateSets.
     * @return The MCSPairSet which contains the connected MCS.
     */
    public MCSPairSet connectMCS();

    /**
     * Finds all lower GateSets that have a start node that is contained in the node collection.
     * @param nodesToFind The collection of start nodes
     * @return All lower GateSet with a start node that is contained in the collection of start nodes.
     */
    public HashSet<GateSet> findGateSets(Collection<Node> nodesToFind);

    /**
     * Returns the starting Node of the GateSet.<br>
     * The start Node of a GateSet is the Node that was used to create the GateSet.<br>
     * 
     * @return The start Node of the GateSet.
     */
    public Node getStartNode();

    /**
     * Returns the HashSet of MCS of the GateSet.<br>
     * 
     * @return the HashSet of MCS of the GateSet.
     */
    public HashSet<MCS> getMCSSet();

    /**
     * Returns the HashSet of negated MCS of the GateSet.<br>
     * 
     * @return the HashSet of negated MCS of the GateSet.
     */
    public HashSet<MCS> getNegatedMCSSet();

    /**
     * Returns a deep copy of the HashSet of MCS of the GateSet.<br>
     * 
     * @return a deep copy of the HashSet of MCS.
     */
    public HashSet<MCS> getMCSSetCopy();

    /**
     * Returns a deep copy of the HashSet of negated MCS of the GateSet.<br>
     * 
     * @return a deep copy of the HashSet of negated MCS of the GateSet.
     */
    public HashSet<MCS> getNegatedMCSSetCopy();

    /**
     * Returns a deep copy of the GateSet.
     * All contents are copied so that they can be modified in the copy without affecting the original.
     * @return a deep copy of the GateSet.
     */
    public GateSet getGateSetCopy();

    /**
     * Returns the GateSet Type which is an Integer that identifies which implementation of the GateSet interface is used for the GateSet.<br>
     * 
     * @return The GateSet type.
     */
    public int getGateType();

    /**
     * Prints the structure of the GateSet.
     * Used to find errors in a GateSet
     * @param tab Tabs used in the recursion to style the output.
     * @return The GateSet structure as a String.
     */
    public String getGateSetString(String tab);

    /**
     * Prints the minimal cut sets of the GateSet.<br>
     */
    public String getMCSString();

}
