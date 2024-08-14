package ru.biosoft.analysis;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPathSet;

@SuppressWarnings ( "serial" )
public class IlluminaNormalizationParameters extends NormalizationParameters
{
    private DataElementPathSet illuminaFiles;
    
    
    public IlluminaNormalizationParameters()
    {
        super();
        setIlluminaFiles(new DataElementPathSet());
    }

    public DataElementPathSet getIlluminaFiles()
    {
        return illuminaFiles;
    }

    public void setIlluminaFiles(DataElementPathSet illuminaFiles)
    {
        Object oldValue = this.illuminaFiles;
        this.illuminaFiles = illuminaFiles;
        firePropertyChange("illuminaFiles", oldValue, this.illuminaFiles);
    }
    
    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"illuminaFiles"};
    }
}
