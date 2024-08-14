package biouml.standard.type;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class DatabaseReferenceBeanInfo extends BeanInfoEx
{
    public DatabaseReferenceBeanInfo()
    {
        this(DatabaseReference.class, "DATABASE_REFERENCE", "biouml.standard.type.MessageBundle");
    }

    public DatabaseReferenceBeanInfo(Class beanClass, String key, String messageBundle)
    {
        super(beanClass, messageBundle);
        if( key != null && messageBundle != null )
        {
            beanDescriptor.setDisplayName(getResourceString("CN_" + key));
            beanDescriptor.setShortDescription(getResourceString("CD_" + key));
        }
    }

    public DatabaseReferenceBeanInfo(Class beanClass, String key)
    {
        this(beanClass, key, MessageBundle.class.getName());
    }

    @Override
    public void initProperties()
    {
        try
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("databaseName", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "DN");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_DATABASE_NAME"), getResourceString("PD_DATABASE_REFERENCE_DATABASE_NAME"));

            pde = new PropertyDescriptorEx("id", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "ID");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_ID"), getResourceString("PD_DATABASE_REFERENCE_ID"));

            pde = new PropertyDescriptorEx("ac", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "AC");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_AC"), getResourceString("PD_DATABASE_REFERENCE_AC"));

            pde = new PropertyDescriptorEx("comment", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "CM");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_COMMENT"), getResourceString("PD_DATABASE_REFERENCE_COMMENT"));

            pde = new PropertyDescriptorEx("databaseVersion", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "DV");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_DBVERSION"), getResourceString("PD_DATABASE_REFERENCE_DBVERSION"));

            pde = new PropertyDescriptorEx("idVersion", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "IV");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_IDVERSION"), getResourceString("PD_DATABASE_REFERENCE_IDVERSION"));

            pde = new PropertyDescriptorEx("relationshipType", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "RT");
            add(pde, getResourceString("PN_DATABASE_REFERENCE_TYPE"), getResourceString("PD_DATABASE_REFERENCE_TYPE"));
        }
        catch( Throwable th )
        {
            this.logError("Cannot init bean properties.", th);
        }
    }
}
