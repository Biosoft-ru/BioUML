package ru.biosoft.bsa.analysis;

import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SiteModel;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.Property;

import one.util.streamex.StreamEx;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class ChangeProfileThresholdsParameters extends AbstractAnalysisParameters
{
    public static final String CUSTOM_TEMPLATE = "Custom...";
    private DataElementPath inputProfile, outputProfile;
    private String template = CUSTOM_TEMPLATE;
    private double threshold;
    private DynamicPropertySet properties = new DynamicPropertySetSupport();

    @PropertyName("Input profile")
    @PropertyDescription("Input profile")
    public DataElementPath getInputProfile()
    {
        return inputProfile;
    }

    public void setInputProfile(DataElementPath inputProfile)
    {
        Object oldValue = this.inputProfile;
        this.inputProfile = inputProfile;
        firePropertyChange("inputProfile", oldValue, inputProfile);
        if(oldValue == null || inputProfile == null || !oldValue.equals(inputProfile))
        {
            setTemplate(CUSTOM_TEMPLATE);
            if( inputProfile != null )
            {
                try
                {
                    DataCollection<SiteModel> profile = inputProfile.getDataCollection( SiteModel.class );
                    setProfileProperties(createProfileProperties(profile));
                    profile.stream().findAny().ifPresent( sm -> setThreshold( sm.getThreshold() ) );
                }
                catch( RepositoryException e )
                {
                    // ignore
                }
            } else
            {
                setProfileProperties( new DynamicPropertySetAsMap() );
            }
        }
    }

    private DynamicPropertySet createProfileProperties(DataCollection<SiteModel> profile)
    {
        Iterator<SiteModel> iterator = profile.iterator();
        DynamicPropertySetSupport dps = new DynamicPropertySetSupport();
        if(!iterator.hasNext())
            return dps;
        SiteModel sm = iterator.next();
        for(Property prop : BeanUtil.properties( sm ))
        {
            if(Number.class.isAssignableFrom( prop.getValueClass()) && !prop.getName().equals( "threshold" ))
            {
                dps.add( new DynamicProperty( "update_"+prop.getName(), "Update "+prop.getDisplayName(), Boolean.class, false ) );
                dps.add( new DynamicProperty( prop.getName(), prop.getDisplayName(), prop.getValueClass(), prop.getValue() ) );
            }
        }
        return dps;
    }

    @PropertyName("Output profile")
    @PropertyDescription("Path to resulting profile")
    public DataElementPath getOutputProfile()
    {
        return outputProfile;
    }

    public void setOutputProfile(DataElementPath outputProfile)
    {
        Object oldValue = this.outputProfile;
        this.outputProfile = outputProfile;
        firePropertyChange("outputProfile", oldValue, outputProfile);
    }

    @PropertyName("Template")
    @PropertyDescription("Site models will have threshold set according to selected template")
    public String getTemplate()
    {
        return template;
    }

    public void setTemplate(String template)
    {
        Object oldValue = this.template;
        this.template = template;
        firePropertyChange("template", oldValue, template);
    }

    @PropertyName("Cutoff")
    @PropertyDescription("All site models will have given cutoff set")
    public double getThreshold()
    {
        return threshold;
    }

    public void setThreshold(double threshold)
    {
        Object oldValue = this.threshold;
        this.threshold = threshold;
        firePropertyChange("threshold", oldValue, threshold);
    }

    @PropertyName("Profile properties")
    @PropertyDescription("Properties specific to given site mode")
    public DynamicPropertySet getProfileProperties()
    {
        return properties;
    }
    
    public void setProfileProperties(DynamicPropertySet properties)
    {
        Object oldValue = this.properties;
        this.properties = properties;
        firePropertyChange("properties", oldValue, properties);
    }
    
    public boolean isThresholdHidden()
    {
        return !template.equals(CUSTOM_TEMPLATE);
    }
    
    public boolean isProfilePropertiesHidden()
    {
        return isThresholdHidden() || properties.isEmpty();
    }
    
    public static class TemplateSelector extends GenericComboBoxEditor
    {
        private DataCollection<?> lastProfile = null;
        private String[] values = new String[] {CUSTOM_TEMPLATE};
        
        @Override
        protected Object[] getAvailableValues()
        {
            DataCollection<SiteModel> profile = null;
            try
            {
                profile = ((ChangeProfileThresholdsParameters)getBean()).getInputProfile().getDataCollection(SiteModel.class);
            }
            catch( Exception e )
            {
            }
            if(profile != lastProfile)
            {
                lastProfile = profile;
                if(profile == null)
                {
                    values = new String[] {CUSTOM_TEMPLATE};
                } else
                {
                    return StreamEx.of( profile.stream() ).flatCollection( SiteModel::getThresholdTemplates ).distinct().sorted()
                    		.append( CUSTOM_TEMPLATE ).toArray( String[]::new );
                }
            }
            return values;
        }
    }
}
