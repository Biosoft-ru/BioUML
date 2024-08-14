package biouml.plugins.obo;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.biohub.BioHub;
import biouml.model.Module;
import biouml.standard.StandardQueryEngine;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;

public class OboQueryEngine extends StandardQueryEngine
{
    @Override
    protected DataCollection<Reaction> getReactions(Module module, Base kernel, int direction) throws Exception
    {
        return null;
    }

    @Override
    protected DataCollection<SemanticRelation> getSemanticRelations(Module module, Base kernel, int direction) throws Exception
    {
        DataCollection<?> parent = (DataCollection<?>)module.get( Module.DATA );
        DataCollection<SemanticRelation> relations = new VectorDataCollection<>( parent, new Properties() );
        String kernelName = kernel.getOrigin().getName()+"/"+kernel.getName();

        DataCollection<SemanticRelation> dc = (DataCollection<SemanticRelation>) ( parent.get( "relations" ) );
        for(SemanticRelation sr : dc)
        {
            String inputName = sr.getInputElementName();
            String outputName = sr.getOutputElementName();
            if( BioHub.DIRECTION_UP == direction )
            {
                if( kernelName.endsWith(inputName) )
                {
                    relations.put(sr);
                }
            }
            else if( BioHub.DIRECTION_DOWN == direction )
            {
                if( kernelName.endsWith(outputName) )
                {
                    relations.put(sr);
                }
            }
            else if( BioHub.DIRECTION_BOTH == direction || BioHub.DIRECTION_UNDEFINED == direction )
            {
                if( kernelName.endsWith(inputName) || kernelName.endsWith(outputName) )
                {
                    relations.put(sr);
                }
            }
        }
        return relations;
    }
}
