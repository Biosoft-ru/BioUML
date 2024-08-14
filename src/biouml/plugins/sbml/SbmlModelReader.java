package biouml.plugins.sbml;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlReader;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.sbml.extensions.SbmlAnnotationRegistry;
import biouml.plugins.sbml.extensions.SbmlAnnotationRegistry.SbmlAnnotationInfo;
import biouml.plugins.sbml.extensions.SbmlExtension;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;
import biouml.standard.type.Unit;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathLayouterWrapper;
import ru.biosoft.graphics.View;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

/**
 * @pending generation of name for parameters defined in reaction.
 * Currently name is generated as <code>reactioName_parameterName</code>.
 * @pending name validations that they are valid SNames
 */
public abstract class SbmlModelReader extends SbmlSupport
{
    /**global names (such as "C1.C2.C3")*/
    protected Map<String, Compartment> compartmentMap;

    /**keys are global names (such as "C1.C2.C3.S1")*/
    protected Map<String, Node> specieMap;

    /**Map from SBML ID to full path to diagram element in the BioUML diagram*/
    protected Map<String, String> sbmlNameToBioUML;

    protected SbmlVariableResolver variableResolver = new SbmlVariableResolver();
    protected LinearFormatter linearFormatter = new LinearFormatter();

    protected List<SbmlExtensionHelper> annotationsExtensions = null;
    protected Map<String, SbmlExtension> additionalAnnotationsExtensions = null;

    /**Map indicating how sbml ids should be replaced. For example "fucntion" is a key word for our parser so it should be replaced by "function_"*/
    protected Map<String, String> replacements;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //
    public SbmlModelReader()
    {
        log = Logger.getLogger( SbmlModelReader.class.getName() );
    }

    protected DiagramType getDiagramType(Element modelElement)
    {
        Element modelAnnotation = getElement( modelElement, ANNOTATION_ELEMENT );
        if( modelAnnotation != null )
        {
            Element xmlDiagramType = getElement( modelAnnotation, XML_DIAGRAM_TYPE_ELEMENT );
            if( xmlDiagramType != null )
            {
                String notation = xmlDiagramType.getAttribute( "notation" );
                if( notation != null && !"".equals( notation ) )
                {
                    XmlDiagramType type = notation.indexOf( '/' ) == -1 ? XmlDiagramType.getTypeObject( notation )
                            : (XmlDiagramType)CollectionFactory.getDataElement( notation );
                    if( type != null )
                        return type;
                }
            }
        }
        return new SbmlDiagramType();
    }

    public Diagram read(Document document, String name, DataCollection<?> origin) throws Exception
    {
        modelName = name;
        diagram = null;
        compartmentMap = new HashMap<>();
        specieMap = new SpecificalHashMap();
        sbmlNameToBioUML = new HashMap<>();


        Element root = document.getDocumentElement();
        Element model = getElement( root, MODEL_ELEMENT );

        if( model != null )
            diagram = readDiagram( model, origin );
        else
            error( "ERROR_SBML_PROCESSING", new String[] {modelName} );

        if( shouldLayout )
            layout( diagram );

        return diagram;
    }

    public String readNotes(Element element)
    {
        element = getElement( element, NOTES_ELEMENT );
        return ( element != null ) ? readXhtml( element ) : null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods to be defined or refined in different SBML versions
    //

    protected abstract boolean isValid(String paramName, Object element, String name);
    protected void validateReaction(Node reaction)
    {
    }

    public abstract String getTitle(Element element);
    public abstract Node readRule(Element model, int i);
    public abstract Node readInitialAssignment(Element initialAssignmentsList, int i);

    public NodeList getSpecieElement(Element specieList)
    {
        return specieList.getElementsByTagName( SPECIE_ELEMENT );
    }

    protected NodeList getSpecieReference(Element list)
    {
        return list.getElementsByTagName( SPECIE_REFERENCE_ELEMENT );
    }

    protected String getSpecieAttribute(Element specieRef)
    {
        return specieRef.getAttribute( SPECIE_ATTR );
    }

    protected void readModifiers(Element element, String reactionName, List<SpecieRefenceInfo> specieReferences)
    {
    }

    public abstract String getId(Element element);
    abstract protected void readStoichiometry(Element element, SpecieReference reference, Node reactionNode);
    abstract protected void readKineticLawFormula(Element element, Node reaction, KineticLaw law);

    // extensions for level 2
    protected void readFunctionDefinitionList(Element model)
    {
    }
    public Node readFunctionDefinition(Element funcDefElement, Map<String, Element> functions, Set<String> alreadyRead) throws Exception
    {
        return null;
    }
    protected void readEventList(Element element)
    {
    }
    public Node readEvent(Element eventElement, int i)
    {
        return null;
    }
    protected void readModelAttributes(Element element, Diagram diagram)
    {
    }
    protected void readCompartmentTypeList(Element model)
    {
    }
    protected void readSpecieTypeList(Element model)
    {
    }
    protected void readInitialAssignmentsList(Element model)
    {
        Element initialAssignmentList = getElement( model, INITIAL_ASSIGNMENT_LIST_ELEMENT );

        if( initialAssignmentList == null )
            return;

        if( !isValid( INITIAL_ASSIGNMENT_LIST_ELEMENT, initialAssignmentList, null ) )
            return;

        NodeList list = initialAssignmentList.getChildNodes();

        for( int i = 0; i < list.getLength(); i++ )
        {
            try
            {
                if( ( list.item( i ) ).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE )
                    readInitialAssignment( (Element)list.item( i ), i );
            }
            catch( Throwable t )
            {
                error( "ERROR_RULE_PROCESSING", new String[] {modelName, t.getMessage()} );
            }
        }

    }
    protected void readConstraints(Element model)
    {
    }

    protected void readIds(Element element, Set<String> result)
    {
        for( Element child : XmlStream.elements( element ) )
        {
            if( child.hasAttribute( ID_ATTR ) )
                result.add( child.getAttribute( ID_ATTR ) );

            readIds( child, result );
        }
    }

    protected Diagram readDiagram(Element element, DataCollection<?> origin) throws Exception
    {
        compartmentMap = new HashMap<>();
        specieMap = new SpecificalHashMap();
        sbmlNameToBioUML = new HashMap<>();
        replacements = createReplacements( element );

        diagram = createDiagram( element, origin );
        diagram.setNotificationEnabled( false );
        emodel = diagram.getRole( SbmlEModel.class );
        emodel.setNotificationEnabled( false );
        emodel.setAutodetectTypes( false );

        readPathLayouter( element, diagram );

        // while model name is optional, then we try to use it as diagram title
        String title = element.getAttribute( NAME_ATTR );
        if( !title.isEmpty() )
            diagram.setTitle( title );
        log.finer( "Reading SBML model : " + modelName + "." );

        readDiagramElements( element );

        // read model annotation
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement != null )
        {
            readBioUMLAnnotation( annotationElement, diagram );
            readAnnotation( annotationElement, diagram );
        }

        diagram.setNotificationEnabled( true );
        emodel.setNotificationEnabled( true );

        // set view builder for new diagram elements
        diagram.setNodeViewBuilders();
        return diagram;
    }

    protected void readDiagramElements(Element element)
    {
        readFunctionDefinitionList( element );
        readUnitList( element );
        readCompartmentTypeList( element );
        readCompartmentList( element );
        readSpecieTypeList( element );
        readParameterList( element, null );
        readSpecieList( element );
        readReactionList( element );
        readRuleList( element );
        readEventList( element );
        readInitialAssignmentsList( element );
        readConstraints( element );
        readModelAttributes( element, diagram );
    }

    /**
     * Create diagram object before add new elements to it
     * @return
     */
    protected Diagram createDiagram(Element element, DataCollection<?> origin) throws Exception
    {
        DiagramType diagramType = getDiagramType( element );
        if( modelName == null || modelName.isEmpty() )
            modelName = getBriefId( element, BIOUML_DIAGRAM_INFO_ELEMENT );
        DiagramInfo diagramInfo = new DiagramInfo( modelName );
        diagramInfo.setDescription( readNotes( element ) );
        return diagramType.createDiagram( origin, diagramInfo.getName(), diagramInfo );
    }

    private void readPathLayouter(Element annotationElement, Diagram diagram)
    {
        NodeList list = annotationElement.getElementsByTagName( "propertyRef" );
        for( int i = 0; i < list.getLength(); i++ )
        {
            Element el = (Element)list.item( i );
            if( el.getAttribute( NAME_ATTR ).equals( "pathLayouter" ) )
            {
                try
                {
                    String className = el.getAttribute( TYPE_ATTR );
                    if( !className.isEmpty() )
                    {
                        Class<?> clazz = ClassLoading.loadClass( className );
                        Layouter layouter = (Layouter)clazz.newInstance();
                        diagram.getViewOptions().setPathLayouterWrapper( new PathLayouterWrapper( layouter ) );
                    }
                }
                catch( Exception e )
                {
                    error( "Error reading diagram path layouter", new String[] {modelName, e.getMessage()} );
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Unit issues
    //
    public void readUnitList(Element model)
    {
        Element unitDefinitionList = getElement( model, UNIT_DEFINITION_LIST_ELEMENT );
        if( !isValid( UNIT_DEFINITION_LIST_ELEMENT, unitDefinitionList, null ) )
            return;

        NodeList list = unitDefinitionList.getElementsByTagName( UNIT_DEFINITION_ELEMENT );
        for( int i = 0; i < list.getLength(); i++ )
        {
            String unitId = "";
            try
            {
                Element element = (Element)list.item( i );
                unitId = getId( element );
                emodel.addUnit( readUnitDefinition( element, unitId, getTitle( element ) ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_UNIT_DEFINITION_PROCESSING", new String[] {modelName, unitId, t.getMessage()} );
            }
        }
    }

    protected boolean validateList(Object element, String tag, String name)
    {
        if( element instanceof Element )
            return validateList( ( (Element)element ).getElementsByTagName( tag ), null, name );
        return element instanceof NodeList && ( (NodeList)element ).item( 0 ) != null;
    }

    /** It converts value to unified unit system. */
    protected double convertUnit(String unitName, double value)
    {
        return value;
    }

    /**
     * Method has next logic:<br>
     * <ul>
     * <li>if there is no BioUML annotation then this is pure SBML file and we should simply return SBML ID
     * <li>Else if there is fully valid annotation with BIOUML_COMPLETE_NAME_ATTR attribute than this should return its value
     * <li>Else if there is BioUML annotation but without IOUML_COMPLETE_NAME_ATTR attribute (which means that this diagram was saved in old style)
     * Then we should parse SBML id using getFullNameById method.
     */
    public String getCompleteId(Element element, String expectedBioUMLInfo)
    {
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement == null )
            return getId( element );

        Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
        if( bioumlElement == null )
            bioumlElement = annotationElement;//try to look inside of ANNOTATION instead

        Element nodeInfoElement = getElement( bioumlElement, expectedBioUMLInfo );

        if( nodeInfoElement == null ) //this is somewhat strange situation, probably file is incorrect
            return getId( element );

        String fullName = nodeInfoElement.getAttribute( BIOUML_COMPLETE_NAME_ATTR );
        if( fullName.isEmpty() ) //means that this document is created by old version of BIOUML
            fullName = getFullNameById( getId( element ) );

        fullName = castFullName( fullName );
        return fullName;
    }

    /**
     * Returns id to be used as node id in BioUML diagram
     */
    protected String getBriefId(Element element, String expectedBioUMLInfo)
    {
        return getBriefName( getCompleteId( element, expectedBioUMLInfo ) );
    }

    public String getFullNameById(String id)
    {
        String input = id;
        StringBuilder result = new StringBuilder();

        DataCollection<?> dc = diagram;
        boolean continueSearch = true;
        while( continueSearch )
        {
            continueSearch = false;
            for( Object obj : dc )
            {
                if( obj instanceof Compartment )
                {
                    String cName = ( (Compartment)obj ).getName();
                    if( input.startsWith( castStringToSId( cName ) + "_" ) )
                    {
                        result.append( cName ).append( '.' );
                        input = input.substring( cName.length() + 1 );
                        dc = (Compartment)obj;
                        continueSearch = true;
                        break;
                    }
                }
            }
        }
        result.append( input );
        return result.toString();
    }

    /** Read list of compartments. */
    protected void readCompartmentList(Element model)
    {
        Element compartmentList = getElement( model, COMPARTMENT_LIST_ELEMENT );
        if( !isValid( COMPARTMENT_LIST_ELEMENT, compartmentList, null ) )
            return;

        NodeList list = compartmentList.getElementsByTagName( COMPARTMENT_ELEMENT );
        Map<String, Element> nameToCompartment = XmlStream.elements( list ).toMap( this::getId, e -> e );
        Map<String, String> nameToParent = XmlStream.elements( list )
                .mapToEntry( this::getId, e -> e.getAttribute( COMPARTMENT_OUTSIDE_ATTR ) ).toCustomMap( LinkedHashMap::new );
        List<String> compartments = topoSort( nameToParent );
        for( String id : compartments )
        {
            String compartmentId = "";
            try
            {
                Element element = nameToCompartment.get( id );
                compartmentId = getCompleteId( element, BIOUML_COMPARTMENT_INFO_ELEMENT );
                String compartmentTitle = getTitle( element );
                if( compartmentTitle.isEmpty() )
                    compartmentTitle = compartmentId;
                Compartment compartment = readCompartment( element, compartmentId, getParentId( compartmentId ), compartmentTitle );
                if( compartment != null )
                    sbmlNameToBioUML.put( getId( element ), compartment.getCompleteNameInDiagram() );
            }
            catch( Throwable t )
            {
                error( "ERROR_COMPARTMENT_PROCESSING", new String[] {modelName, compartmentId, t.getMessage()} );
            }
        }
    }

    private List<String> topoSort(Map<String, String> nameToParent)
    {
        List<String> compartments = new ArrayList<>( nameToParent.keySet() );
        boolean changed = true;
        int iter = 100;
        while( changed )
        {
            changed = false;
            if( iter-- == 0 )
            {
                error( "ERROR_COMPARTMENT_REORDERING", new String[] {modelName} );
            }
            for( int i = 0; i < compartments.size() - 1; i++ )
            {
                int j = compartments.indexOf( nameToParent.get( compartments.get( i ) ) );
                if( j > i )
                {
                    Collections.swap( compartments, i, j );
                    changed = true;
                    i--;
                }
            }
        }
        return compartments;
    }

    public static String getParentId(String fullName)
    {
        int ind = fullName.lastIndexOf( "." );
        return ( ind != -1 ) ? fullName.substring( 0, ind ) : null;
    }

    public Compartment readCompartment(Element element, String compartmentCompleteId, String parentId, String compartmentTitle)
            throws Exception
    {
        // ignore the first compartment with the same name as diagram
        if( isDefaultCompartment( element ) || diagram.getName().equals( compartmentCompleteId ) ) //old logic added to support backward compatibility
        {
            compartmentMap.put( diagram.getName(), diagram );
            compartmentMap.put( compartmentCompleteId, diagram );
            sbmlNameToBioUML.put( getId( element ), diagram.getName() );
            return null;
        }
        Compartment compartment = null;
        DataCollection<DiagramElement> parent = diagram;
        if( parentId != null && compartmentMap.containsKey( parentId ) )
        {
            parent = compartmentMap.get( parentId );
        }
        else if( element.hasAttribute( COMPARTMENT_OUTSIDE_ATTR ) )
        {
            String outside = element.getAttribute( COMPARTMENT_OUTSIDE_ATTR );
            if( compartmentMap.containsKey( outside ) )
                parent = compartmentMap.get( outside );
            else
                error( "ERROR_COMPARTMENT_OUTSIDE", new String[] {modelName, compartmentCompleteId, outside} );
        }

        String compartmentId = getBriefName( compartmentCompleteId );

        biouml.standard.type.Compartment kernel = new biouml.standard.type.Compartment( null, compartmentId );
        compartment = new Compartment( parent, compartmentId, kernel );
        compartment.setTitle( compartmentTitle );
        parent.put( compartment );
        //        compartmentPrefixes.put( getCompartmentDashedName( compartment ), compartment );
        compartmentMap.put( compartmentCompleteId, compartment );


        double initialValue = 1.0;
        if( element.hasAttribute( COMPARTMENT_SIZE_ATTR ) )
            initialValue = readDouble( element, COMPARTMENT_SIZE_ATTR, "Error during compartment size parsing: " + compartmentCompleteId,
                    1.0 );
        else if( element.hasAttribute( COMPARTMENT_VOLUME_ATTR ) )
            initialValue = readDouble( element, COMPARTMENT_VOLUME_ATTR,
                    "Error during compartment volume parsing: " + compartmentCompleteId, 1.0 );

        VariableRole var = new VariableRole( compartment, initialValue );

        String notes = readNotes( element );
        if( notes != null )
            var.setComment( notes );

        compartment.setRole( var );
        emodel.put( var );

        String units = element.getAttribute( UNITS_ATTR );
        if( !units.isEmpty() )
            var.setUnits( units );

        // read extensions
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement != null )
        {
            Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
            if( bioumlElement != null )
                readVariable( var, bioumlElement );
            readBioUMLAnnotation( annotationElement, compartment, BIOUML_COMPARTMENT_INFO_ELEMENT );
            readAnnotation( annotationElement, compartment );
        }

        return compartment;
    }


    private boolean isDefaultCompartment(Element element)
    {
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement == null )
            return false;

        Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
        if( bioumlElement == null )
            return false;

        Element compartmentInfoElement = getElement( bioumlElement, BIOUML_COMPARTMENT_INFO_ELEMENT );
        if( compartmentInfoElement == null )
            return false;
        String attr = compartmentInfoElement.getAttribute( BIOUML_DEFAULT_COMPARTMENT_ATTR );
        return !attr.isEmpty() && attr.equals( "true" );
    }

    ///////////////////////////////////////////////////////////////////
    // Species issues
    //

    /** Read list of species. */
    protected void readSpecieList(Element model)
    {
        Element speciesList = getElement( model, SPECIE_LIST_ELEMENT );
        if( !isValid( SPECIE_LIST_ELEMENT, speciesList, null ) || speciesList == null )
            return;

        for( Element element : XmlStream.elements( getSpecieElement( speciesList ) ) )
        {
            String specieId = "";
            try
            {
                specieId = getBriefId( element, BIOUML_NODE_INFO_ELEMENT );
                Node node = readSpecie( element, specieId, getTitle( element ) );
                sbmlNameToBioUML.put( getId( element ), node.getCompleteNameInDiagram() );
            }
            catch( Throwable t )
            {
                error( "ERROR_SPECIE_PROCESSING", new String[] {modelName, specieId, t.getMessage()} );
            }
        }
    }


    public Node readSpecie(Element element, String specieId, String specieName) throws Exception
    {
        // ignore stubs
        if( REACTANT_STUB.equals( specieId ) || PRODUCT_STUB.equals( specieId ) )
            return null;

        if( ( specieName == null ) || ( specieName.trim().length() == 0 ) )
            specieName = specieId;

        Node specie = null;
        Compartment parent = diagram;

        if( !element.hasAttribute( COMPARTMENT_ATTR ) )
        {
            error( "ERROR_SPECIE_COMPARTMENT_NOT_SPECIFIED", new String[] {modelName, specieId, parent.getName()} );
        }
        else
        {
            parent = compartmentMap.get( sbmlNameToBioUML.get( element.getAttribute( COMPARTMENT_ATTR ) ) );
            // For some reason the following statement is necessary for HMR SBML file
            if( parent == null )
                parent = compartmentMap.get( element.getAttribute( COMPARTMENT_ATTR ) );

            if( parent == null )
            {
                error( "ERROR_SPECIE_COMPARTMENT_NOT_FOUND",
                        new String[] {modelName, specieId, element.getAttribute( COMPARTMENT_ATTR ), diagram.getName()} );
                parent = diagram;
            }
        }

        Specie kernel = new Specie( null, specieId );
        kernel.setTitle( specieName );
        specie = new Node( parent, kernel );
        specie.setTitle( specieName );

        specieMap.put( specie.getCompleteNameInDiagram(), specie );

        VariableRole var = new VariableRole( specie, 0.0 );

        String notes = readNotes( element );
        if( notes != null )
            var.setComment( notes );

        specie.setRole( var );
        emodel.put( var );

        parent.put( specie );

        if( element.hasAttribute( SPECIE_BOUNDARY_CONDITION_ATTR ) )
            var.setBoundaryCondition( "true".equals( element.getAttribute( SPECIE_BOUNDARY_CONDITION_ATTR ) ) );

        if( element.hasAttribute( SPECIE_CHARGE_ATTR ) )
        {
            String charge = element.getAttribute( SPECIE_CHARGE_ATTR );
            try
            {
                kernel.setCharge( Integer.parseInt( charge ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_SPECIE_CHARGE", new String[] {modelName, specieId, charge, t.toString()} );
            }
        }

        // read specie info as BioUML extension
        readBioUMLAnnotation( element, specie );
        return specie;
    }

    private void readBioUMLAnnotation(Element element, Node node) throws Exception
    {
        boolean needTypeFind = true;
        Specie kernel = (Specie)node.getKernel();
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement != null )
        {
            Element nodeInfoElement = null;
            Element specieInfoElement = null;

            Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
            if( bioumlElement != null )
            {
                nodeInfoElement = getElement( bioumlElement, BIOUML_NODE_INFO_ELEMENT );
                specieInfoElement = getElement( bioumlElement, BIOUML_SPECIE_INFO_ELEMENT );
                readVariable( node.getRole( VariableRole.class ), bioumlElement );
            }
            else
            {
                nodeInfoElement = getElement( annotationElement, BIOUML_NODE_INFO_ELEMENT );
                specieInfoElement = getElement( annotationElement, BIOUML_SPECIE_INFO_ELEMENT );
            }

            if( nodeInfoElement != null )
            {
                shouldLayout = false;
                DiagramXmlReader.readNodeInfo( nodeInfoElement, node, diagram.getName() );
            }
            if( specieInfoElement != null && specieInfoElement.hasAttribute( BIOUML_SPECIE_TYPE_ATTR ) )
            {
                kernel.setType( specieInfoElement.getAttribute( BIOUML_SPECIE_TYPE_ATTR ) );
                DynamicPropertySet dps = DiagramXmlReader.readDPS( specieInfoElement, null );
                for( DynamicProperty dp : dps )
                    kernel.getAttributes().add( dp );
                needTypeFind = false;
            }
            readAnnotation( annotationElement, node );
        }

        if( needTypeFind )
        {
            String type = SbmlTypeMapper.getInstance().getSpecieType( kernel );
            if( type != null )
                kernel.setType( type );
        }
    }

    public void readVariable(Variable variable, Element element)
    {
        Element varInfoElement = getElement( element, BIOUML_VARIABLE_INFO_ELEMENT );
        if( varInfoElement == null )
            return;

        if( varInfoElement.hasAttribute( BIOUML_VARIABLE_COMMENT ) )
            variable.setComment( varInfoElement.getAttribute( BIOUML_VARIABLE_COMMENT ) );
    }

    ///////////////////////////////////////////////////////////////////
    // Parameter issues
    //

    /**
     * Read list of parameters for the specified element. If element is model, then global parameters will be read.
     */
    protected void readParameterList(Element parent, Node reaction)
    {
        Element parameterList = getElement( parent, PARAMETER_LIST_ELEMENT );
        if( !isValid( PARAMETER_LIST_ELEMENT, parameterList, null ) )
            return;

        for( Element element : XmlUtil.elements( parameterList, PARAMETER_ELEMENT ) )
        {
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
                    error( "ERROR_REACTION_PARAMETER_PROCESSING",
                            new String[] {modelName, parameterId, reaction.getName(), t.getMessage()} );
            }
        }
    }

    public Variable readParameter(Element element, String parameterId, Node reaction) throws Exception
    {
        String baseId = parameterId;
        // resolve possible conflicts between global and reaction parameters
        if( reaction != null )
            parameterId = reaction.getName() + "_" + parameterId;

        parameterId = normalize( parameterId );

        Variable parameter = new Variable( parameterId, emodel, emodel.getVariables() );

        if( reaction != null )
            parameter.getAttributes().add( new DynamicProperty( "baseId", String.class, baseId ) );

        emodel.put( parameter );

        String notes = readNotes( element );
        if( notes != null )
            parameter.setComment( notes );

        if( isValid( PARAMETER_VALUE_ATTR, element, parameterId ) )
        {
            String value = element.getAttribute( PARAMETER_VALUE_ATTR );
            try
            {
                parameter.setInitialValue( parseSBMLDoubleValue( value.replace( ',', '.' ) ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_PARAMETER_VALUE", new String[] {modelName, parameterId, value, t.toString()} );
            }
        }

        String units = element.getAttribute( UNITS_ATTR );
        if( !units.isEmpty() )
        {
            if( !emodel.getUnits().containsKey( units ) )
                emodel.addUnit( new Unit( null, units ) );
            parameter.setUnits( units );
        }

        if( element.hasAttribute( NAME_ATTR ) )
            parameter.setTitle( element.getAttribute( NAME_ATTR ) );

        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement != null )
        {
            Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
            if( bioumlElement != null )
                readVariable( parameter, bioumlElement );
        }
        return parameter;
    }



    ///////////////////////////////////////////////////////////////////
    // Reaction issues
    //

    /** Read list of reactions.  */
    protected void readReactionList(Element model)
    {
        Element reactionList = getElement( model, REACTION_LIST_ELEMENT );
        if( !isValid( REACTION_LIST_ELEMENT, reactionList, null ) || reactionList == null )
            return;

        for( Element element : XmlUtil.elements( reactionList, REACTION_ELEMENT ) )
        {
            String reactionId = "";
            try
            {
                reactionId = getBriefId( element, BIOUML_NODE_INFO_ELEMENT );
                readReaction( element, reactionId, getTitle( element ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_REACTION_PROCESSING", new String[] {modelName, reactionId, t.getMessage()} );
            }
        }
    }

    public Node readReaction(Element element, String reactionId, String reactionName) throws Exception
    {
        Reaction kernel = new Reaction( null, reactionId );
        kernel.setTitle( reactionName );
        List<SpecieRefenceInfo> specieReferences = new ArrayList<>();
        readReactants( element, reactionId, specieReferences );
        readProducts( element, reactionId, specieReferences );
        readModifiers( element, reactionId, specieReferences );

        Compartment parent = null;
        for( SpecieRefenceInfo si : specieReferences )
        {
            Node n = specieMap.get( si.id );
            Compartment c = n.getCompartment();
            if( parent == null )
            {
                parent = c;
            }
            else if( parent != c ) //TODO: put reaction inside some compartment not diagram
            {
                parent = diagram;
                break;
            }
        }

        if( parent == null )
            parent = diagram;

        Node reaction = new Node( parent, reactionId, kernel );
        kernel.setParent( reaction );

        String notes = readNotes( element );
        if( notes != null )
            reaction.setComment( notes );

        if( element.hasAttribute( REACTION_REVERSIBLE_ATTR ) )
            kernel.setReversible( "true".equals( element.getAttribute( REACTION_REVERSIBLE_ATTR ) ) );
        else
            kernel.setReversible( true );

        if( element.hasAttribute( REACTION_FAST_ATTR ) )
            kernel.setFast( "true".equals( element.getAttribute( REACTION_FAST_ATTR ) ) );

        for( SpecieRefenceInfo si : specieReferences )
        {
            try
            {
                readSpecieReference( reaction, si.element, si.id, si.type );
            }
            catch( Exception e )
            {
                error( "ERROR_SPECIE_PROCESSING", new String[] {modelName, reactionName, si.id, e.getMessage()} );
            }
        }
        readKineticLaw( element, reaction );

        // read reaction info as BioUML extension
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement != null )
        {
            readBioUMLAnnotation( annotationElement, reaction, BIOUML_NODE_INFO_ELEMENT );
            Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
            if( bioumlElement != null )
            {
                Element reactionInfoElement = getElement( bioumlElement, BIOUML_REACTION_INFO_ELEMENT );
                if( reactionInfoElement != null )
                {
                    for( DynamicProperty dp : DiagramXmlReader.readDPS( reactionInfoElement, null ) )
                        kernel.getAttributes().add( dp );
                }
            }
            readAnnotation( annotationElement, reaction );
        }
        validateReaction( reaction );
        parent.put( reaction );
        return reaction;
    }

    protected void readReactants(Element element, String reactionName, List<SpecieRefenceInfo> specieReferences)
    {
        Element reactantList = getElement( element, REACTANT_LIST_ELEMENT );
        if( !isValid( REACTANT_LIST_ELEMENT, reactantList, reactionName ) )
            return;

        for( Element specieRef : XmlUtil.elements( getSpecieReference( reactantList ) ) )
        {
            String specieId = "";
            try
            {
                specieId = sbmlNameToBioUML.get( getSpecieAttribute( specieRef ) );
                specieReferences.add( new SpecieRefenceInfo( specieId, SpecieReference.REACTANT, specieRef ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_REACTANT_PROCESSING", new String[] {modelName, reactionName, specieId, t.getMessage()} );
            }
        }
    }

    protected void readProducts(Element element, String reactionName, List<SpecieRefenceInfo> specieReferences)
    {
        Element productList = getElement( element, PRODUCT_LIST_ELEMENT );
        if( !isValid( PRODUCT_LIST_ELEMENT, productList, reactionName ) )
            return;

        for( Element specieRef : XmlUtil.elements( getSpecieReference( productList ) ) )
        {
            String specieId = "";
            try
            {
                specieId = sbmlNameToBioUML.get( getSpecieAttribute( specieRef ) );
                specieReferences.add( new SpecieRefenceInfo( specieId, SpecieReference.PRODUCT, specieRef ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_PRODUCT_PROCESSING", new String[] {modelName, reactionName, specieId, t.getMessage()} );
            }
        }
    }

    protected String createSpecieReferenceId(Element element, Reaction reaction, Node specie, String role)
    {
        String id = SpecieReference.generateSpecieReferenceName( reaction.getName(), specie.getName(), role );
        Diagram d = Diagram.getDiagram( specie );
        return DefaultSemanticController.generateUniqueName( d, id );
    }

    protected Edge readSpecieReference(Node reactionNode, Element element, String specieId, String role) throws Exception
    {
        String nodeName = getBriefName( specieId );

        // ignore stubs for version 1
        if( nodeName.equals( REACTANT_STUB ) || nodeName.equals( PRODUCT_STUB ) )
            return null;

        Node specieNode = specieMap.get( specieId );
        if( specieNode == null )
        {
            error( "ERROR_SPECIE_REFERENCE_INVALID", new String[] {modelName, reactionNode.getName(), specieId} );
            return null;
        }

        String fullName = specieNode.getCompleteNameInDiagram();
        Reaction reactionKernel = (Reaction)reactionNode.getKernel();

        String id = "";
        //try to read edge id from BioUML extension
        Element annotationElement = getElement( element, ANNOTATION_ELEMENT );
        if( annotationElement != null )
            id = readEdgeName( annotationElement, BIOUML_EDGE_INFO_ELEMENT );

        if( id.isEmpty() )
            id = createSpecieReferenceId( element, reactionKernel, specieNode, role );
        if( reactionKernel.contains( id ) && !role.equals( SpecieReference.MODIFIER ) )
        //in this case we just add stoichiometry of new reference to old one TODO: move this to preprocessor
        {
            SpecieReference ref = reactionKernel.get( id );
            String stoichiometry = ref.getStoichiometry();
            if( !stoichiometry.startsWith( "-" ) )
                stoichiometry = "+" + stoichiometry;
            readStoichiometry( element, ref, reactionNode );
            ref.setStoichiometry( ref.getStoichiometry() + stoichiometry );
        }
        else
        {
            SpecieReference ref = new SpecieReference( reactionKernel, id, role );
            ref.setSpecie( fullName );
            if( !role.equals( SpecieReference.MODIFIER ) )
                readStoichiometry( element, ref, reactionNode );
            reactionKernel.put( ref );

            // usually we associate notes with diagram element, but this is an exception
            String notes = readNotes( element );
            if( notes != null )
                ref.setComment( notes );

            // here we create corresponding edge
            Edge edge = role.equals( SpecieReference.PRODUCT ) ? new Edge( id, ref, reactionNode, specieNode )
                    : new Edge( id, ref, specieNode, reactionNode );

            if( ref.isReactantOrProduct() )
            {
                VariableRole var = specieNode.getRole( VariableRole.class );
                Equation equation = new Equation( edge, Equation.TYPE_RATE, var.getName() );
                equation.setFast( reactionKernel.isFast() );
                edge.setRole( equation );
            }

            // read edge info as BioUML extension
            if( annotationElement != null )
            {
                readBioUMLAnnotation( annotationElement, edge, BIOUML_EDGE_INFO_ELEMENT );
                readAnnotation( annotationElement, edge );
            }
            ref.setTitle( edge.getTitle() );
            edge.save();
            return edge;
        }
        return null;
    }

    /**
     * Set layout flag
     */
    public void setShouldLayout(boolean shouldLayout)
    {
        this.shouldLayout = shouldLayout;
    }

    protected void readKineticLaw(Element element, Node reaction)
    {
        Reaction reactionKernel = (Reaction)reaction.getKernel();
        KineticLaw kineticLaw = new KineticLaw( reactionKernel );
        reactionKernel.setKineticLaw( kineticLaw );

        String notes = readNotes( element );
        if( notes != null )
            kineticLaw.setComment( notes );

        Element kineticLawElement = getElement( element, KINETIC_LAW_ELEMENT );

        if( kineticLawElement != null )
        {
            readLocalParameterList( kineticLawElement, reaction );
            readKineticLawFormula( kineticLawElement, reaction, kineticLaw );

            if( kineticLawElement.hasAttribute( TIME_UNITS_ATTR ) )
                kineticLaw.setTimeUnits( kineticLawElement.getAttribute( TIME_UNITS_ATTR ) );

            if( kineticLawElement.hasAttribute( SUBSTANCE_UNITS_ATTR ) )
                kineticLaw.setSubstanceUnits( kineticLawElement.getAttribute( SUBSTANCE_UNITS_ATTR ) );
        }

        // in correct SBML all reactions have unique name
        String varName = getRateVariableName( reaction.getName() );
        emodel.put( new Variable( varName, emodel, emodel.getVariables() ) );
        Equation rule = new Equation( reaction, Equation.TYPE_SCALAR, varName );
        rule.setFast( reactionKernel.isFast() );
        reaction.setRole( rule );
    }

    protected void readLocalParameterList(Element parent, Node reaction)
    {
        Element parameterList = getElement( parent, LOCAL_PARAMETER_LIST_ELEMENT_L2 );
        if( !isValid( LOCAL_PARAMETER_LIST_ELEMENT_L2, parameterList, null ) || parameterList == null )
            return;

        for( Element element : XmlUtil.elements( parameterList, LOCAL_PARAMETER_ELEMENT_L2 ) )
        {
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
                    error( "ERROR_REACTION_PARAMETER_PROCESSING",
                            new String[] {modelName, parameterId, reaction.getName(), t.getMessage()} );
            }
        }
    }

    /**
     * Add SBML extension to reader
     */
    public void addExtension(String namespace, SbmlExtension extension)
    {
        if( additionalAnnotationsExtensions == null )
            additionalAnnotationsExtensions = new HashMap<>();
        additionalAnnotationsExtensions.put( namespace, extension );
    }

    private void readBioUMLAnnotation(Element annotationElement, Diagram diagram)
    {
        Element viewOptionsElement = null;
        Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );
        if( bioumlElement != null )
        {
            try
            {
                Element plotsElement = getElement( bioumlElement, BIOUML_PLOT_INFO_ELEMENT );
                if( plotsElement != null )
                    DiagramXmlReader.readPlotsInfo( plotsElement, diagram, newPaths );
            }
            catch( Exception ex )
            {

            }
            Element simulationElement = getElement( bioumlElement, BIOUML_SIMULATION_INFO_ELEMENT );
            if( simulationElement != null )
                DiagramXmlReader.readSimulationOptions( simulationElement, diagram );

            viewOptionsElement = getElement( bioumlElement, BIOUML_VIEW_OPTIONS_ELEMENT );

            Element dbInfo = getElement( bioumlElement, BIOUML_DB_INFO_ELEMENT );
            if( dbInfo != null )
            {
                String bioHub = dbInfo.getAttribute( BIOUML_BIOHUB_ATTR );
                if( !bioHub.isEmpty() )
                    diagram.getAttributes().add( new DynamicProperty( BIOUML_BIOHUB_ATTR, String.class, bioHub ) );

                String referenceType = dbInfo.getAttribute( BIOUML_REFERENCE_TYPE_ATTR );
                if( !referenceType.isEmpty() )
                    diagram.getAttributes().add( new DynamicProperty( BIOUML_REFERENCE_TYPE_ATTR, String.class, referenceType ) );

                String converter = dbInfo.getAttribute( BIOUML_CONVERTER_ATTR );
                if( !converter.isEmpty() )
                    diagram.getAttributes().add( new DynamicProperty( BIOUML_CONVERTER_ATTR, String.class, converter ) );

                String species = dbInfo.getAttribute( BIOUML_SPECIES_ATTR );
                if( !species.isEmpty() )
                    diagram.getAttributes().add( new DynamicProperty( BIOUML_SPECIES_ATTR, String.class, species ) );
            }
        }
        else
        {
            viewOptionsElement = getElement( annotationElement, BIOUML_VIEW_OPTIONS_ELEMENT );
        }
        if( viewOptionsElement != null )
        {
            DiagramViewOptions viewOptions = diagram.getViewOptions();
            viewOptions.setNotificationEnabled( false );
            viewOptions.setDependencyEdges( Boolean.parseBoolean( viewOptionsElement.getAttribute( BIOUML_DEPENDENCY_EDGES_ATTR ) ) );
            viewOptions.setAutoLayout( Boolean.parseBoolean( viewOptionsElement.getAttribute( BIOUML_AUTOLAYOUT_ATTR ) ) );
            viewOptions.setNotificationEnabled( true );
        }
    }

    protected void readAnnotation(Element element, DiagramElement diagramElement) throws Exception
    {
        if( annotationsExtensions == null )
        {
            annotationsExtensions = new ArrayList<>();
            List<SbmlAnnotationInfo> annotations = SbmlAnnotationRegistry.getAnnotations();

            if( annotations == null )
                return;

            for( SbmlAnnotationInfo extension : annotations )
            {
                SbmlExtension se = extension.create();
                se.setSbmlModelReader( this );
                annotationsExtensions.add( new SbmlExtensionHelper( se, extension.getNamespace(), extension.getPriority() ) );
            }
            if( additionalAnnotationsExtensions != null )
            {
                for( Map.Entry<String, SbmlExtension> entry : additionalAnnotationsExtensions.entrySet() )
                    annotationsExtensions.add( new SbmlExtensionHelper( entry.getValue(), entry.getKey(), 100 ) );
            }
            //sort extensions by priority
            Collections.sort( annotationsExtensions, Comparator.comparingInt( h -> h.priority ) );
        }

        for( Element child : XmlUtil.elements( element ) )
        {
            for( SbmlExtensionHelper ex : annotationsExtensions )
            {
                if( child.getNodeName().startsWith( ex.namespace ) )
                    ex.extension.readElement( child, diagramElement, diagram );
            }
        }
    }

    public static class SbmlExtensionHelper
    {
        protected SbmlExtension extension;
        protected String namespace;
        protected Integer priority;

        public SbmlExtensionHelper(SbmlExtension extension, String namespace, Integer priority)
        {
            this.extension = extension;
            this.namespace = namespace;
            this.priority = priority;
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Rule issues
    //

    protected void readRuleList(Element model)
    {
        Element ruleList = getElement( model, RULE_LIST_ELEMENT );

        if( ruleList == null || !isValid( RULE_LIST_ELEMENT, ruleList, null ) )
            return;

        NodeList list = ruleList.getChildNodes();

        for( int i = 0; i < list.getLength(); i++ )
        {
            try
            {
                if( ( list.item( i ) ).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE )
                    readRule( (Element)list.item( i ), i );
            }
            catch( Throwable t )
            {
                error( "ERROR_RULE_PROCESSING", new String[] {modelName, t.getMessage()} );
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Math processing issues
    //

    class SbmlVariableResolver implements VariableResolver
    {
        protected DiagramElement diagramElement;

        @Override
        public String getVariableName(String variableTitle)
        {
            if( variableTitle.equals( "time" ) )
                return variableTitle;

            // check for reaction parameter first
            if( diagramElement instanceof Node && diagramElement.getKernel() != null
                    && diagramElement.getKernel().getType().equals( Type.TYPE_REACTION ) )
            {
                String name = diagramElement.getName() + "_" + variableTitle;
                if( emodel.containsVariable( name ) )
                    return emodel.getVariable( name ).getName();
            }

            //transform to simple name
            String fullName = sbmlNameToBioUML.get( variableTitle );
            if( fullName == null )
                fullName = variableTitle;

            String vName = fullName;
            if( !vName.equals( variableTitle ) )
            {
                String cName = "$\"" + vName + "\"";
                if( emodel.containsVariable( cName ) )
                    return emodel.getVariable( cName ).getName();
            }
            else
            {
                vName = getBriefName( vName );
            }

            if( specieMap.containsKey( fullName ) )
                return specieMap.get( fullName ).getRole( VariableRole.class ).getName();

            if( compartmentMap.containsKey( fullName ) )
                return compartmentMap.get( fullName ).getRole( VariableRole.class ).getName();

            if( emodel.containsVariable( fullName ) )
                return emodel.getVariable( fullName ).getName();


            //In SBML any element can refer to reaction rate by reaction ID,
            //however in BioUML model reaction rate can be referred only as "$$rate_"<REACTION_ID>
            String rateVariableName = getRateVariableName( vName );
            if( emodel.containsVariable( rateVariableName ) )
                return emodel.getVariable( rateVariableName ).getName();

            return null;
        }
        @Override
        public String resolveVariable(String variableName)
        {
            return variableName;
        }
    }

    ////
    @Override
    protected boolean parseAsSpecie(String token, StringBuffer result)
    {
        if( specieMap.containsKey( token ) )
        {
            result.append( "$" + token );
            return true;
        }

        return false;
    }

    @Override
    protected boolean parseAsCompartment(String token, StringBuffer result)
    {
        if( compartmentMap.containsKey( token ) )
        {
            result.append( "$" + token );
            return true;
        }

        return false;
    }

    @Override
    protected boolean parseAsParameter(String token, StringBuffer result, Node reaction)
    {
        EModel model = diagram.getRole( EModel.class );

        // check for reaction parameter
        String name = reaction.getName() + "_" + token;
        if( model.containsVariable( name ) )
        {
            result.append( name );
            return true;
        }

        // check for global parameter
        if( model.containsVariable( token ) )
        {
            result.append( token );
            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Diagram layout issues
    //

    int layerDeltaX = 70;
    int layerDeltaY = 70;

    /** Stub implimentation for diagram layout. */
    public void layout(Compartment compartment)
    {
        diagram.setNotificationEnabled( false );
        layout( compartment, 0, 0 );
        diagram.setNotificationEnabled( true );
    }

    /**
     * Hierarchically layout compartments and species.
     *
     * @pending currently we use ApplicationFrame to get Graphics object
     */
    public void layout(Compartment compartment, int x, int y)
    {
        // layout internal compartments
        Rectangle rect = new Rectangle( x, y, 1, 1 );
        for( DiagramElement de : compartment )
        {
            if( de instanceof Compartment )
            {
                Compartment comp = (Compartment)de;
                layout( comp, x, y );

                Rectangle r = new Rectangle( comp.getLocation().x, comp.getLocation().y, comp.getShapeSize().width,
                        comp.getShapeSize().height );
                rect = rect.union( r );
                x = rect.x + rect.width + layerDeltaX;
            }
        }

        // calculate number of species and put them into the list
        // also build view for each specie and calculate maximum node width an height
        SbmlDiagramViewBuilder diagramViewBuilder = new SbmlDiagramViewBuilder();
        DiagramViewOptions viewOptions = diagramViewBuilder.createDefaultDiagramViewOptions();
        Graphics graphics = ApplicationUtils.getGraphics();
        int nodeWidth = 10;
        int nodeHeight = 10;

        List<DiagramElement> species = new ArrayList<>();
        for( DiagramElement de : compartment )
        {
            if( isSpecie( de ) )
            {
                species.add( de );
                View view = diagramViewBuilder.createNodeView( (Node)de, viewOptions, graphics );
                de.setView( view );
                Rectangle bounds = view.getBounds();
                nodeWidth = Math.max( nodeWidth, bounds.width );
                nodeHeight = Math.max( nodeHeight, bounds.height );
            }
        }

        // calculate number of species in row and row number
        int n = species.size();
        int nX = Math.max( (int)Math.sqrt( n ) + 1, rect.width / ( nodeWidth + layerDeltaX ) );
        int nY = n / nX;
        if( nY * nX != n )
        {
            nY++;

            // layout species
        }
        for( int k = 0; k < nY && n > 0; k++ )
        {
            y = rect.y + rect.height + ( layerDeltaY + nodeHeight ) * ( k );

            for( int m = 0; m < nX; m++ )
            {
                if( m + nX * k >= n )
                    break;

                Node node = (Node)species.get( m + nX * k );
                Rectangle bounds = node.getView().getBounds();
                x = rect.x + ( nodeWidth + layerDeltaX ) * m;
                x += ( nodeWidth - bounds.width ) / 2;
                node.setLocation( x, y );
                node.getView().setLocation( x, y );
            }
        }

        // layout reactions
        for( DiagramElement de : compartment )
        {
            if( isReaction( de ) )
                diagramViewBuilder.locateReaction( (Node)de, viewOptions, graphics );
        }

        // calculate compartment shape size and location
        x = rect.x;
        y = rect.y;

        int width = layerDeltaX;
        int height = layerDeltaY;

        // place for species
        if( n > 0 )
        {
            int dx = ( nodeWidth + layerDeltaX ) * nX - nodeWidth;
            int dy = ( nodeHeight + layerDeltaY ) * nY;
            //y -= dy;
            width += dx;
            height += ( dy - nodeHeight );
        }

        // place for compartments
        if( rect.width > 1 )
        {
            width += rect.width;
            height += rect.height;
        }

        compartment.setLocation( x, y );

        if( compartment instanceof SubDiagram )
        {
            width = Math.max( width, 296 );
            height = Math.max( height, 140 );
        }

        compartment.setShapeSize( new Dimension( width, height ) );
    }

    public boolean isSpecie(DiagramElement de)
    {
        return de instanceof Node
                && ! ( de instanceof Compartment || de.getKernel() == null || de.getKernel().getType().equals( Type.TYPE_REACTION ) );
    }

    public boolean isReaction(DiagramElement de)
    {
        return de instanceof Node && de.getKernel() instanceof Reaction;
    }

    public static class SpecificalHashMap extends HashMap
    {
        @Override
        public boolean containsKey(Object key)
        {
            boolean result = super.containsKey( key );

            if( !result )
            {
                for( Object obj : keySet() )
                {
                    if( obj.toString().endsWith( "/" + key.toString() ) )
                        return true;
                }
            }

            return result;
        }

        @Override
        public Object get(Object key)
        {
            Object result = super.get( key );

            if( result == null )
            {
                for( Object obj : keySet() )
                {
                    if( obj.toString().endsWith( "/" + key.toString() ) )
                        return super.get( obj );
                }
            }

            return result;
        }
    }

    protected static class SpecieRefenceInfo
    {
        String id;
        String type;
        Element element;

        public SpecieRefenceInfo(String id, String referenceType, Element element)
        {
            this.id = id;
            this.type = referenceType;
            this.element = element;
        }
    }

    public String getRateVariableName(String reactionID)
    {
        return "$$rate_" + reactionID;
    }

    public Unit readUnitDefinition(Element element, String unitId, String unitTitle) throws Exception
    {
        Unit unit = new Unit( null, unitId );
        unit.setTitle( unitTitle );

        Element unitDefinitionList = getElement( element, UNIT_LIST_ELEMENT );
        if( unitDefinitionList == null )
            return unit;

        NodeList list = unitDefinitionList.getElementsByTagName( UNIT_ELEMENT );

        List<BaseUnit> baseUnitsList = new ArrayList<>();
        int length = list.getLength();
        for( int i = 0; i < length; i++ )
        {
            String baseUnitId = "";
            try
            {
                Element baseUnitElement = (Element)list.item( i );
                baseUnitId = getId( baseUnitElement );
                BaseUnit baseUnit = new BaseUnit();
                baseUnit.setUnitId( unitId );

                if( baseUnitElement.hasAttribute( UNIT_KIND_ATTR ) )
                    baseUnit.setType( baseUnitElement.getAttribute( UNIT_KIND_ATTR ) );
                if( baseUnitElement.hasAttribute( UNIT_SCALE_ATTR ) )
                    baseUnit.setScale( Integer.parseInt( baseUnitElement.getAttribute( UNIT_SCALE_ATTR ) ) );
                if( baseUnitElement.hasAttribute( UNIT_EXPONENT_ATTR ) )
                    baseUnit.setExponent( Integer.parseInt( baseUnitElement.getAttribute( UNIT_EXPONENT_ATTR ) ) );
                if( baseUnitElement.hasAttribute( UNIT_MULTIPLIER_ATTR ) )
                    baseUnit.setMultiplier( Double.parseDouble( baseUnitElement.getAttribute( UNIT_MULTIPLIER_ATTR ) ) );

                baseUnitsList.add( baseUnit );
            }
            catch( Throwable t )
            {
                error( "ERROR_UNIT_PROCESSING", new String[] {modelName, baseUnitId, t.getMessage()} );
            }
        }
        if( baseUnitsList.size() > 0 )
            unit.setBaseUnits( baseUnitsList.toArray( new BaseUnit[baseUnitsList.size()] ) );

        return unit;
    }

    protected Map<String, String> createReplacements(Element element)
    {
        Set<String> ids = new HashSet<String>(); //read all ids from document
        readIds( element, ids );

        Map<String, String> result = new HashMap<>();
        for( String id : ids )
        {
            if( isForbidden( id ) )
            {
                String newId = generateName( id, "_", parserConstants, ids );
                result.put( id, newId );
            }
        }
        return result;
    }

    protected String checkReplacement(String id)
    {
        String result = replacements.get( id );
        return result == null ? id : result;
    }

    private boolean isForbidden(String name)
    {
        return parserConstants.contains( name );
    }

    protected String normalize(String name)
    {
        if( emodel.containsConstant( name ) )
            return name + "_CONFLICTS_WITH_CONSTANT_";

        if( name.equals( "time" ) )
            return "_CONFLICTS_WITH_TIME_";

        return checkReplacement( name );
    }

    public static String generateName(String baseName, String suffix, Set<String> forbidden, Set<String> existing)
    {
        String name = baseName;
        while( forbidden.contains( name ) || existing.contains( name ) )
        {
            name += suffix;
        }
        return name;
    }

    public static Set<String> parserConstants = new HashSet<String>()
    {
        {
            add( "function" );
            add( "diff" );
            add( "piecewise" );
            add( "xor" );
        }
    };
}
