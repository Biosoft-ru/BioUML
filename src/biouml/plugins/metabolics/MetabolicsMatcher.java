package biouml.plugins.metabolics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisJobControl;
import biouml.model.Module;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class MetabolicsMatcher
{
    public static final String PRODUCT_FILTER = "products";
    public static final String REACTANT_FILTER = "reactants";
    public static final String DEFAULT_FILTER = "all";

    private final String roleFilter;
    private final Class<? extends DataElement> inputType;
    private final Class<? extends DataElement> outputType;

    private final AnalysisJobControl jobControl;

    public MetabolicsMatcher(String roleFilter, Class<? extends DataElement> inputType, Class<? extends DataElement> outputType,
            AnalysisJobControl jobControl)
    {
        this.roleFilter = roleFilter;
        this.inputType = inputType;
        this.outputType = outputType;
        this.jobControl = jobControl;
    }

    public Map<String, String[]> getReferences(String[] ids, Module module)
    {
        Map<String, Set<String>> result = new HashMap<>();
        DataCollection<Reaction> category = module.getCategory( Reaction.class );
        List<String> namesCopy = new ArrayList<>( category.getNameList() ); //dirty hack to avoid iterations over namelist in several threads
        jobControl.forCollection( namesCopy, rName -> {
            Reaction r;
            try
            {
                r = category.get( rName );
                fillRefs( ids, r, module, result );
            }
            catch( Exception e )
            {
            }

            return true;
        } );
        return EntryStream.of( result ).mapValues( set -> set.toArray( new String[0] ) ).toMap();
    }

    private void fillRefs(String[] ids, Reaction reaction, Module module, Map<String, Set<String>> result)
    {
        for( String id : ids )
        {
            if( !containsElement( id, reaction, module ) )
                continue;
            Set<String> newRefs = reaction.stream().filter( this::filter ).map( SpecieReference::getSpecie )
                    .map( s -> getFromCategory( s, module, outputType ) ).nonNull().map( ru.biosoft.access.core.DataElement::getName ).toSet();
            Set<String> refSet = result.get( id );
            if( refSet == null )
                result.put( id, newRefs );
            else
                refSet.addAll( newRefs );
        }
    }

    private boolean filter(SpecieReference sr)
    {
        switch( roleFilter )
        {
            case MetabolicsMatcher.REACTANT_FILTER:
                return sr.isReactant();
            case MetabolicsMatcher.PRODUCT_FILTER:
                return sr.isProduct();
            case MetabolicsMatcher.DEFAULT_FILTER:
            default:
                return true;
        }
    }

    private boolean containsElement(String id, Reaction reaction, Module module)
    {
        return reaction.stream().map( SpecieReference::getSpecie ).map( s -> getFromCategory( s, module, inputType ) ).nonNull()
                .anyMatch( p -> id.equals( p.getName() ) );
    }

    private @CheckForNull <T extends DataElement> T getFromCategory(String species, Module module, Class<? extends T> categoryType)
    {
        try
        {
            return module.getCategory( categoryType ).get( species.substring( species.lastIndexOf( '/' ) + 1 ) );
        }
        catch( Throwable t )
        {
            return null;
        }
    }
}
