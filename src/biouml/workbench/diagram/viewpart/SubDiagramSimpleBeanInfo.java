package biouml.workbench.diagram.viewpart;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SubDiagramSimpleBeanInfo extends BeanInfoEx2<SubDiagramSimple>
{
    public SubDiagramSimpleBeanInfo()
    {
        super( SubDiagramSimple.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly("name");
        add( "title" );
        addReadOnly( "diagramPath" );
        addReadOnly( "state" );
        add( "comment" );
    }
}