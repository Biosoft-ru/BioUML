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
    private View3DOptions options;
    
    public RotateListener(View3DOptions options)
    {
        this.options = options;
    }
    @Override
    public void mouseDragged(MouseEvent e)
    {
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
        xStart = e.getX();
        yStart = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        xEnd = e.getX();
        yEnd = e.getY();
        if (dragged)
        {
            int xMove = (xStart - xEnd) / 10;
            int yMove = -(yStart - yEnd) / 10;
            options.setAngle(  options.getHead() - xMove, options.getPitch() - yMove );
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