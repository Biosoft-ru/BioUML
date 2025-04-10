package biouml.plugins.research.workflow.items;

import biouml.model.Diagram;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

/**
 * @author lan
 */
public class DataElementType
{
    public static class DataElementTypeSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return TYPES;
        }
    }

    private static final DataElementType[] TYPES =
    {
        new DataElementType("(any)", DataElement.class),
        new DataElementType("Diagram", Diagram.class),
        new DataElementType("Folder", FolderCollection.class),
        new DataElementType("Image", ImageDataElement.class),
        new DataElementType("Raw file", FileDataElement.class),
        new DataElementType("Sequence collection", SequenceCollection.class),
        new DataElementType("Species", Species.class),
        new DataElementType("Table", TableDataCollection.class),
        new DataElementType("Text", TextDataElement.class),
        new DataElementType("Track", Track.class)
    };
    
    private String name;
    private Class<? extends DataElement> type;

    /**
     * @param name
     * @param type
     */
    private DataElementType(String name, Class<? extends DataElement> type)
    {
        super();
        this.name = name;
        this.type = type;
    }

    /**
     * @return the type
     */
    public Class<? extends DataElement> getTypeClass()
    {
        return type;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public static DataElementType getType(Class <? extends DataElement> clazz)
    {
        if(clazz == null) return TYPES[0];
        for(DataElementType type: TYPES)
        {
            if(type.getTypeClass().equals(clazz)) return type;
        }
        return TYPES[0];
    }

    public static DataElementType getType(String name)
    {
        if(name == null) return TYPES[0];
        for(DataElementType type: TYPES)
        {
            if(type.toString().equals(name)) return type;
        }
        return TYPES[0];
    }
    
    public static DataElementType getTypeOrNull(String name)
    {
        for( DataElementType type : TYPES )
            if( type.toString().equals( name ) )
                return type;
        return null;
    }
}
