package ru.biosoft.analysis.optimization;

import com.developmentontheedge.beans.BeanInfoConstants;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ParameterBeanInfo extends BeanInfoEx2<Parameter>
{
    public ParameterBeanInfo()
    {
        super(Parameter.class);
    }

    @Override
    public void initProperties() throws Exception
    {
    	property("name").readOnly().add();
    	property("title").readOnly().add();
    	property("parentDiagramName").readOnly().add();
    	property("lowerBound").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
    	property("value").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
    	property("upperBound").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
    	property("locality").tags(Parameter.Locality.getLocality()).add();
    	property("units").readOnly().add();
    	property("comment").add();
    }
}
