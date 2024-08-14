package biouml.plugins.ensembl.biohub;

import java.util.Properties;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.GeneSymbolTableType;
import biouml.plugins.ensembl.tabletype.UniGeneTableType;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;


public class ExternalToEnsemblGeneHub extends ExternalToEnsemblHub
{
    static final ExternalDBRestriction  GENE_SYMBOL_RESTRICTIONS = new ExternalDBRestriction()
            .setSpeciesDBName( "Homo sapiens", "HGNC" )
            .setSpeciesDBName( "Mus musculus", "MGI" )
            .setSpeciesDBName( "Rattus norvegicus", "RGD" );
    
    private final TypeRecord[] supportedTypeRecords =
    {
            new TypeRecord("60-", EntrezGeneTableType.class, "EntrezGene", 0.9),
            new TypeRecord("*", GeneSymbolTableType.class, GENE_SYMBOL_RESTRICTIONS, 0.8, true, "^(LOC|loc)(\\d+)$/$2"),
            new TypeRecord("60-", UniGeneTableType.class, "UniGene"),
            //new TypeRecord(RefSeqGeneTableType.class, "RefSeq_genomic"),
    };
    
    
    
    private ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class);
    
    public ExternalToEnsemblGeneHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected ReferenceType getOutputType()
    {
        return outputType;
    }
    
    @Override
    protected String getQueryTemplate(String species, DataElementPath ensemblPath, TypeRecord typeRecord)
    {
        if(typeRecord.getType().getClass().equals(GeneSymbolTableType.class))
        {
            return
                // Entrez ID match via translation (like LOC12345 -- LOC is stripped prior to query)
                "SELECT DISTINCT gs.stable_id,sr.name FROM xref x JOIN object_xref o USING(xref_id) " +
                "JOIN translation ON(translation_id=o.ensembl_id) JOIN transcript t USING(transcript_id) " +
                "JOIN gene_stable_id gs USING(gene_id) " +
                "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) " +
                "WHERE external_db_id=(SELECT external_db_id FROM external_db WHERE db_name='EntrezGene') " +
                "AND ensembl_object_type='Translation' AND dbprimary_acc = ? "+
                // Entrez ID match via gene (like LOC12345 -- LOC is stripped prior to query)
                "UNION DISTINCT SELECT DISTINCT gs.stable_id,sr.name FROM xref x JOIN object_xref o USING(xref_id) " +
                "JOIN gene_stable_id gs ON(gs.gene_id=o.ensembl_id) " +
                "JOIN gene g ON (g.gene_id=gs.gene_id) " +
                "JOIN seq_region sr ON(g.seq_region_id=sr.seq_region_id) " +
                "WHERE external_db_id=(SELECT external_db_id FROM external_db WHERE db_name='EntrezGene') " +
                "AND ensembl_object_type='Gene' AND dbprimary_acc = ? "+
                // Direct gene symbol match
                "UNION DISTINCT SELECT DISTINCT gs.stable_id,sr.name FROM xref x JOIN object_xref o USING(xref_id) " +
                "JOIN gene_stable_id gs ON(gs.gene_id=o.ensembl_id) " + 
                "JOIN gene g ON (g.gene_id=gs.gene_id) " +
                "JOIN seq_region sr ON(g.seq_region_id=sr.seq_region_id) " +
                "WHERE ensembl_object_type='Gene' AND " + typeRecord.getRestrictionClause( species ) + " " +
                // Transcript match
                "UNION DISTINCT SELECT DISTINCT gs.stable_id,sr.name FROM xref x JOIN object_xref o USING(xref_id) " +
                "JOIN transcript t ON(transcript_id=o.ensembl_id) " + 
                "JOIN gene_stable_id gs USING(gene_id) " + 
                "JOIN seq_region sr ON(t.seq_region_id=sr.seq_region_id) " +
                "WHERE ensembl_object_type='Transcript' AND " + typeRecord.getRestrictionClause( species );
        }
        return "SELECT DISTINCT gs.stable_id,sr.name FROM xref x " +
                "JOIN object_xref o USING(xref_id) " +
                "JOIN gene_stable_id gs ON(gs.gene_id=o.ensembl_id) " +
                "JOIN gene g ON (g.gene_id=gs.gene_id) " +
                "JOIN seq_region sr ON(g.seq_region_id=sr.seq_region_id) " +
                "WHERE ensembl_object_type='Gene' AND " + typeRecord.getRestrictionClause(species);
    }

    @Override
    protected TypeRecord[] getSupportedTypeRecords()
    {
        return supportedTypeRecords;
    }

    @Override
    protected String getObjectType()
    {
        return "Gene";
    }
}
