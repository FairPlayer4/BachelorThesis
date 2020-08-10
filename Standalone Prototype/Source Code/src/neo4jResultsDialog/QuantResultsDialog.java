package neo4jResultsDialog;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import neo4jDatabase.DBUtility;
import neo4jTraversal.MainTraversal;

import java.awt.Color;

public class QuantResultsDialog extends JDialog
{
    /**
     * 
     */
    private static final long serialVersionUID = 5848122285926028769L;

    private JTextField txtResult;

    private JTextField txtOutport;

    private QuantResultsDialog quantDialog;

    private JTextField txtTravtime;

    private JTextField txtMcstime;

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
	try
	{
	    QuantResultsDialog dialog = new QuantResultsDialog();
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
    public QuantResultsDialog()
    {
    	setTitle("Results of the Quantitative Analysis");
	this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	setBounds(100, 100, 650, 420);
	getContentPane().setLayout(null);

	JButton buttonOK = new JButton("Close");
	buttonOK.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		if (quantDialog != null)
		{
		    quantDialog.dispose();
		}
	    }
	});
	buttonOK.setFont(new Font("Tahoma", Font.PLAIN, 22));
	buttonOK.setBounds(466, 251, 147, 82);
	getContentPane().add(buttonOK);

	txtResult = new JTextField();
	txtResult.setBorder(new LineBorder(Color.BLACK, 2));
	txtResult.setEditable(false);
	txtResult.setFont(new Font("Tahoma", Font.PLAIN, 22));
	txtResult.setBounds(301, 155, 300, 40);
	getContentPane().add(txtResult);
	if (MainTraversal.getQuantitativeAnalysisResults() != -1)
	{
	    txtResult.setText("" + MainTraversal.getQuantitativeAnalysisResults());
	}
	txtResult.setColumns(10);
	
	txtOutport = new JTextField();
	txtOutport.setBorder(new LineBorder(Color.BLACK, 2));
	txtOutport.setEditable(false);
	txtOutport.setFont(new Font("Tahoma", Font.PLAIN, 22));
	if (MainTraversal.getOutport() != null)
	{
	    txtOutport.setText(DBUtility.getIDAndName(MainTraversal.getOutport()));
	}
	txtOutport.setBounds(301, 99, 300, 40);
	getContentPane().add(txtOutport);
	txtOutport.setColumns(10);

	txtTravtime = new JTextField();
	txtTravtime.setBorder(new LineBorder(Color.BLACK, 2));
	txtTravtime.setEditable(false);
	txtTravtime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	if (MainTraversal.getTraversalTime() != -1)
	{
	    txtTravtime.setText(MainTraversal.getTraversalTime() + " ms");
	}
	else
	{
	    txtTravtime.setText("-----");
	}
	txtTravtime.setBounds(301, 251, 120, 40);
	getContentPane().add(txtTravtime);
	txtTravtime.setColumns(10);

	txtMcstime = new JTextField();
	txtMcstime.setBorder(new LineBorder(Color.BLACK, 2));
	txtMcstime.setEditable(false);
	txtMcstime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	if (MainTraversal.getQuantTime() != 1)
	{
	    txtMcstime.setText(MainTraversal.getQuantTime() + " ms");
	}
	else
	{
	    txtMcstime.setText("-----");
	}
	txtMcstime.setBounds(301, 300, 120, 40);
	getContentPane().add(txtMcstime);
	txtMcstime.setColumns(10);

	JLabel lblNewLabel = new JLabel("Probability of Occurence:");
	lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 22));
	lblNewLabel.setBounds(26, 154, 270, 40);
	getContentPane().add(lblNewLabel);

	JLabel lblAnalyzedCftOutport = new JLabel("Analyzed CFT Outport:");
	lblAnalyzedCftOutport.setFont(new Font("Tahoma", Font.PLAIN, 22));
	lblAnalyzedCftOutport.setBounds(26, 98, 270, 40);
	getContentPane().add(lblAnalyzedCftOutport);
	
	JLabel lblQuantitativeAnalysisResults = new JLabel("Quantitative Analysis Results");
	lblQuantitativeAnalysisResults.setFont(new Font("Tahoma", Font.BOLD, 24));
	lblQuantitativeAnalysisResults.setBounds(122, 16, 400, 40);
	getContentPane().add(lblQuantitativeAnalysisResults);
	
	JLabel lblTraversalTime = new JLabel("Traversal Time:");
	lblTraversalTime.setFont(new Font("Tahoma", Font.PLAIN, 19));
	lblTraversalTime.setBounds(26, 250, 260, 40);
	getContentPane().add(lblTraversalTime);

	JLabel lblMinimalCutSet = new JLabel("Quantitative Analysis Time:");
	lblMinimalCutSet.setFont(new Font("Tahoma", Font.PLAIN, 19));
	lblMinimalCutSet.setBounds(26, 299, 260, 40);
	getContentPane().add(lblMinimalCutSet);
	this.setVisible(true);
	quantDialog = this;
    }
}
