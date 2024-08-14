package ru.biosoft.bsa.analysis.createsitemodel;

import java.beans.PropertyDescriptor;

import biouml.standard.type.Species;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

/**
 * @author lan
 *
 */
public class FilterProfileByGeneSetParametersBeanInfo extends BeanInfoEx2<FilterProfileByGeneSetParameters>
{
    public FilterProfileByGeneSetParametersBeanInfo()
    {
        super(FilterProfileByGeneSetParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "table" ).inputElement( TableDataCollection.class ).add();
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        addHidden(new PropertyDescriptor("defaultProfile", beanClass, "getDefaultProfile", null));
        property( "profile" ).inputElement( SiteModelCollection.class ).auto( "$defaultProfile$" ).add();
        property( "output" ).outputElement( SiteModelCollection.class ).auto( "$table$ profile" ).add();
    }
}
