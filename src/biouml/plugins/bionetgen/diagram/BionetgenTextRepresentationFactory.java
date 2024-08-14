package biouml.plugins.bionetgen.diagram;

import biouml.model.Diagram;
import biouml.workbench.diagram.AbstractDiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentationFactory;

public class BionetgenTextRepresentationFactory extends DiagramTextRepresentationFactory
{

    @Override
    protected DiagramTextRepresentation create(Diagram diagram)
    {
        if( !BionetgenUtils.checkDiagramType( diagram ) )
            return null;
        return new BionetgenTextRepresentation( diagram );
    }

    private static class BionetgenTextRepresentation extends AbstractDiagramTextRepresentation
    {
        private static BionetgenDiagramListener listener = new BionetgenDiagramListener();

        public BionetgenTextRepresentation(Diagram diagram)
        {
            super( diagram );
            listener.register( diagram );
        }

        @Override
        public String getContentType()
        {
            return "text/x-bionetgen";
        }

        @Override
        public String getContent()
        {
            String bngText = BionetgenUtils.getBionetgenAttr( diagram );
            if( bngText == null )
            {
                Bionetgen bionetgen = new Bionetgen( diagram );
                bngText = bionetgen.generateText();
                BionetgenUtils.setBionetgenAttr( diagram, bngText );
            }
            return bngText;
        }

        @Override
        public void doSetContent(String text)
        {
            BionetgenUtils.setBionetgenAttr( diagram, text );
        }
    }

}
