package ru.biosoft.access;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.file.FileTypeRegistry;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.access.generic.TransformerRegistry.TransformerInfo;
import ru.biosoft.access.security.BiosoftClassLoading;

public class AccessCoreInit
{
    private static Logger log = Logger.getLogger( AccessCoreInit.class.getName() );
    public static void init()
    {
        Environment.setClassLoading( new BiosoftClassLoading() );
        Environment.setIconManager( new BiosoftIconManager() );
        Environment.setValue( FileTypeRegistry.FILE_TYPE_REGISTRY_CLASS, BiosoftFileTypeRegistry.class );
        
        
        ru.biosoft.access.file.v1.Environment.INSTANCE = new ru.biosoft.access.file.v1.Environment()
        {

            @Override
            public DataElement createFileDataElement(String name, DataCollection<?> parent, File file)
            {
                return new FileDataElement( name, parent, file );
            }

            @Override
            public Class<? extends DataElement> getFileDataElementClass()
            {
                return FileDataElement.class;
            }
            
            public Transformer getTransformerForFile(File file)
            {
                try {
                    TransformerInfo ti = TransformerRegistry.detectFileTransformer( file.getName() );
                    if(ti == null)
                        return null;
                    Class<? extends Transformer> transformerClass = ti.getTransformerClass();
                    return transformerClass.newInstance();
                }catch(Exception e)
                {
                    log.log( Level.WARNING, "Can not find transformer for " + file.getAbsolutePath(), e );
                    return null;
                }
            }
            
            public Transformer getTransformerForDataElement(DataElement de)
            {
                try
                {
                    return TransformerRegistry.getBestTransformer( de, FileDataElement.class );
                }
                catch( Exception e )
                {
                    log.log( Level.WARNING, "Can not find transformer for " + de.getCompletePath(), e );
                    return null;
                }
            }

            @Override
            public File getFile(DataElement fde)
            {
                return ((FileDataElement)fde).getFile();
            }
            
        };
        
        TransformerRegistry.initTransformers();

        /*
        Transformers.registerTransformer( new FastaFileTransformer(), "fa" );
        Transformers.registerTransformer( new FastaFileTransformer(), "fna" );
        Transformers.registerTransformer( new FastaFileTransformer(), "fasta" );
        Transformers.registerTransformer( new FastaFileTransformer(), "ffn" );
        Transformers.registerTransformer( new FastaFileTransformer(), "fsa" );
        Transformers.registerTransformer( new GenbankFileTransformer(), "gbk" );
        Transformers.registerTransformer( new GFFFileTransformer(), "gff" );
        Transformers.registerTransformer( new VCFFileTransformer(), "vcf" );
        Transformers.registerTransformer( new FileTextTransformer(), "txt" );
        Transformers.registerTransformer( new FileTextTransformer(), "tsv" );
        Transformers.registerTransformer( new FileTextTransformer(), "tbl" );
        Transformers.registerTransformer( new FileTextTransformer(), "log" );
        Transformers.registerTransformer( new FileHtmlTransformer(), "html" );
        */
    }
}
