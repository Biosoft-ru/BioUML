package biouml.plugins.biopax;

import javax.swing.Icon;

import biouml.standard.diagram.PathwayDiagramViewBuilder;

public class BioPAXDiagramViewBuilder extends PathwayDiagramViewBuilder
{
    @Override
    public Icon getIcon(Object type)
    {
        Icon icon = getIcon((Class<?>)type, getClass());
        if( icon == null )
        {
            icon = getIcon((Class<?>)type, PathwayDiagramViewBuilder.class);
        }
        return icon;
    }
}
