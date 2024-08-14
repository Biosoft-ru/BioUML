package biouml.plugins.ensembl.access;

import ru.biosoft.access.SqlDataCollectionBeanInfo;

public class EnsemblGeneDataCollectionBeanInfo2 extends SqlDataCollectionBeanInfo
{
    public EnsemblGeneDataCollectionBeanInfo2()
    {
        super(EnsemblGeneDataCollection2.class, MessageBundle.class.getName() );
        
        initResources("biouml.plugins.ensembl.access.MessageBundle");
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_ENSEMBL_GENE_DC") );
        beanDescriptor.setShortDescription( getResourceString("CD_ENSEMBL_GENE_DC") );
    }
}