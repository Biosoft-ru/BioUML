package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.standard.type.Species;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.ReferenceTypeSelector;

public class SiteModelsToProteinsParametersBeanInfo extends BeanInfoEx2<SiteModelsToProteinsParameters>
{
    protected SiteModelsToProteinsParametersBeanInfo(Class<? extends SiteModelsToProteinsParameters> clazz)
    {
        super(clazz);
    }
    
    public SiteModelsToProteinsParametersBeanInfo()
    {
        this(SiteModelsToProteinsParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("sitesCollection").inputElement( TableDataCollection.class ).add();
        addHidden(new PropertyDescriptor("defaultProfile", beanClass, "getDefaultProfile", null));
        property("siteModelsCollection").inputElement( SiteModelCollection.class ).auto( "$defaultProfile$" ).canBeNull().add();
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        add(ReferenceTypeSelector.registerSelector("targetType", beanClass));
        addHidden(new PropertyDescriptorEx("shortTargetType", beanClass, "getShortTargetType", null));
        property( "ignoreNaNInAggregator" ).titleRaw( "Ignore empty values" ).descriptionRaw( "Ignore empty values during aggregator work" ).add();
        property( "aggregator" ).simple().editor( NumericAggregatorEditor.class ).add();
        property( ColumnNameSelector.registerSelector( "columnName", beanClass, "sitesCollection" ) ).hidden( "isAggregatorColumnHidden" )
                .add();
        property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sitesCollection$ TFs $shortTargetType$" )
                .value( DataElementPathEditor.ICON_ID, SiteModelsToProteinsParameters.class.getMethod( "getIcon" ) ).add();
    }
}
