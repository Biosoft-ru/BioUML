package biouml.plugins.enrichment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

public class FunctionalCategory extends DataElementSupport
{
    private String[][] elements;
    private final Set<String> fullSet = new HashSet<>();
    private String description = "";
    private final String originalLine;
    
    public FunctionalCategory(String name, DataCollection<?> origin, String line)
    {
        super(name, origin);
        originalLine = line;
        parse(line);
    }

    protected void parse(String line)
    {
        String[] fields = line.split("\t");
        if(fields.length>1)
            description = fields[1];
        if(fields.length<2)
            return;
        elements = StreamEx.of( fields ).skip( 2 )
                .map( field -> StreamEx.split( field, "///" )
                        .map( sf -> sf.replaceFirst( "^[\\s_]+", "" ).replaceFirst( "[\\s_]+$", "" ) )
                        .peek( sf -> fullSet.add( sf.toUpperCase() ) ).toArray( String[]::new ) )
                .toArray( String[][]::new );
    }

    public String getDescription()
    {
        return description;
    }
    
    public Integer getSize()
    {
        return elements.length;
    }
    
    public boolean contains(String element)
    {
        return fullSet.contains(element.toUpperCase());
    }
    
    public boolean containsAny(Collection<String> elements)
    {
        for(String element : elements)
        {
            if(fullSet.contains(element.toUpperCase()))
                return true;
        }
        return false;
    }
    
    public int getHits(Set<String> listSet)
    {
        Set<String> upperListSet = StreamEx.of(listSet).map( String::toUpperCase ).toSet();
        return (int)Stream.of(elements)
            .filter( arr -> Stream.of(arr).map( String::toUpperCase ).anyMatch( upperListSet::contains ) )
            .count();
    }

    protected String getOriginalLine()
    {
        return originalLine;
    }
    
    public static FunctionalCategory createJoinedCategory(DataCollection<FunctionalCategory> origin)
    {
        String allCategories = origin.stream().flatMap( c -> StreamEx.of( c.elements ) )
                .map( element -> StreamEx.of( element ).sorted().joining( " /// " ) ).distinct().collect( Collectors.joining( "\t" ) );
        return new FunctionalCategory( "_joined_", origin, "_joined_\t_joined_\t" + allCategories );
    }
}
