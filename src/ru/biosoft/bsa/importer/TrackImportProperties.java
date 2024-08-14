package ru.biosoft.bsa.importer;

import java.util.Properties;

import ru.biosoft.bsa.GenomeSelector;

public class TrackImportProperties extends GenomeSelector
{
    private static final long serialVersionUID = 1L;
    
    private Properties otherProperties = new Properties();
    private int skipLines = 0;
    private boolean ignoreSiteProperties = false;
    
    public Properties getTrackProperties()
    {
        return otherProperties;
    }
    
    public int getSkipLines()
    {
        return skipLines;
    }
    
    public void setSkipLines(int n)
    {
        Object oldValue = skipLines;
        skipLines = n;
        firePropertyChange("skipLines", oldValue, skipLines);
    }
    
    public boolean isIgnoreSiteProperties()
    {
        return ignoreSiteProperties;
    }
    
    public void setIgnoreSiteProperties(boolean ignoreSiteProperties)
    {
        boolean oldValue = ignoreSiteProperties;
        this.ignoreSiteProperties = ignoreSiteProperties;
        firePropertyChange("ignoreSiteProperties", oldValue, ignoreSiteProperties);
    }
}