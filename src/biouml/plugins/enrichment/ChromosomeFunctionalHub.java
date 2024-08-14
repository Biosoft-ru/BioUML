package biouml.plugins.enrichment;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.standard.type.Gene;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WithSite;

public class ChromosomeFunctionalHub extends SqlCachedFunctionalHubSupport
{

    public ChromosomeFunctionalHub(Properties properties)
    {
        super( properties );
    }

    @Override
    protected Iterable<Group> getGroups() throws Exception
    {
        Map<String, Group> groups = new HashMap<>();
        
        DataElementPath ensemblPath = DataElementPath.create( properties.getProperty( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY ) );
        DataCollection<Gene> genes = TrackUtils.getGenesCollection(ensemblPath);
        
        for( Gene gene : genes )
        {
            if ( gene instanceof WithSite )
            {
                String chr = ((WithSite) gene).getSite().getOriginalSequence().getName();
                groups.computeIfAbsent(chr, Group::new).addElement(gene.getName());
            }
        }
        
        return groups.values();
    }

    @Override
    protected ReferenceType getInputReferenceType()
    {
        return ReferenceTypeRegistry.getReferenceType( EnsemblGeneTableType.class );
    }

    @Override
    protected String getTableName()
    {
        return "chromosome_classification";
    }
}
