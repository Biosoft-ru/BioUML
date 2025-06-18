package ru.biosoft.access.biohub;

import static ru.biosoft.util.j2html.TagCreator.b;
import static ru.biosoft.util.j2html.TagCreator.br;
import static ru.biosoft.util.j2html.TagCreator.div;
import static ru.biosoft.util.j2html.TagCreator.li;
import static ru.biosoft.util.j2html.TagCreator.ul;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.j2html.tags.ContainerTag;

//TODO: think about move to separate plugin
public class MatchingPathWriter
{
    public static final String MATCHING_PATH_PROP = "converter.matchingPath";

    public String getMatchingPath(DataCollection<?> dc)
    {
        if( dc == null )
            return "";
        String matchingPath = dc.getInfo().getProperty( MATCHING_PATH_PROP );
        if( matchingPath == null )
            return "";

        String[] result = (String[])TextUtil2.fromString( String[].class, matchingPath );
        if( result.length == 0 )
            return "";

        ContainerTag tag = div();
        tag = tag.with( b().withText( "Used BioHubs" ) ).withText( ": " ).with( br() );
        ContainerTag ul = ul();
        for( String step : result )
        {
            ul.with( li().withText( step ) );
        }

        return tag.with( ul ).render();
    }
}
