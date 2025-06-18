package biouml.plugins.wdl.texteditor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

@SuppressWarnings ( "serial" )
public class LogPanel extends JPanel
{
    protected JTextArea textArea = null;

    public LogPanel()
    {
        this(null);
    }

    public LogPanel(String titledBorderName)
    {
        super(new BorderLayout());
        if( titledBorderName != null )
        {
            TitledBorder titledBorder = new TitledBorder(titledBorderName);
            setBorder(titledBorder);
        }
        textArea = new JTextArea();
        textArea.setMargin(new Insets(5, 5, 5, 5));

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        Font font = textArea.getFont();
        Font newFont = new Font("Monospaced", font.getStyle(), font.getSize());
        textArea.setFont(newFont);
        textArea.setEditable(false);
    }

    public void setText(String text)
    {
        textArea.setText(text);
    }

    void newEvents(TextPaneAppender.EventBufferElement[] evts)
    {
        for( int i = 0; i < evts.length; i++ )
        {
            textArea.append(evts[i].text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
