package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CellLineBeanInfo extends BeanInfoEx
{
    public CellLineBeanInfo()
    {
        super( CellLine.class, true );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pde, "Cell ID", "Cell ID");

        pde = new PropertyDescriptorEx("title", beanClass, "getTitle", null);
        add(pde, "Title", "Title");

        pde = new PropertyDescriptorEx("species", beanClass, "getSpecies", null);
        add(pde, "Species", "Species");

        pde = new PropertyDescriptorEx("cellosaurusId", beanClass, "getCellosaurusId", null);
        add(pde, "Cellosaurus ID", "Cellosaurus ID");
        
        pde = new PropertyDescriptorEx("cellOntologyId", beanClass, "getCellOntologyId", null);
        add(pde, "Cell Ontology ID", "Cell Ontology ID");
        
        pde = new PropertyDescriptorEx("expFactorOntologyId", beanClass, "getExpFactorOntologyId", null);
        add(pde, "Exp Factor Ontology ID", "Exp Factor Ontology ID");

        pde = new PropertyDescriptorEx("uberonId", beanClass, "getUberonId", null);
        add(pde, "Uberon ID", "Uberon ID");
    }
}
