package ru.biosoft.analysis;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RandomNumberAnalysisParametersBeanInfo extends BeanInfoEx2<RandomNumberAnalysisParameters>
{
    public RandomNumberAnalysisParametersBeanInfo()
    {
        super(RandomNumberAnalysisParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("output").outputElement(TableDataCollection.class).add();
        add("count");
        add("randomNumber1");
        add("randomNumber2");
        add("randomNumbers");
    }
}