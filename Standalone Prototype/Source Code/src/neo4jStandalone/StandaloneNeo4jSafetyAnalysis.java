package neo4jStandalone;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.neo4j.graphdb.Node;

import neo4jDatabase.DBConnection;
import neo4jDatabase.DBCFTGenerator;
import neo4jDatabase.DBUtility;
import neo4jDatabase.DBChecker;
import neo4jDatabase.DBUpdater;
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
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import java.awt.Font;
import java.awt.Color;
import javax.swing.SwingConstants;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.ActionListener;
import javax.swing.border.TitledBorder;
import javax.swing.UIManager;

public class StandaloneNeo4jSafetyAnalysis
{

    private final JFrame frame = new JFrame();

    private JFileChooser fc = new JFileChooser();

    // TextFields

    private final JTextField textFieldEAPFile = new JTextField();

    private final JTextField textFieldNeo4jDBPath = new JTextField();

    // Customizable Labels

    private final JLabel labelShowingNeo4jDBStatus = new JLabel("Neo4j Database Not Ready!");

    // Static Labels

    private final JLabel labelEAPFile = new JLabel("Enterprise Architect Project File (EAP)");

    private final JLabel labelNoOfLevels = new JLabel("No. of Levels");

    private final JLabel labelNoOfLinks = new JLabel("No. of Links per Level");

    private final JLabel labelStartingGate = new JLabel("Starting Gate");

    private final JLabel labelGateSwitching = new JLabel("Gate Switching");

    private final JLabel labelNeo4jDBStatus = new JLabel("Neo4j Database Status");

    private final JLabel labelNeo4jDBPath = new JLabel("Neo4j Database location");

    private final JLabel labelAnalysisTarget = new JLabel("Analysis Target");

    private final JLabel labelLinksStartingGate = new JLabel("Links after Starting Gate");

    private final JLabel labelMOONNumber = new JLabel("MOON-Gate Number");

    // Buttons

    private final JButton buttonUpdateDB = new JButton("Update Neo4j Database");

    private final JButton buttonSelectNeo4jDBPath = new JButton("Select Neo4j Database Folder");

    private final JButton buttonShowCFT = new JButton("Show CFT List");

    private final JButton buttonMCSAnalysis = new JButton("<html><center>Qualitative<br>Analysis</center></html>");

    private final JButton buttonShowOutports = new JButton("Show Outports");

    private final JButton buttonSelectEAPFile = new JButton("Select EAP File");

    private final JButton buttonCFTGen = new JButton("Generate CFT");

    private final JButton buttonShowHelp = new JButton("Show Help");

    private final JButton buttonQuantAnalysis = new JButton("<html><center>Quantitative<br>Analysis</center></html>");

    private final JButton buttonCheckDatabaseFor = new JButton("Check database for Errors");

    private final JButton buttonClearDatabase = new JButton("Clear database");

    private final JButton btnClearMinimalCut = new JButton("<html><center>Clear Minimal Cut Sets <br>and Quantitative Results</center></html>");

    // ScrollPanes and Lists

    private final JScrollPane scrollPaneConsole = new JScrollPane();

    private final JScrollPane scrollPaneForComponentList = new JScrollPane();

    private final JTextArea textAreaConsole = new JTextArea();

    private JList<String> listComponent;

    private final DefaultListModel<String> listModel = new DefaultListModel<String>();

    // TabbedPane und Panels

    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

    private final JPanel panelEA = new JPanel();

    private final JPanel panelCFTGen = new JPanel();

    private final JPanel panelAnalysis = new JPanel();

    // ComboBoxes and CheckBoxes

    private final JComboBox<String> comboBoxStartingGate = new JComboBox<String>();

    private final JComboBox<String> comboBoxNoOfLevels = new JComboBox<String>();

    private final JComboBox<String> comboBoxNoOfLinks = new JComboBox<String>();

    private final JComboBox<String> comboBoxGateSwitching = new JComboBox<String>();

    private final JComboBox<String> comboBoxAnalysisTarget = new JComboBox<String>();

    private int lastSelectedTarget = 0;

    private final JComboBox<String> comboBoxLinksStartingGate = new JComboBox<String>();

    private final JComboBox<String> comboBoxMOONNumber = new JComboBox<String>();

    private final JCheckBox checkBoxMultithreading = new JCheckBox("Use Multithreading");

    private final JCheckBox checkboxAnalysisResultsPopup = new JCheckBox("Analysis Results Popup");

    private final JCheckBox checkboxReuseAnalysisResults = new JCheckBox("Reuse Analysis Results");

    private final JCheckBox checkboxCalculatePrimeImplicants = new JCheckBox("Calculate Prime Implicants");

    private final JCheckBox checkboxTraversalFramework = new JCheckBox("Use the Neo4j Traversal Framework");

    private final JPanel panelExtra = new JPanel();

    private final JCheckBox checkboxHideInformationMessages = new JCheckBox("Hide Information Messages");

    private final JCheckBox checkboxUseCombinations = new JCheckBox("Use Combinations");

    static {
	    try {
		
	    	System.loadLibrary("SSJavaCOM");
	    	System.out.println("Native code library loaded.\n");
	    } catch (UnsatisfiedLinkError e) {
	      System.err.println("Native code library failed to load.\n" + e);
	      System.exit(1);
	    }
	  }
    
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
		    StandaloneNeo4jSafetyAnalysis window = new StandaloneNeo4jSafetyAnalysis();
		    ThreadUtility.getThreadUtility();
		    window.frame.setVisible(true);
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
    private StandaloneNeo4jSafetyAnalysis()
    {
	initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
	// Frame

	frame.setBounds(100, 100, 1100, 1200);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().setLayout(null);

	// TextArea Console

	scrollPaneConsole.setToolTipText("");
	scrollPaneConsole.setViewportBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Output Console", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
	scrollPaneConsole.setBounds(20, 770, 1043, 358);
	frame.getContentPane().add(scrollPaneConsole);
	textAreaConsole.setFont(new Font("Tahoma", Font.PLAIN, 16));
	scrollPaneConsole.setViewportView(textAreaConsole);
	textAreaConsole.setLineWrap(true);
	textAreaConsole.setEditable(false);
	PrintStream printStream = new PrintStream(CustomOutputStream.addTextAreaAndGetCustomOutputStream(textAreaConsole));

	// Output Redirection

	System.setOut(printStream);
	System.setErr(printStream);

	// Component Selection List

	scrollPaneForComponentList.setBounds(745, 150, 318, 604);
	frame.getContentPane().add(scrollPaneForComponentList);
	scrollPaneForComponentList.setToolTipText("");
	scrollPaneForComponentList.setViewportBorder(new TitledBorder(null, "Model Selection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	listComponent = new JList<String>(listModel);
	listComponent.setFont(new Font("Tahoma", Font.PLAIN, 18));
	listComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	scrollPaneForComponentList.setViewportView(listComponent);

	// Label Showing Neo4j DB Status

	labelShowingNeo4jDBStatus.setBorder(new LineBorder(new Color(0, 0, 0), 3));
	labelShowingNeo4jDBStatus.setForeground(Color.WHITE);
	labelShowingNeo4jDBStatus.setBackground(Color.RED);
	labelShowingNeo4jDBStatus.setBounds(427, 55, 400, 65);
	labelShowingNeo4jDBStatus.setFont(new Font("Tahoma", Font.PLAIN, 20));
	labelShowingNeo4jDBStatus.setHorizontalAlignment(SwingConstants.CENTER);
	labelShowingNeo4jDBStatus.setOpaque(true);
	frame.getContentPane().add(labelShowingNeo4jDBStatus);

	// TabbedPane

	tabbedPane.setFont(new Font("Tahoma", Font.PLAIN, 18));
	tabbedPane.setBounds(20, 165, 469, 585);
	frame.getContentPane().add(tabbedPane);
	panelEA.setToolTipText("Tab for Enterprise Architect specific options.");

	// Panel EA

	tabbedPane.addTab("Enterprise Architect", null, panelEA, null);
	panelEA.setLayout(null);
	panelCFTGen.setToolTipText("Tab for CFT Generation");

	// Panel CFT Gen

	tabbedPane.addTab("CFT Generator", null, panelCFTGen, null);
	panelCFTGen.setLayout(null);

	// Panel Analysis Settings

	tabbedPane.addTab("Analysis Settings", null, panelAnalysis, null);
	panelAnalysis.setLayout(null);

	tabbedPane.addTab("Extra Options", null, panelExtra, null);
	panelExtra.setLayout(null);

	initializeTextFields();
	initializeComboCheckBoxes();
	initializeButtons();
	initializeLabels();
    }

    private void initializeTextFields()
    {
	// TextField Neo4j DB Path

	textFieldNeo4jDBPath.setBounds(20, 55, 350, 30);
	textFieldNeo4jDBPath.setFont(new Font("Tahoma", Font.PLAIN, 18));
	textFieldNeo4jDBPath.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (!guiReady())
		{
		    textFieldNeo4jDBPath.setText(DBConnection.getCurrentDBPath());
		}
		else
		{
		    if (isValidDBPath(true))
		    {
			textFieldNeo4jDBPath.setSelectedTextColor(Color.BLUE);
			ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				DBConnection.startDB(textFieldNeo4jDBPath.getText());
				setDBStatus();
			    }
			});
		    }
		    else
		    {
			textFieldNeo4jDBPath.setSelectedTextColor(Color.RED);
			textFieldNeo4jDBPath.selectAll();
		    }
		}
	    }
	});
	frame.getContentPane().add(textFieldNeo4jDBPath);

	// TextField EAP File

	textFieldEAPFile.setBounds(30, 65, 400, 30);
	textFieldEAPFile.setFont(new Font("Tahoma", Font.PLAIN, 18));
	textFieldEAPFile.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady() && isEAPFile())
		{
		    if (isValidDBPath(false))
		    {
			ThreadUtility.getThreadUtility().submitTask(new Runnable()
			{
			    @Override
			    public void run()
			    {
				DBConnection.startDB(textFieldNeo4jDBPath.getText());
				setDBStatus();
			    }
			});
		    }
		    textFieldEAPFile.setSelectedTextColor(Color.BLUE);
		}
		else
		{
		    textFieldEAPFile.setSelectedTextColor(Color.RED);
		    textFieldEAPFile.selectAll();
		}
	    }
	});
	panelEA.add(textFieldEAPFile);
    }

    private void initializeComboCheckBoxes()
    {
	comboBoxNoOfLevels.setToolTipText("The number of Levels that the generated CFT shall have.");
	// ComboBox No of Levels

	comboBoxNoOfLevels.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxNoOfLevels.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
	comboBoxNoOfLevels.setSelectedIndex(0);
	comboBoxNoOfLevels.setBounds(390, 120, 50, 30);
	panelCFTGen.add(comboBoxNoOfLevels);
	comboBoxNoOfLinks.setToolTipText("The number of Links per Level for each gate after the starting gate.");

	// ComboBox No of Links

	comboBoxNoOfLinks.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxNoOfLinks.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "2", "3", "4", "5", "6" }));
	comboBoxNoOfLinks.setSelectedIndex(0);
	comboBoxNoOfLinks.setBounds(390, 170, 50, 30);
	panelCFTGen.add(comboBoxNoOfLinks);

	// ComboBox Starting Gate

	comboBoxStartingGate.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxStartingGate.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "AND-Gate", "OR-Gate", "XOR-Gate", "NOT-Gate", "MOON-Gate" }));
	comboBoxStartingGate.setSelectedIndex(0);
	comboBoxStartingGate.setBounds(290, 270, 150, 30);
	panelCFTGen.add(comboBoxStartingGate);

	// ComboBox Gate Switching

	comboBoxGateSwitching.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxGateSwitching.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "Switch Gate at every Level", "Switch Gate at every Link" }));
	comboBoxGateSwitching.setSelectedIndex(0);
	comboBoxGateSwitching.setBounds(190, 220, 250, 30);
	panelCFTGen.add(comboBoxGateSwitching);

	comboBoxLinksStartingGate.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
	comboBoxLinksStartingGate.setSelectedIndex(1);
	comboBoxLinksStartingGate.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxLinksStartingGate.setBounds(390, 320, 50, 30);
	panelCFTGen.add(comboBoxLinksStartingGate);

	comboBoxMOONNumber.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
	comboBoxMOONNumber.setSelectedIndex(0);
	comboBoxMOONNumber.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxMOONNumber.setBounds(390, 370, 50, 30);
	panelCFTGen.add(comboBoxMOONNumber);

	comboBoxAnalysisTarget.setFont(new Font("Tahoma", Font.PLAIN, 18));
	comboBoxAnalysisTarget.setModel(new DefaultComboBoxModel<String>(new String[]
	{ "EAP File", "Generated CFT" }));
	comboBoxAnalysisTarget.setSelectedIndex(0);
	comboBoxAnalysisTarget.setBounds(530, 216, 180, 30);
	comboBoxAnalysisTarget.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (!guiReady())
		{
		    comboBoxAnalysisTarget.setSelectedIndex(lastSelectedTarget);
		}
		else
		{
		    if (isValidDBPath(true))
		    {
			int selectedIndex = comboBoxAnalysisTarget.getSelectedIndex();
			if (selectedIndex != lastSelectedTarget)
			{
			    if (selectedIndex == 0)
			    {
				ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{
				    @Override
				    public void run()
				    {
					DBConnection.startDB(textFieldNeo4jDBPath.getText());
				    }
				});
				lastSelectedTarget = 0;
			    }
			    else
			    {
				if (selectedIndex == 1)
				{
				    ThreadUtility.getThreadUtility().submitTask(new Runnable()
				    {
					@Override
					public void run()
					{
					    DBConnection.startDB(textFieldNeo4jDBPath.getText() + File.separator + "Generated CFT");
					}
				    });
				    lastSelectedTarget = 1;
				}
			    }
			}
		    }
		}
	    }

	});
	frame.getContentPane().add(comboBoxAnalysisTarget);

	// CheckBox Use Multithreading

	checkBoxMultithreading.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkBoxMultithreading.setBounds(40, 40, 300, 30);
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
	panelAnalysis.add(checkBoxMultithreading);

	// CheckBox Analysis Results Popup

	checkboxAnalysisResultsPopup.setSelected(true);
	checkboxAnalysisResultsPopup.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxAnalysisResultsPopup.setBounds(40, 90, 300, 30);
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
	panelAnalysis.add(checkboxAnalysisResultsPopup);

	checkboxReuseAnalysisResults.setFont(new Font("Tahoma", Font.PLAIN, 18));
	checkboxReuseAnalysisResults.setBounds(69, 179, 300, 30);
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
	panelAnalysis.add(checkboxReuseAnalysisResults);

	checkboxCalculatePrimeImplicants.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxCalculatePrimeImplicants.setBounds(40, 220, 300, 30);
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
	panelAnalysis.add(checkboxCalculatePrimeImplicants);

	checkboxTraversalFramework.setSelected(true);
	checkboxTraversalFramework.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxTraversalFramework.setBounds(40, 141, 400, 30);
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
	panelAnalysis.add(checkboxTraversalFramework);

	checkboxUseCombinations.setFont(new Font("Tahoma", Font.PLAIN, 20));
	checkboxUseCombinations.setBounds(40, 268, 300, 30);
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
	panelAnalysis.add(checkboxUseCombinations);

	// CheckBox Hide Info

	checkboxHideInformationMessages.setFont(new Font("Tahoma", Font.PLAIN, 18));
	checkboxHideInformationMessages.setBounds(40, 280, 300, 30);
	checkboxHideInformationMessages.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		SettingsUtility.setPrintInfo(!checkboxHideInformationMessages.isSelected());
	    }
	});
	panelExtra.add(checkboxHideInformationMessages);
    }

    private void initializeButtons()
    {
	// Button Show Help

	buttonShowHelp.setBounds(863, 20, 200, 80);
	buttonShowHelp.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonShowHelp.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (!isValidDBPath(false))
		{
		    JOptionPane.showMessageDialog(frame, "Select a path for the Neo4j database.\n" + "Click the Button \"Select Neo4j Database Folder\" and pick a folder for the Neo4j database.\n"
			    + "Close this dialog and reopen it when the Neo4j database is running.", "Help Dialog", JOptionPane.PLAIN_MESSAGE);
		}
		else
		{
		    if (!DBUtility.containsCFT())
		    {
			JOptionPane.showMessageDialog(frame,
				"The Neo4j database must be updated.\n" + "Select the tab \"Enterprise Architect\" and press the button \"Select EAP File\".\n"
					+ "Then select an Enterprise Architect Project file that contains CFT model.\n"
					+ "Then press the button \"Update Neo4j Database\" and wait until the update is completed.\n" + "Close this dialog and reopen it when the update is complete.",
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
	    }
	});
	frame.getContentPane().add(buttonShowHelp);

	// Button Select Neo4j Path

	buttonSelectNeo4jDBPath.setBounds(20, 90, 350, 60);
	buttonSelectNeo4jDBPath.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonSelectNeo4jDBPath.addActionListener(new ActionListener()
	{

	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady())
		{
		    boolean wasValidDBPath = isValidDBPath(false);
		    fc = new JFileChooser();
		    fc.resetChoosableFileFilters();
		    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    fc.setAcceptAllFileFilterUsed(false);
		    int returnVal = fc.showOpenDialog(frame);
		    if (returnVal == JFileChooser.APPROVE_OPTION)
		    {
			File file = fc.getSelectedFile();
			textFieldNeo4jDBPath.setText(file.getAbsolutePath());
			if (isValidDBPath(true))
			{
			    ThreadUtility.getThreadUtility().submitTask(new Runnable()
			    {
				@Override
				public void run()
				{
				    DBConnection.startDB(textFieldNeo4jDBPath.getText());
				    setDBStatus();
				}
			    });
			}
			else
			{
			    if (wasValidDBPath)
			    {
				ThreadUtility.getThreadUtility().submitTask(new Runnable()
				{

				    @Override
				    public void run()
				    {
					DBConnection.shutdownDB();
					setDBStatus();
				    }
				});
			    }
			}
		    }
		}
	    }
	});
	frame.getContentPane().add(buttonSelectNeo4jDBPath);
	buttonSelectEAPFile.setToolTipText("Opens a Window to select an EAP File.");

	// Button Select EAP File

	buttonSelectEAPFile.setBounds(30, 100, 400, 60);
	buttonSelectEAPFile.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonSelectEAPFile.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady())
		{
		    fc = new JFileChooser();
		    fc.resetChoosableFileFilters();
		    fc.addChoosableFileFilter(new FileNameExtensionFilter(".eap Files", "eap", "EAP"));
		    fc.setAcceptAllFileFilterUsed(false);
		    int returnVal = fc.showOpenDialog(frame);
		    if (returnVal == JFileChooser.APPROVE_OPTION)
		    {
			File file = fc.getSelectedFile();
			textFieldEAPFile.setText(file.getAbsolutePath());
			if (isValidDBPath(false))
			{
			    comboBoxAnalysisTarget.setSelectedIndex(0);
			    ThreadUtility.getThreadUtility().submitTask(new Runnable()
			    {
				@Override
				public void run()
				{
				    DBConnection.startDB(textFieldNeo4jDBPath.getText());
				}
			    });
			}
		    }
		}
	    }
	});
	panelEA.add(buttonSelectEAPFile);
	buttonUpdateDB.setToolTipText("Updates the Neo4j database with the contents of the selected EAP file.");

	// Button Update DB

	buttonUpdateDB.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonUpdateDB.setBounds(30, 170, 400, 60);
	buttonUpdateDB.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady() && isValidDBPath(true) && isEAPFile())
		{
		    textAreaConsole.setText("");
		    comboBoxAnalysisTarget.setSelectedIndex(0);
		    ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    DBConnection.startDB(textFieldNeo4jDBPath.getText());
			    DBUpdater.updateFromEA(textFieldEAPFile.getText());
			    setDBStatus();
			}
		    });
		}
	    }
	});
	panelEA.add(buttonUpdateDB);
	buttonCFTGen.setToolTipText("Generates a CFT with the specified settings.");

	// Button CFT Gen

	buttonCFTGen.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonCFTGen.setBounds(60, 30, 350, 50);
	buttonCFTGen.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady() && isValidDBPath(true))
		{
		    ThreadUtility.getThreadUtility().submitTask(new Runnable()
		    {
			@Override
			public void run()
			{
			    DBConnection.startDB(textFieldNeo4jDBPath.getText());
			    File file = new File(textFieldNeo4jDBPath.getText() + File.separator + "Generated CFT");
			    GeneralUtility.deleteDir(file);
			    file.mkdir();
			    DBConnection.startBatchInserter(textFieldNeo4jDBPath.getText() + File.separator + "Generated CFT");
			    int levels = comboBoxNoOfLevels.getSelectedIndex() + 1;
			    int followlinks = comboBoxNoOfLinks.getSelectedIndex() + 2;
			    int links = comboBoxLinksStartingGate.getSelectedIndex() + 1;
			    int startingGateType = comboBoxStartingGate.getSelectedIndex() + 1;
			    int moonnumber = comboBoxMOONNumber.getSelectedIndex() + 1;
			    int switchingNumber = comboBoxGateSwitching.getSelectedIndex();
			    DBCFTGenerator.createRandomCFTBatch(levels, links, followlinks, startingGateType, moonnumber, switchingNumber);
			    DBConnection.shutdownBatchInserter();
			}
		    });
		}
	    }
	});
	panelCFTGen.add(buttonCFTGen);

	// Button Show CFT

	buttonShowCFT.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonShowCFT.setBounds(530, 280, 180, 80);
	buttonShowCFT.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady() && isValidDBPath(true))
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
	buttonShowOutports.setBounds(530, 380, 180, 80);
	buttonShowOutports.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (guiReady() && isValidDBPath(true))
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
	buttonMCSAnalysis.setBounds(530, 480, 180, 80);
	buttonMCSAnalysis.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		textAreaConsole.setText("");
		if (guiReady() && isValidDBPath(true))
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
			Set<Future<?>> futureSet = new HashSet<Future<?>>();
			futureSet.add(ThreadUtility.getThreadUtility().submitTask(new Runnable()
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
			}));
			timer.scheduleAtFixedRate(new TimerTask()
			{
			    @Override
			    public void run()
			    {
				for (Future<?> future : futureSet)
				{
				    try
				    {
					future.get();
				    }
				    catch (InterruptedException | ExecutionException e)
				    {
					e.printStackTrace();
				    }
				}
			    }
			}, GeneralUtility.waitTime2, GeneralUtility.waitTime2);
		    }
		}
	    }
	});
	frame.getContentPane().add(buttonMCSAnalysis);

	// Button Quantitative Analysis

	buttonQuantAnalysis.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonQuantAnalysis.setBounds(530, 580, 180, 80);
	buttonQuantAnalysis.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		textAreaConsole.setText("");
		if (guiReady() && isValidDBPath(true))
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

	buttonCheckDatabaseFor.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonCheckDatabaseFor.setBounds(40, 200, 300, 60);
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
	panelExtra.add(buttonCheckDatabaseFor);

	buttonClearDatabase.setFont(new Font("Tahoma", Font.PLAIN, 20));
	buttonClearDatabase.setBounds(40, 40, 300, 60);
	buttonClearDatabase.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		ThreadUtility.getThreadUtility().submitTask(new Runnable()
		{
		    @Override
		    public void run()
		    {
			DBUtility.clearDB();
		    }
		});
	    }
	});
	panelExtra.add(buttonClearDatabase);

	// Button Clear MCS

	btnClearMinimalCut.setFont(new Font("Tahoma", Font.PLAIN, 20));
	btnClearMinimalCut.setBounds(40, 120, 300, 60);
	btnClearMinimalCut.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		textAreaConsole.setText("");
		if (guiReady() && isValidDBPath(true))
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
	panelExtra.add(btnClearMinimalCut);
    }

    private void initializeLabels()
    {
	// Label Neo4j DB Status

	labelNeo4jDBStatus.setFont(new Font("Tahoma", Font.PLAIN, 20));
	labelNeo4jDBStatus.setBounds(498, 20, 250, 30);
	frame.getContentPane().add(labelNeo4jDBStatus);

	// Label Neo4j DB Path

	labelNeo4jDBPath.setLabelFor(textFieldNeo4jDBPath);
	labelNeo4jDBPath.setBounds(20, 20, 350, 30);
	labelNeo4jDBPath.setFont(new Font("Tahoma", Font.PLAIN, 20));
	frame.getContentPane().add(labelNeo4jDBPath);

	// Label EAP File

	labelEAPFile.setBounds(30, 30, 400, 30);
	labelEAPFile.setFont(new Font("Tahoma", Font.PLAIN, 20));
	labelEAPFile.setLabelFor(textFieldEAPFile);
	panelEA.add(labelEAPFile);
	labelNoOfLevels.setToolTipText("The number of Levels that the generated CFT shall have.");

	// Label No of Levels

	labelNoOfLevels.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelNoOfLevels.setBounds(20, 120, 150, 30);
	panelCFTGen.add(labelNoOfLevels);
	labelNoOfLinks.setToolTipText("The number of Links per Level for each gate after the starting gate.");

	// Label No of Links

	labelNoOfLinks.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelNoOfLinks.setBounds(20, 170, 200, 30);
	panelCFTGen.add(labelNoOfLinks);

	// Label Starting Gate

	labelStartingGate.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelStartingGate.setBounds(20, 270, 150, 30);
	panelCFTGen.add(labelStartingGate);

	// Label Gate Switching

	labelGateSwitching.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelGateSwitching.setBounds(20, 220, 150, 30);
	panelCFTGen.add(labelGateSwitching);

	// Label Links after Starting Gate

	labelLinksStartingGate.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelLinksStartingGate.setBounds(20, 320, 200, 30);
	panelCFTGen.add(labelLinksStartingGate);

	// Label MOON Number

	labelMOONNumber.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelMOONNumber.setBounds(20, 370, 200, 30);
	panelCFTGen.add(labelMOONNumber);

	// Label Analysis Target

	labelAnalysisTarget.setFont(new Font("Tahoma", Font.PLAIN, 18));
	labelAnalysisTarget.setBounds(530, 170, 180, 30);
	frame.getContentPane().add(labelAnalysisTarget);
    }

    private void setDBStatus()
    {
	String db = textFieldNeo4jDBPath.getText();
	boolean dbPathValid = (db.length() != 0 && new File(db).isDirectory());
	if (!dbPathValid)
	{
	    labelShowingNeo4jDBStatus.setText("Neo4j Database Not Ready!");
	    labelShowingNeo4jDBStatus.setForeground(Color.WHITE);
	    labelShowingNeo4jDBStatus.setBackground(Color.RED);
	    labelShowingNeo4jDBStatus.setToolTipText("Please select a location for the Neo4j Database.");
	}
	else
	{
	    if (DBConnection.isReady())
	    {
		if (DBUtility.containsCFT())
		{
		    labelShowingNeo4jDBStatus.setText("Neo4j Database Ready For Analysis!");
		    labelShowingNeo4jDBStatus.setForeground(Color.BLACK);
		    labelShowingNeo4jDBStatus.setBackground(Color.GREEN);
		    labelShowingNeo4jDBStatus.setToolTipText("The Analysis can be performed.\nAlso the Neo4j database can be updated or a CFT can be generated.");
		}
		else
		{
		    labelShowingNeo4jDBStatus.setText("Neo4j Database Ready For Update!");
		    labelShowingNeo4jDBStatus.setForeground(Color.BLACK);
		    labelShowingNeo4jDBStatus.setBackground(Color.YELLOW);
		    labelShowingNeo4jDBStatus.setToolTipText("The Neo4j database contains no CFT and must be updated from Enterprise Architect.\nAlternatively a CFT can be generated.");
		}
	    }
	}
    }

    private boolean isEAPFile()
    {
	String eap = textFieldEAPFile.getText();
	int length = eap.length();
	if (length > 4 && new File(eap).exists())
	{
	    String end = eap.substring(length - 4, length);
	    return end.equalsIgnoreCase(".eap");
	}
	return false;
    }

    private boolean isValidDBPath(boolean message)
    {
	String db = textFieldNeo4jDBPath.getText();
	boolean dbPathValid = (db.length() != 0 && new File(db).isDirectory());
	if (!dbPathValid && message)
	{
	    PrintUtility.printWarning("The selected Neo4j database path is invalid.\nPlease select a valid Neo4j database folder.");
	}
	return dbPathValid;
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
