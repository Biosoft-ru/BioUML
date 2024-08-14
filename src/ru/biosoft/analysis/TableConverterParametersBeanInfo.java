package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.ReferenceTypeSelector;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TableConverterParametersBeanInfo extends BeanInfoEx2<TableConverterParameters>
{
    public TableConverterParametersBeanInfo()
    {
        super(TableConverterParameters.class);
    }

    protected TableConverterParametersBeanInfo(Class<? extends TableConverterParameters> clazz, String messageBundle)
    {
        super(clazz, messageBundle);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property( "sourceTable" ).inputElement( TableDataCollection.class ).add();
        property( ColumnNameSelector.registerSelector( "idsColumnName", beanClass, "sourceTable", true ) ).expert().add();
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        add(ReferenceTypeSelector.registerSelector("sourceType", beanClass, true));
        add(ReferenceTypeSelector.registerSelector("targetType", beanClass, "sourceType", "species"));
        addHidden(new PropertyDescriptorEx("shortTargetType", beanClass, "getShortTargetType", null));
        add( "ignoreNaNInAggregator" );
        property( "aggregator" ).simple().editor( NumericAggregatorEditor.class ).add();
        addHidden(ColumnNameSelector.registerNumericSelector("columnName", beanClass, "sourceTable", true), "isAggregatorColumnHidden");
        addExpert( "maxMatches" );
        addExpert( "outputSourceIds" );
        PropertyDescriptorEx pde = DataElementPathEditor.registerOutput("unmatchedTable", beanClass, TableDataCollection.class, true);
        pde.setValue(DataElementPathEditor.ICON_ID, TableConverterParameters.class.getMethod("getUnmatchedIcon"));
        pde.setExpert(true);
        add(pde);
        pde = OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTable", beanClass, TableDataCollection.class),
                "$sourceTable$ $shortTargetType$");
        pde.setValue(DataElementPathEditor.ICON_ID, TableConverterParameters.class.getMethod("getIcon"));
        add(pde);
    }
}
