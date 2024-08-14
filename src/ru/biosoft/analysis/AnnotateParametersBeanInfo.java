package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.editors.AnnotationPropertiesEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.OptionEx;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

import biouml.standard.type.Species;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class AnnotateParametersBeanInfo extends BeanInfoEx
{
    public AnnotateParametersBeanInfo()
    {
        super(AnnotateParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("inputTablePath", beanClass, TableDataCollection.class),
                getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH),
                getResourceString("PN_ANNOTATION_SPECIES"), getResourceString("PD_ANNOTATION_SPECIES"));
        addHidden(new PropertyDescriptorEx( "defaultAnnotationPath", beanClass, "getDefaultAnnotationPath", null ));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerInputChild(new PropertyDescriptorEx("annotationCollectionPath", beanClass), null), "$defaultAnnotationPath$"),
                getResourceString("PN_ANNOTATION_COLLECTION"), getResourceString("PD_ANNOTATION_COLLECTION"));
        PropertyDescriptorEx pde = new PropertyDescriptorEx( "annotationColumns", beanClass );
        pde.setHideChildren( true );
        add( pde, AnnotationPropertiesEditor.class,
                getResourceString("PN_ANNOTATION_COLUMNS"), getResourceString("PD_ANNOTATION_COLUMNS"));
        add(BeanUtil.createExpertDescriptor("replaceDuplicates", beanClass), getResourceString("PN_ANNOTATION_REPLACE_DUPLICATES"), getResourceString("PD_ANNOTATION_REPLACE_DUPLICATES"));
        pde = OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "outputTablePath", beanClass, TableDataCollection.class ),
                "$inputTablePath$ annotated" );
        pde.setValue(DataElementPathEditor.ICON_ID, AnnotateParameters.class.getMethod("getOutputIcon"));
        add( pde, getResourceString( "PN_OUTPUT_TABLE" ), getResourceString( "PD_OUTPUT_TABLE" ) );
        addHidden(new PropertyDescriptorEx("annotationColumnKeys", beanClass));
    }
}
