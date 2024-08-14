package biouml.workbench.graph.layouter;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Properties;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Layouter;

public class DiagramLayouterInfo
{
    protected DataElementPath diagramPath;
    protected String layouterName;
    protected Properties layouterProperties;
    protected DataCollection targetCollection;

    public DiagramLayouterInfo(DataElementPath diagramPath, String layouterName, Properties layouterProperties, DataCollection targetCollection)
    {
        this.diagramPath = diagramPath;
        this.layouterName = layouterName;
        this.layouterProperties = layouterProperties;
        this.targetCollection = targetCollection;
    }

    public DataElementPath getDiagramCompleteName()
    {
        return diagramPath;
    }

    public Layouter getLayouter() throws Exception
    {
        Layouter layouter = ClassLoading.loadSubClass( layouterName, Layouter.class ).newInstance();
        Iterator<?> iter = layouterProperties.keySet().iterator();
        while( iter.hasNext() )
        {
            String name = (String)iter.next();
            String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
            Method[] methods = layouter.getClass().getMethods();
            for( Method method : methods )
            {
                if( method.getName().equals(methodName) )
                {
                    Class[] types = method.getParameterTypes();
                    if( types.length == 1 )
                    {
                        if( types[0].getName().equals("int") || Integer.class.isAssignableFrom(types[0]) )
                        {
                            method.invoke(layouter, new Object[] {Integer.parseInt(layouterProperties.getProperty(name))});
                        }
                        else if( types[0].getName().equals("double") || Double.class.isAssignableFrom(types[0]) )
                        {
                            method.invoke(layouter, new Object[] {Double.parseDouble(layouterProperties.getProperty(name))});
                        }
                        else if( String.class.isAssignableFrom(types[0]) )
                        {
                            method.invoke(layouter, new Object[] {layouterProperties.getProperty(name)});
                        }
                    }
                    break;
                }
            }
        }
        return layouter;
    }

    public DataCollection getTargetCollection()
    {
        return targetCollection;
    }
}
