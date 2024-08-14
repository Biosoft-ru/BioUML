package biouml.plugins.ensembl.type;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class GeneBeanInfo extends biouml.standard.type.GeneBeanInfo
{     
    public GeneBeanInfo()
    {
        super(Gene.class);
        initResources(MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("CN_ENSEMBL_GENE"));
        beanDescriptor.setShortDescription(getResourceString("CD_ENSEMBL_GENE"));
    }

    @Override
    public void initProperties() throws Exception
    {
        // init parent properties
        super.initProperties();

        initResources(MessageBundle.class.getName());
        
        PropertyDescriptor pd = findPropertyDescriptor("regulation");
        pd.setHidden(true);
        pd = findPropertyDescriptor("literatureReferences");
        pd.setHidden(true);
        pd = findPropertyDescriptor("structureReferences");
        pd.setHidden(true);
        

        int index = findPropertyIndex("comment");
        add( index, new PropertyDescriptorEx( "site", beanClass, "getSite", null ) );
        
        PropertyDescriptor pde = new PropertyDescriptorEx("status", beanClass);
        pde.setPropertyEditorClass(StatusEditor.class);
        HtmlPropertyInspector.setDisplayName(pde, "ES");
        add(++index, pde,
            getResourceString("PN_ENSEMBL_GENE_STATUS"),
            getResourceString("PD_ENSEMBL_GENE_STATUS"));
                
        pde = new PropertyDescriptorEx("version", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EV");
        add(++index, pde,
            getResourceString("PN_ENSEMBL_GENE_VERSION"),
            getResourceString("PD_ENSEMBL_GENE_VERSION"));

        index = findPropertyIndex("date");
                
        pde = new PropertyDescriptorEx("createdDate", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "ED");
        add(index, pde,
            getResourceString("PN_ENSEMBL_GENE_CREATED_DATE"),
            getResourceString("PD_ENSEMBL_GENE_CREATED_DATE"));
        
        pde = findPropertyDescriptor("title");
        pde.setDisplayName(getResourceString("PN_ENSEMBL_TITLE"));
        pde.setShortDescription(getResourceString("PD_ENSEMBL_TITLE"));

        pde = findPropertyDescriptor("description");
        pde.setDisplayName(getResourceString("PN_ENSEMBL_DESCRIPTION"));
        pde.setShortDescription(getResourceString("PD_ENSEMBL_DESCRIPTION"));
    }

    public static class StatusEditor extends StringTagEditorSupport
    {
        public StatusEditor()
        {
            super(MessageBundle.class.getName(), MessageBundle.class, "ENSEMBLE_GENE_STATUS");
        }
    }
    
}
