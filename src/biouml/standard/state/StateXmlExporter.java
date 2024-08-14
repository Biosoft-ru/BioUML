package biouml.standard.state;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.jobcontrol.FunctionJobControl;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;

/**
 * Exports state into BioUML format (.xml)
 */
public class StateXmlExporter implements DataElementExporter
{
    @Override
    public int accept(DataElement de)
    {
        return de instanceof State ? ACCEPT_HIGH_PRIORITY : ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( State.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport( de, file, null );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        new StateTransformer().save( file, de.cast( State.class ));
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

}
