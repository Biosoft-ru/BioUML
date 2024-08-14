package biouml.plugins.riboseq.db.model;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.riboseq.db.DatabaseCollections;
import biouml.plugins.riboseq.db.editors.ConditionMultiSelector;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector.SortOrder;

public class ExperimentBeanInfo extends BeanInfoEx
{
    public ExperimentBeanInfo()
    {
        super( Experiment.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        add( "title" );
        add( "description" );
        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, DatabaseCollections.SPECIES_COLLECTION_PATH,
                SortOrder.STRING ) );
        add( DataElementComboBoxSelector.registerSelector( "cellSource", beanClass, DatabaseCollections.CELL_COLLECTION_PATH,
                SortOrder.STRING ) );
        add( "translationInhibition" );
        add( "minFragmentSize" );
        add( "maxFragmentSize" );
        add( "digestion" );
        add( DataElementComboBoxSelector.registerSelector( "sequenceAdapter", beanClass, DatabaseCollections.ADAPTER_COLLECTION_PATH,
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
