package ru.biosoft.server.servlets.webservices.imports;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.biohub.ReferenceType;

public enum OmicsType
{
    Transcriptomics('T'),
    Proteomics('P'),
    Genomics('G'),
    Epigenomics('E'),
    Metabolomics('M');
    
    final char abbrev;
    OmicsType(char abbrev)
    {
        this.abbrev = abbrev;
    }
    
    public static OmicsType getByAbbrev(char abbrev)
    {
        switch( abbrev )
        {
            case 'T': return Transcriptomics;
            case 'P': return Proteomics;
            case 'G': return Genomics;
            case 'E': return Epigenomics;
            case 'M': return Metabolomics;
            default:
                throw new AssertionError();
        }
    }

    private static Map<String, OmicsType> OBJECT_TYPE_TO_OMICS = new HashMap<>();
    static
    {
        OBJECT_TYPE_TO_OMICS.put( "Genes", OmicsType.Transcriptomics );
        OBJECT_TYPE_TO_OMICS.put( "miRNA", OmicsType.Transcriptomics );
        OBJECT_TYPE_TO_OMICS.put( "Probes", OmicsType.Transcriptomics );
        OBJECT_TYPE_TO_OMICS.put( "Transcripts", OmicsType.Transcriptomics );

        OBJECT_TYPE_TO_OMICS.put( "Proteins", OmicsType.Proteomics );
        OBJECT_TYPE_TO_OMICS.put( "Enzymes", OmicsType.Proteomics );
        OBJECT_TYPE_TO_OMICS.put( "Isoforms", OmicsType.Proteomics );
        OBJECT_TYPE_TO_OMICS.put( "Structures", OmicsType.Proteomics );

        OBJECT_TYPE_TO_OMICS.put( "Substances", OmicsType.Metabolomics );

        OBJECT_TYPE_TO_OMICS.put( "SNP", OmicsType.Genomics );
    }

    public static OmicsType getByReferenceType(ReferenceType referenceType)
    {
        String objectType = referenceType.getObjectType();
        return OBJECT_TYPE_TO_OMICS.get( objectType );
    }
}
