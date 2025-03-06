package biouml.plugins.physicell.document;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class RotateListener implements MouseListener, MouseMotionListener
{
    private boolean dragged = false;
    private int xStart;
    private int xEnd;
    private int yStart;
    private int yEnd;
    private ViewOptions options;

    public RotateListener(ViewOptions options)
    {
        this.options = options;
    }
    @Override
    public void mouseDragged(MouseEvent e)
    {
        if( options.is2D() )
            return;
        dragged = true;
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if( options.is2D() )
            return;
        xStart = e.getX();
        yStart = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if( options.is2D() )
            return;
        xEnd = e.getX();
        yEnd = e.getY();
        if( dragged )
        {
            int xMove = ( xStart - xEnd ) / 10;
            int yMove = - ( yStart - yEnd ) / 10;
            options.getOptions3D().setAngle( options.getOptions3D().getHead() - xMove, options.getOptions3D().getPitch() - yMove );
        }
        dragged = false;
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }
}