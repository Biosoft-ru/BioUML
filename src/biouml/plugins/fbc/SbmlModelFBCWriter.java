package biouml.plugins.fbc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.plugins.sbml.SbmlPackageWriter;
import biouml.standard.diagram.DiagramUtility;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class SbmlModelFBCWriter extends SbmlPackageWriter implements FbcConstant
{

    @Override
    protected void processSBML(Element sbmlElement, Diagram diagram)
    {
        sbmlElement.setAttribute( FBC_REQUIRED, "false" );
    }

    @Override
    protected void processModel(Element modelElement, Diagram diagram)
    {
        this.diagram = diagram;
        this.document = modelElement.getOwnerDocument();

        modelElement.setAttribute( FBC_STRICT, "false" );
        writeListOfBoundsElement( diagram, modelElement );
        writeListOfObjectives( diagram, modelElement );
    }

    @Override
    protected void processSpecie(Element speciesElement, Node species)
    {
        if( speciesElement != null )
        {
            try
            {
                DynamicPropertySet dps = species.getAttributes();
                String charge = dps.getValueAsString( FBC_CHARGE );
                String chemicalFormula = dps.getValueAsString( FBC_CHEMICAL_FORMULA );
                if( charge != null )
                    speciesElement.setAttribute( FBC_CHARGE, charge );
                if( chemicalFormula != null )
                    speciesElement.setAttribute( FBC_CHEMICAL_FORMULA, chemicalFormula );
            }
            catch( Exception e )
            {

            }
        }
    }

    @Override
    public String getNameSpace()
    {
        return "http://www.sbml.org/sbml/level3/version1/fbc/version2";
    }

    protected void writeListOfObjectives(Diagram sourceDiagram, Element model)
    {
        Element listOfObjectives = document.createElement( LIST_OF_OBJECTIVES );
        DynamicPropertySet dps = sourceDiagram.getAttributes();
        String activObj = dps.getValueAsString( FBC_ACTIVE_OBJECTIVE );
        Map<String, String> listObj = (Map<String, String>)dps.getValue( FBC_LIST_OBJECTIVES );
        listOfObjectives.setAttribute( FBC_ACTIVE_OBJECTIVE, activObj );
        if( listObj == null ) //TODO: check later what to do in this case.
            return;
        for( Entry<String, String> entry : listObj.entrySet() )
        {
            Element objective = document.createElement( OBJECTIVE );
            String idObjective = entry.getKey();
            objective.setAttribute( FBC_ID, idObjective );
            objective.setAttribute( FBC_TYPE, entry.getValue() );
            List<Node> reactionNodes = DiagramUtility.getReactionNodes( sourceDiagram );
            Element listOfFluxObjectives = document.createElement( LIST_OF_FLUX_OBJECTIVES );
            for( Node reactionNode : reactionNodes )
            {
                DynamicProperty dp = reactionNode.getAttributes().getProperty( FBC_OBJECTIVES );
                if( dp == null || !dp.getType().isAssignableFrom( FluxObjFunc.class ) )
                    continue;
                FluxObjFunc fluxObjFunc = (FluxObjFunc)dp.getValue();
                if( fluxObjFunc.idObj.contains( idObjective ) )
                {
                    int index = fluxObjFunc.idObj.indexOf( idObjective );
                    Double coef = fluxObjFunc.coefficient.get( index );
                    if( coef == 0 )
                        continue;
                    Element fluxObjective = document.createElement( FLUX_OBJECTIVE );
                    String nameObj = fluxObjFunc.nameObj.get( index );
                    if( ! ( nameObj == null || nameObj.isEmpty() ) && !objective.hasAttribute( FBC_NAME ) )
                        objective.setAttribute( FBC_NAME, nameObj );
                    String coefName = fluxObjFunc.nameCoefficient.get( index );
                    if( ! ( coefName == null || coefName.isEmpty() ) )
                        fluxObjective.setAttribute( FBC_NAME, coefName );
                    String coefId = fluxObjFunc.idCoefficient.get( index );
                    if( ! ( coefId == null || coefId.isEmpty() ) )
                        fluxObjective.setAttribute( FBC_ID, coefId );
                    fluxObjective.setAttribute( FBC_REACTION, reactionNode.getName() );
                    fluxObjective.setAttribute( FBC_COEFFICIENT, coef.toString() );
                    listOfFluxObjectives.appendChild( fluxObjective );
                }
            }
            objective.appendChild( listOfFluxObjectives );
            listOfObjectives.appendChild( objective );
        }
        model.appendChild( listOfObjectives );
    }

    @Override
    protected void processReaction(Element reactionElement, Node reaction)
    {
        Object obj = reaction.getAttributes().getValue( FBC_BOUNDS );
        if( obj != null && obj instanceof FluxBounds )
        {
            FluxBounds fluxBounds = (FluxBounds)obj;
            for( int i = 0; i < fluxBounds.sign.size(); i++ )
            {
                String sign = fluxBounds.sign.get( i );
                String value = fluxBounds.value.get( i );
                if( !value.isEmpty() )
                {
                    switch( sign )
                    {
                        case FBC_GREATER_EQUAL:
                        {
                            reactionElement.setAttribute( FBC_LOWER_FLUX_BOUND, value );
                            break;
                        }
                        case FBC_LESS_EQUAL:
                        {
                            reactionElement.setAttribute( FBC_UPPER_FLUX_BOUND, value );
                            break;
                        }
                        case FBC_EQUAL:
                        {
                            reactionElement.setAttribute( FBC_LOWER_FLUX_BOUND, value );
                            reactionElement.setAttribute( FBC_UPPER_FLUX_BOUND, value );
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }
    }

    protected void writeListOfBoundsElement(Diagram sourceDiagram, Element model)
    {
        Element listOfFluxBoundsElement = document.createElement( LIST_OF_FLUX_BOUNDS );
        List<Node> reactionNodes = DiagramUtility.getReactionNodes( sourceDiagram );
        for( Node reactionNode : reactionNodes )
        {
            DynamicProperty dp = reactionNode.getAttributes().getProperty( FBC_BOUNDS );
            if( dp == null || !dp.getType().isAssignableFrom( FluxBounds.class ) )
                continue;
            FluxBounds fluxBounds = (FluxBounds)dp.getValue();
            for( int i = 0; i < fluxBounds.sign.size(); i++ )
            {
                Element fluxBoundElement = document.createElement( FLUX_BOUND );
                //                String id = fluxBounds.id.get( i );
                //                if( id != null && !id.isEmpty() )
                //                    fluxBoundElement.setAttribute( FBC_ID, id );
                //                String name = fluxBounds.name.get( i );
                //                if( name != null && !name.isEmpty() )
                //                    fluxBoundElement.setAttribute( FBC_NAME, name );
                fluxBoundElement.setAttribute( FBC_REACTION, reactionNode.getName() );
                fluxBoundElement.setAttribute( FBC_OPERATION, fluxBounds.sign.get( i ) );
                fluxBoundElement.setAttribute( FBC_VALUE, fluxBounds.value.get( i ).toString() );
                listOfFluxBoundsElement.appendChild( fluxBoundElement );
            }
        }
        model.appendChild( listOfFluxBoundsElement );
    }

    @Override
    public String getPackageName()
    {
        return "fbc";
    }
}
