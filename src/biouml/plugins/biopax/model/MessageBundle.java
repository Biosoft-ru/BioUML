package biouml.plugins.biopax.model;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
        
        //----- Open Controller Vocabulary specific fields -------------------------------------/
                {"CN_OPENCONTROLLEDVOCABULARY", "Open Controller Vocabulary"},
                {"CD_OPENCONTROLLEDVOCABULARY", "Open Controller Vocabulary"},
                {"PN_OPENCONTROLLEDVOCABULARY_XREFS", "References"},
                {"PD_OPENCONTROLLEDVOCABULARY_XREFS", "References"},
                {"PN_OPENCONTROLLEDVOCABULARY_TERM", "Term"},
                {"PD_OPENCONTROLLEDVOCABULARY_TERM", "Term"},
                {"PN_OPENCONTROLLEDVOCABULARY_TYPE", "Vocabulary type"},
                {"PD_OPENCONTROLLEDVOCABULARY_TYPE", "Vocabulary type"},
        //----- Xref specific fields -------------------------------------/
                {"CN_XREF", "Reference"},
                {"CD_XREF", "Reference"},
                {"PN_XREF_DBVERSION", "Database Version"},
                {"PD_XREF_DBVERSION", "Database Version"},
                {"PN_XREF_DB", "Database"},
                {"PD_XREF_DB", "Database"},
                {"PN_XREF_IDVERSION", "Id Version"},
                {"PD_XREF_IDVERSION", "Id Version"},
                {"PN_XREF_ID", "Id"},
                {"PD_XREF_ID", "Id"},
                {"PN_XREF_TYPE", "Type"},
                {"PD_XREF_TYPE", "Type"},
         //----- BioSource specific fields -------------------------------------/
                {"CN_BIOSOURCE", "BioSource"},
                {"CD_BIOSOURCE", "BioSource"},
                {"PN_BIOSOURCE_CELLTYPE", "Cell Type"},
                {"PD_BIOSOURCE_CELLTYPE", "Cell Type"},
                {"PN_BIOSOURCE_TAXONXREF", "Reference"},
                {"PD_BIOSOURCE_TAXONXREF", "Taxon reference"},
                {"PN_BIOSOURCE_TISSUE", "Tissue"},
                {"PD_BIOSOURCE_TISSUE", "Tissue"},
         //----- Confidence specific fields -------------------------------------/
                {"CN_CONFIDENCE", "Confidence"},
                {"CD_CONFIDENCE", "Confidence"},
                {"PN_CONFIDENCE_VALUE", "Confidence Value"},
                {"PD_CONFIDENCE_VALUE", "Confidence Value"},
                {"PN_CONFIDENCE_XREF", "Reference"},
                {"PD_CONFIDENCE_XREF", "Reference"},
                
         //----- Evidence specific fields -------------------------------------/
                {"CN_EVIDENCE", "Evidence"},
                {"CD_EVIDENCE", "Evidence"},
                {"PN_EVIDENCE_CONFIDENCE", "Confidence"},
                {"PD_EVIDENCE_CONFIDENCE", "Confidence"},
                {"PN_EVIDENCE_CODE", "Evidence Code"},
                {"PD_EVIDENCE_CODE", "Evidence Code"},
                {"PN_EVIDENCE_XREF", "Reference"},
                {"PD_EVIDENCE_XREF", "Reference"},
                
         //----- Sequence Site specific fields -------------------------------------/
                {"CN_SEQUENCESITE", "Sequence Site"},
                {"CD_SEQUENCESITE", "Sequence Site"},
                {"PN_SEQUENCESITE_POSITIONSTATUS", "Position status"},
                {"PD_SEQUENCESITE_POSITIONSTATUS", "Position status"},
                {"PN_SEQUENCESITE_SEQUENCEPOSITION", "Sequence position"},
                {"PD_SEQUENCESITE_SEQUENCEPOSITION", "Sequence position"},
           
         //----- Sequence Interval specific fields -------------------------------------/
                {"CN_SEQUENCEINTERVAL", "Sequence Interval"},
                {"CD_SEQUENCEINTERVAL", "Sequence Interval"},
                {"PN_SEQUENCEINTERVAL_BEGIN", "Begin"},
                {"PD_SEQUENCEINTERVAL_BEGIN", "Sequence Interval Begin"},
                {"PN_SEQUENCEINTERVAL_END", "End"},
                {"PD_SEQUENCEINTERVAL_END", "Sequence Interval End"},
                
         //----- Sequence Feature specific fields -------------------------------------/
                {"CN_SEQUENCEFEATURE", "Sequence Feature"},
                {"CD_SEQUENCEFEATURE", "Sequence Feature"},
                {"PN_SEQUENCEFEATURE_LOCATION", "Location"},
                {"PD_SEQUENCEFEATURE_LOCATION", "Feature Location"},
                {"PN_SEQUENCEFEATURE_TYPE", "Type"},
                {"PD_SEQUENCEFEATURE_TYPE", "Feature Type"},
        
         //----- Entity Feature specific fields -------------------------------------/
                {"CN_ENTITYFEATURE", "Entity Feature"},
                {"CD_ENTITYFEATURE", "A feature or aspect of an entity that can be changed while the entity still retains its biological identity"},
                {"PN_ENTITYFEATURE_LOCATION", "Location"},
                {"PD_ENTITYFEATURE_LOCATION", "Location of the feature on the sequence of the interactor"},
                {"PN_ENTITYFEATURE_TYPE", "Type"},
                {"PD_ENTITYFEATURE_TYPE", "Feature Type"}
        
         
        };
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable th )
        {

        }
        return key;
    }
}

