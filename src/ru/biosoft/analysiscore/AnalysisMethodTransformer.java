package ru.biosoft.analysiscore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.AbstractFileTransformer;

public class AnalysisMethodTransformer extends AbstractFileTransformer<AnalysisMethodElement>
{
    @Override
    public Class<AnalysisMethodElement> getOutputType()
    {
        return AnalysisMethodElement.class;
    }

    @Override
    public AnalysisMethodElement load(File input, String name, DataCollection<AnalysisMethodElement> origin) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( input ))
        {
            return new AnalysisMethodReader( name, fis ).read( origin );
        }
    }

    @Override
    public void save(File output, AnalysisMethodElement element) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( output ))
        {
            new AnalysisMethodWriter( fos ).write( element );
        }
    }
}