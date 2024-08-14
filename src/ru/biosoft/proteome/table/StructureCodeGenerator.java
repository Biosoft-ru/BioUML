package ru.biosoft.proteome.table;

import static ru.biosoft.util.j2html.TagCreator.div;
import static ru.biosoft.util.j2html.TagCreator.span;

import ru.biosoft.util.ControlCodeGenerator;
import ru.biosoft.util.j2html.tags.Tag;

public class StructureCodeGenerator implements ControlCodeGenerator
{
    @Override
    public Tag<?> getControlCode(Object value) throws Exception
    {
        StringBuilder title = new StringBuilder();
        Structure3D structure = (Structure3D)value;
        for( int i = 0; i < structure.getSize(); i++ )
        {
            if( i > 0 )
                title.append( ',' );
            title.append( structure.getLink( i ).getFirst() );
        }
        //TODO: support of several links for web
        return div().with( span().withText( title.toString() ),
                div().withClass( "structureDetails" ).withText( structure.getLink( 0 ).getSecond() ) );
    }
    @Override
    public Class<?> getSupportedItemType()
    {
        return Structure3D.class;
    }

}
