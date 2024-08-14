package biouml.plugins.ensembl.type;

import java.util.ListResourceBundle;

/**
 *
 */
public class MessageBundle  extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //Ensembl Gene fields
            {"CN_ENSEMBL_GENE", "Gene"},
            {"CD_ENSEMBL_GENE", "Ensembl gene."},
        
            {"PN_ENSEMBL_TITLE", "Gene symbol"},
            {"PD_ENSEMBL_TITLE", "Gene symbol"},
            
            {"PN_ENSEMBL_DESCRIPTION", "Gene description"},
            {"PD_ENSEMBL_DESCRIPTION", "Gene description"},
            
            {"PN_ENSEMBL_GENE_STATUS", "Status"},
            {"PD_ENSEMBL_GENE_STATUS", "The status of the gene. Normally one of these Strings: 'KNOWN','NOVEL','PUTATIVE','PREDICTED'."},
        
            {"ENSEMBLE_GENE_STATUS", geneStatus},
            
            {"PN_ENSEMBL_GENE_VERSION", "Version"},
            {"PD_ENSEMBL_GENE_VERSION", "The version of this object."},
        
            {"PN_ENSEMBL_GENE_CREATED_DATE", "Created date"},
            {"PD_ENSEMBL_GENE_CREATED_DATE", "The date this object was created."},
        
            //Ensembl DatabaseReference fields
            {"CN_ENSEMBL_DATABESE_REF", "Ensembl external reference"},
            {"CD_ENSEMBL_DATABESE_REF", "Reference to the external databases from the Ensembl database."},
            
            {"PN_ENSEMBL_DATABASE_VERSION", "Version"},
            {"PD_ENSEMBL_DATABASE_VERSION", "The version of this external database."},
                    
            {"PN_ENSEMBL_DATABASE_SYNONYMS", "Synonyms"},
            {"PD_ENSEMBL_DATABASE_SYNONYMS", "Synonyms of this data base."},
            
            {"PN_ENSEMBL_DATABASE_INFO", "Info"},
            {"PD_ENSEMBL_DATABASE_INFO", "Info text."},
            
            // EnsemblSequence constants
            {"CN_SEQUENCE"                  , "Sequence"},
            {"CD_SEQUENCE"                  , "Sequence"},
        
            {"PN_SEQUENCE_LENGTH"             , "Length"},
            {"PD_SEQUENCE_LENGTH"             , "Sequence length"},
            {"PN_KARYOTYPE"                   , "Karyotype"},
            {"PD_KARYOTYPE"                   , "Karyotype"},
            {"PN_NAME"                        , "Name"},
            {"PD_NAME"                        , "Sequence/chromosome name"},
        };
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
    
    public static final String STATUS_KNOWN     = "KNOWN";
    public static final String STATUS_NOVEL     = "NOVEL";
    public static final String STATUS_PUTATIVE  = "PUTATIVE";
    public static final String STATUS_PREDICTED = "PREDICTED";
    
    private final String[] geneStatus = { STATUS_KNOWN, STATUS_NOVEL, STATUS_PREDICTED, STATUS_PUTATIVE};
}
