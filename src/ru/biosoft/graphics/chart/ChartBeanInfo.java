package ru.biosoft.graphics.chart;

import com.developmentontheedge.beans.BeanInfoEx;

public class ChartBeanInfo extends BeanInfoEx
{
    public ChartBeanInfo()
    {
        super(Chart.class, ChartMessageBundle.class.getName());
        setBeanEditor(ChartViewer.class);
    }
}
