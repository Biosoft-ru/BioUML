package ru.biosoft.access.history;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class HistoryElementBeanInfo extends BeanInfoEx
{
    public HistoryElementBeanInfo()
    {
        super(HistoryElement.class, "ru.biosoft.access.history.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_HISTORY"));
        beanDescriptor.setShortDescription(getResourceString("CD_HISTORY"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("timestamp", beanClass, "getTimestampFormated", null);
        add(pd, getResourceString("PN_HISTORY_TIMESTAMP"), getResourceString("PD_HISTORY_TIMESTAMP"));

        pd = new PropertyDescriptorEx("version", beanClass, "getVersion", null);
        add(pd, getResourceString("PN_HISTORY_VERSION"), getResourceString("PD_HISTORY_VERSION"));

        pd = new PropertyDescriptorEx("author", beanClass, "getAuthor", null);
        add(pd, getResourceString("PN_HISTORY_AUTHOR"), getResourceString("PD_HISTORY_AUTHOR"));

        pd = new PropertyDescriptorEx("comment", beanClass, "getComment", null);
        add(pd, getResourceString("PN_HISTORY_COMMENT"), getResourceString("PD_HISTORY_COMMENT"));
    }
}
