package biouml.model.dynamics.plot;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ExperimentBeanInfo extends BeanInfoEx2<Experiment>
{
    public ExperimentBeanInfo()
    {
        super(Experiment.class);
        setHideChildren(true);
        setCompositeEditor("path;nameX;nameY;title;pen", new java.awt.GridLayout(1, 5));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("path", beanClass, TableDataCollection.class));
        property( "nameX" ).tags( bean -> bean.columns() ).add();
        property( "nameY" ).tags( bean -> bean.columns() ).add();            
        add( "title" );
        add("pen", PenEditor.class);
    }
}