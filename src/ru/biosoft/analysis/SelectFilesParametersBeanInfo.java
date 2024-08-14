package ru.biosoft.analysis;

import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SelectFilesParametersBeanInfo extends BeanInfoEx2<SelectFilesParameters>
{
    public SelectFilesParametersBeanInfo()
    {
        super(SelectFilesParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {        
        add(DataElementPathEditor.registerInput( "inputCollection", beanClass, GenericDataCollection.class ));
        add("mask");
        addExpert("workDir");
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputCollection", beanClass, GenericDataCollection.class),
                "$inputCollection$ filtered"));
    }
}