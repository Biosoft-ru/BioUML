package biouml.plugins.physicell.javacode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.UndoableEditListener;

import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.workbench.documents.FileTextPane;


public class JavaPanel extends JPanel implements ActionListener
{

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -6212382604952082370L;

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
        int result = textArea.getLineStartOffset( line );
        return result;
    }

    /**
     * Add undo/redo manager
     */
    public void addUndoableEditListener(UndoableEditListener listener)
    {
        textArea.getDocument().addUndoableEditListener( listener );
    }

    /**
     * Remove undo/redo manager
     */
    public void removeUndoableEditListener(UndoableEditListener listener)
    {
        textArea.getDocument().removeUndoableEditListener( listener );
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
     * Creates a new FileWindow.
     */
    public JavaPanel(String text)
    {
        super( new BorderLayout() );
        currentPos = -1;

        textArea = new FileTextPane( ScriptTypeRegistry.getScriptTypes().get( "Java" ).getHighlightedDocument() );
        p = new JScrollPane();
        fileHeader = new FileHeader( this );
        p.setViewportView( textArea );
        p.setRowHeaderView( fileHeader );
        add( p, BorderLayout.CENTER );
        updateText( text );
        textArea.select( 0 );
    }
    /**
     * Called when the text of the script has changed.
    */
    public void updateText(String text)
    {
        if( textArea != null && !textArea.getText().equals( text ) )
        {
            textArea.setText( text );
            int pos = 0;
            if( currentPos != -1 )
            {
                pos = currentPos;
            }
            textArea.select( pos );
        }
        fileHeader.update();
        fileHeader.repaint();
    }

    /**
     * Sets the cursor position.
     */
    public void setPosition(int pos)
    {
        textArea.select( pos );
        currentPos = pos;
        fileHeader.repaint();
    }

    /**
     * Selects a range of characters.
     */
    public void select(int start, int end)
    {
        int docEnd = textArea.getDocument().getLength();
        textArea.select( docEnd, docEnd );
        textArea.select( start, end );
    }

    /**
     * Performs an action.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if( cmd.equals( "Cut" ) )
        {
            // textArea.cut();
        }
        else if( cmd.equals( "Copy" ) )
        {
            textArea.copy();
        }
        else if( cmd.equals( "Paste" ) )
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
         * The owning FileWindow.
         */
        private final JavaPanel jsPanel;

        /**
         * Creates a new FileHeader.
         */
        public FileHeader(JavaPanel jsPanel)
        {
            this.jsPanel = jsPanel;
            addMouseListener( this );
            update();
        }

        /**
         * Updates the gutter.
         */
        public void update()
        {
            FileTextPane textArea = jsPanel.textArea;
            Font font = textArea.getFont();
            setFont( font );
            FontMetrics metrics = getFontMetrics( font );
            int h = metrics.getHeight();
            int lineCount = textArea.getLineCount() + 1;
            String dummy = Integer.toString( lineCount );
            if( dummy.length() < 2 )
            {
                dummy = "99";
            }
            Dimension d = new Dimension();
            d.width = metrics.stringWidth( dummy ) + 16;
            d.height = lineCount * h + 100;
            setPreferredSize( d );
            setSize( d );
        }

        /**
         * Paints the component.
         */
        @Override
        public void paint(Graphics g)
        {
            super.paint( g );
            FileTextPane textArea = jsPanel.textArea;
            Font font = textArea.getFont();
            g.setFont( font );
            FontMetrics metrics = getFontMetrics( font );
            Rectangle clip = g.getClipBounds();
            g.setColor( getBackground() );
            g.fillRect( clip.x, clip.y, clip.width, clip.height );
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
                int pos = textArea.getLineStartOffset( i );
                text = Integer.toString( i + 1 ) + " ";
                int y = i * h;
                g.setColor( Color.blue );
                g.drawString( text, 0, y + ascent );
                int x = width - ascent;

                if( pos == jsPanel.currentPos )
                {
                    Polygon arrow = new Polygon();
                    int dx = x;
                    y += ascent - 10;
                    int dy = y;
                    arrow.addPoint( dx, dy + 3 );
                    arrow.addPoint( dx + 5, dy + 3 );
                    for( x = dx + 5; x <= dx + 10; x++, y++ )
                    {
                        arrow.addPoint( x, y );
                    }
                    for( x = dx + 9; x >= dx + 5; x--, y++ )
                    {
                        arrow.addPoint( x, y );
                    }
                    arrow.addPoint( dx + 5, dy + 7 );
                    arrow.addPoint( dx, dy + 7 );
                    g.setColor( Color.yellow );
                    g.fillPolygon( arrow );
                    g.setColor( Color.black );
                    g.drawPolygon( arrow );
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
                FontMetrics metrics = getFontMetrics( font );
                int h = metrics.getHeight();
                int line = y / h;
            }
        }
    }
}
