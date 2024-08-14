package biouml.standard.type;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class PublicationBeanInfo extends BeanInfoEx
{
    public PublicationBeanInfo()
    {
        super(Publication.class, "biouml.standard.type.MessageBundle" );
        beanDescriptor.setDisplayName     (getResourceString("CN_PUBLICATION"));
        beanDescriptor.setShortDescription(getResourceString("CD_PUBLICATION"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;

        pde = new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null);
        HtmlPropertyInspector.setDisplayName(pde, "ID");
        add(pde,
            getResourceString("PN_IDENTIFIER"),
            getResourceString("PD_IDENTIFIER"));

        pde = new PropertyDescriptorEx("reference", beanClass.getMethod("getReference"), null);
        HtmlPropertyInspector.setDisplayName(pde, "reference");
        add(pde,
            getResourceString("PN_REFERENCE"),
            getResourceString("PD_REFERENCE"));

        pde = new PropertyDescriptorEx("pubMedId", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PMID");
        add(pde,
            getResourceString("PN_PUBMED_ID"),
            getResourceString("PD_PUBMED_ID"));

        pde = new PropertyDescriptorEx("authors", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "AU");
        add(pde,
            getResourceString("PN_AUTHORS"),
            getResourceString("PD_AUTHORS"));

        pde = new PropertyDescriptorEx("affiliation", beanClass);
        pde.setExpert(true);
        HtmlPropertyInspector.setDisplayName(pde, "AD");
        add(pde,
            getResourceString("PN_AFFILIATION"),
            getResourceString("PD_AFFILIATION"));

        pde = new PropertyDescriptorEx("title", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TI");
        add(pde,
            getResourceString("PN_TITLE"),
            getResourceString("PD_TITLE"));

        pde = new PropertyDescriptorEx("source", beanClass.getMethod("getSource"), null);
        HtmlPropertyInspector.setDisplayName(pde, "SO");
        add(pde,
            getResourceString("PN_LITERATURE_SOURCE"),
            getResourceString("PD_LITERATURE_SOURCE"));

        pde = new PropertyDescriptorEx("journalTitle", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TA");
        add(pde,
            getResourceString("PN_JOURNAL_TITLE"),
            getResourceString("PD_JOURNAL_TITLE"));

        pde = new PropertyDescriptorEx("volume", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "VI");
        add(pde,
            getResourceString("PN_JOURNAL_VOLUME"),
            getResourceString("PD_JOURNAL_VOLUME"));

        pde = new PropertyDescriptorEx("issue", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IP");
        add(pde,
            getResourceString("PN_JOURNAL_ISSUE"),
            getResourceString("PD_JOURNAL_ISSUE"));

        pde = new PropertyDescriptorEx("pageFrom", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PF");
        add(pde,
            getResourceString("PN_PAGE_FROM"),
            getResourceString("PD_PAGE_FROM"));

        pde = new PropertyDescriptorEx("pageTo", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PT");
        add(pde,
            getResourceString("PN_PAGE_TO"),
            getResourceString("PD_PAGE_TO"));

        pde = new PropertyDescriptorEx("publicationAbstract", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "AB");
        add(pde,
            getResourceString("PN_PUBLICATION_ABSTRACT"),
            getResourceString("PD_PUBLICATION_ABSTRACT"));

        pde = new PropertyDescriptorEx("fullTextURL", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "URL");
        add(pde,
            getResourceString("PN_FULL_TEXT_URL"),
            getResourceString("PD_FULL_TEXT_URL"));

        pde = new PropertyDescriptorEx("year", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PY");
        add(pde,
            getResourceString("PN_PUBLICATION_YEAR"),
            getResourceString("PD_PUBLICATION_YEAR"));

        pde = new PropertyDescriptorEx("month", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PM");
        add(pde,
            getResourceString("PN_PUBLICATION_MONTH"),
            getResourceString("PD_PUBLICATION_MONTH"));

        pde = new PropertyDescriptorEx("language", beanClass);
        pde.setExpert(true);
        HtmlPropertyInspector.setDisplayName(pde, "LA");
        add(pde,
            getResourceString("PN_LANGUAGE"),
            getResourceString("PD_LANGUAGE"));

        pde = new PropertyDescriptorEx("publicationType", beanClass);
        pde.setExpert(true);
        HtmlPropertyInspector.setDisplayName(pde, "PT");
        add(pde,
            getResourceString("PN_PUBLICATION_TYPE"),
            getResourceString("PD_PUBLICATION_TYPE"));

        ///////////////////////////////////////////////////////////////
        // Utility fields to organize, categorize and  store user defined information.

        pde = new PropertyDescriptorEx("status", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "ST");
        add(pde,
            getResourceString("PN_STATUS"),
            getResourceString("PD_STATUS"));

        pde = new PropertyDescriptorEx("keywords", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "KW");
        add(pde,
            getResourceString("PN_KEYWORDS"),
            getResourceString("PD_KEYWORDS"));

        pde = new PropertyDescriptorEx("importance", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IM");
        add(pde,
            getResourceString("PN_IMPORTANCE"),
            getResourceString("PD_IMPORTANCE"));

        pde = new PropertyDescriptorEx("comment", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CC");
        add(pde,
            getResourceString("PN_COMMENT"),
            getResourceString("PD_COMMENT"));
        
        ///////////////////////////////////////////////////////////////
        // BioPAX properties
        //
        
        pde = new PropertyDescriptorEx("db", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "DB");
        add(pde,
            getResourceString("PN_DBNAME"),
            getResourceString("PD_DBNAME"));
        
        pde = new PropertyDescriptorEx("dbVersion", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "DV");
        add(pde,
            getResourceString("PN_DBVERSION"),
            getResourceString("PD_DBVERSION"));
        
        pde = new PropertyDescriptorEx("idName", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IN");
        add(pde,
            getResourceString("PN_IDNAME"),
            getResourceString("PD_IDNAME"));
        
        pde = new PropertyDescriptorEx("idVersion", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IV");
        add(pde,
            getResourceString("PN_IDVERSION"),
            getResourceString("PD_IDVERSION"));
        
        pde = new PropertyDescriptorEx("simpleSource", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "SS");
        add(pde,
            getResourceString("PN_SIMPLESOURCE"),
            getResourceString("PD_SIMPLESOURCE"));
    }
}
