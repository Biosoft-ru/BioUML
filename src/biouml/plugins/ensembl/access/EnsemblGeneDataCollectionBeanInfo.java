package biouml.plugins.ensembl.access;

import ru.biosoft.access.SqlDataCollectionBeanInfo;


/**
 * @pending Document
 */
public class EnsemblGeneDataCollectionBeanInfo extends SqlDataCollectionBeanInfo
{
    public EnsemblGeneDataCollectionBeanInfo()
    {
        super(EnsemblGeneDataCollection.class, MessageBundle.class.getName() );
        
        initResources("biouml.plugins.ensembl.access.MessageBundle");
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_ENSEMBL_GENE_DC") );
        beanDescriptor.setShortDescription( getResourceString("CD_ENSEMBL_GENE_DC") );
    }
}
