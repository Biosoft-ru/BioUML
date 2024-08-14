package ru.biosoft.access;

import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.util.LazyValue;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

/**
 * Utility class to support correct importer selection by display name
 * @author manikitos
 */
public class ImporterFormat implements SerializableAsText
{
    public static final ImporterFormat FORMAT_AUTO = new ImporterFormat( "(auto)" );

    private String format;
    private String displayName;

    public ImporterFormat(String format)
    {
        this( format, format );
    }
    public ImporterFormat(String format, String displayName)
    {
        this.format = format;
        this.displayName = displayName;
    }
    public ImporterFormat(ImporterInfo importer)
    {
        this( importer.getFormat(), importer.getDisplayName() );
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    public String getFormat()
    {
        return format;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getAsText()
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put( "format", format );
            json.put( "displayName", displayName );
        }
        catch( JSONException e )
        {
        }
        return json.toString();
    }

    @Override
    public int hashCode()
    {
        int fHC = format == null ? 0 : format.hashCode();
        int dnHC = displayName == null ? 0 : displayName.hashCode();
        return fHC + 31 * dnHC;
    }
    @Override
    public boolean equals(Object obj)
    {
        if( obj == null || ! ( obj instanceof ImporterFormat ) )
            return false;
        ImporterFormat otherFormat = (ImporterFormat)obj;
        return Objects.equals( format, otherFormat.format ) && Objects.equals( displayName, otherFormat.displayName );
    }

    public static ImporterFormat createInstance(String str)
    {
        JSONObject json = new JSONObject( str );
        return new ImporterFormat( json.getString( "format" ), json.getString( "displayName" ) );
    }

    public static class DefaultImporterFormatEditor extends GenericComboBoxEditor
    {
        protected final LazyValue<ImporterFormat[]> formats = getFormats();
        protected LazyValue<ImporterFormat[]> getFormats()
        {
            return new LazyValue<>( () -> DataElementImporterRegistry.importers().sortedBy( ImporterInfo::getFormat )
                    .map( ImporterFormat::new ).toArray( ImporterFormat[]::new ) );
        }

        @Override
        public void setAsText(String newValue) throws IllegalArgumentException
        {
            setValue( parse( newValue ) );
        }
        private ImporterFormat parse(String str)
        {
            for( ImporterFormat format : getAvailableValues() )
            {
                if( format.getDisplayName().equals( str ) )
                    return format;
            }
            //TODO: think what to call here, maybe IllegalArgumentException
            return new ImporterFormat( str );
        }
        @Override
        protected ImporterFormat[] getAvailableValues()
        {
            return formats.get();
        }
    }
}
