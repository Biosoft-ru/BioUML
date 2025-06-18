package biouml.plugins.wdl.texteditor;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.Ostermiller.Syntax.HighlightedDocument;

import biouml.plugins.wdl.colorer.WDLColorer;


public class WDLPane extends JTextPane
{
    private UndoHandler undoHandler = new UndoHandler();
    private UndoManager undoManager = new UndoManager();
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();

    public WDLPane()
    {
        super();
        setEditorKit(new StyledEditorKit());
        HighlightedDocument document = new HighlightedDocument();
        document.setHighlightStyle(WDLColorer.class);
        this.setDocument(document);
        this.getDocument().addUndoableEditListener(undoHandler);
        KeyStroke undoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
        KeyStroke redoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK);

        getInputMap().put(undoKeystroke, "undoKeystroke");
        getActionMap().put("undoKeystroke", undoAction);

        getInputMap().put(redoKeystroke, "redoKeystroke");
        getActionMap().put("redoKeystroke", redoAction);
    }

    private class UndoHandler implements UndoableEditListener
    {

        @Override
        public void undoableEditHappened(UndoableEditEvent e)
        {
            undoManager.addEdit(e.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }

    private class UndoAction extends AbstractAction
    {
        public UndoAction()
        {
            super("Undo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                undoManager.undo();
            }
            catch( CannotUndoException ex )
            {
                // TODO deal with this
                ex.printStackTrace();
            }
            update();
            redoAction.update();
        }

        protected void update()
        {
            if( undoManager.canUndo() )
            {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getUndoPresentationName());
            }
            else
            {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    private class RedoAction extends AbstractAction
    {
        public RedoAction()
        {
            super("Redo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                undoManager.redo();
            }
            catch( CannotRedoException ex )
            {
                ex.printStackTrace();
            }
            update();
            undoAction.update();
        }

        protected void update()
        {
            if( undoManager.canRedo() )
            {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getRedoPresentationName());
            }
            else
            {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    /** Method to set text without notifying listeners */
    public void setTextSilent(String str)
    {
        getDocument().removeUndoableEditListener(undoHandler);
        setText(str);
        getDocument().addUndoableEditListener(undoHandler);
    }

    /**
     * Sets the position of the text insertion caret for the
     * <code>AntimonyEditorPane</code>.  The position
     * must be greater than 0 or else an exception is thrown. 
     * If the position is greater than the length of the component's text, 
     * it will be reset to the length of the document.
     */
    @Override
    public void setCaretPosition(int position)
    {
        Document doc = getDocument();
        if( doc != null )
        {
            if( position < 0 )
            {
                throw new IllegalArgumentException("bad position: " + position);
            }
            else if( position > doc.getLength() )
                position = doc.getLength();

            getCaret().setDot(position);
        }
    }

}
