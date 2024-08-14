package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.standard.type.Gene;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.TrackUtils;

@ClassIcon("resources/gene-set-to-track.gif")
public class GeneSetToTrack extends ExtractPromoters<GeneSetToTrackParameters>
{
    public static final String GENE_NAME_PROPERTY = "gene";
    public static final String GENE_ID_PROPERTY = "id";
    
    public GeneSetToTrack(DataCollection<?> origin, String name)
    {
        super(origin, name, new GeneSetToTrackParameters());
    }
    
    @Override
    protected DataCollection<Gene> getEnsemblDataCollection()
    {
        return TrackUtils.getGenesCollection(parameters.getSpecies(), parameters.getDestPath());
    }
    
    @Override
    protected String getLabelProperty()
    {
        return GENE_ID_PROPERTY;
    }

    @Override
    protected void initPromoterFromEnsemblElement(Site promoter, DataElement ensemblElement)
    {
        DynamicPropertySet properties = promoter.getProperties();
        Gene gene = (Gene)ensemblElement;

        if(gene.getTitle() != null)
            properties.add(new DynamicProperty(GENE_NAME_PROPERTY, String.class, gene.getTitle()));

        properties.add(new DynamicProperty(GENE_ID_PROPERTY, String.class, gene.getName()));
    }
    
}
