package biouml.plugins.kegg.type;

import java.beans.PropertyDescriptor;

import biouml.standard.type.SubstanceBeanInfo;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class GlycanBeanInfo extends SubstanceBeanInfo
{
    public GlycanBeanInfo()
    {
        super( Glycan.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        PropertyDescriptor pd = findPropertyDescriptor( "description" );
        pd.setHidden( true );
        pd = findPropertyDescriptor( "literatureReferences" );
        pd.setHidden( true );

        initResources( "biouml.plugins.kegg.type.MessageBundle" );

        beanDescriptor.setDisplayName( getResourceString( "CN_GLYCAN" ) );
        beanDescriptor.setShortDescription( getResourceString( "CD_GLYCAN" ) );

        int index = findPropertyIndex( "comment" );

        PropertyDescriptorEx pde = new PropertyDescriptorEx( "binding", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "BI" );
        add( index, pde, getResourceString( "PN_GLYCAN_BINDING" ), getResourceString( "PD_GLYCAN_BINDING" ) );

        pde = new PropertyDescriptorEx( "composition", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "CS" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_COMPOSITION" ), getResourceString( "PD_GLYCAN_COMPOSITION" ) );

        pde = new PropertyDescriptorEx( "compound", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "CP" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_COMPOUND" ), getResourceString( "PD_GLYCAN_COMPOUND" ) );

        pde = new PropertyDescriptorEx( "enzyme", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "EZ" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_ENZYME" ), getResourceString( "PD_GLYCAN_ENZYME" ) );

        pde = new PropertyDescriptorEx( "glycanClass", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "GC" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_GLYCAN_CLASS" ), getResourceString( "PD_GLYCAN_GLYCAN_CLASS" ) );

        pde = new PropertyDescriptorEx( "mass", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "MA" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_MASS" ), getResourceString( "PD_GLYCAN_MASS" ) );

        pde = new PropertyDescriptorEx( "ortholog", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "OR" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_ORTHOLOG" ), getResourceString( "PD_GLYCAN_ORTHOLOG" ) );

        pde = new PropertyDescriptorEx( "pathways", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "PW" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_PATHWAY" ), getResourceString( "PD_GLYCAN_PATHWAY" ) );

        pde = new PropertyDescriptorEx( "reaction", beanClass );
        HtmlPropertyInspector.setDisplayName( pde, "RE" );
        add( ++index, pde, getResourceString( "PN_GLYCAN_REACTION" ), getResourceString( "PD_GLYCAN_REACTION" ) );
    }
}
