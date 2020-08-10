package neo4jResultsDialog;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JScrollPane;
import java.awt.Font;
import java.util.HashSet;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import neo4jDatabase.DBUtility;
import neo4jGateSets.ResultGateSet;
import neo4jMCS.MCS;
import neo4jMCS.MCSNode;
import neo4jTraversal.MainTraversal;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import java.awt.Color;

public class MCSResultsDialog extends JDialog
{

    /**
     * 
     */
    private static final long serialVersionUID = 92425285016765017L;

    private final JPanel contentPanel = new JPanel();

    private MCSResultsDialog mcsResultsDialog = null;
    private JTextField txtTravtime;
    private JTextField txtMcstime;
    private JTextField txtPrimetime;

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
	try
	{
	    MCSResultsDialog dialog = new MCSResultsDialog();
	    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	    dialog.setVisible(true);
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

    /**
     * Create the dialog.
     */
    public MCSResultsDialog()
    {
	String idname = "TestElement";
	HashSet<MCS> mcsSet = new HashSet<MCS>();
	ResultGateSet rgs = MainTraversal.getQualititativeAnalysisResults();
	if (rgs != null)
	{
	    idname = DBUtility.getIDAndName(rgs.getStartNode()) + " [" + DBUtility.getRelevantLabel(rgs.getStartNode()) + "]";
	    mcsSet = rgs.getMCSSet();
	}

	setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	setTitle("Results of the Qualitative Analysis");
	setBounds(100, 100, 650, 870);
	getContentPane().setLayout(new BorderLayout());
	contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
	getContentPane().add(contentPanel, BorderLayout.CENTER);
	contentPanel.setLayout(null);

	JScrollPane scrollPane = new JScrollPane();
	scrollPane.setBorder(new LineBorder(Color.BLACK, 2));
	scrollPane.setBounds(15, 80, 600, 550);
	contentPanel.add(scrollPane);

	DefaultMutableTreeNode top = null;
	DefaultMutableTreeNode mcs = null;
	DefaultMutableTreeNode mcsNode = null;

	top = new DefaultMutableTreeNode("Minimal Cut Sets of the Element " + idname);

	int mcsID = 1;
	for (MCS m : mcsSet)
	{
	    mcs = new DefaultMutableTreeNode(mcsID++ + ". Minimal Cut Set");
	    for (MCSNode mNode : m.getMCSNodeSet())
	    {
		mcsNode = new DefaultMutableTreeNode(DBUtility.getIDAndName(mNode) + " [" + DBUtility.getRelevantLabel(mNode.getNode()) + "]");
		mcs.add(mcsNode);
	    }
	    top.add(mcs);
	}

	JTree tree = new JTree();
	scrollPane.setViewportView(tree);
	tree.setRowHeight(30);
	tree.setFont(new Font("Tahoma", Font.PLAIN, 20));
	tree.setModel(new DefaultTreeModel(top));

	JButton buttonOK = new JButton("Close");
	buttonOK.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (mcsResultsDialog != null)
		{
		    mcsResultsDialog.dispose();
		}
	    }
	});
	buttonOK.setFont(new Font("Tahoma", Font.PLAIN, 22));
	buttonOK.setBounds(475, 678, 140, 80);
	contentPanel.add(buttonOK);
	
	JLabel lblMinimalCutSets = new JLabel("Qualitative Analysis Results");
	lblMinimalCutSets.setFont(new Font("Tahoma", Font.BOLD, 24));
	lblMinimalCutSets.setBounds(105, 16, 400, 40);
	contentPanel.add(lblMinimalCutSets);
	
	txtTravtime = new JTextField();
	txtTravtime.setEditable(false);
	txtTravtime.setBorder(new LineBorder(new Color(0, 0, 0), 2));
	txtTravtime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	if (MainTraversal.getTraversalTime() != -1)
	{
	    txtTravtime.setText(MainTraversal.getTraversalTime() + " ms");
	}
	else
	{
	    txtTravtime.setText("-----");
	}
	txtTravtime.setBounds(340, 650, 120, 40);
	contentPanel.add(txtTravtime);
	txtTravtime.setColumns(10);
	
	txtMcstime = new JTextField();
	txtMcstime.setEditable(false);
	txtMcstime.setBorder(new LineBorder(Color.BLACK, 2));
	txtMcstime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	if (MainTraversal.getMCSCalculationTime() != -1)
	{
	    txtMcstime.setText(MainTraversal.getMCSCalculationTime() + " ms");
	}
	else
	{
	    txtMcstime.setText("-----");
	}
	txtMcstime.setBounds(340, 700, 120, 40);
	contentPanel.add(txtMcstime);
	txtMcstime.setColumns(10);
	
	txtPrimetime = new JTextField();
	txtPrimetime.setEditable(false);
	txtPrimetime.setBorder(new LineBorder(Color.BLACK, 2));
	txtPrimetime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	if (MainTraversal.getPrimeImplicantCalcTime() != -1)
	{
	    txtPrimetime.setText(MainTraversal.getPrimeImplicantCalcTime() + " ms");
	}
	else
	{
	    txtPrimetime.setText("-----");
	}
	txtPrimetime.setBounds(340, 750, 120, 40);
	contentPanel.add(txtPrimetime);
	txtPrimetime.setColumns(10);
	
	JLabel lblTraversalTime = new JLabel("Traversal Time:");
	lblTraversalTime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	lblTraversalTime.setBounds(20, 650, 300, 40);
	contentPanel.add(lblTraversalTime);
	
	JLabel lblMinimalCutSet = new JLabel("Minimal Cut Set Calculation Time:");
	lblMinimalCutSet.setFont(new Font("Tahoma", Font.PLAIN, 19));
	lblMinimalCutSet.setBounds(20, 700, 300, 40);
	contentPanel.add(lblMinimalCutSet);
	
	JLabel lblPrimeImplicantsCalculation = new JLabel("Prime Implicants Calculation Time:");
	lblPrimeImplicantsCalculation.setFont(new Font("Tahoma", Font.PLAIN, 19));
	lblPrimeImplicantsCalculation.setBounds(20, 750, 300, 40);
	contentPanel.add(lblPrimeImplicantsCalculation);
	this.setVisible(true);
	mcsResultsDialog = this;
    }
}
