package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class DatabaseInfoBeanInfo extends ReferrerBeanInfo<DatabaseInfo>
{
    public DatabaseInfoBeanInfo()
    {
        super(DatabaseInfo.class, "DATABASE_INFO");
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("queryById", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "QID");
        add(5, pde,
            getResourceString("PN_QUERY_BY_ID"),
            getResourceString("PD_QUERY_BY_ID"));

        pde = new PropertyDescriptorEx("queryByAc", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "QAC");
        add(6, pde,
            getResourceString("PN_QUERY_BY_AC"),
            getResourceString("PD_QUERY_BY_AC"));
    }

}
