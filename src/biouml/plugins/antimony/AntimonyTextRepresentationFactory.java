package biouml.plugins.antimony;

import biouml.model.Diagram;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.workbench.diagram.AbstractDiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentation;
import biouml.workbench.diagram.DiagramTextRepresentationFactory;

public class AntimonyTextRepresentationFactory extends DiagramTextRepresentationFactory
{

    @Override
    protected DiagramTextRepresentation create(Diagram diagram)
    {
        if( !AntimonyUtility.checkDiagramType(diagram) )
            return null;
        return new AntimonyTextRepresentation(diagram);
    }

    private static class AntimonyTextRepresentation extends AbstractDiagramTextRepresentation
    {
        private static AntimonyDiagramListener listener = new AntimonyDiagramListener();

        public AntimonyTextRepresentation(Diagram diagram)
        {
            super(diagram);
            listener.register(diagram);
        }

        @Override
        public String getContentType()
        {
            return "text/x-antimony";
        }

        @Override
        public String getContent()
        {
            String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            if( antimonyText == null )
            {
                AntimonyAstCreator antimony = new AntimonyAstCreator(diagram);
                AstStart start = antimony.getAST();
                AntimonyTextGenerator generator = new AntimonyTextGenerator(start);
                antimonyText = generator.generateText();
                AntimonyUtility.setAntimonyAttribute(diagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            }
            return antimonyText;
        }

        @Override
        public void doSetContent(String text)
        {
            AntimonyUtility.setAntimonyAttribute(diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR);
        }
    }
}
