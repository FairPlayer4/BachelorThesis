package neo4jSocket;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBUtility;
import neo4jDatabase.DBChecker;
import neo4jEnum.NodeLabels;
import neo4jResultsDialog.MCSResultsDialog;
import neo4jResultsDialog.QuantResultsDialog;
import neo4jTraversal.MainTraversal;
import neo4jUtility.CustomOutputStream;
import neo4jUtility.GeneralUtility;
import neo4jUtility.PrintUtility;
import neo4jUtility.SettingsUtility;
import neo4jUtility.ThreadUtility;

import javax.swing.JLabel;

public class SocketAnalysisWindow
{

    private final JFrame frame = new JFrame();

    private final JButton buttonShowCFT = new JButton("Show CFT List");

    private final JButton buttonMCSAnalysis = new JButton("<html><center>Qualitative<br>Analysis</center></html>");

    private final JButton buttonShowOutports = new JButton("Show Outports");

    private final JButton buttonShowHelp = new JButton("Show Help");

    private final JButton buttonQuantAnalysis = new JButton("<html><center>Quantitative<br>Analysis</center></html>");

    private final JButton buttonCheckDatabaseFor = new JButton("Check database for Errors");

    private final JButton btnClearMinimalCut = new JButton("<html><center>Clear Minimal Cut Sets <br>and Quantitative Results</center></html>");

    // ScrollPanes and Lists

    private final JScrollPane scrollPaneConsole = new JScrollPane();

    private final JScrollPane scrollPaneForComponentList = new JScrollPane();

    private final JTextArea textAreaConsole = new JTextArea();

    private JList<String> listComponent;

    private final DefaultListModel<String> listModel = new DefaultListModel<String>();

    private final JCheckBox checkBoxMultithreading = new JCheckBox("Use Multithreading");

    private final JCheckBox checkboxAnalysisResultsPopup = new JCheckBox("Analysis Results Popup");

    private final JCheckBox checkboxReuseAnalysisResults = new JCheckBox("Reuse Analysis Results");

    private final JCheckBox checkboxCalculatePrimeImplicants = new JCheckBox("Calculate Prime Implicants");

    private final JCheckBox checkboxTraversalFramework = new JCheckBox("Use the Neo4j Traversal Framework");

    private final JCheckBox checkboxHideInformationMessages = new JCheckBox("Hide Information Messages");

    private final JCheckBox checkboxUseCombinations = new JCheckBox("Use Combinations");

    private final JLabel labelAnalysisSettings = new JLabel("Analysis Settings");

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
	EventQueue.invokeLater(new Runnable()
	{
	    public void run()
	    {
		try
		{
		    new SocketAnalysisWindow();
		}
		catch (Exception e)
		{
		    e.printStackTrace();
		}
	    }
	});
    }

    /**
     * Create the application.
     */
    SocketAnalysisWindow()
    {
	initialize();
	frame.setExtendedState(JFrame.NORMAL);
	frame.setAlwaysOnTop(true);
	frame.requestFocus();
	frame.setAlwaysOnTop(false);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
	// Frame
	ThreadUtility.getThreadUtility();
	frame.setBounds(100, 100, 972, 1056);
	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	frame.getContentPane().setLayout(null);

	// TextArea Console

	scrollPaneConsole.setToolTipText("");
	scrollPaneConsole.setViewportBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Output Console", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
	scrollPaneConsole.setBounds(15, 619, 920, 365);
	frame.getContentPane().add(scrollPaneConsole);
	textAreaConsole.setFont(new Font("Tahoma", Font.PLAIN, 16));
	scrollPaneConsole.setViewportView(textAreaConsole);
	textAreaConsole.setLineWrap(true);
	textAreaConsole.setEditable(false);
	PrintStream printStream = new PrintStream(CustomOutputStream.addTextAreaAndGetCustomOutputStream(textAreaConsole));

	// Analysis Label

	labelAnalysisSettings.setFont(new Font("Tahoma", Font.BOLD, 22));
	labelAnalysisSettings.setBounds(30, 110, 200, 40);
	frame.getContentPane().add(labelAnalysisSettings);

	// Output Redirection

	System.setOut(printStream);
	System.setErr(printStream);

	// Component Selection List

	scrollPaneForComponentList.setBounds(645, 110, 290, 493);
	frame.getContentPane().add(scrollPaneForComponentList);
	scrollPaneForComponentList.setToolTipText("");
	scrollPaneForComponentList.setViewportBorder(new TitledBorder(null, "Model Selection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	listComponent = new JList<String>(listModel);
	listComponent.setFont(new Font("Tahoma", Font.PLAIN, 18));
	listComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	scrollPaneForComponentList.setViewportView(listComponent);

	// Finish initialization

	initializeComboCheckBoxes();
	initializeButtons();
	frame.setVisible(true);
    }

    private void initializeComboCheckBoxes()
    {
	// CheckBox Use Multithreading

	checkBoxMultithreading.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkBoxMultithreading.setSelected(false);
	checkBoxMultithreading.addActionListener(new ActionListener()
	{

	    public void actionPerformed(ActionEvent e)
	    {
		if (!guiReady())
		{
		    checkBoxMultithreading.setSelected(ThreadUtility.getThreadUtility().isMultiThreading());
		}
		else
		{
		    ThreadUtility.getThreadUtility().setMultiThreading(checkBoxMultithreading.isSelected());
		}
	    }
	});
	checkBoxMultithreading.setBounds(30, 160, 300, 30);
	frame.getContentPane().add(checkBoxMultithreading);

	// CheckBox Analysis Results Popup

	checkboxAnalysisResultsPopup.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxAnalysisResultsPopup.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (checkboxAnalysisResultsPopup.isSelected())
		{
		    SettingsUtility.setResultsPopUp(true);
		}
		else
		{
		    SettingsUtility.setResultsPopUp(false);
		}
	    }
	});
	checkboxAnalysisResultsPopup.setSelected(true);
	checkboxAnalysisResultsPopup.setBounds(30, 210, 300, 30);
	frame.getContentPane().add(checkboxAnalysisResultsPopup);

	// CheckBox Reuse

	checkboxReuseAnalysisResults.setFont(new Font("Tahoma", Font.PLAIN, 19));
	checkboxReuseAnalysisResults.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (checkboxReuseAnalysisResults.isSelected() && checkboxReuseAnalysisResults.isEnabled())
		{
		    SettingsUtility.setReuseAnalysisResults(true);
		}
		else
		{
		    SettingsUtility.setReuseAnalysisResults(false);
		}
	    }
	});
	checkboxReuseAnalysisResults.setBounds(60, 300, 300, 30);
	frame.getContentPane().add(checkboxReuseAnalysisResults);

	// CheckBox Prime Implicants

	checkboxCalculatePrimeImplicants.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxCalculatePrimeImplicants.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (checkboxCalculatePrimeImplicants.isSelected())
		{
		    SettingsUtility.setCalculatePrimeImplicants(true);
		}
		else
		{
		    SettingsUtility.setCalculatePrimeImplicants(false);
		}
	    }
	});
	checkboxCalculatePrimeImplicants.setBounds(30, 350, 300, 30);
	frame.getContentPane().add(checkboxCalculatePrimeImplicants);

	// CheckBox Traversal

	checkboxTraversalFramework.setSelected(true);
	checkboxTraversalFramework.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxTraversalFramework.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (!guiReady())
		{
		    checkboxTraversalFramework.setSelected(!checkboxTraversalFramework.isSelected());
		}
		else
		{
		    if (checkboxTraversalFramework.isSelected())
		    {
			SettingsUtility.setUseManualTraversal(false);
			checkboxReuseAnalysisResults.setSelected(false);
			SettingsUtility.setReuseAnalysisResults(false);
			checkboxReuseAnalysisResults.setEnabled(true);
		    }
		    else
		    {
			SettingsUtility.setUseManualTraversal(true);
			checkboxReuseAnalysisResults.setSelected(false);
			SettingsUtility.setReuseAnalysisResults(false);
			checkboxReuseAnalysisResults.setEnabled(false);
		    }
		}
	    }
	});
	checkboxTraversalFramework.setBounds(30, 260, 360, 30);
	frame.getContentPane().add(checkboxTraversalFramework);

	// CheckBox Combinations

	checkboxUseCombinations.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxUseCombinations.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (!guiReady())
		{
		    checkboxUseCombinations.setSelected(!checkboxUseCombinations.isSelected());
		}
		else
		{
		    SettingsUtility.setAlwaysRefineFully(!checkboxUseCombinations.isSelected());
		}
	    }
	});
	checkboxUseCombinations.setBounds(30, 400, 300, 30);
	frame.getContentPane().add(checkboxUseCombinations);

	// CheckBox Hide Info

	checkboxHideInformationMessages.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxHideInformationMessages.setBounds(30, 520, 300, 30);
	checkboxHideInformationMessages.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		SettingsUtility.setPrintInfo(!checkboxHideInformationMessages.isSelected());
	    }
	});
	frame.getContentPane().add(checkboxHideInformationMessages);
    }

    private void initializeButtons()
    {

	// Button Show Help

	buttonShowHelp.setBounds(735, 16, 200, 60);
	buttonShowHelp.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonShowHelp.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (!DBUtility.containsCFT())
		{
		    JOptionPane.showMessageDialog(frame,
			    "The Neo4j database must be updated or the there are no CFT in the EA Project.\n"
				    + "Close this dialog and the analysis window and update the Neo4j database or add CFT to the EA Project and perform an update.\n"
				    + "After that you can use the Analysis Window to analyze CFT.",
			    "Help Dialog", JOptionPane.PLAIN_MESSAGE);
		}
		else
		{
		    JOptionPane.showMessageDialog(frame, "The Analysis can now be performed.\n"
			    + "First, a CFT must be selected which is done by pressing the button \"Show CFT List\" and then selecting a CFT in the \"Model Selection\"."
			    + "Then an Outport of the CFT must be selected which is done by pressing the button \"Show Outports\" and then selecting an Outport from the \"Model Selection\".\n"
			    + "Then the analysis can be performed on the selected Outport.\n" + "Press the button \"Qualitative Analysis\" or \"Quantitative Analysis\" to perform the analsis.\n"
			    + "Select the tab \"Analysis Settings\" and choose the settings for your analysis.\n"
			    + "Additional information about buttons and other options can be found by hovering over these elements.", "Help Dialog", JOptionPane.PLAIN_MESSAGE);
		}
	    }
	});
	frame.getContentPane().add(buttonShowHelp);

	// Button Show CFT

	buttonShowCFT.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonShowCFT.setBounds(420, 150, 200, 80);
	buttonShowCFT.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady())
		{
		    if (!DBUtility.containsCFT())
		    {
			PrintUtility.printWarning("The Neo4j database contains no CFT!");
		    }
		    else
		    {
			textAreaConsole.setText("");
			listModel.clear();
			ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				HashSet<String[]> cftcoll = DBUtility.getElementsbyLabel(NodeLabels.CFT);
				for (String[] cft : cftcoll)
				{
				    listModel.addElement("" + cft[0] + " | " + cft[1] + " [CFT]");
				}
			    }
			});
		    }
		}
	    }
	});
	frame.getContentPane().add(buttonShowCFT);

	// Button Show Outports

	buttonShowOutports.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonShowOutports.setBounds(420, 250, 200, 80);
	buttonShowOutports.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady())
		{
		    textAreaConsole.setText("");
		    if (listComponent.getSelectedValue() != null)
		    {
			String elements = listComponent.getSelectedValue();
			int indexafterID = elements.indexOf(" ");
			int elementid = Integer.parseInt(elements.substring(0, indexafterID));
			listModel.clear();
			ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				HashSet<String[]> outports = DBUtility.getOutportsOfCFT(elementid);
				if (!outports.isEmpty())
				{
				    for (String[] outport : outports)
				    {
					listModel.addElement("" + outport[0] + " " + outport[1]);
				    }
				}
				else
				{
				    PrintUtility.printWarning("Select a CFT!");
				}
			    }
			});
		    }
		    else
		    {
			PrintUtility.printWarning("Select a CFT!");
		    }
		}
	    }
	});
	frame.getContentPane().add(buttonShowOutports);

	// Button MCS Analysis

	buttonMCSAnalysis.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonMCSAnalysis.setBounds(420, 350, 200, 80);
	buttonMCSAnalysis.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		textAreaConsole.setText("");
		if (guiReady())
		{
		    if (listComponent.getSelectedValue() == null)
		    {
			PrintUtility.printWarning("Select an element from the list! The list can be displayed by pressing the buttons near it.");
		    }
		    else
		    {
			Timer timer = new Timer();
			String elements = listComponent.getSelectedValue();
			int indexafterID = elements.indexOf(" ");
			int elementid = Integer.parseInt(elements.substring(0, indexafterID));
			// Set<Future<?>> futureSet = new HashSet<Future<?>>();
			/* futureSet.add( */ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				MainTraversal.qualitativeAnalysis(DBUtility.getNodebyID(elementid));
				timer.cancel();
				if (SettingsUtility.isResultsPopUp())
				{
				    new MCSResultsDialog();
				}
			    }
			})/* ) */;
			timer.scheduleAtFixedRate(new TimerTask()
			{
			    @Override
			    public void run()
			    {
				/*
				 * for (Future<?> future : futureSet) { try { future.get(); } catch (InterruptedException | ExecutionException e) {
				 * e.printStackTrace(); } }
				 */
			    }
			}, GeneralUtility.waitTime2, GeneralUtility.waitTime2);
		    }
		}
	    }
	});
	frame.getContentPane().add(buttonMCSAnalysis);

	// Button Quantitative Analysis

	buttonQuantAnalysis.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonQuantAnalysis.setBounds(420, 450, 200, 80);
	buttonQuantAnalysis.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		textAreaConsole.setText("");
		if (guiReady())
		{
		    if (listComponent.getSelectedValue() == null)
		    {
			PrintUtility.printWarning("Select an element from the list! The list can be displayed by pressing the buttons near it.");
		    }
		    else
		    {
			Timer timer = new Timer();
			String elements = listComponent.getSelectedValue();
			int indexafterID = elements.indexOf(" ");
			int elementid = Integer.parseInt(elements.substring(0, indexafterID));
			// Set<Future<?>> futureSet = new HashSet<Future<?>>();
			/* futureSet.add( */ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				MainTraversal.quantitativeAnalysis(DBUtility.getNodebyID(elementid));
				timer.cancel();
				if (SettingsUtility.isResultsPopUp())
				{
				    new QuantResultsDialog();
				}
			    }
			})/* ) */;
			timer.scheduleAtFixedRate(new TimerTask()
			{
			    @Override
			    public void run()
			    {
				/*
				 * for (Future<?> future : futureSet) { try { future.get(); } catch (InterruptedException | ExecutionException e) {
				 * e.printStackTrace(); } }
				 */
			    }
			}, GeneralUtility.waitTime2, GeneralUtility.waitTime2);
		    }
		}
	    }
	});
	frame.getContentPane().add(buttonQuantAnalysis);

	// Button Clear MCS

	btnClearMinimalCut.setFont(new Font("Tahoma", Font.PLAIN, 20));
	btnClearMinimalCut.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		textAreaConsole.setText("");
		if (guiReady())
		{
		    ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    DBUtility.clearMCS();
			}
		    });
		}
	    }
	});
	btnClearMinimalCut.setBounds(15, 16, 300, 60);
	frame.getContentPane().add(btnClearMinimalCut);

	// Button Check DB

	buttonCheckDatabaseFor.setBounds(376, 16, 300, 60);
	buttonCheckDatabaseFor.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonCheckDatabaseFor.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		ThreadUtility.getThreadUtility().submitTask(new Runnable()
		{
		    @Override
		    public void run()
		    {
			HashSet<Node> failureNodes = DBChecker.checkCFTModels();
			if (failureNodes.isEmpty())
			{
			    PrintUtility.printInfo("All CFT in the database are constructed correctly.");
			}
			else
			{
			    PrintUtility.printWarning("There are some elements in the database that have errors.");
			}
			DBChecker.checkMCS();
			if (DBChecker.hasCFTCycles())
			{
			    PrintUtility.printWarning("There are some CFT in the database that contain themselves.");
			}
			else
			{
			    PrintUtility.printInfo("There are no CFT in the database that contain themselves.");
			}
		    }
		});
	    }
	});
	frame.getContentPane().add(buttonCheckDatabaseFor);

	JLabel lblExtraSettings = new JLabel("Extra Settings");
	lblExtraSettings.setFont(new Font("Tahoma", Font.BOLD, 22));
	lblExtraSettings.setBounds(30, 470, 200, 40);
	frame.getContentPane().add(lblExtraSettings);
    }

    private boolean guiReady()
    {
	if (!ThreadUtility.getThreadUtility().noTasksRunning())
	{
	    PrintUtility.printWarning("The Neo4j database is currently busy.\nFurther actions are not permitted until all processes are finished.");
	}
	return ThreadUtility.getThreadUtility().noTasksRunning();
    }
}
