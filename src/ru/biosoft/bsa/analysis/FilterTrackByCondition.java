package ru.biosoft.bsa.analysis;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.plugins.javascript.JScriptContext;

/**
 * @author lan
 *
 */
@ClassIcon("resources/filter-track-by-condition.gif")
public class FilterTrackByCondition extends AnalysisMethodSupport<FilterTrackByConditionParameters>
{
    private static final class PropertiesScriptable extends ScriptableObject
    {
        private static final long serialVersionUID = 1L;
        @Override
        public String getClassName()
        {
            return "Properties";
        }
    }

    private Script script;

    public FilterTrackByCondition(DataCollection<?> origin, String name)
    {
        super(origin, name, new FilterTrackByConditionParameters());
    }

    public boolean isFilterAccept(Site site) throws Exception
    {
        Context context = JScriptContext.getContext();
        Scriptable scope = JScriptContext.getScope();
        if(site.getOriginalSequence() != null)
            scope.put("seq", scope, site.getOriginalSequence().getName());
        scope.put("from", scope, site.getFrom());
        scope.put("to", scope, site.getTo());
        scope.put("length", scope, site.getLength());
        scope.put("strand", scope, site.getStrand());
        scope.put("type", scope, site.getType());
        Scriptable properties = new PropertiesScriptable();
        scope.put("properties", scope, properties);
        Iterator<DynamicProperty> iterator = site.getProperties().propertyIterator();
        while(iterator.hasNext())
        {
            DynamicProperty property = iterator.next();
            properties.put(property.getName(), properties, property.getValue());
        }
        AtomicReference<Object> ref = new AtomicReference<>();
        BiosoftSecurityManager.runInSandbox( () -> {
            Object result = script.exec(context, scope);
            ref.set( result );
        } );
        Object result = ref.get();
        if( result instanceof Boolean )
        {
            return (Boolean)result;
        }
        return true;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmpty("condition");
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        log.info("Compiling the condition...");
        Context context = JScriptContext.getContext();
        try
        {
            script = context.compileString(parameters.getCondition(), "", 1, null);
        }
        catch( Exception e1 )
        {
            throw new IllegalArgumentException("Invalid condition: "+e1.getMessage());
        }
        log.info("Initializing resulting track...");
        Track input = parameters.getInputTrack().getDataElement(Track.class);
        final WritableTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), input, input.getClass() );
        jobControl.pushProgress(10, 99);
        log.info("Filtering...");
        jobControl.forCollection(DataCollectionUtils.asCollection(input.getAllSites(), Site.class),
                new Iteration<Site>()
        {
            @Override
            public boolean run(Site element)
            {
                try
                {
                    if(isFilterAccept(element))
                    {
                        try
                        {
                            result.addSite(element);
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, "Unable to add site: "+e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Filter error: "+e.getMessage());
                    return false;
                }
                return true;
            }
        });
        try
        {
            Scriptable scope = JScriptContext.getScope();
            for( String key : new String[] {"seq", "from", "to", "strand", "type", "properties"} )
                scope.delete(key);
        }
        catch(Exception e)
        {
        }
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            result.getOrigin().remove(result.getName());
            return null;
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save( result );
        log.info("Track created ("+result.getAllSites().getSize()+" sites)");
        jobControl.popProgress();
        return result;
    }
}
