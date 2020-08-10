package neo4jSocket;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import neo4jUtility.CustomOutputStream;
import neo4jUtility.GeneralLogger;

import java.awt.Font;

public class SocketConnection
{

    private JFrame frmJavaApplicationConsole;

    private static SocketConnection socketConnection;

    private static String[] argumentsForJava;

    private JTextArea textAreaConsole;

    private JScrollPane scrollPane;

    public static void main(String[] args)
    {
	if (args != null && args.length != 0)
	{
	    argumentsForJava = args;
	}
	if (argumentsForJava != null)
	{
	    GeneralLogger.startLogger(argumentsForJava[2]);
	}
	EventQueue.invokeLater(new Runnable()
	{
	    public void run()
	    {
		try
		{
		    new SocketConnection();
		}
		catch (Exception e)
		{
		    GeneralLogger.logError(e);
		}
	    }
	});
    }

    static void showSocketConnectionWindow(boolean visible)
    {
	socketConnection.frmJavaApplicationConsole.setVisible(visible);
    }

    /**
     * Create the application.
     */
    private SocketConnection()
    {
	initialize();
	socketConnection = this;
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
	frmJavaApplicationConsole = new JFrame();
	frmJavaApplicationConsole.setTitle("Java Application Console Output");
	frmJavaApplicationConsole.setBounds(100, 100, 600, 800);
	frmJavaApplicationConsole.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	frmJavaApplicationConsole.getContentPane().setLayout(null);

	scrollPane = new JScrollPane();
	scrollPane.setBounds(20, 20, 540, 710);
	frmJavaApplicationConsole.getContentPane().add(scrollPane);

	textAreaConsole = new JTextArea();
	textAreaConsole.setEnabled(false);
	textAreaConsole.setEditable(false);
	textAreaConsole.setLineWrap(true);
	textAreaConsole.setWrapStyleWord(true);
	textAreaConsole.setFont(new Font("Tahoma", Font.PLAIN, 16));
	scrollPane.setViewportView(textAreaConsole);
	PrintStream printStream = new PrintStream(CustomOutputStream.addTextAreaAndGetCustomOutputStream(textAreaConsole));
	System.setOut(printStream);
	System.setErr(printStream);
	new Thread(new Runnable()
	{
	    @Override
	    public void run()
	    {
		if (argumentsForJava != null)
		{
		    try
		    {
			Socket socket = new Socket(argumentsForJava[0], Integer.parseInt(argumentsForJava[1]));
			GeneralLogger.log("Socket connected!");
			new SocketReader(socket).start();
		    }
		    catch (IOException e)
		    {
			GeneralLogger.log("Error: Connection to Socket failed!");
			GeneralLogger.logError(e);
			System.exit(0);
		    }
		}
		else
		{
		    GeneralLogger.log("Error: Connection to Socket failed!");
		    System.exit(0);
		}
	    }
	}).start();
    }

}
