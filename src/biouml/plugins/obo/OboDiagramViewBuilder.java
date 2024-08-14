package biouml.plugins.obo;

import javax.swing.Icon;

import biouml.standard.diagram.PathwayDiagramViewBuilder;

public class OboDiagramViewBuilder extends PathwayDiagramViewBuilder
{
    public Icon getIcon(Class<?> type)
    {
        Icon icon = getIcon(type, getClass());
        if( icon == null )
        {
            icon = getIcon(type, PathwayDiagramViewBuilder.class);
        }
        return icon;
    }

    @Override
    protected boolean showRelationTitle(String titleString)
    {
        //relation titles are forbidden on OBO diagrams for now
        return false;
    }
}
