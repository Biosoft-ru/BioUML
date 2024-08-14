package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

public class CubicVertexTypeAction extends VertexTypeAction
{
    public static final String KEY = "Cubic vertex type";
    
    @Override
    public void actionPerformed(ActionEvent evt)
    {
        super.actionPerformed(evt);
        setVertexType(2);
    }
}
