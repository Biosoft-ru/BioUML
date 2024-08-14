package com.developmentontheedge.log;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

// TextPaneAppender

@SuppressWarnings ( "serial" )
public class LogTextPanel extends JPanel
{
   protected JTextArea textArea = null;

    public LogTextPanel()
    {
        this(null);
    }

    public LogTextPanel(String titledBorderName)
    {
        super(new BorderLayout());
        if(titledBorderName != null)
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
        //textArea.setBackground(null);
        //constructComponents();
        //createDefaultFontAttributes();
    }

    public void setText(String text)
    {
        textArea.setText(text);
    }

/*
    private void constructComponents()
    {
        // setup the panel's additional components...
        this.setLayout(new BorderLayout());

        cbxTail = new JCheckBox();
        cbxTail.setSelected(true);
        cbxTail.setText("Tail log events");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(cbxTail, null);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setText("");
        doc = textPane.getStyledDocument();

        scrollBar = new JScrollBar(JScrollBar.VERTICAL);

        this.add(bottomPanel, BorderLayout.SOUTH);
        this.add(scrollBar, BorderLayout.EAST);
        this.add(textPane, BorderLayout.CENTER);
    }

    public void setTextBackground(Color color)
    {
        textPane.setBackground(color);
    }

    public void setTextBackground(String v)
    {
        textPane.setBackground(parseColor(v));
    }

    private void createDefaultFontAttributes()
    {
        Priority[] prio = Priority.getAllPossiblePriorities();

        fontAttributes = new Hashtable();
        for(int i=0; i<prio.length;i++)
        {
            MutableAttributeSet att = new SimpleAttributeSet();
            fontAttributes.put(prio[i], att);
            //StyleConstants.setFontSize(att,11);
        }

        setTextColor(Priority.FATAL, Color.red);
        setTextColor(Priority.ERROR, Color.magenta);
        setTextColor(Priority.WARN, Color.orange);
        setTextColor(Priority.INFO, Color.blue);
        setTextColor(Priority.DEBUG, Color.black);
    }

    private Color parseColor (String v)
    {
        StringTokenizer st = new StringTokenizer(v,",");
        int val[] = {255,255,255,255};
        int i=0;
        while(st.hasMoreTokens())
        {
            val[i]=Integer.parseInt(st.nextToken());
            i++;
        }
        return new Color(val[0],val[1],val[2],val[3]);
    }

    void setTextColor(Priority p, String v)
    {
        StyleConstants.setForeground(
                                    (MutableAttributeSet)fontAttributes.get(p),parseColor(v));
    }

    void setTextColor(Priority p, Color c)
    {
        StyleConstants.setForeground(
                                    (MutableAttributeSet)fontAttributes.get(p),c);
    }

    void setTextFontSize(int size)
    {
        Enumeration e = fontAttributes.elements();
        while(e.hasMoreElements())
        {
            StyleConstants.setFontSize((MutableAttributeSet)e.nextElement(),size);
        }
        return;
    }

    void setTextFontName(String name)
    {
        Enumeration e = fontAttributes.elements();
        while(e.hasMoreElements())
        {
            StyleConstants.setFontFamily((MutableAttributeSet)e.nextElement(),name);
        }
        return;
    }

    void setEventBufferSize(int bufferSize)
    {
        eventBufferMaxSize = bufferSize;
    }
*/
    void newEvents(TextPaneAppender.EventBufferElement[] evts)
    {
        for( int i = 0; i < evts.length; i++ )
        {
            textArea.append( evts[i].text );
            textArea.setCaretPosition( textArea.getDocument().getLength() );
        }
    }
    /*
    int maxR = -1;
    
    void drawText()
    {
        if(maxR < 0)
            maxR =  textPane.getHeight() / textPane.getFontMetrics(textPane.getFont()).getHeight();
        try
        {
            doc.remove(0, doc.getLength());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    
        for(int i=eventViewIndex; (i < eventBuffer.size()) && (i < (eventViewIndex + maxR)); i++)
        {
            EventBufferElement evt = (EventBufferElement)eventBuffer.get(i);
    
            try
            {
                doc.insertString(doc.getLength(), evt.text, (MutableAttributeSet)fontAttributes.get(evt.prio));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    */
}