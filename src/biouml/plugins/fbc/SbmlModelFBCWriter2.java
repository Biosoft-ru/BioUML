package biouml.plugins.fbc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.plugins.fbc.SbmlModelFBCReader2.GeneAssociations;
import biouml.plugins.fbc.SbmlModelFBCReader2.GeneProduct;
import biouml.plugins.sbml.SbmlPackageWriter;
import biouml.plugins.sbml.extensions.RdfExtensionReader;
import biouml.standard.diagram.DiagramUtility;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class SbmlModelFBCWriter2 extends SbmlPackageWriter implements FbcConstant
{
    RdfExtensionReader rdfReader = new RdfExtensionReader();//TODO: use registry instead

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
        writeListOfObjectives( diagram, modelElement );
        writeListOfGeneProducts( diagram, modelElement );
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
        return FBC_VERSION2;
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

    private void writeListOfGeneProducts(Diagram sourceDiagram, Element model)
    {
        DynamicProperty dp = sourceDiagram.getAttributes().getProperty( FBC_LIST_OF_GENE_PRODUCTS );
        if( dp == null || ! ( dp.getValue() instanceof List<?> ) )
            return;
        List<?> list = (List<?>)dp.getValue();
        if( list.isEmpty() )
            return;

        Element listElement = document.createElement( FBC_LIST_OF_GENE_PRODUCTS );
        for( Object obj : list )
        {
            if( obj instanceof GeneProduct )
            {
                GeneProduct gp = (GeneProduct)obj;
                Element gpElement = document.createElement( FBC_GENE_PRODUCT );
                gpElement.setAttribute( FBC_ID, gp.id );
                if( !gp.name.isEmpty() )
                    gpElement.setAttribute( FBC_NAME, gp.name );
                gpElement.setAttribute( FBC_LABEL, gp.label );
                if( !gp.species.isEmpty() )
                    gpElement.setAttribute( FBC_ASSOCIATED_SPECIES, gp.species );
                //                writeSBOTerm( gpElement, null );

                Element[] elements = rdfReader.writeElement( gp.refs, document );
                if( elements != null && elements.length > 0 )
                {
                    Element annotationElement = document.createElement( ANNOTATION_ELEMENT );
                    for( Element element : elements )
                        annotationElement.appendChild( element );
                    gpElement.appendChild( annotationElement );
                }
                listElement.appendChild( gpElement );
            }
        }
        if( listElement.hasChildNodes() )
            model.appendChild( listElement );
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
        Object ga = reaction.getAttributes().getValue( GENE_PRODUCT_ASSOCIATION_ATTR );
        if( ga != null && ga instanceof GeneAssociations )
        {
            Element geneProductAssociation = document.createElement( FBC_GENE_PRODUCT_ASSOCIATION );
            writeGeneAssociations( geneProductAssociation, (GeneAssociations)ga );
            reactionElement.appendChild( geneProductAssociation );
        }
    }

    protected void writeGeneAssociations(Element element, GeneAssociations ga)
    {
        if( ga.type != null )
        {
            Element child = document.createElement( ga.type );
            for( GeneAssociations inner : ga.innerAssociations )
                writeGeneAssociations( child, inner );
            element.appendChild( child );
        }
        else
        {
            Element ref = document.createElement( FBC_GENE_PRODUCT_REF );
            ref.setAttribute( FBC_GENE_PRODUCT, ga.geneRef );
            element.appendChild( ref );
        }
    }

    @Override
    public String getPackageName()
    {
        return "fbc";
    }
}
