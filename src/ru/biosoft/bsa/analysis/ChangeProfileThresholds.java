package ru.biosoft.bsa.analysis;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SiteModelTransformedCollection;
import ru.biosoft.bsa.transformer.SiteModelTransformer;

/**
 * @author lan
 */
@ClassIcon( "resources/change_profile_thresholds.gif" )
public class ChangeProfileThresholds extends AnalysisMethodSupport<ChangeProfileThresholdsParameters>
{
    public ChangeProfileThresholds(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new ChangeProfileThresholdsParameters());
    }

    @Override
    public DataCollection<SiteModel> justAnalyzeAndPut() throws Exception
    {
        DataCollection<SiteModel> result = SiteModelTransformer.createCollection(parameters.getOutputProfile());
        int total = 0, copied = 0;
        jobControl.setPreparedness(5);
        DynamicPropertySet properties = parameters.getProfileProperties();
        for(SiteModel model: parameters.getInputProfile().getDataCollection(SiteModel.class))
        {
            SiteModel clone = model.clone(result, model.getName());
            total++;
            if(parameters.getTemplate().equals(ChangeProfileThresholdsParameters.CUSTOM_TEMPLATE))
            {
                clone.setThreshold(parameters.getThreshold());
                for(DynamicProperty prop : properties)
                {
                    if(prop.getName().startsWith( "update_" ) && Boolean.TRUE.equals( prop.getValue()))
                    {
                        String name = prop.getName().substring( "update_".length() );
                        updateProperty(clone, name, properties.getValue( name ) );
                    }
                }
            } else
            {
                try
                {
                    clone.setThresholdTemplate(parameters.getTemplate());
                }
                catch(Exception e)
                {
                    result.remove(clone.getName());
                    continue;
                }
            }
            result.put(clone);
            copied++;
        }
        if(copied == 0)
        {
            log.log(Level.SEVERE, total == 0?"No site models copied: result is not created":"No site models support template '"+parameters.getTemplate()+"', thus result is not created.");
            parameters.getOutputProfile().remove();
            return null;
        }
        if(copied < total)
        {
            log.warning("Only "+copied+" of "+total+" site model(s) were copied. The rest ones don't support template '"+parameters.getTemplate()+"'");
        }
        parameters.getOutputProfile().save(result);
        jobControl.setPreparedness(100);
        return result;
    }

    private void updateProperty(SiteModel sm, String name, Object value)
    {
        try
        {
            Class<?> clazz = value.getClass();
            if( clazz == Double.class )
                clazz = double.class;
            else if( clazz == Float.class )
                clazz = float.class;
            else if( clazz == Integer.class )
                clazz = int.class;
            else if( clazz == Boolean.class )
                clazz = boolean.class;
            else if( clazz == Long.class )
                clazz = long.class;
            // Cannot use beans here as for some reason SiteModel properties are readonly
            sm.getClass().getMethod( "set" + name.substring( 0, 1 ).toUpperCase( Locale.ENGLISH ) + name.substring( 1 ), clazz )
                    .invoke( sm, value );
        }
        catch( IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e )
        {
            // ignore
        }
    }
}
