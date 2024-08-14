package ru.biosoft.plugins.javascript.document;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditListener;

import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.plugins.javascript.StopAction;
import ru.biosoft.workbench.documents.FileTextPane;

import com.developmentontheedge.application.Application;

public class JSPanel extends JPanel implements ActionListener, GuiCallback
{

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -6212382604952082370L;

    /**
     * The debugger.
     */
    Dim dim;

    /**
     * The SourceInfo object that describes the file.
     */
    private Dim.SourceInfo sourceInfo;

    /**
     * The FileTextArea that displays the file.
     */
    FileTextPane textArea;

    /**
     * The FileHeader that is the gutter for {@link #textArea}.
     */
    private final FileHeader fileHeader;

    /**
     * Scroll pane for containing {@link #textArea}.
     */
    private final JScrollPane p;

    /**
     * The current offset position.
     */
    int currentPos;

    /**
     * Returns the offset position for the given line.
     */
    public int getPosition(int line)
    {
        int result = textArea.getLineStartOffset(line);
        return result;
    }

    /**
     * Add undo/redo manager
     */
    public void addUndoableEditListener(UndoableEditListener listener)
    {
        textArea.getDocument().addUndoableEditListener(listener);
    }

    /**
     * Remove undo/redo manager
     */
    public void removeUndoableEditListener(UndoableEditListener listener)
    {
        textArea.getDocument().removeUndoableEditListener(listener);
    }

    /**
     * Return text
     */
    public String getText(boolean onlySelected)
    {
        if( onlySelected )
        {
            return textArea.getSelectedText();
        }
        else
        {
            return textArea.getText();
        }
    }

    /**
     * Get debugger
     */
    public Dim getDim()
    {
        return dim;
    }

    /**
     * Change listeners
     */
    protected List<ChangeListener> changeListeners;

    /**
     * Returns whether the given line has a breakpoint.
     */
    public boolean isBreakPoint(int line)
    {
        return sourceInfo != null && sourceInfo.breakableLine(line) && sourceInfo.breakpoint(line);
    }

    /**
     * Toggles the breakpoint on the given line.
     */
    public void toggleBreakPoint(int line)
    {
        if( !isBreakPoint(line) )
        {
            setBreakPoint(line);
        }
        else
        {
            clearBreakPoint(line);
        }
    }

    /**
     * Sets a breakpoint on the given line.
     */
    public void setBreakPoint(int line)
    {
        if( sourceInfo.breakableLine(line) )
        {
            boolean changed = sourceInfo.breakpoint(line, true);
            if( changed )
            {
                fileHeader.repaint();
            }
        }
    }

    /**
     * Clears a breakpoint from the given line.
     */
    public void clearBreakPoint(int line)
    {
        if( sourceInfo.breakableLine(line) )
        {
            boolean changed = sourceInfo.breakpoint(line, false);
            if( changed )
            {
                fileHeader.repaint();
            }
        }
    }

    /**
     * Creates a new FileWindow.
     */
    public JSPanel(Dim.SourceInfo sourceInfo, Dim dim, String text)
    {
        super(new BorderLayout());
        this.sourceInfo = sourceInfo;
        this.dim = dim;
        currentPos = -1;

        textArea = new FileTextPane(ScriptTypeRegistry.getScriptTypes().get("js").getHighlightedDocument());
        p = new JScrollPane();
        fileHeader = new FileHeader(this);
        p.setViewportView(textArea);
        p.setRowHeaderView(fileHeader);
        add(p, BorderLayout.CENTER);
        updateText(sourceInfo, text);
        textArea.select(0);
        dim.setGuiCallback(this);
        changeListeners = new ArrayList<>();
    }
    /**
     * Called when the text of the script has changed.
    */
    public void updateText(Dim.SourceInfo sourceInfo, String text)
    {
        this.sourceInfo = sourceInfo;
        if( textArea != null && !textArea.getText().equals(text) )
        {
            textArea.setText(text);
            int pos = 0;
            if( currentPos != -1 )
            {
                pos = currentPos;
            }
            textArea.select(pos);
        }
        fileHeader.update();
        fileHeader.repaint();
    }

    /**
     * Sets the cursor position.
     */
    public void setPosition(int pos)
    {
        textArea.select(pos);
        currentPos = pos;
        fileHeader.repaint();
    }

    public boolean executionStarted()
    {
        return ( currentPos != -1 );
    }

    /**
     * Selects a range of characters.
     */
    public void select(int start, int end)
    {
        int docEnd = textArea.getDocument().getLength();
        textArea.select(docEnd, docEnd);
        textArea.select(start, end);
    }

    // GuiCallback

    /**
     * Called when the source text for a script has been updated.
     */
    @Override
    public void updateSourceText(Dim.SourceInfo sourceInfo)
    {
        RunProxy proxy = new RunProxy(this, RunProxy.UPDATE_SOURCE_TEXT);
        proxy.sourceInfo = sourceInfo;
        SwingUtilities.invokeLater(proxy);
    }

    public void addChangeListener(ChangeListener changeListener)
    {
        if( !changeListeners.contains(changeListener) )
        {
            changeListeners.add(changeListener);
        }
    }

    public void stateChanged()
    {
        boolean started = executionStarted();
        Application.getActionManager().enableActions( started, StopAction.KEY );
        for( ChangeListener changeListener : changeListeners )
        {
            changeListener.stateChanged();
        }
    }

    /**
     * Called when the interrupt loop has been entered.
     */
    @Override
    public void enterInterrupt(Dim.StackFrame lastFrame, String threadTitle, String alertMessage)
    {
        if( SwingUtilities.isEventDispatchThread() )
        {
            enterInterruptImpl(lastFrame, threadTitle, alertMessage);
        }
        else
        {
            RunProxy proxy = new RunProxy(this, RunProxy.ENTER_INTERRUPT);
            proxy.lastFrame = lastFrame;
            proxy.threadTitle = threadTitle;
            proxy.alertMessage = alertMessage;
            SwingUtilities.invokeLater(proxy);
        }
    }

    /**
     * Returns whether the current thread is the GUI event thread.
     */
    @Override
    public boolean isGuiEventThread()
    {
        return SwingUtilities.isEventDispatchThread();
    }

    /**
     * The AWT EventQueue.  Used for manually pumping AWT events from
     * {@link #dispatchNextGuiEvent()}.
     */
    private EventQueue awtEventQueue;

    /**
     * Processes the next GUI event.
     */
    @Override
    public void dispatchNextGuiEvent() throws InterruptedException
    {
        EventQueue queue = awtEventQueue;
        if( queue == null )
        {
            queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
            awtEventQueue = queue;
        }
        AWTEvent event = queue.getNextEvent();
        if( event instanceof ActiveEvent )
        {
            ( (ActiveEvent)event ).dispatch();
        }
        else
        {
            Object source = event.getSource();
            if( source instanceof Component )
            {
                Component comp = (Component)source;
                comp.dispatchEvent(event);
            }
        }
    }

    /**
     * Handles script interruption.
     */
    void enterInterruptImpl(Dim.StackFrame lastFrame, String threadTitle, String alertMessage)
    {
        int lineNumber = lastFrame.getLineNumber();
        setFilePosition(lineNumber);
        stateChanged();

        if( alertMessage != null )
        {
            MessageDialogWrapper.showMessageDialog(this, alertMessage, "Exception in Script", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Moves the current position in the given {@link FileWindow} to the
     * given line.
     */
    private void setFilePosition(int line)
    {
        boolean activate = true;
        FileTextPane ta = textArea;
        if( line == -1 )
        {
            setPosition( -1);
        }
        else
        {
            int loc = ta.getLineStartOffset(line - 1);
            setPosition(loc);
        }
    }

    // ActionListener

    /**
     * Performs an action.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if( cmd.equals("Cut") )
        {
            // textArea.cut();
        }
        else if( cmd.equals("Copy") )
        {
            textArea.copy();
        }
        else if( cmd.equals("Paste") )
        {
            // textArea.paste();
        }
    }

    /**
     * Gutter for FileWindows.
     */
    static class FileHeader extends JPanel implements MouseListener
    {

        /**
         * Serializable magic number.
         */
        private static final long serialVersionUID = -2858905404778259127L;

        /**
         * The line that the mouse was pressed on.
         */
        private int pressLine = -1;

        /**
         * The owning FileWindow.
         */
        private final JSPanel jsPanel;

        /**
         * Creates a new FileHeader.
         */
        public FileHeader(JSPanel jsPanel)
        {
            this.jsPanel = jsPanel;
            addMouseListener(this);
            update();
        }

        /**
         * Updates the gutter.
         */
        public void update()
        {
            FileTextPane textArea = jsPanel.textArea;
            Font font = textArea.getFont();
            setFont(font);
            FontMetrics metrics = getFontMetrics(font);
            int h = metrics.getHeight();
            int lineCount = textArea.getLineCount() + 1;
            String dummy = Integer.toString(lineCount);
            if( dummy.length() < 2 )
            {
                dummy = "99";
            }
            Dimension d = new Dimension();
            d.width = metrics.stringWidth(dummy) + 16;
            d.height = lineCount * h + 100;
            setPreferredSize(d);
            setSize(d);
        }

        /**
         * Paints the component.
         */
        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            FileTextPane textArea = jsPanel.textArea;
            Font font = textArea.getFont();
            g.setFont(font);
            FontMetrics metrics = getFontMetrics(font);
            Rectangle clip = g.getClipBounds();
            g.setColor(getBackground());
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
            int ascent = metrics.getMaxAscent();
            int h = metrics.getHeight();
            int lineCount = textArea.getLineCount() + 1;
            int startLine = clip.y / h;
            int endLine = ( clip.y + clip.height ) / h + 1;
            int width = getWidth();
            if( endLine > lineCount )
                endLine = lineCount;
            for( int i = startLine; i < endLine; i++ )
            {
                String text;
                int pos = textArea.getLineStartOffset(i);
                boolean isBreakPoint = jsPanel.isBreakPoint(i + 1);
                text = Integer.toString(i + 1) + " ";
                int y = i * h;
                g.setColor(Color.blue);
                g.drawString(text, 0, y + ascent);
                int x = width - ascent;
                if( isBreakPoint )
                {
                    g.setColor(new Color(0x80, 0x00, 0x00));
                    int dy = y + ascent - 9;
                    g.fillOval(x, dy, 9, 9);
                    g.drawOval(x, dy, 8, 8);
                    g.drawOval(x, dy, 9, 9);
                }
                if( pos == jsPanel.currentPos )
                {
                    Polygon arrow = new Polygon();
                    int dx = x;
                    y += ascent - 10;
                    int dy = y;
                    arrow.addPoint(dx, dy + 3);
                    arrow.addPoint(dx + 5, dy + 3);
                    for( x = dx + 5; x <= dx + 10; x++, y++ )
                    {
                        arrow.addPoint(x, y);
                    }
                    for( x = dx + 9; x >= dx + 5; x--, y++ )
                    {
                        arrow.addPoint(x, y);
                    }
                    arrow.addPoint(dx + 5, dy + 7);
                    arrow.addPoint(dx, dy + 7);
                    g.setColor(Color.yellow);
                    g.fillPolygon(arrow);
                    g.setColor(Color.black);
                    g.drawPolygon(arrow);
                }
            }
        }

        // MouseListener

        /**
         * Called when the mouse enters the component.
         */
        @Override
        public void mouseEntered(MouseEvent e)
        {
        }

        /**
         * Called when a mouse button is pressed.
         */
        @Override
        public void mousePressed(MouseEvent e)
        {
            Font font = jsPanel.textArea.getFont();
            FontMetrics metrics = getFontMetrics(font);
            int h = metrics.getHeight();
            pressLine = e.getY() / h;
        }

        /**
         * Called when the mouse is clicked.
         */
        @Override
        public void mouseClicked(MouseEvent e)
        {
        }

        /**
         * Called when the mouse exits the component.
         */
        @Override
        public void mouseExited(MouseEvent e)
        {
        }

        /**
         * Called when a mouse button is released.
         */
        @Override
        public void mouseReleased(MouseEvent e)
        {
            if( e.getComponent() == this && ( e.getModifiers() & MouseEvent.BUTTON1_MASK ) != 0 )
            {
                int y = e.getY();
                Font font = jsPanel.textArea.getFont();
                FontMetrics metrics = getFontMetrics(font);
                int h = metrics.getHeight();
                int line = y / h;
                if( line == pressLine )
                {
                    jsPanel.toggleBreakPoint(line + 1);
                }
                else
                {
                    pressLine = -1;
                }
            }
        }
    }
}

/**
 * Class to consolidate all cases that require to implement Runnable
 * to avoid class generation bloat.
 */
class RunProxy implements Runnable
{

    // Constants for 'type'.
    static final int OPEN_FILE = 1;
    static final int LOAD_FILE = 2;
    static final int UPDATE_SOURCE_TEXT = 3;
    static final int ENTER_INTERRUPT = 4;

    /**
     * The debugger GUI.
     */
    private final JSPanel jsPanel;

    /**
     * The type of Runnable this object is.  Takes one of the constants
     * defined in this class.
     */
    private final int type;

    /**
     * The name of the file to open or load.
     */
    String fileName;

    /**
     * The source text to update.
     */
    String text;

    /**
     * The source for which to update the text.
     */
    Dim.SourceInfo sourceInfo;

    /**
     * The frame to interrupt in.
     */
    Dim.StackFrame lastFrame;

    /**
     * The name of the interrupted thread.
     */
    String threadTitle;

    /**
     * The message of the exception thrown that caused the thread
     * interruption, if any.
     */
    String alertMessage;

    /**
     * Creates a new RunProxy.
     */
    public RunProxy(JSPanel jsPanel, int type)
    {
        this.jsPanel = jsPanel;
        this.type = type;
    }

    /**
     * Runs this Runnable.
     */
    @Override
    public void run()
    {
        switch( type )
        {
            case OPEN_FILE:
                try
                {
                    jsPanel.dim.compileScript(fileName, text);
                }
                catch( RuntimeException ex )
                {
                    MessageDialogWrapper.showMessageDialog(jsPanel, ex.getMessage(), "Error Compiling " + fileName,
                            JOptionPane.ERROR_MESSAGE);
                }
                break;

            case LOAD_FILE:
                try
                {
                    jsPanel.dim.evalScript(fileName, text, null);
                }
                catch( RuntimeException ex )
                {
                    MessageDialogWrapper
                            .showMessageDialog(jsPanel, ex.getMessage(), "Run error for " + fileName, JOptionPane.ERROR_MESSAGE);
                }
                break;

            case ENTER_INTERRUPT:
                jsPanel.enterInterruptImpl(lastFrame, threadTitle, alertMessage);
                break;

            default:
                throw new IllegalArgumentException(String.valueOf(type));

        }
    }
}

/**
 * Helper class for showing a message dialog.
 */
class MessageDialogWrapper
{

    /**
     * Shows a message dialog, wrapping the <code>msg</code> at 60
     * columns.
     */
    public static void showMessageDialog(Component parent, String msg, String title, int flags)
    {
        if( msg.length() > 60 )
        {
            StringBuffer buf = new StringBuffer();
            int len = msg.length();
            int j = 0;
            int i;
            for( i = 0; i < len; i++, j++ )
            {
                char c = msg.charAt(i);
                buf.append(c);
                if( Character.isWhitespace(c) )
                {
                    int k;
                    for( k = i + 1; k < len; k++ )
                    {
                        if( Character.isWhitespace(msg.charAt(k)) )
                        {
                            break;
                        }
                    }
                    if( k < len )
                    {
                        int nextWordLen = k - i;
                        if( j + nextWordLen > 60 )
                        {
                            buf.append('\n');
                            j = 0;
                        }
                    }
                }
            }
            msg = buf.toString();
        }
        JOptionPane.showMessageDialog(parent, msg, title, flags);
    }
}
