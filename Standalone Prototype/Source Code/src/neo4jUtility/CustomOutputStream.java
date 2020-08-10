package neo4jUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JTextArea;

/**
 * This class is used to print information and errors of the application on the GUI Console. Directs a PrintStream to a textArea.
 * 
 * @author Kevin Bartik
 *
 */
public final class CustomOutputStream extends OutputStream
{

    /**
     * The textArea where the Output is directed.
     */
    private ArrayList<JTextArea> textAreaList;

    private static CustomOutputStream outputStream;

    /**
     * Constructor of the CustomOutputStream.
     * 
     * @param textArea
     *            The textArea where the Output is directed.
     */
    private CustomOutputStream(JTextArea textArea)
    {
	textAreaList = new ArrayList<JTextArea>();
	textAreaList.add(textArea);
    }

    public static CustomOutputStream addTextAreaAndGetCustomOutputStream(JTextArea _textArea)
    {
	if (outputStream == null)
	{
	    outputStream = new CustomOutputStream(_textArea);
	}
	else
	{
	    outputStream.textAreaList.add(_textArea);
	}
	return outputStream;
    }

    @Override
    public void write(int b) throws IOException
    {
	Iterator<JTextArea> iterator = textAreaList.iterator();
	while (iterator.hasNext())
	{
	    JTextArea textArea = iterator.next();
	    if (textArea != null)
	    {
		textArea.append(String.valueOf((char) b));
		textArea.setCaretPosition(textArea.getDocument().getLength());
	    }
	    else
	    {
		iterator.remove();
	    }
	}
    }

}
