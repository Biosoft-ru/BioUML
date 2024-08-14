package biouml.workbench.graphsearch.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.graphsearch.GraphSearchViewPart;
import biouml.workbench.graphsearch.SearchElement;

public class AddDiagramElementAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(AddDiagramElementAction.class.getName());

    public static final String KEY = "Add diagram element";

    public static final String DIAGRAM_DOCUMENT = "DiagramDocument";
    public static final String SEARCH_PANE = "SearchPane";

    public AddDiagramElementAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        DiagramDocument document = (DiagramDocument)getValue(DIAGRAM_DOCUMENT);
        if( document == null )
        {
            log.log(Level.SEVERE, "Diagram is undefined");
            return;
        }

        GraphSearchViewPart searchPane = (GraphSearchViewPart)getValue(SEARCH_PANE);
        if( searchPane == null )
        {
            log.log(Level.SEVERE, "Search view part is undefined");
            return;
        }

        Object[] selectedModels = ((DiagramDocument)document).getDiagramViewPane().getSelectionManager().getSelectedModels();
        if( selectedModels != null )
        {
            String species = searchPane.getOptions().getSpecies().getLatinName();

            List<Base> kernels = StreamEx.of(selectedModels).select( Node.class ).map( Node::getKernel ).map( kernel -> {
                DynamicProperty dp = kernel.getAttributes().getProperty(Util.ORIGINAL_PATH);
                if (dp != null)
                {
                    return DataElementPath.create((String)dp.getValue()).getDataElement(Base.class);
                }
                return kernel;
            }).toList();
            DataElementPath modulePath = StreamEx.of(kernels).findFirst().map( Module::optModulePath ).orElse( null );
            StreamEx.of( kernels ).forEach( k -> {
                SearchElement se = new SearchElement( k );
                se.setUse( true );
                se.setRelationType( species );
                searchPane.addInputElement( se );
            } );

            if( modulePath != null )
            {
                TargetOptions targetOptions = searchPane.getOptions().getTargetOptions();
                Optional<CollectionRecord> ocr = targetOptions.collections().findFirst( cr -> cr.getPath().equals( modulePath ) );
                if(ocr.isPresent())
                {
                    ocr.get().setUse( true );
                } else
                {
                    targetOptions.setCollections( targetOptions.collections().append( new CollectionRecord( modulePath, true ) )
                            .toArray( CollectionRecord[]::new ) );
                }
                searchPane.invalidateOptions();
            }
        }
    }
}
