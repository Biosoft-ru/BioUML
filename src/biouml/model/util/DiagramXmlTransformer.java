package biouml.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import biouml.model.Diagram;
import biouml.model.Module;
import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.access.file.FileDataElement;

public class DiagramXmlTransformer extends AbstractFileTransformer<Diagram> implements  PriorityTransformer
{
    /**
     * Return class of output data element. Output data element stored in
     * transformed data collection.
     * 
     * @return Class of output data element.
     */
    @Override
    public Class<Diagram> getOutputType()
    {
        return Diagram.class;
    }

    @Override
    public Diagram load(File input, String name, DataCollection<Diagram> origin) throws Exception
    {
        // find module
        Module module = Module.optModule(origin);

        // transformation
        try (FileInputStream fis = new FileInputStream( input ))
        {
            return DiagramXmlReader.readDiagram(name, fis, null, getTransformedCollection(), module);
        }
    }

    @Override
    public void save(File output, Diagram diagram) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( output ))
        {
            DiagramXmlWriter.writeDiagram(diagram, fos);
        }
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        if(FileDataElement.class.isAssignableFrom( inputClass ) && output instanceof Diagram)
            return 10;
        return 0;
    }

    @Override
    public int getOutputPriority(String name)
    {
        return name.endsWith( ".dml" ) ? 2 : 0;
    }
}
