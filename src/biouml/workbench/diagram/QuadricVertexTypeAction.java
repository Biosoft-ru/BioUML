package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

public class QuadricVertexTypeAction extends VertexTypeAction
{
    public static final String KEY = "Quadric vertex type";
    
    @Override
    public void actionPerformed(ActionEvent evt)
    {
        super.actionPerformed(evt);
        setVertexType(1);
    }
}