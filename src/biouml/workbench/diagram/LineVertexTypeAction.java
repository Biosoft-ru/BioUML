package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

public class LineVertexTypeAction extends VertexTypeAction
{
    public static final String KEY = "Line vertex type";
    
    @Override
    public void actionPerformed(ActionEvent evt)
    {
        super.actionPerformed(evt);
        setVertexType(0);
    }
}
