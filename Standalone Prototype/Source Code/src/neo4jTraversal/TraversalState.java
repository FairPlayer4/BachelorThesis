package neo4jTraversal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.neo4j.graphdb.Node;

import neo4jGateSets.GateSet;

/**
 * This class stores traversal information which is used to continue the traversal.<br>
 * TraversalStates copied in each traversal branch so they can be modified independently.<br>
 * It stores the last GateSet so further traversals can connect their GateSet or Nodes to it.<br>
 * It stores a List of Inport to Inport Instance Maps which is used to continue traversal at Inports (they can have multiple Inport Instances).<br>
 * The an Inport Map is added to the List at each Outport Instance and the last Inport Map is removed at an Inport after it was mapped to a specific Inport
 * Instance.<br>
 * It contains two HashSets for Nodes which are used to find simple and deep cycles.<br>
 * A simple cycle is a cycle inside a CFT that does not traverse inside other CFT.<br>
 * A deep cycle is a cycle between two CFT Instances in which a specific Inport Instance or Outport Instance is traversed twice.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class TraversalState
{

    /**
     * The last GateSet that was added to the TraversalState.<br>
     * Is used to connect GateSets and Nodes during a traversal.<br>
     */
    private GateSet lastGateSet;

    /**
     * List of Inport to Inport Instance Maps.<br>
     * Is used to continue traversal at Inports as they can have multiple Inport Instances. <br>
     */
    private LinkedList<HashMap<Node, Node>> inportMapList;

    /**
     * All Outport Instances and Inport Instances that are traversed are added to this set. <br>
     * Is used to find deep cycles.<br>
     */
    private HashSet<Node> traversedInstances;

    /**
     * All traversed Nodes except Outport Instances and Inport Instances are added to this set.<br>
     * Is cleared when the traversal enters another CFT.<br>
     * Is used to find simple cycles.<br>
     */
    private HashSet<Node> simpleTraversedNodes;

    /**
     * Constructor for a new TraversalState.<br>
     * Adds the last GateSet and initializes the List and both Sets.<br>
     * 
     * @param nextGateSet
     *            The last GateSet.
     */
    TraversalState()
    {
	lastGateSet = null;
	inportMapList = new LinkedList<HashMap<Node, Node>>();
	traversedInstances = new HashSet<Node>();
	simpleTraversedNodes = new HashSet<Node>();
    }
    
    /**
     * Constructor for a new TraversalState.<br>
     * Adds the last GateSet and initializes the List and both Sets.<br>
     * 
     * @param nextGateSet
     *            The last GateSet.
     */
    TraversalState(GateSet nextGateSet)
    {
	lastGateSet = nextGateSet;
	inportMapList = new LinkedList<HashMap<Node, Node>>();
	traversedInstances = new HashSet<Node>();
	simpleTraversedNodes = new HashSet<Node>();
    }

    /**
     * Copy Constructor.<br>
     * Copies the previous TraversalState to allow independent modification.<br>
     * Is used when different traversal branches appear for example if a Node has multiple incoming Failure Propagations.<br>
     * 
     * @param ts
     *            The old TraversalState.
     */
    TraversalState(TraversalState ts)
    {
	lastGateSet = ts.lastGateSet;
	inportMapList = new LinkedList<HashMap<Node, Node>>(ts.inportMapList);
	traversedInstances = new HashSet<Node>(ts.traversedInstances);
	simpleTraversedNodes = new HashSet<Node>(ts.simpleTraversedNodes);
    }

    /**
     * Returns the last GateSet.<br>
     * 
     * @return the last GateSet.
     */
    GateSet getLastGateSet()
    {
	return lastGateSet;
    }

    /**
     * Sets the last GateSet.<br>
     * 
     * @param lastGateSet
     *            The next last GateSet.
     */
    void setLastGateSet(GateSet lastGateSet)
    {
	this.lastGateSet = lastGateSet;
    }

    /**
     * Adds a Node to the set for finding simple cycles.<br>
     * Returns false if the Node was added (the set did not contain the Node).<br>
     * Returns true if the Node was already contained in the set (Then a simple cycle was found).<br>
     * 
     * @param node
     *            The Node that is added.
     * @return false if the Node was added (the set did not contain the Node). true if the Node was already contained in the set (Then a simple cycle was
     *         found).
     */
    boolean addNodeToSimple(Node node)
    {
	return !simpleTraversedNodes.add(node);
    }

    /**
     * Adds a Node to the set for finding deep cycles.<br>
     * Returns false if the Node was added (the set did not contain the Node).<br>
     * Returns true if the Node was already contained in the set (Then a deep cycle was found).<br>
     * 
     * @param node
     *            The Node that is added.
     * @return false if the Node was added (the set did not contain the Node). true if the Node was already contained in the set (Then a deep cycle was found).
     */
    boolean addNodeToInstances(Node node)
    {
	return !traversedInstances.add(node);
    }

    /**
     * Adds a Node to the last GateSet.<br>
     * 
     * @param node
     *            The Node that is added.
     */
    void addNodeToLastGateSet(Node node)
    {
	lastGateSet.addNode(node);
    }

    /**
     * Adds a GateSet to the last GateSet.<br>
     * 
     * @param gateSet
     *            The GateSet that is added.
     */
    void addGateSetToLastGateSet(GateSet gateSet)
    {
	lastGateSet.addLowerGateSet(gateSet);
    }

    /**
     * Adds an Inport Map to the List of Inport Maps.<br>
     * 
     * @param inportMap
     *            The Inport Map that is added.
     */
    void addInportMap(HashMap<Node, Node> inportMap)
    {
	inportMapList.add(inportMap);
    }

    /**
     * Clears the set for finding simple cycles.<br>
     * Is used when the traversal enters a different CFT.
     */
    void clearSimple()
    {
	simpleTraversedNodes.clear();
    }

    /**
     * Removes and returns the last Inport Map from the List of Inport Maps.<br>
     * 
     * @return The last Inport Map of the List of Inport Maps.
     */
    HashMap<Node, Node> removeLastInportMap()
    {
	return inportMapList.removeLast();
    }

    /**
     * Returns true if the last Inport Map in the List of Inport Maps contains the Inport as a key.<br>
     * Otherwise false. <br>
     * 
     * @param inport
     *            The Inport.
     * @return true if the last Inport Map in the List of Inport Maps contains the Inport as a key. Otherwise false.
     */
    boolean containsInport(Node inport)
    {
	return inportMapList.getLast().containsKey(inport);
    }
}
