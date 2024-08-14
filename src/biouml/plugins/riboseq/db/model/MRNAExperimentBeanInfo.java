package biouml.plugins.riboseq.db.model;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector.SortOrder;
import biouml.plugins.riboseq.db.DatabaseCollections;
import biouml.plugins.riboseq.db.editors.ConditionMultiSelector;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MRNAExperimentBeanInfo extends BeanInfoEx2<MRNAExperiment>
{
    public MRNAExperimentBeanInfo()
    {
        super( MRNAExperiment.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx( "name", beanClass, "getName", null );
        add( pde );
        add( "title" );
        add( "description" );
        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, DatabaseCollections.SPECIES_COLLECTION_PATH,
                SortOrder.STRING ) );
        add( DataElementComboBoxSelector.registerSelector( "cellSource", beanClass, DatabaseCollections.CELL_COLLECTION_PATH,
                SortOrder.STRING ) );
        add( ConditionMultiSelector.registerSelector( "conditions", beanClass ) );
        add( DataElementComboBoxSelector.registerSelector( "sequencingPlatform", beanClass,
                DatabaseCollections.SEQUENCING_PLATFORM_COLLECTION_PATH, SortOrder.STRING ) );
        add( "sequenceData" );
        add( "sraProjectId" );
        add( "sraExperimentId" );
        add( "geoSeriesId" );
        add( "geoSampleId" );
        add( "pubMedIds" );
    }
}
