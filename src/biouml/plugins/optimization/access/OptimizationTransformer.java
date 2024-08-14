package biouml.plugins.optimization.access;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import biouml.plugins.optimization.Optimization;

public class OptimizationTransformer extends AbstractFileTransformer<Optimization>
{
    /**
     * Return class of output data element. Output data element stored in
     * transformed data collection.
     * 
     * @return Class of output data element.
     */
    @Override
    public Class<Optimization> getOutputType()
    {
        return Optimization.class;
    }

    @Override
    public Optimization load(File input, String name, DataCollection<Optimization> origin) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( input ))
        {
            return new OptimizationReader(name, fis).read(origin);
        }
    }

    @Override
    public void save(File output, Optimization element) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( output ))
        {
            new OptimizationWriter(fos).write(element);
        }
    }
}
