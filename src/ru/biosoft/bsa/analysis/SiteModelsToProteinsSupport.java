package ru.biosoft.bsa.analysis;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.TableConverterSupport;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.jobcontrol.StackProgressJobControl;

@ClassIcon("resources/matrices-to-molecules.gif")
public abstract class SiteModelsToProteinsSupport<T extends SiteModelsToProteinsParameters> extends TableConverterSupport<T>
{
    public SiteModelsToProteinsSupport(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }

    public static class Link
    {
        private final String id;
        private final ReferenceType type;

        public Link(String id, ReferenceType type)
        {
            this.id = id;
            this.type = type;
        }

        public String getId()
        {
            return id;
        }

        public ReferenceType getType()
        {
            return type;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( id, type );
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null || getClass() != obj.getClass() )
                return false;
            Link other = (Link)obj;
            return Objects.equals( id, other.id ) && Objects.equals( type, other.type );
        }
    }

    protected Map<String, Set<Link>> getFactors(final DataCollection<SiteModel> siteLibrary, String[] siteNames, final Species species)
    {
        return getFactors( siteLibrary, siteNames, species, jobControl, log );
    }
    public static Map<String, Set<Link>> getFactors(final DataCollection<SiteModel> siteLibrary, String[] siteNames, final Species species,
            StackProgressJobControl jobControl, Logger log)
    {
        final Map<String, Set<Link>> result = new HashMap<>();
        jobControl.forCollection(Arrays.asList(siteNames), siteModelName -> {
            try
            {
                SiteModel siteModel = siteLibrary.get(siteModelName);
                if(siteModel != null)
                {
                    Set<Link> factors = factors( species, siteModel )
                            .map( factor -> new Link(factor.getName(), factor.getType()) )
                            .toSet();
                    result.put(siteModelName, factors);
                }
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Error while reading site model " + siteModelName, e );
            }
            return true;
        });
        return result;
    }

    protected static StreamEx<TranscriptionFactor> factors(final Species species, SiteModel siteModel)
    {
        return StreamEx.of( siteModel.getBindingElement().getFactors() )
                .filter(
                        factor -> factor.getSpeciesName() == null || species.getLatinName().equals( Species.ANY_SPECIES )
                                || factor.getSpeciesName().equals( Species.ANY_SPECIES )
                                || factor.getSpeciesName().equals( species.getLatinName() ) );
    }

    protected Map<String, String[]> getMolecules(Map<String, Set<Link>> factors, final ReferenceType targetType, Species species)
    {
        return getMolecules( factors, targetType, species, jobControl );
    }
    public static Map<String, String[]> getMolecules(Map<String, Set<Link>> factors, final ReferenceType targetType, Species species,
            AnalysisJobControl jobControl)
    {
        Properties outputProperties = BioHubSupport.createProperties( species, targetType );
        Map<String, String[]> result = new HashMap<>();
        
        Map<ReferenceType, Set<Link>> linksByType = new HashMap<>();
        for(Set<Link> links : factors.values())
            for(Link link : links)
                linksByType
                    .computeIfAbsent( link.getType(), k->new HashSet<>() )
                    .add( link );
        
        Map<Link, String[]> link2target = new HashMap<>();
        linksByType.forEach( (type, linksSameType) -> {
            String[] linkIds = StreamEx.of( linksSameType ).map( l->l.getId() ).distinct().toArray( String[]::new );
            Properties inputProperties = BioHubSupport.createProperties( species, type );
            Map<String, String[]> references = BioHubRegistry.getReferences( linkIds, inputProperties, outputProperties, null );
            references.forEach( (linkId, targetId) ->{
                link2target.put( new Link( linkId, type ), targetId );
            } );
        } );
        
        factors.forEach( (inputId, links)->{
            Set<String> outputIds = new HashSet<>();
            for(Link link : links)
                for( String outputId : link2target.getOrDefault( link, new String[0] ) )
                    outputIds.add( outputId );
            result.put( inputId, outputIds.toArray( new String[0] ) );
        } );
        
        return result;
    }

    protected Collection<String> getModels() throws Exception
    {
        return getParameters().getSitesCollection().getDataCollection().getNameList();
    }

    @Override
    protected String getSourceColumnName()
    {
        return getSourceColumnNameStatic();
    }

    public static String getSourceColumnNameStatic()
    {
        return "Site model ID";
    }
}
