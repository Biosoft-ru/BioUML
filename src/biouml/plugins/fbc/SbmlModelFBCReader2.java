package biouml.plugins.fbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.XmlUtil;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.plugins.sbml.SbmlPackageReader;
import biouml.plugins.sbml.extensions.RdfExtensionReader;
import biouml.standard.type.DatabaseReference;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class SbmlModelFBCReader2 extends SbmlPackageReader implements FbcConstant
{
    RdfExtensionReader rdfReader = new RdfExtensionReader();//TODO: use registry instead

    @Override
    public void processSpecie(Element element, Node node) throws Exception
    {
        String charge = element.getAttribute( FBC_CHARGE );
        String chemicalFormula = element.getAttribute( FBC_CHEMICAL_FORMULA );

        node.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_CHARGE, String.class, charge ) );
        node.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_CHEMICAL_FORMULA, String.class, chemicalFormula ) );
    }

    @Override
    public void preprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        this.diagram = diagram;
    }

    @Override
    public void postprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        readFluxBounds( element );
        readListOfObjectives( element );
        readListOfGeneProducts( element );
    }

    @Override
    public String getPackageName()
    {
        return FBC_PACKAGE_NAME;
    }

    private void readListOfObjectives(Element model)
    {
        Element listOfObjectives = getElement( model, LIST_OF_OBJECTIVES );
        if( listOfObjectives == null )
            return;
        DynamicProperty dp = DPSUtils.createHiddenReadOnlyTransient( FBC_ACTIVE_OBJECTIVE, String.class,
                listOfObjectives.getAttribute( FBC_ACTIVE_OBJECTIVE ) );
        diagram.getAttributes().add( dp );
        XmlUtil.elements( listOfObjectives, OBJECTIVE ).forEach( e -> readObjective( e ) );
    }

    private void readObjective(Element element)
    {
        String objId = element.getAttribute( FBC_ID );
        String objName = element.getAttribute( FBC_NAME );
        String objType = element.getAttribute( FBC_TYPE );

        DynamicProperty dp = diagram.getAttributes().getProperty( FBC_LIST_OBJECTIVES );
        if( dp == null || !dp.getType().isAssignableFrom( HashMap.class ) )
        {
            HashMap<String, String> listObj = new HashMap<>();
            listObj.put( objId, objType );
            diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_LIST_OBJECTIVES, HashMap.class, listObj ) );
        }
        else
        {
            ( (HashMap<String, String>)dp.getValue() ).put( objId, objType );
        }

        Element listFluxObjectives = getElement( element, LIST_OF_FLUX_OBJECTIVES );
        if( listFluxObjectives == null )
            return;

        XmlUtil.elements( listFluxObjectives, FLUX_OBJECTIVE ).forEach( fluxObj -> readFluxObjective( fluxObj, objId, objName ) );
    }

    private void readFluxObjective(Element fluxObj, String idObj, String nameObj)
    {
        try
        {
            biouml.model.Node reactionNode = diagram.findNode( fluxObj.getAttribute( FBC_REACTION ) );
            if( reactionNode != null )
            {
                String idCoefficient = fluxObj.getAttribute( FBC_ID );
                String nameCoefficient = fluxObj.getAttribute( FBC_NAME );
                double coefficient = Double.parseDouble( fluxObj.getAttribute( FBC_COEFFICIENT ) );
                getFluxObjFunc( reactionNode ).addObjectiveCoefficient( idCoefficient, nameCoefficient, idObj, nameObj, coefficient );
            }
        }
        catch( NumberFormatException ex )
        {

        }
    }

    protected static FluxObjFunc getFluxObjFunc(Node reactionNode)
    {
        FluxObjFunc fluxObjective;

        DynamicPropertySet dps = reactionNode.getAttributes();
        DynamicProperty dp = dps.getProperty( FBC_OBJECTIVES );
        if( dp == null || !dp.getType().isAssignableFrom( FluxObjFunc.class ) )
        {
            fluxObjective = new FluxObjFunc();
            dps.add( DPSUtils.createHiddenReadOnlyTransient( FBC_OBJECTIVES, FluxObjFunc.class, fluxObjective ) );
        }
        else
        {
            fluxObjective = (FluxObjFunc)dp.getValue();
        }
        return fluxObjective;
    }

    protected void readFluxBounds(Element model)
    {
        Element fluxBoundsList = getElement( model, LIST_OF_FLUX_BOUNDS );
        if( fluxBoundsList != null )
            XmlUtil.elements( fluxBoundsList, FLUX_BOUND ).forEach( e -> readBound( e ) );
    }

    private void readBound(Element element)
    {
        try
        {
            String reaction = element.getAttribute( FBC_REACTION );
            biouml.model.Node reactionNode = diagram.findNode( reaction );
            if( reactionNode != null )
            {
                FluxBounds fluxBounds = getFluxBounds( reactionNode );
                String sign = element.getAttribute( FBC_OPERATION );
                String value = element.getAttribute( FBC_VALUE );
                fluxBounds.addBound( sign, value );
            }
        }
        catch( NumberFormatException ex )
        {

        }
    }

    protected static FluxBounds getFluxBounds(Node reactionNode)
    {
        FluxBounds fluxBounds;

        DynamicProperty dp = reactionNode.getAttributes().getProperty( FBC_BOUNDS );
        if( dp == null || !dp.getType().isAssignableFrom( FluxBounds.class ) )
        {
            fluxBounds = new FluxBounds();
            reactionNode.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_BOUNDS, FluxBounds.class, fluxBounds ) );
        }
        else
        {
            fluxBounds = (FluxBounds)dp.getValue();
        }
        return fluxBounds;
    }

    @Override
    public void processReaction(Element element, Node node) throws Exception
    {
        String charge = element.getAttribute( FBC_CHARGE );
        String chemicalFormula = element.getAttribute( FBC_CHEMICAL_FORMULA );

        node.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_CHARGE, String.class, charge ) );
        node.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_CHEMICAL_FORMULA, String.class, chemicalFormula ) );

        String lowerBound = element.getAttribute( FBC_LOWER_FLUX_BOUND );
        String upperBound = element.getAttribute( FBC_UPPER_FLUX_BOUND );

        if( lowerBound.equals( upperBound ) )
        {
            getFluxBounds( node ).addBound( FBC_EQUAL, lowerBound );
        }
        else
        {
            getFluxBounds( node ).addBound( FBC_GREATER_EQUAL, lowerBound );
            getFluxBounds( node ).addBound( FBC_LESS_EQUAL, upperBound );
        }

        Element geneProductAssociationList = getElement( element, FBC_GENE_PRODUCT_ASSOCIATION );
        if( geneProductAssociationList != null )
        {
            GeneAssociations ga = readGeneAssociations( geneProductAssociationList );
            node.getAttributes().add( new DynamicProperty( GENE_PRODUCT_ASSOCIATION_ATTR, GeneAssociations.class, ga ) );
        }
    }

    private void readListOfGeneProducts(Element model)
    {
        Element listOfGeneProducts = getElement( model, FBC_LIST_OF_GENE_PRODUCTS );
        if( listOfGeneProducts == null )
            return;

        List<GeneProduct> products = new ArrayList<>();
        for( Element element : XmlUtil.elements( listOfGeneProducts, FBC_GENE_PRODUCT ) )
        {
            String id = element.getAttribute( FBC_ID );
            String name = element.getAttribute( FBC_NAME );
            String label = element.getAttribute( FBC_LABEL );
            String species = element.getAttribute( FBC_ASSOCIATED_SPECIES );
            //            String metaid = element.getAttribute( METAID_ATTR );
            GeneProduct geneProduct = new GeneProduct( id, name, label, species );
            //            this.readSBOTerm( element, geneProduct.dps );
            Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
            List<DatabaseReference> references = new ArrayList<>();
            if( annotationElement != null )
            {
                for( Element child : XmlUtil.elements( annotationElement ) )
                {
                    if( child.getNodeName().equals( "rdf:RDF" ) )
                    {
                        DatabaseReference[] refs = rdfReader.readElement( child );
                        if( refs == null )
                            continue;
                        for( DatabaseReference ref : refs )
                            references.add( ref );
                    }
                }
            }
            geneProduct.refs = references.toArray( new DatabaseReference[references.size()] );
            products.add( geneProduct );
        }
        if( !products.isEmpty() )
            this.diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( FBC_LIST_OF_GENE_PRODUCTS, List.class, products ) );
    }

    private GeneAssociations readGeneAssociations(Element element)
    {
        GeneAssociations result = new GeneAssociations();
        String tag = element.getTagName();

        if( tag.equals( FBC_GENE_PRODUCT_ASSOCIATION ) )
            return readGeneAssociations( XmlUtil.elements( element ).iterator().next() );

        if( tag.equals( FBC_OR ) || tag.equals( FBC_AND ) )
        {
            result.type = tag;
            XmlUtil.elements( element ).forEach( e -> result.add( readGeneAssociations( e ) ) );
        }
        else if( tag.equals( FBC_GENE_PRODUCT_REF ) )
        {
            result.geneRef = element.getAttribute( FBC_GENE_PRODUCT );
        }
        return result;
    }

    public static class GeneProduct
    {
        DatabaseReference[] refs = new DatabaseReference[0];
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        String id;
        String name;
        String label;
        String species;

        public GeneProduct(String id, String name, String label, String species)
        {
            this.id = id;
            this.name = name;
            this.label = label;
            this.species = species;
        }
    }

    public static class GeneAssociations
    {
        String type = null;
        List<GeneAssociations> innerAssociations = new ArrayList<>();
        String geneRef;

        public void add(GeneAssociations association)
        {
            innerAssociations.add( association );
        }
    }


}
