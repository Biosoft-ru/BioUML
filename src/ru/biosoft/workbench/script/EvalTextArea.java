package ru.biosoft.workbench.script;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import ru.biosoft.access.script.ScriptTypeRegistry.ScriptType;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.tasks.TaskInfo;

public class EvalTextArea extends JTextPane implements KeyListener, DocumentListener
{
    private final List<String> history;
    private int historyIndex = -1;
    private int outputMark = 0;
    private ScriptType type;

    public EvalTextArea()
    {
        super(new DefaultStyledDocument());

        history = new ArrayList<>();
        Document doc = getDocument();
        doc.addDocumentListener(this);
        addKeyListener(this);
        setFont(new Font("Monospaced", 0, 12));
    }
    
    public void setScriptType(ScriptType type)
    {
        if(type != this.type)
        {
            this.type = type;
            if(getDocument().getLength()>0) append("\n", null);
            displayPrompt();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // KeyListener interface implimentation
    //

    @Override
    public void keyPressed(KeyEvent e)
    {
        int code = e.getKeyCode();
        if( code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_LEFT )
        {
            if( outputMark == getCaretPosition() )
            {
                e.consume();
            }
        }
        else if( code == KeyEvent.VK_HOME )
        {
            int caretPos = getCaretPosition();
            if( caretPos == outputMark )
            {
                e.consume();
            }
            else if( caretPos > outputMark )
            {
                if( !e.isControlDown() )
                {
                    if( e.isShiftDown() )
                    {
                        moveCaretPosition(outputMark);
                    }
                    else
                    {
                        setCaretPosition(outputMark);
                    }
                    e.consume();
                }
            }
        }
        else if( code == KeyEvent.VK_ENTER )
        {
            returnPressed();
            e.consume();
        }
        else if( code == KeyEvent.VK_UP )
        {
            historyIndex--;
            if( historyIndex >= 0 )
            {
                if( historyIndex >= history.size() )
                {
                    historyIndex = history.size() - 1;
                }
                if( historyIndex >= 0 )
                {
                    String str = history.get(historyIndex);
                    int len = getDocument().getLength();
                    replace(str, outputMark, len, getCommandStyle());
                    int caretPos = outputMark + str.length();
                    select(caretPos, caretPos);
                }
                else
                {
                    historyIndex++;
                }
            }
            else
            {
                historyIndex++;
            }
            e.consume();
        }
        else if( code == KeyEvent.VK_DOWN )
        {
            int caretPos = outputMark;
            if( history.size() > 0 )
            {
                historyIndex++;
                if( historyIndex < 0 )
                {
                    historyIndex = 0;
                }
                int len = getDocument().getLength();
                if( historyIndex < history.size() )
                {
                    String str = history.get(historyIndex);
                    replace(str, outputMark, len, getCommandStyle());
                    caretPos = outputMark + str.length();
                }
                else
                {
                    historyIndex = history.size();
                    replace("", outputMark, len, getCommandStyle());
                }
            }
            select(caretPos, caretPos);
            e.consume();
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
        int keyChar = e.getKeyChar();
        if( keyChar == 0x8 /* KeyEvent.VK_BACK_SPACE */)
        {
            if( outputMark == getCaretPosition() )
            {
                e.consume();
            }
        }
        else if( getCaretPosition() < outputMark )
        {
            setCaretPosition(outputMark);
        }
    }

    @Override
    public synchronized void keyReleased(KeyEvent e)
    {
    }

    synchronized void returnPressed()
    {
        Runnable thread = new Runnable()
        {
            @Override
            public void run()
            {
                Document doc = EvalTextArea.this.getDocument();
                int len = doc.getLength();
                Segment segment = new Segment();
                try
                {
                    doc.getText(outputMark, len - outputMark, segment);
                }
                catch( javax.swing.text.BadLocationException ignored )
                {
                    ignored.printStackTrace();
                }

                String text = segment.toString();

                replace(text, outputMark, len, getCommandStyle());

                if( text.isEmpty() )
                {
                    EvalTextArea.this.append("\n", null);
                    displayPrompt();
                }
                else
                {
                    if( text.trim().length() > 0 )
                    {
                        history.add(text);
                        historyIndex = history.size();
                    }
                    EvalTextArea.this.append("\n", null);

                    SwingScriptEnvironment env = new SwingScriptEnvironment(EvalTextArea.this);
                    String result = type.execute(text, env);
                    if( !env.hasData() && result != null && !result.isEmpty() )
                    {
                        EvalTextArea.this.append(result, null);
                        EvalTextArea.this.append("\n", null);
                    }

                    //write to journal if no errors
                    Journal journal = JournalRegistry.getCurrentJournal();
                    if( journal != null )
                    {
                        TaskInfo action = journal.getEmptyAction();
                        action.setType(TaskInfo.SCRIPT);
                        action.setData(text);
                        journal.addAction(action);
                    }

                    displayPrompt();
                    select(outputMark, outputMark);
                }
            }
        };
        new Thread(thread).start();
    }

    ////////////////////////////////////////////////////////////////////////////
    // DocumentListener implementation
    //

    @Override
    public synchronized void insertUpdate(DocumentEvent e)
    {
        int len = e.getLength();
        int off = e.getOffset();
        if( outputMark > off )
        {
            outputMark += len;
        }
    }

    @Override
    public synchronized void removeUpdate(DocumentEvent e)
    {
        int len = e.getLength();
        int off = e.getOffset();
        if( outputMark > off )
        {
            if( outputMark >= off + len )
            {
                outputMark -= len;
            }
            else
            {
                outputMark = off;
            }
        }
    }

    @Override
    public synchronized void changedUpdate(DocumentEvent e)
    {
    }

    protected void displayPrompt()
    {
        append(type.getType() + "> ", getSourceStyle());
        outputMark = getDocument().getLength();
    }

    protected void append(String str, SimpleAttributeSet attr)
    {
        try
        {
            Document doc = getDocument();
            doc.insertString(doc.getLength(), str, attr);
        }
        catch( BadLocationException e )
        {
        }
    }

    protected void replace(String str, int offset, int len, SimpleAttributeSet attr)
    {
        try
        {
            Document doc = getDocument();
            doc.remove(offset, len - offset);
            doc.insertString(offset, str, attr);
        }
        catch( BadLocationException e )
        {
        }
    }

    protected SimpleAttributeSet getSourceStyle()
    {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, Color.black);
        StyleConstants.setBold(attr, true);
        return attr;
    }

    protected SimpleAttributeSet getCommandStyle()
    {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, Color.blue);
        StyleConstants.setBold(attr, true);
        return attr;
    }
}
