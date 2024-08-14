package biouml.plugins.ensembl.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

/**
 *
 */
public class DatabaseReferenceBeanInfo extends biouml.standard.type.DatabaseReferenceBeanInfo
{
    public DatabaseReferenceBeanInfo()
    {
        super(DatabaseReference.class, null);
        
        initResources(MessageBundle.class.getName());
        
        beanDescriptor.setDisplayName     (getResourceString("CN_ENSEMBL_DATABESE_REF"));
        beanDescriptor.setShortDescription(getResourceString("CD_ENSEMBL_DATABESE_REF"));
    }

    @Override
    public void initProperties()
    {
        //init parent properties
        super.initProperties();
        try
        {
            initResources(MessageBundle.class.getName());
            
            int index = findPropertyIndex("databaseName");
            
            PropertyDescriptorEx pde = new PropertyDescriptorEx("version", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "DV");
            add(index, pde,
                getResourceString("PN_ENSEMBL_DATABASE_VERSION"),
                getResourceString("PD_ENSEMBL_DATABASE_VERSION"));
                    
            pde = new PropertyDescriptorEx("synonyms", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "DS");
            add(++index, pde,
                getResourceString("PN_ENSEMBL_DATABASE_SYNONYMS"),
                getResourceString("PD_ENSEMBL_DATABASE_SYNONYMS"));

            index = findPropertyIndex("comment");
            
            pde = new PropertyDescriptorEx("info", beanClass);
            HtmlPropertyInspector.setDisplayName(pde, "DI");
            add(index, pde,
                getResourceString("PN_ENSEMBL_DATABASE_INFO"),
                getResourceString("PD_ENSEMBL_DATABASE_INFO"));

        }
        catch(Throwable th)
        {
            this.logError("Cannot init bean properties.", th);
        }
    }
}
