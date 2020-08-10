package neo4jMCS;

import java.util.HashSet;

/**
 * This class is used to store two MCS HashSets together.<br>
 * One MCS HashSet is a regular MCS HashSet and the other is the negated MCS HashSet.<br>
 * 
 * @author Kevin Bartik
 *
 */
public class MCSPairSet
{

    /**
     * The regular MCS HashSet.<br>
     */
    private HashSet<MCS> mcsSet;

    /**
     * The negated MCS HashSet.<br>
     */
    private HashSet<MCS> negatedMCSSet;

    /**
     * Simple Constructor.<br>
     * Sets both MCS HashSets.<br>
     * 
     * @param _mcsSet
     *            The regular MCS HashSet.
     * @param _negatedMCSSet
     *            The negated MCS HashSet.
     */
    public MCSPairSet(HashSet<MCS> _mcsSet, HashSet<MCS> _negatedMCSSet)
    {
	mcsSet = _mcsSet;
	negatedMCSSet = _negatedMCSSet;
    }

    /**
     * Returns the regular MCS HashSet.<br>
     * 
     * @return the regular MCS HashSet.<br>
     */
    public HashSet<MCS> getMCSSet()
    {
	return mcsSet;
    }

    /**
     * Returns the negated MCS HashSet.<br>
     * 
     * @return the negated MCS HashSet.<br>
     */
    public HashSet<MCS> getNegatedMCSSet()
    {
	return negatedMCSSet;
    }

    /**
     * Returns true if both MCS HashSets are empty. Otherwise false.<br>
     * 
     * @return true if both MCS HashSets are empty. Otherwise false.
     */
    public boolean isEmpty()
    {
	return mcsSet.isEmpty() && negatedMCSSet.isEmpty();
    }
}
