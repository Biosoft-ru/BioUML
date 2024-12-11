package biouml.plugins.virtualcell.core;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import biouml.plugins.virtualcell.diagram.MetabolismProperties;
import biouml.plugins.virtualcell.diagram.PopulationProperties;
import biouml.plugins.virtualcell.diagram.ProteinDegradationProperties;
import biouml.plugins.virtualcell.diagram.TranscriptionProperties;
import biouml.plugins.virtualcell.diagram.TranslationProperties;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;

public class ProcessDataCollection extends VectorDataCollection<DataElement>
{
    private TreeMap<String, DataElement> elements = new TreeMap<>();

    public ProcessDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super( parent, properties );
        
        elements.put( "Metabolism", new MetabolismProperties("Metabolism") );
        elements.put( "Population", new PopulationProperties("Population") );
        elements.put( "Translation", new TranslationProperties("Translation") );
        elements.put( "Protein Degradation", new ProteinDegradationProperties("Protein Degradation") );
        elements.put( "Transcription", new TranscriptionProperties("Transcription") );
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public @Nonnull Class<DataElement> getDataElementType()
    {
        return ru.biosoft.access.core.DataElement.class;
    }

    @Override
    public int getSize()
    {
        return elements.size();
    }

    @Override
    protected DataElement doGet(String name)
    {
        return elements.get( name );
    }

    @Override
    public @Nonnull Iterator<DataElement> iterator()
    {
        return elements.values().iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return elements.keySet().stream().toList();
    }
}