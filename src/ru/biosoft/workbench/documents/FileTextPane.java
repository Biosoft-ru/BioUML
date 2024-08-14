package ru.biosoft.workbench.documents;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationStatusBar;

/**
 * Text area to display script source.
 */
public class FileTextPane extends JTextPane implements MouseListener
{
    private static final long serialVersionUID = -25032065448563720L;

    public FileTextPane()
    {
        super();
        init();
    }

    public FileTextPane(StyledDocument doc)
    {
        super(doc);
        init();
    }
    
    private void init()
    {
        addMouseListener(this);
        setFont(new Font("Monospaced", 0, 12));
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent event)
            {
                Application.getActionManager().enableActions( true, UndoAction.KEY );
            }
        });
        addCaretListener(e -> {
            ApplicationStatusBar statusBar = Application.getApplicationFrame().getStatusBar();
            statusBar.setMessage("Line: " + ( getLineOfOffset(e.getDot()) + 1 ) + "/" + getLineCount() + " Col: "
                    + getColumnOfOffset(e.getDot()));
        });
    }
    public int getLineOfOffset(int pos)
    {
        return getDocument().getDefaultRootElement().getElementIndex(pos);
    }

    public int getLineStartOffset(int line)
    {
        javax.swing.text.Element lineElement = getDocument().getDefaultRootElement().getElement(line);
        if( lineElement == null )
            return -2;
        else
            return lineElement.getStartOffset();
    }

    public int getLineCount()
    {
        return getDocument().getDefaultRootElement().getElementCount();
    }

    protected int getColumnOfOffset(int pos)
    {
        int line = getLineOfOffset(pos);
        return pos - getLineStartOffset(line);
    }

    /**
     * Moves the selection to the given offset.
     */
    public void select(int pos)
    {
        if( pos >= 0 )
        {
            try
            {
                int line = getLineOfOffset(pos);
                Rectangle rect = modelToView(pos);
                if( rect == null )
                {
                    select(pos, pos);
                }
                else
                {
                    try
                    {
                        Rectangle nrect = modelToView(getLineStartOffset(line + 1));
                        if( nrect != null )
                        {
                            rect = nrect;
                        }
                    }
                    catch( Exception exc )
                    {
                    }
                    JViewport vp = (JViewport)getParent();
                    Rectangle viewRect = vp.getViewRect();
                    if( viewRect.y + viewRect.height > rect.y )
                    {
                        // need to scroll up
                        select(pos, pos);
                    }
                    else
                    {
                        // need to scroll down
                        rect.y += ( viewRect.height - rect.height ) / 2;
                        scrollRectToVisible(rect);
                        select(pos, pos);
                    }
                }
            }
            catch( BadLocationException exc )
            {
                select(pos, pos);
                //exc.printStackTrace();
            }
        }
    }

    // MouseListener

    /**
     * Called when a mouse button is pressed.
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    /**
     * Called when the mouse is clicked.
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        requestFocus();
        getCaret().setVisible(true);
    }

    /**
     * Called when the mouse enters the component.
     */
    @Override
    public void mouseEntered(MouseEvent e)
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
    }
}