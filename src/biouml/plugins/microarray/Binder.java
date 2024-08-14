package biouml.plugins.microarray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.biohub.BioHub;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.util.ReferencesHandler;
import biouml.standard.type.DatabaseInfo;

import com.developmentontheedge.beans.DynamicProperty;

public class Binder
{
    protected Logger log = Logger.getLogger(Binder.class.getName());

    protected BioHub geneHub;

    public Binder(BioHub geneHub)
    {
        this.geneHub = geneHub;
    }

    public void bindDiagram(@Nonnull Diagram diagram, String experimentName)
    {
        try
        {
            ReferencesHandler.initDatabaseInfoMap(Module.getModule(diagram), databaseInfoMap);
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        for(DiagramElement de : diagram.recursiveStream())
        {
            de.getAttributes().remove("maLink");
            if( de instanceof Node && de.getKernel() != null )
            {
                List<String> ids = ReferencesHandler.idsForDataElement(de.getKernel(), databaseInfoMap, geneHub).collect(Collectors.toList());

                if( !ids.isEmpty() )
                {
                    MicroarrayLink ml = new MicroarrayLink();
                    ml.setMicroarray(experimentName);
                    ml.setGenes(ids);
                    try
                    {
                        de.getAttributes().add(new DynamicProperty("maLink", MicroarrayLink.class, ml));
                    }
                    catch( Exception ex )
                    {
                        log.log(Level.SEVERE, "can't add microarray to element " + de.getName());
                    }
                }
            }
        }
    }

    protected Map<String, DatabaseInfo> databaseInfoMap = new HashMap<>();
}
