package ru.biosoft.bsa.analysis.createsitemodel;

import java.beans.PropertyDescriptor;

import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CreateProfileFromTableParametersBeanInfo extends BeanInfoEx2<CreateProfileFromTableParameters>
{
    public CreateProfileFromTableParametersBeanInfo()
    {
        super(CreateProfileFromTableParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "table" ).inputElement( TableDataCollection.class ).add();
        addHidden(new PropertyDescriptor("defaultProfile", beanClass, "getDefaultProfile", null));
        property( "profile" ).inputElement( SiteModelCollection.class ).auto( "$defaultProfile$" ).add();
        add(ColumnNameSelector.registerNumericSelector("thresholdsColumn", beanClass, "table"));
        property( "outputProfile" ).outputElement( SiteModelCollection.class ).auto( "$table$ profile" ).add();
    }
}