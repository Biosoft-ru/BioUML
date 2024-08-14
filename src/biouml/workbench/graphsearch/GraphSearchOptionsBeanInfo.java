package biouml.workbench.graphsearch;

import biouml.standard.type.Species;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

public class GraphSearchOptionsBeanInfo extends BeanInfoEx2<GraphSearchOptions>
{
    
    public GraphSearchOptionsBeanInfo ( )
    {
        super ( GraphSearchOptions.class, MessageBundle.class.getName ( ) );
    }

    @Override
    public void initProperties ( ) throws Exception
    {
        property( "searchType" ).readOnly().title( "PN_QUERY_TYPE" ).description( "PD_QUERY_TYPE" ).add();
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        
        property( "queryOptions" ).title( "PN_QUERY_OPTIONS" ).description( "PD_QUERY_OPTIONS" ).add();
        property( "targetOptions" ).title( "PN_TARGET_OPTIONS" ).description( "PD_TARGET_OPTIONS" ).add();
    }
}
