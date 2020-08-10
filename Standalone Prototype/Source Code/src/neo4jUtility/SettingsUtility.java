package neo4jUtility;

/**
 * Stores all settings that are used by the prototype.
 * @author Kevin Bartik
 *
 */
public final class SettingsUtility
{

    private static boolean alwaysRefineFully = true;

    /**
     * Boolean that indicates if the application shall print information on the GUI console. Results are always printed on the console.
     */
    private static boolean printInfo = true;

    private static boolean resultsPopUp = true;

    private static boolean reuseAnalysisResults = false;

    private static boolean calculatePrimeImplicants = false;

    private static boolean useManualTraversal = false;

    private static boolean resetTraversal = true;
    
    private SettingsUtility() {
	
    }

    public static boolean isAlwaysRefineFully()
    {
	return alwaysRefineFully;
    }

    public static void setAlwaysRefineFully(boolean alwaysRefineFully)
    {
	SettingsUtility.alwaysRefineFully = alwaysRefineFully;
    }

    public static boolean isPrintInfo()
    {
	return printInfo;
    }

    public static void setPrintInfo(boolean printInfo)
    {
	SettingsUtility.printInfo = printInfo;
    }

    public static boolean isResultsPopUp()
    {
	return resultsPopUp;
    }

    public static void setResultsPopUp(boolean resultsPopUp)
    {
	SettingsUtility.resultsPopUp = resultsPopUp;
    }

    public static boolean isReuseAnalysisResults()
    {
	return reuseAnalysisResults;
    }

    public static void setReuseAnalysisResults(boolean reuseAnalysisResults)
    {
	if (SettingsUtility.reuseAnalysisResults != reuseAnalysisResults)
	{
	    resetTraversal = true;
	    SettingsUtility.reuseAnalysisResults = reuseAnalysisResults;
	}
    }

    public static boolean isCalculatePrimeImplicants()
    {
	return calculatePrimeImplicants;
    }

    public static void setCalculatePrimeImplicants(boolean calculatePrimeImplicants)
    {
	SettingsUtility.calculatePrimeImplicants = calculatePrimeImplicants;
    }

    public static boolean isUseManualTraversal()
    {
	return useManualTraversal;
    }

    public static void setUseManualTraversal(boolean useManualTraversal)
    {
	if (SettingsUtility.useManualTraversal != useManualTraversal)
	{
	    resetTraversal = true;
	    SettingsUtility.useManualTraversal = useManualTraversal;
	}
    }

    public static boolean isResetTraversal()
    {
	return resetTraversal;
    }

    public static void resetTraversal()
    {
	SettingsUtility.resetTraversal = true;
    }

    public static void traversalPerformed()
    {
	SettingsUtility.resetTraversal = false;
    }

}
