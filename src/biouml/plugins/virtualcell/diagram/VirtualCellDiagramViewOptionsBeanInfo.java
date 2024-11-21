package biouml.plugins.virtualcell.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VirtualCellDiagramViewOptionsBeanInfo extends BeanInfoEx2<VirtualCellDiagramViewOptions>
{
    public VirtualCellDiagramViewOptionsBeanInfo()
    {
        super( VirtualCellDiagramViewOptions.class );
    }

    @Override
    public void initProperties()
    {
        add( "processBrush" );
        add( "datasetBrush" );
    }
}