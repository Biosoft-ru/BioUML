package biouml.plugins.sbml;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.DPSUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;

public class SbmlModelReader_31 extends SbmlModelReader_24
{
    protected List<SbmlPackageReader> packageReaders = new ArrayList<>();

    public SbmlModelReader_31()
    {
        log = Logger.getLogger( SbmlModelReader_31.class.getName() );
    }

    public List<SbmlPackageReader> getPackageReaders()
    {
        return packageReaders;
    }

    public void initPackageReaders(Document document, boolean modelDefinition)
    {
        packageReaders = SbmlPackageRegistry.getReaders( readPackages( document ) );

        for( SbmlPackageReader reader : packageReaders )        
            reader.setNewPaths( newPaths );
        
        if( !modelDefinition ) //default
            return;

        for( SbmlPackageReader reader : packageReaders )
            reader.setModelDefintion( modelDefinition );
    }

    public Diagram read(Document document, String name, DataCollection<?> origin, SbmlPackageReader ... packageReaders) throws Exception
    {
        for( SbmlPackageReader packageReader : packageReaders )
            this.packageReaders.add( packageReader );

        for( SbmlPackageReader reader : packageReaders )
            reader.preprocess( document, origin );
        return super.read( document, name, origin );
    }

    @Override
    public Diagram read(Document document, String name, DataCollection<?> origin) throws Exception
    {
        initPackageReaders( document, false );

        for( SbmlPackageReader reader : packageReaders )
            reader.preprocess( document, origin );
        return super.read( document, name, origin );
    }

    public Diagram readModelDefinition(Element element, String name) throws Exception
    {
        for( SbmlPackageReader reader : packageReaders )
            reader.preprocess( element.getOwnerDocument(), null );
        this.modelName = name;
        Diagram diagram = readDiagram( element, null );
        if( shouldLayout )
            this.layout( diagram );
        return diagram;
    }

    @Override
    protected void readDiagramElements(Element element)
    {
        super.readDiagramElements( element );
        for( SbmlPackageReader reader : packageReaders )
        {
            try
            {
                reader.postprocessDiagram( element, diagram );
            }
            catch( Exception ex )
            {

            }
        }
    }

    @Override
    protected Diagram createDiagram(Element element, DataCollection<?> origin) throws Exception
    {
        Diagram diagram = super.createDiagram( element, origin );
        if( packageReaders.size() > 0 )
        {
            String[] packageNames = new String[packageReaders.size()];
            int i = 0;
            for( SbmlPackageReader reader : packageReaders )
            {
                reader.preprocessDiagram( element, diagram );
                packageNames[i++] = reader.getPackageName();
            }
            diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient( "Packages", String[].class, packageNames ) );
        }
        return diagram;
    }

    public static List<String> readPackages(Document document)
    {
        List<String> requiredPackages = new ArrayList<>();
        Element root = document.getDocumentElement();
        for( int i = 0; i < root.getAttributes().getLength(); i++ )
        {
            String nextAttribute = root.getAttributes().item( i ).toString();

            if( nextAttribute.startsWith( "xmlns:" ) )
            {
                nextAttribute = nextAttribute.replace( "xmlns:", "" );
                String packageName = nextAttribute.substring( 0, nextAttribute.indexOf( "=" ) );
                requiredPackages.add( packageName );
            }
        }
        return requiredPackages;
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        //for now only comp package implies separate digram type
        for( SbmlPackageReader reader : packageReaders )
        {
            DiagramType type = reader.getDiagramType();
            if( type != null )
                return type;
        }
        return new SbmlDiagramType_L3v1();
    }

    @Override
    public void readModelAttributes(Element element, Diagram diagram)
    {
        super.readModelAttributes( element, diagram );

        //reading of model properties, at this point all variables should be already in emodel
        if( element.hasAttribute( CONVERSION_FACTOR_ATTR ) )
        {
            String conversionFactorAttr = element.getAttribute( CONVERSION_FACTOR_ATTR );
            if( emodel.containsVariable( conversionFactorAttr ))
                emodel.setConversionFactor( conversionFactorAttr );
            else
                error( "UNKNOWN_CONVERSION_FACTOR", new String[] {modelName, conversionFactorAttr} );
        }

        if( element.hasAttribute(VOLUME_UNITS_ATTR) )
            emodel.setVolumeUnits(element.getAttribute(VOLUME_UNITS_ATTR));
        if( element.hasAttribute(SUBSTANCE_UNITS_ATTR) )
            emodel.setSubstanceUnits(element.getAttribute(SUBSTANCE_UNITS_ATTR));
        if( element.hasAttribute(TIME_UNITS_ATTR) )
            emodel.setTimeUnits(element.getAttribute(TIME_UNITS_ATTR));
        if( element.hasAttribute(AREA_UNITS_ATTR) )
            emodel.setAreaUnits(element.getAttribute(AREA_UNITS_ATTR));
        if( element.hasAttribute(EXTENT_UNITS_ATTR) )
            emodel.setExtentUnits(element.getAttribute(EXTENT_UNITS_ATTR));
        if( element.hasAttribute(LENGTH_UNITS_ATTR) )
            emodel.setLengthUnits(element.getAttribute(LENGTH_UNITS_ATTR));

        String conversionFactor = emodel.getConversionFactor();
        if( !conversionFactor.isEmpty() )
        {
            for( VariableRole role : emodel.getVariableRoles() )
            {
                boolean isCompartment = ( role.getDiagramElement().getKernel() != null && Type.TYPE_COMPARTMENT.equals(role
                        .getDiagramElement().getKernel().getType()) );

                if( role.getConversionFactor() == null && !isCompartment )
                    role.setConversionFactor(conversionFactor);
            }
        }
    }

    @Override
    protected void readLocalParameterList(Element parent, Node reaction)
    {
        Element parameterList = getElement( parent, LOCAL_PARAMETER_LIST_ELEMENT_L3 );
        if( !isValid( LOCAL_PARAMETER_LIST_ELEMENT_L3, parameterList, null ) || parameterList == null )
            return;

        NodeList list = parameterList.getElementsByTagName( LOCAL_PARAMETER_ELEMENT_L3 );
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            Element element = (Element)list.item( i );
            String parameterId = getId( element );
            try
            {
                readParameter( element, parameterId, reaction );
            }
            catch( Throwable t )
            {
                if( reaction == null )
                    error( "ERROR_GLOBAL_PARAMETER_PROCESSING", new String[] {modelName, parameterId, t.getMessage()} );
                else
                    error( "ERROR_REACTION_PARAMETER_PROCESSING", new String[] {modelName, parameterId, reaction.getName(), t.getMessage()} );
            }
        }
    }

    @Override
    protected void readStoichiometry(Element element, SpecieReference reference, Node reaction)
    {
        String stoichiometry = "1";
        String species = "null";
        if( element.hasAttribute( SPECIE_ATTR ) )
            species = element.getAttribute( SPECIE_ATTR );

        if( element.hasAttribute( ID_ATTR ) )
        {
            String constantAttr = element.getAttribute(CONSTANT_ATTR);
            stoichiometry = element.getAttribute( ID_ATTR );
            Variable stoichiometryVar = new Variable( stoichiometry, emodel, emodel.getVariables() );
            stoichiometryVar.setConstant(Boolean.parseBoolean(constantAttr));
            stoichiometryVar.setInitialValue(this.readDouble( element, STOICHIOMETRY_ATTR, "", 0 ));
            emodel.put( stoichiometryVar );
        }
        else if( element.hasAttribute( STOICHIOMETRY_ATTR ) )
        {
            stoichiometry = element.getAttribute( STOICHIOMETRY_ATTR );
        }
        else
            error( "SPECIES_STOICHIOMETRY_PROCESSING", new String[] {modelName, species, stoichiometry} );
        //there is no STOICHIOMETRY MATH ELEMENT in level 3 version 1
        reference.setStoichiometry( stoichiometry );
    }

    @Override
    public Node readEvent(Element eventElement, int i)
    {
        Node node = super.readEvent( eventElement, i );
        Event event = node.getRole( Event.class );

        //useValuesFromTriggerTime attribute reading is in SbmlModelReader_24
        if( !eventElement.hasAttribute( EVENT_USE_VALUES_FROM_TRIGGER_TIME_ATTR ) )
            error( "ERROR_USE_VALUES_FROM_TRIGGER_TIME_ABSENT", new String[] {modelName, node.getName()} );

        //trigger attributes
        try
        {
            Element triggerElement = getElement( eventElement, TRIGGER_ELEMENT );
            event.setTriggerInitialValue( Boolean.parseBoolean( triggerElement.getAttribute( TRIGGER_INITIAL_VALUE_ATTR ) ) );
            event.setTriggerPersistent( Boolean.parseBoolean( triggerElement.getAttribute( TRIGGER_PERSISTENT_ATTR ) ) );
        }
        catch( Exception ex )
        {
            error( "ERROR_TRIGGER_PROCESSING", new String[] {modelName, node.getName(), ex.getMessage()} );
        }

        try
        {
            Element priorityElement = getElement( eventElement, PRIORITY_ELEMENT );
            if( priorityElement != null )
            {
                String priority = readMath( priorityElement, node );
                if( priority != null )
                {
                    event.setPriority( priority );
                    MetaIdInfo metaIdInfo = readMetaId( priorityElement, node.getName(), Node.class, "priority" );
                    if( metaIdInfo != null )
                    {
                        DynamicProperty dp = node.getAttributes().getProperty( METAID_ATTR );
                        if( dp != null )
                        {
                            List<MetaIdInfo> metaInfos = (List<MetaIdInfo>)dp.getValue();
                            metaInfos.add( metaIdInfo );
                        }
                    }
                }
            }
        }
        catch( Exception ex )
        {
            error( "ERROR_PRIORITY_PROCESSING", new String[] {modelName, node.getName(), ex.getMessage()} );
        }

        //delay attributes
        Element delayElement = getElement( eventElement, DELAY_ELEMENT );
        if( delayElement != null && delayElement.hasAttribute( DELAY_TIME_UNITS_ATTR ) )
            event.setTimeUnits( delayElement.getAttribute( DELAY_TIME_UNITS_ATTR ) );
        return node;
    }


    @Override
    public Compartment readCompartment(Element element, String compartmentId, String parentId, String compartmentName) throws Exception
    {
        Compartment compartment = super.readCompartment( element, compartmentId, parentId, compartmentName );
        if (compartment == null)
            return null;

        for( SbmlPackageReader reader : packageReaders )
            reader.processCompartment( element, compartment );
        return compartment;
    }

    @Override
    public Node readSpecie(Element element, String specieId, String specieName) throws Exception
    {
        Node node = super.readSpecie( element, specieId, specieName );

        if( element.hasAttribute( CONVERSION_FACTOR_ATTR ) )
        {
            String conversionFactor = element.getAttribute( CONVERSION_FACTOR_ATTR );
            if( !emodel.containsVariable( conversionFactor ) )
                error( "UNKNOWN_CONVERSION_FACTOR", new String[] {modelName, conversionFactor} );
            else
            {
                String resolvedName = variableResolver.resolveVariable( conversionFactor );
                node.getRole( VariableRole.class ).setConversionFactor( resolvedName );
            }
        }

        for( SbmlPackageReader reader : packageReaders )
            reader.processSpecie( element, node );
        return node;
    }

    @Override
    public Variable readParameter(Element element, String parameterId, Node reaction) throws Exception
    {
        Variable parameter = super.readParameter( element, parameterId, reaction );
        for( SbmlPackageReader reader : packageReaders )
            reader.processParameter( element, parameter );
        return parameter;
    }

    @Override
    public Node readRule(Element ruleElement, int i)
    {
        Node rule = super.readRule( ruleElement, i );
        for( SbmlPackageReader reader : packageReaders )
            reader.processRule( ruleElement, rule );
        return rule;
    }

    @Override
    public Node readReaction(Element element, String reactionId, String reactionName) throws Exception
    {
        Node reaction = super.readReaction( element, reactionId, reactionName );
        for( SbmlPackageReader reader : packageReaders )
            reader.processReaction( element, reaction );
        return reaction;
    }
    
    @Override
    protected Edge readSpecieReference(Node reactionNode, Element element, String specieId, String role) throws Exception
    {
        Edge reference = super.readSpecieReference(reactionNode, element, specieId, role);
        for( SbmlPackageReader reader : packageReaders )
            reader.processSpecieReference( element, reference );
        return reference;
    }
    
    @Override
    protected String createSpecieReferenceId(Element element, Reaction reaction, Node specie, String role)
    {
        String id = "";
        if( element.hasAttribute(ID_ATTR) )
            id = element.getAttribute(ID_ATTR);
        if( id.isEmpty() )
            id = super.createSpecieReferenceId(element, reaction, specie, role);
        return id;
    }
}
