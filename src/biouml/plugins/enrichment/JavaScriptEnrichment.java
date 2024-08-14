package biouml.plugins.enrichment;

import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.analysiscore.javascript.JavaScriptAnalysisHost;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName("enrichment")
@PropertyDescription("Functional enrichment-related analyses")
public class JavaScriptEnrichment extends JavaScriptAnalysisHost
{
    // TODO: revive custom methods in host objects
    @PropertyDescription("Returns list of available classifications")
    public static String[] getClassifications()
    {
        return BioHubRegistry
                .bioHubs( new TargetOptions( new CollectionRecord( FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD, true ) ) )
                .map( Object::toString ).toArray( String[]::new );
    }
}
