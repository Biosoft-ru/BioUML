package ru.biosoft.analysis;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType.BooleanType;

public class RunAnalysis extends AnalysisMethodSupport<RunAnalysisParameters>
{
    public RunAnalysis(DataCollection origin, String name)
    {
        super( origin, name, new RunAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut()
    {
        try
        {
            AnalysisMethodInfo methodInfo = parameters.getAnalysis().getDataElement( AnalysisMethodInfo.class );
            AnalysisMethod method = methodInfo.createAnalysisMethod();
            AnalysisParameters innerParameters = method.getParameters();
            ComponentModel model = ComponentFactory.getModel( innerParameters, Policy.DEFAULT, true );
            ComponentModel modelGlobal =  ComponentFactory.getModel( innerParameters, Policy.DEFAULT, true );
            
            Property parametersProperty = model.findProperty( "parameters" ); //hack for CWLAnalysis
            if( parametersProperty != null )
                model = ComponentFactory.getModel( parametersProperty.getValue(), Policy.DEFAULT, true );

            TableDataCollection table = parameters.getParametersTable().getDataElement( TableDataCollection.class );
            String rowName = parameters.getRow();

            if (rowName.contains( "/" )) //in some cases (e.g. in workflow we may receive RowDataElementPath path instead of name            
                rowName = rowName.substring( rowName.lastIndexOf( "/" )+1 );
          
            if (!table.contains( rowName ))
            {
                log.info( "Analysis RunAnalysis was aborted:" );
                log.info( "Row "+rowName+" was not found in table "+ table.getName() );
                return null;
            }
            
            RowDataElement row = table.get( rowName );

            Map<String, Property> paramsMap = new HashMap<>();
            for( int i = 0; i < model.getPropertyCount(); i++ )
            {
                Property p = model.getPropertyAt( i );
                paramsMap.put( p.getName(), p );
            }
            
            if (modelGlobal!= null)
            {
                for( int i = 0; i < modelGlobal.getPropertyCount(); i++ )
                {
                    Property p = modelGlobal.getPropertyAt( i );
                    paramsMap.put( p.getName(), p );
                }
            }

            Object[] values = row.getValues();
            String[] names = TableDataCollectionUtils.getColumnNames( table );
            //            Map<String, Object> result = new HashMap<>();
            for( int i = 0; i < names.length; i++ )
            {

                Object obj = values[i];
                if (obj == null)
                {
                    log.info( "No value in column " + names[i] + " was found!" );
                    continue;
                }
                Property p = model.findProperty( names[i] );
                if( p == null && modelGlobal != null)                
                    p = modelGlobal.findProperty(names[i]);
                
                if (p == null)
                {
                    log.info( "Parameter " + names[i] + " was not found in analysis!" );
                    continue;
                }
                Class clazz = p.getValueClass();
                Object value = null;
                if( clazz.isAssignableFrom( DataElementPath.class ) )
                {
                    value = DataElementPath.createInstance( values[i].toString() );
                }
                else if( clazz.isAssignableFrom( Boolean.class ) && obj instanceof BooleanType )
                {
                    value = ( (BooleanType)obj ).getValue();
                }
                else if( clazz.isAssignableFrom( Integer.class ) && obj instanceof Integer )
                {
                    value = obj;
                }
                else if (clazz.isAssignableFrom( String.class ))
                {
                    value = obj.toString();
                }
                else
                {
                    Constructor c = clazz.getConstructor( String.class );
                    if( c == null )
                    {
                        log.info( "Can not set value to property" + p.getName() + " No constructor from String for class "
                                + p.getValueClass() );
                        continue;
                    }
                    value = c.newInstance( values[i] );
                }
                paramsMap.remove( names[i] );
                p.setValue( value );
                log.info( "Parameter " + names[i] + " of type " + p.getValueClass().getName() + ". New value: " + values[i] );
            }

            for( Entry<String, Property> e : paramsMap.entrySet() )
            {
                log.info( "Could not find parameter " + e.getKey() + "! Deafult value " + e.getValue().getValue() + " was used." );
            }

            method.setLogger( this.log );
            method.getJobControl().run();

            return null;
        }
        catch( Exception ex )
        {
            log.log( java.util.logging.Level.SEVERE, "Error during analysis init: " + ex.getMessage(), ex );
            return null;
        }
    }
}
//}