package neo4jMCS;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBUtility;

/**
 * The MCSNode is used in MCS to save a Node and if that Node is considered negated or not.
 * @author Kevin Bartik
 *
 */
public class MCSNode
{

    /**
     * The Node of the MCSNode.
     */
    private Node node;

    /**
     * True if the MCSNode is negated. Otherwise false.
     */
    private boolean negated;
    
    /**
     * Main Constructor.
     * Creates a new MCSNode.
     * 
     * @param node The Node of the new MCSNode.
     * @param negated If the MCSNode is negated or not.
     */
    public MCSNode(Node node, boolean negated)
    {
	this.node = node;
	this.negated = negated;
    }

    /**
     * Copy Constructor.
     * Creates a new MCSNode from a MCSNode.
     * @param mcsNode The MCSNode that is copied.
     */
    MCSNode(MCSNode mcsNode)
    {
	this.node = mcsNode.node;
	this.negated = mcsNode.negated;
    }

    /**
     * Copies the MCSNode flips the Negation.
     * @return Copies the MCSNode and flips the Negation.
     */
    public MCSNode getNegatedMCSNode()
    {
	MCSNode mcsNode = new MCSNode(this);
	mcsNode.negate();
	return mcsNode;
    }

    /**
     * Returns the Node of the MCSNode.
     * @return The Node of the MCSNode.
     */
    public Node getNode()
    {
	return node;
    }

    /**
     * Returns true if the MCSNode is negated. Otherwise false.
     * @return True if the MCSNode is negated. Otherwise false.
     */
    public boolean isNegated()
    {
	return negated;
    }
    
    /**
     * Flips the Negation of a MCSNode.
     * If the MCSNode was negated then after calling this method the MCSNode will not be negated and vice versa.
     */
    void negate()
    {
	negated = !negated;
    }

    @Override
    public int hashCode()
    {
	final int prime = 31;
	int result = 1;
	result = prime * result + (negated ? 1231 : 1237);
	result = prime * result + ((node == null) ? 0 : node.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj)
    {
	if (this == obj) return true;
	if (obj == null) return false;
	if (getClass() != obj.getClass()) return false;
	MCSNode other = (MCSNode) obj;
	if (negated != other.negated) return false;
	if (node == null)
	{
	    if (other.node != null) return false;
	}
	else if (!node.equals(other.node)) return false;
	return true;
    }
    
    @Override
    public String toString() {
	return DBUtility.getIDAndName(this);
    }

}
