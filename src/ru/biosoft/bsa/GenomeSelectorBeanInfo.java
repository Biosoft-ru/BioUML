package ru.biosoft.bsa;

import ru.biosoft.util.bean.BeanInfoEx2;

public class GenomeSelectorBeanInfo extends BeanInfoEx2<GenomeSelector>
{
    public GenomeSelectorBeanInfo()
    {
        this(GenomeSelector.class);
    }
    
    protected GenomeSelectorBeanInfo(Class<? extends GenomeSelector> beanClass)
    {
        super(beanClass, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("PN_TRACKIMPORT_PROPERTIES"));
        beanDescriptor.setShortDescription(getResourceString("PD_TRACKIMPORT_PROPERTIES"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property( "dbSelector" ).canBeNull().title( "PN_SITESEARCH_SEQDATABASE" ).description( "PD_SITESEARCH_SEQDATABASE" ).add();
        property( "sequenceCollectionPath" ).inputElement( SequenceCollection.class ).canBeNull().hidden( "isSequenceCollectionPathHidden" )
                .title( "PN_TRACKIMPORT_SEQCOLLECTION" ).description( "PD_TRACKIMPORT_SEQCOLLECTION" ).add();
        property( "genomeId" )
            .hidden( "isSequenceCollectionPathHidden" )
            .auto( "$sequenceCollectionPath/element/properties/genomeBuild$" )
            .title( "PN_TRACKIMPORT_GENOME_ID" )
            .description( "PD_TRACKIMPORT_GENOME_ID" )
            .add();
    }
}
