package biouml.plugins.sbml;

import org.w3c.dom.Element;

import biouml.model.DiagramType;

public class SbmlModelReader_32 extends SbmlModelReader_31
{
    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        //for now only comp package implies separate diagram type
        for( SbmlPackageReader reader : packageReaders )
        {
            DiagramType type = reader.getDiagramType();
            if( type != null )
                return type;
        }
        return new SbmlDiagramType_L3v2();
    }
}