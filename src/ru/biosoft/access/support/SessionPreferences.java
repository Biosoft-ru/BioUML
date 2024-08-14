package ru.biosoft.access.support;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

public class SessionPreferences extends Preferences
{
    private boolean addToParent = true;

    public SessionPreferences()
    {
        super();
    }

    @Override
    public Object getValue(String name)
    {
        Object value = super.getValue(name);
        if( value == null )
        {
            value = SessionPreferencesManager.getPreferences().getValue(name);
        }
        return value;
    }

    @Override
    public void add(DynamicProperty property)
    {
        if( isAddToParent() )
            super.add(property);
        else
        {
            Preferences preferences = SessionPreferencesManager.getPreferences();
            preferences.add(property);
            SessionPreferencesManager.saveCurrentSessionPreferences(preferences);
        }
    }

    @Override
    public void addValue(String name, Object value, String displayName, String shortDescription)
    {
        if( isAddToParent() )
            super.addValue(name, value, displayName, shortDescription);
        else
        {
            Preferences preferences = SessionPreferencesManager.getPreferences();
            DynamicProperty property = preferences.getProperty(name);
            if( property != null )
                preferences.setValue(name, value);
            else
            {
                preferences.addValue(name, value, displayName, shortDescription);
                SessionPreferencesManager.saveCurrentSessionPreferences(preferences);
            }
        }
    }

    public boolean isAddToParent()
    {
        return addToParent;
    }

    public void setAddToParent(boolean addToParent)
    {
        this.addToParent = addToParent;
    }

    @Override
    public void load(String fileName)
    {
        super.load(fileName);
        setAddToParent(false);
    }

    @Override
    public DynamicProperty getProperty(String name)
    {
        DynamicProperty prop = super.getProperty(name);
        if( prop == null )
            prop = SessionPreferencesManager.getPreferences().getProperty(name);
        return prop;
    }

    @Override
    protected DynamicProperty findProperty(String name)
    {
        DynamicProperty prop = super.findProperty(name);
        if( prop == null )
            prop = SessionPreferencesManager.getPreferences().getProperty(name);
        return prop;
    }
}
