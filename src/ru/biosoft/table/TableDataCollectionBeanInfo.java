package ru.biosoft.table;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.ReferenceTypeSelector;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TableDataCollectionBeanInfo extends BeanInfoEx2<TableDataCollection>
{
    protected TableDataCollectionBeanInfo(Class<? extends TableDataCollection> c)
    {
        super(c, MessageBundle.class.getName());

        initResources(MessageBundle.class.getName());

        beanDescriptor.setDisplayName(getResourceString("CN_TABLE_DC"));
        beanDescriptor.setShortDescription(getResourceString("CD_TABLE_DC"));
    }

    public TableDataCollectionBeanInfo()
    {
        this(TableDataCollection.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources(MessageBundle.class.getName());

        add(new PropertyDescriptorEx("name", beanClass, "getName", null), getResourceString("PN_TABLE_DC_NAME"),
                getResourceString("PD_TABLE_DC_NAME"));

        add(new PropertyDescriptorEx("size", beanClass, "getSize", null), getResourceString("PN_TABLE_DC_SIZE"),
                getResourceString("PD_TABLE_DC_SIZE"));

        add(new PropertyDescriptorEx("description", beanClass, "getDescription", "setDescription"),
                getResourceString("PN_TABLE_DC_DESCRIPTION"), getResourceString("PD_TABLE_DC_DESCRIPTION"));

        add(ReferenceTypeSelector.registerSelector("referenceType", beanClass), getResourceString("PN_TABLE_SUB_TYPE"),
                getResourceString("PD_TABLE_SUB_TYPE"));

        property( "species" ).title( "PN_IMPORT_SPECIES" ).description( "PD_IMPORT_SPECIES" )
            .tags( bean -> Species.allSpecies().map( Species::getName ).prepend( "Unspecified" ) ).add();
    }
}
