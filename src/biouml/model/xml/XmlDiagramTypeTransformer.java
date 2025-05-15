package biouml.model.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;

public class XmlDiagramTypeTransformer extends AbstractFileTransformer<XmlDiagramType>
{
    @Override
    public Class<XmlDiagramType> getOutputType()
    {
        return XmlDiagramType.class;
    }

    @Override
    public XmlDiagramType load(File input, String name, DataCollection<XmlDiagramType> origin) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( input ))
        {
            return new XmlDiagramTypeReader(name, fis).read(origin);
        }
    }

    @Override
    public void save(File output, XmlDiagramType element) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( output ))
        {
            new XmlDiagramTypeWriter(fos).write(element);
        }
    }
    
}
