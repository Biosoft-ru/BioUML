package biouml.plugins.antimony;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.undo.Transaction;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModel.NodeFilter;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.astparser_v2.AntimonyNotationParser;
import biouml.plugins.antimony.astparser_v2.AstAfter;
import biouml.plugins.antimony.astparser_v2.AstAssert;
import biouml.plugins.antimony.astparser_v2.AstAt;
import biouml.plugins.antimony.astparser_v2.AstColon;
import biouml.plugins.antimony.astparser_v2.AstColonEqual;
import biouml.plugins.antimony.astparser_v2.AstComma;
import biouml.plugins.antimony.astparser_v2.AstComment;
import biouml.plugins.antimony.astparser_v2.AstConnectionConversionFactor;
import biouml.plugins.antimony.astparser_v2.AstConversionFactor;
import biouml.plugins.antimony.astparser_v2.AstDelete;
import biouml.plugins.antimony.astparser_v2.AstEOL;
import biouml.plugins.antimony.astparser_v2.AstElse;
import biouml.plugins.antimony.astparser_v2.AstEqual;
import biouml.plugins.antimony.astparser_v2.AstEqualZero;
import biouml.plugins.antimony.astparser_v2.AstEquation;
import biouml.plugins.antimony.astparser_v2.AstFunction;
import biouml.plugins.antimony.astparser_v2.AstHas;
import biouml.plugins.antimony.astparser_v2.AstImport;
import biouml.plugins.antimony.astparser_v2.AstImportAnnotation;
import biouml.plugins.antimony.astparser_v2.AstIn;
import biouml.plugins.antimony.astparser_v2.AstIs;
import biouml.plugins.antimony.astparser_v2.AstLocateFunction;
import biouml.plugins.antimony.astparser_v2.AstModel;
import biouml.plugins.antimony.astparser_v2.AstPersistent;
import biouml.plugins.antimony.astparser_v2.AstPlus;
import biouml.plugins.antimony.astparser_v2.AstPriority;
import biouml.plugins.antimony.astparser_v2.AstProduct;
import biouml.plugins.antimony.astparser_v2.AstProperty;
import biouml.plugins.antimony.astparser_v2.AstRateEqual;
import biouml.plugins.antimony.astparser_v2.AstReactant;
import biouml.plugins.antimony.astparser_v2.AstReactionTitle;
import biouml.plugins.antimony.astparser_v2.AstReactionType;
import biouml.plugins.antimony.astparser_v2.AstRegularFormulaElement;
import biouml.plugins.antimony.astparser_v2.AstRelationshipType;
import biouml.plugins.antimony.astparser_v2.AstSemicolon;
import biouml.plugins.antimony.astparser_v2.AstSingleProperty;
import biouml.plugins.antimony.astparser_v2.AstSpecialFormula;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.plugins.antimony.astparser_v2.AstStoichiometry;
import biouml.plugins.antimony.astparser_v2.AstSubSymbol;
import biouml.plugins.antimony.astparser_v2.AstSubstanceOnly;
import biouml.plugins.antimony.astparser_v2.AstSubtype;
import biouml.plugins.antimony.astparser_v2.AstSymbol;
import biouml.plugins.antimony.astparser_v2.AstSymbolType;
import biouml.plugins.antimony.astparser_v2.AstText;
import biouml.plugins.antimony.astparser_v2.AstTriggerInitialValue;
import biouml.plugins.antimony.astparser_v2.AstUnit;
import biouml.plugins.antimony.astparser_v2.AstUnitFormula;
import biouml.plugins.antimony.astparser_v2.AstUseValuesFromTriggerTime;
import biouml.plugins.antimony.astparser_v2.AstVarOrConst;
import biouml.plugins.antimony.astparser_v2.SimpleNode;
import biouml.plugins.antimony.astparser_v2.Token;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnComplexStructureManager;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.InputConnectionPort;
import biouml.standard.type.Stub.OutputConnectionPort;
import biouml.standard.type.Type;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.util.DPSUtils;

public class AntimonyAstCreator
{
    protected static final Logger log = Logger.getLogger(AntimonyAstCreator.class.getName());

    /**
     * Contains block names and corresponding last nodes of the block
     */
    protected HashMap<String, biouml.plugins.antimony.astparser_v2.Node> blockLocations = new HashMap<String, biouml.plugins.antimony.astparser_v2.Node>();
    Diagram diagram;

    public AntimonyAstCreator(Diagram diagram)
    {
        this.diagram = diagram;
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    /**
     * If diagram have attribute "Antimony", then method returned astTree
     * which generated from attribute.
     * If haven't, then generate astTree from diagram
     * @param diagram
     * @return astTree
     */
    public AstStart getAST()
    {
        AstStart start = new AstStart(0);
        try
        {
            String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            String antimonyVersion = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_VERSION_ATTR);
            if( antimonyText != null && antimonyVersion != null && !antimonyVersion.equals("1.0") )
            {
                Antimony antimony = new Antimony(null);
                start = antimony.generateAstFromText(antimonyText);
            }
            else
            {
                cleanLink(diagram);

                biouml.plugins.antimony.astparser_v2.Node modelParent = start;
                if( DiagramUtility.isComposite(diagram) && DiagramUtility.getInterfacePorts(diagram).toList().size() == 0 )
                {
                    modelParent = start.createOutsideModelNode();
                    writeAnnotationImports(diagram, modelParent);
                    writeUnitDefinitions(diagram, modelParent);
                    writeFunction(diagram, modelParent);
                    writeModel(diagram, modelParent, false, -1);
                }
                else
                {
                    writeAnnotationImports(diagram, modelParent);
                    writeUnitDefinitions(diagram, modelParent);
                    writeFunction(diagram, modelParent);
                    writeModel(diagram, modelParent, true, -1);
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't create ASTree from diagram: " + e.getMessage());
        }
        return start;
    }

    private void writeAnnotationImports(Diagram diagram, biouml.plugins.antimony.astparser_v2.Node start)
    {
        for( String annotationName : AntimonyAnnotationImporter.annotations.keySet() )
        {
            AstImportAnnotation imp = new AstImportAnnotation(AntimonyNotationParser.JJTIMPORTANNOTATION);
            imp.setAnnotationType(annotationName);

            AstEqual eq = new AstEqual(AntimonyNotationParser.JJTEQUAL);
            createSpace(eq);
            imp.addAsLast(eq);

            AstText text = new AstText(AntimonyNotationParser.JJTTEXT);
            StringBuilder path = new StringBuilder();
            path.append("\"").append(Antimony.PATH_ANNOTATIONS).append(annotationName).append(".yaml\"");
            text.setText(path.toString());
            createSpace(text);
            imp.addAsLast(text);

            imp.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            start.addAsLast(imp);
            start.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }
    }

    /**
     * 
     * @param diagram
     * @param parent
     * @param withSignature
     * @param indexToPlace - use -1 value to add astModel as last
     * @return
     * @throws Exception
     */
    private AstModel writeModel(Diagram diagram, biouml.plugins.antimony.astparser_v2.Node parent, boolean withSignature, int indexToPlace)
            throws Exception
    {
        AstModel astModel;
        if( !withSignature )
            astModel = ( (AstModel)parent );
        else
        {
            astModel = new AstModel(AntimonyNotationParser.JJTMODEL);

            AstSymbol symbolName = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            createSpace(symbolName);
            astModel.addAsLast(symbolName);

            //add brackets
            AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            lb.setElement("(");
            AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            rb.setElement(")");
            symbolName.addAsLast(lb);
            symbolName.addAsLast(rb);

            astModel.setModelName(AntimonyUtility.getAntimonyValidDiagramName(diagram.getName()));
            if( indexToPlace > -1 )
                parent.addWithDisplacement(astModel, indexToPlace);
            else
                parent.addAsLast(astModel);
        }

        writePorts(diagram, astModel);
        writeModelDefinitions(diagram, astModel);
        writeSubdiagrams(diagram, astModel);
        writeCompartmentsAndSpecies(diagram, astModel);
        writeSubtypes(diagram, astModel);
        writeInitializations(diagram, astModel);
        writeUnitAssignments(diagram, astModel);
        writeReactions(diagram, astModel);
        writeConstant(diagram, astModel);
        writeEquations(diagram, astModel);
        writeEvents(diagram, astModel);
        writeConstraints(diagram, astModel);
        writeDisplayNames(diagram, astModel);
        writeCVterms(diagram, astModel);
        writeConnections(diagram, astModel);
        writeProperties(diagram, astModel);

        // if model is empty (contain only name)
        if( astModel.jjtGetNumChildren() < 2 )
            astModel.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        // for "end"
        if( indexToPlace > -1 )
            parent.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), indexToPlace + 1);
        else
            parent.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));

        return astModel;
    }

    private void writeCVterms(Diagram diagram, AstModel astModel)
    {
        List<Node> compartmentNodes = AntimonyUtility.getCompartmentNodes(diagram);
        List<Node> specieNodes = AntimonyUtility.getSpecieNodes(diagram, diagram.getType() instanceof SbgnDiagramType);
        List<Node> geneNodes = AntimonyUtility.getGeneNode(diagram, diagram.getType() instanceof SbgnDiagramType);
        List<Node> reactionNodes = DiagramUtility.getReactionNodes(diagram);


        int astModelLen = astModel.jjtGetNumChildren();

        for( Node node : compartmentNodes )
            addCVterms(node, astModel);

        for( Node node : specieNodes )
            addCVterms(node, astModel);

        for( Node node : geneNodes )
            addCVterms(node, astModel);

        for( Node node : reactionNodes )
            addCVterms(node, astModel);

        addCVterms(diagram, astModel);

        if( astModelLen != astModel.jjtGetNumChildren() )
        {
            astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), astModelLen);
            addCommentWithDisplacement(astModel, AntimonyConstants.BLOCK_CV_TERMS, astModelLen + 1);

            for( int i = astModel.jjtGetNumChildren() - 1; i >= 0; i-- )
                if( AntimonyUtility.isDatabaseReferenceAstSymbol(astModel.jjtGetChild(i)) )
                {
                    blockLocations.put(AntimonyConstants.BLOCK_CV_TERMS, astModel.jjtGetChild(i));
                    break;
                }
        }
    }

    private void addCVterms(Node node, AstModel astModel)
    {
        DatabaseReference[] drs = ( (Referrer)node.getKernel() ).getDatabaseReferences();

        if( drs == null )
            return;

        Map<String, List<String>> termsByType = new HashMap<>();

        for( DatabaseReference dr : drs )
        {
            if( dr.getDatabaseName() == null || dr.getDatabaseName().isEmpty() || dr.getId() == null || dr.getId().isEmpty()
                    || dr.getRelationshipType() == null || dr.getRelationshipType().isEmpty() )
                continue;
            String value = "http://identifiers.org/" + dr.getDatabaseName() + "/" + dr.getId();
            String type = dr.getRelationshipType().equals("is") ? "identity" : dr.getRelationshipType();
            if( termsByType.get(type) == null )
                termsByType.put(type, new ArrayList<>());

            termsByType.get(type).add(value);
        }

        for( Map.Entry<String, List<String>> tbt : termsByType.entrySet() )
        {
            addRelTypeCVterms(tbt.getKey(), tbt.getValue(), node, astModel, false);

        }


    }

    protected void addRelTypeCVterms(String reltype, List<String> cvterms, Node node, AstModel astModel, boolean updateBlockLocation)
    {
        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        symbol.setName(validAndNotReservedName(node.getName()));
        symbol.setTypeDeclaration(AstSymbol.DATABASE_REFERENCE);

        AstRelationshipType relType = new AstRelationshipType(AntimonyNotationParser.JJTRELATIONSHIPTYPE);
        relType.setName(reltype);
        createSpace(relType);
        symbol.addAsLast(relType);

        for( int i = 0; i < cvterms.size(); i++ )
        {
            AstText message = new AstText(AntimonyNotationParser.JJTTEXT);
            message.setText("\"" + cvterms.get(i) + "\"");
            createSpace(message);
            symbol.addAsLast(message);
            if( i < cvterms.size() - 1 )
                symbol.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
        }
        createIndent(symbol);
        symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_CV_TERMS) )
            index = astModel.findIndex(blockLocations.get(AntimonyConstants.BLOCK_CV_TERMS)) + 2;
        else
            index = astModel.jjtGetNumChildren();

        astModel.addWithDisplacement(symbol, index);
        astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        linkWithAssociatedDiagramElements(node, symbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_CV_TERMS, astModel.jjtGetChild(index));
    }

    private void writeConstraints(Diagram diagram, AstModel astModel)
    {
        EModel emodel = (EModel)diagram.getRole();
        if( emodel == null )
            return;

        Constraint[] constraints = emodel.getConstraints();
        if( constraints.length == 0 )
            return;

        astModel.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(astModel, AntimonyConstants.BLOCK_CONSTRAINTS);

        for( Constraint constraint : constraints )
        {
            addConstraint(constraint, astModel, false);
        }

        for( int i = astModel.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isConstraintAstSymbol(astModel.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_CONSTRAINTS, astModel.jjtGetChild(i));
                break;
            }
    }

    protected void addConstraint(Constraint constr, AstModel astModel, boolean updateBlockLocation)
    {
        Node constrNode = (Node)constr.getDiagramElement();

        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        symbol.setName(validAndNotReservedName(constrNode.getName()));
        symbol.setTypeDeclaration(AstSymbol.CONSTRAINT);

        AstColon colon = new AstColon(AntimonyNotationParser.JJTCOLON);
        AstAssert ass = new AstAssert(AntimonyNotationParser.JJTASSERT);
        createSpace(ass);
        AstEquation equation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
        setFormulaToAstEquation(constr.getFormula(), equation);

        if( equation.jjtGetChild(0) != null )
            createSpace((SimpleNode)equation.jjtGetChild(0));

        symbol.addAsLast(colon);
        symbol.addAsLast(ass);
        ass.addAsLast(equation);

        String str = constr.getMessage();
        if( str != null && !str.isEmpty() )
        {
            AstElse els = new AstElse(AntimonyNotationParser.JJTELSE);
            createSpace(els);

            AstText message = new AstText(AntimonyNotationParser.JJTTEXT);
            message.setText("\"" + constr.getMessage() + "\"");
            createSpace(message);

            symbol.addAsLast(els);
            symbol.addAsLast(message);
        }

        createIndent(symbol);
        symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_CONSTRAINTS) )
            index = astModel.findIndex(blockLocations.get(AntimonyConstants.BLOCK_CONSTRAINTS)) + 2;
        else
            index = astModel.jjtGetNumChildren();

        astModel.addWithDisplacement(symbol, index);
        astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(constrNode, symbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_CONSTRAINTS, astModel.jjtGetChild(index));
    }

    private void writeUnitAssignments(Diagram diagram, AstModel astModel)
    {
        List<Variable> vars = diagram.getRole(EModel.class).getVariableRoles().stream().collect(Collectors.toList());
        vars.addAll(AntimonyUtility.getParameters(diagram));

        if( vars.stream().filter(n -> !n.getUnits().equals(Unit.UNDEFINED) && n.getInitialValue() == 0).count() == 0 )
            return;

        astModel.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(astModel, AntimonyConstants.BLOCK_UNIT_ASSIGNMENTS);

        for( Variable var : vars )
            addUnitAssignment(var, astModel, false);

        for( int i = astModel.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isHasUnitAstSymbol(astModel.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_UNIT_ASSIGNMENTS, astModel.jjtGetChild(i));
                break;
            }
    }

    protected void addUnitAssignment(Variable var, AstModel astModel, boolean updateBlockLocation)
    {
        if( var.getUnits().equals(Unit.UNDEFINED) || var.getInitialValue() != 0 )
            return;

        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        SimpleNode has = new AstHas(AntimonyNotationParser.JJTHAS);
        AstSymbol unitName = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);

        symbol.setTypeDeclaration(AstSymbol.SET_UNIT);
        symbol.setName(validAndNotReservedName(var.getName()));
        createIndent(symbol);
        createSpace(has);
        unitName.setName(validAndNotReservedName(var.getUnits()));
        createSpace(unitName);

        has.addAsLast(unitName);
        symbol.addAsLast(has);
        symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_UNIT_ASSIGNMENTS) )
            index = astModel.findIndex(blockLocations.get(AntimonyConstants.BLOCK_UNIT_ASSIGNMENTS)) + 2;
        else
            index = astModel.jjtGetNumChildren();

        astModel.addWithDisplacement(symbol, index);
        astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        if( var instanceof VariableRole )
            link( ( (VariableRole)var ).getDiagramElement(), symbol);
        else
            link(var.getAttributes(), symbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_UNIT_ASSIGNMENTS, astModel.jjtGetChild(index));
    }

    private void writeUnitDefinitions(Diagram diagram, biouml.plugins.antimony.astparser_v2.Node start)
    {

        EModel emodel = (EModel)diagram.getRole();
        if( emodel == null || emodel.getUnits().isEmpty() )
            return;

        ArrayList<Unit> units = new ArrayList<>(emodel.getUnits().values());

        if( units.size() == 0 )
            return;

        addComment(start, AntimonyConstants.BLOCK_UNITS);

        for( Unit unit : units )
            addUnit(unit, start);

        for( int i = start.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( start.jjtGetChild(i) instanceof AstUnit )
            {
                blockLocations.put(AntimonyConstants.BLOCK_UNITS, start.jjtGetChild(i));
                break;
            }
        start.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));

    }

    private void addUnit(Unit unit, biouml.plugins.antimony.astparser_v2.Node start)
    {
        AstUnit astUnit = new AstUnit(AntimonyNotationParser.JJTUNIT);
        AstSymbol symbol = createSymbol(validAndNotReservedName(unit.getName()));
        String unitFormula = unit.getUnitFormula();
        if( unitFormula.isEmpty() )
        {
            astUnit.addAsLast(symbol);
            createSpace(symbol);
        }
        else
        {
            SimpleNode equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
            AstUnitFormula formula = new AstUnitFormula(AntimonyNotationParser.JJTUNITFORMULA);
            setFormulaToUnitFormula(unitFormula, formula);
            createSpace(symbol);
            createSpace(equal);
            astUnit.addAsLast(symbol);
            astUnit.addAsLast(equal);
            astUnit.addAsLast(formula);

            if( formula.jjtGetChild(0) != null )
                createSpace((SimpleNode)formula.jjtGetChild(0));
        }
        astUnit.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        start.addAsLast(astUnit);
        start.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        link( ( (Unit)unit ).getAttributes(), astUnit);
    }

    private void writeSubdiagrams(Diagram diagram, AstModel astModel)
    {
        for( SubDiagram model : AntimonyUtility.getSubdiagrams(diagram) )
            addSubdiagram(astModel, model);
    }

    protected void addSubdiagram(AstModel astModel, SubDiagram model)
    {
        if( ! ( astModel.jjtGetChild(astModel.jjtGetNumChildren() - 1) instanceof AstEOL ) )
            astModel.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        AstSymbol astSubdiagram = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        astSubdiagram.setTypeDeclaration(AstSymbol.SUBDIAGRAM);
        astSubdiagram.setName(AntimonyUtility.getAntimonyValidName(model.getName()));
        createIndent(astSubdiagram);

        AstSymbol astDiagramDefinition = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        Diagram innerDiagram = model.getDiagram();
        String nameRefModelDefinition = innerDiagram.getName();
        astDiagramDefinition.setName(AntimonyUtility.getAntimonyValidName(nameRefModelDefinition));
        createSpace(astDiagramDefinition);

        // seek ModelDefinition for this SubDiagram
        ModelDefinition refModelDefinition = (ModelDefinition)innerDiagram.getAttributes().getValue(ModelDefinition.REF_MODEL_DEFINITION);

        // seek ModelDefinition for this SubDiagram
        boolean importExist = false;
        nameRefModelDefinition = refModelDefinition == null ? nameRefModelDefinition : refModelDefinition.getName();
        for( int i = 0; i < astModel.jjtGetNumChildren(); i++ )
        {
            SimpleNode astNode = (SimpleNode)astModel.jjtGetChild(i);
            if( astNode instanceof AstImport && ( (AstImport)astNode ).getPath().equals(nameRefModelDefinition) )
            {
                importExist = true;
                break;
            }
        }
        //add import for external diagram
        if( refModelDefinition == null && !importExist )
        {
            innerDiagram.getCompletePath();

            String topLevelPath = diagram.getOrigin().getCompletePath().toString();
            String modulePath = innerDiagram.getCompletePath().toString();

            if( modulePath.startsWith(topLevelPath + "/") )
                modulePath = modulePath.replaceFirst(topLevelPath + "/", "");

            //URI uriDiagram = URI.create(diagram.getOrigin().getCompletePath().toString());
            //URI uriImportDiagram = URI.create(path.toString());

            AstImport astImport = new AstImport(AntimonyNotationParser.JJTIMPORT);
            createIndent(astImport);
            AstText astText = new AstText(AntimonyNotationParser.JJTTEXT);
            astText.setText("\"" + modulePath + "\"");
            //            astText.setText("\"" + uriDiagram.relativize(uriImportDiagram) + "\"");
            createSpace(astText);
            astImport.addAsLast(astText);
            astImport.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            astModel.addAsLast(astImport);
            astModel.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        astModel.addAsLast(astSubdiagram);
        astSubdiagram.addAsLast(new AstColon(AntimonyNotationParser.JJTCOLON));
        astSubdiagram.addAsLast(astDiagramDefinition);

        AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        lb.setElement("(");
        AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        rb.setElement(")");
        astDiagramDefinition.addAsLast(lb);
        astDiagramDefinition.addAsLast(rb);

        DynamicPropertySet dps = model.getAttributes();
        DynamicProperty dp = dps.getProperty(Util.TIME_SCALE);
        if( dp != null && dp.getValue() != null && !dp.getValue().toString().isEmpty() )
        {
            AstConversionFactor astFactor = createConversionFactor(dp, AstConversionFactor.TIME_CONVERSION_FACTOR);
            astSubdiagram.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
            astSubdiagram.addAsLast(astFactor);
        }

        dp = dps.getProperty(Util.EXTENT_FACTOR);
        if( dp != null && dp.getValue() != null && !dp.getValue().toString().isEmpty() )
        {
            AstConversionFactor astFactor = createConversionFactor(dp, AstConversionFactor.EXTENT_CONVERSION_FACTOR);
            astSubdiagram.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
            astSubdiagram.addAsLast(astFactor);
        }

        astSubdiagram.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        astSubdiagram.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));

        State currentState = innerDiagram.getCurrentState();
        if( currentState != null )
        {
            List<UndoableEdit> edits = currentState.getStateUndoManager().getEdits();
            addStateEdits(astModel, model, edits);
        }
        link(model, astSubdiagram);
    }

    private void addStateEdits(AstModel astModel, SubDiagram model, List<UndoableEdit> edits)
    {
        for( UndoableEdit edit : edits )
        {
            if( edit instanceof Transaction )
                addStateEdits(astModel, model, ( (Transaction)edit ).getEdits());
            if( ! ( edit instanceof DataCollectionRemoveUndo ) )
                continue;
            DataElement de = ( (DataCollectionRemoveUndo)edit ).getDataElement();
            if( ! ( de instanceof Node ) )
                continue;
            AstDelete delete = new AstDelete(AntimonyNotationParser.JJTDELETE);
            AstSubSymbol symbol = new AstSubSymbol(AntimonyNotationParser.JJTSUBSYMBOL);
            createSpace(symbol);
            String name = validAndNotReservedName(model.getName());
            name += "." + validAndNotReservedName(de.getName());
            symbol.setName(name);
            delete.addAsLast(symbol);
            delete.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            delete.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
            astModel.addAsLast(delete);
            link(model, delete);
        }
    }

    protected AstConversionFactor createConversionFactor(DynamicProperty dp, String factor)
    {
        AstConversionFactor astFactor = new AstConversionFactor(AntimonyNotationParser.JJTCONVERSIONFACTOR);
        astFactor.setFactor(factor);
        createSpace(astFactor);
        AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
        createSpace(equal);
        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        symbol.setName(AntimonyUtility.getAntimonyValidName(dp.getValue().toString()));
        createSpace(symbol);
        astFactor.addAsLast(equal);
        astFactor.addAsLast(symbol);
        return astFactor;
    }

    private void writeModelDefinitions(Diagram diagram, AstModel astModel) throws Exception
    {
        for( ModelDefinition model : AntimonyUtility.getModelDefinitions(diagram) )
            addModelDefinition(astModel, model, false);

        for( int i = astModel.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isAstModel(astModel.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_MODEL_DEFINITIONS, astModel.jjtGetChild(i));
                break;
            }
    }

    protected void addModelDefinition(biouml.plugins.antimony.astparser_v2.Node parent, ModelDefinition model, boolean updateBlockLocation)
            throws Exception
    {
        int indexToPlace = 1;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_MODEL_DEFINITIONS) )
            indexToPlace = parent.findIndex(blockLocations.get(AntimonyConstants.BLOCK_MODEL_DEFINITIONS)) + 2;
        else
            indexToPlace = parent.jjtGetNumChildren();

        if( ! ( parent.jjtGetChild(indexToPlace) instanceof AstEOL ) )
            parent.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), indexToPlace);

        AstModel modelDefinition = writeModel(model.getDiagram(), parent, true, indexToPlace + 1);
        link(model, modelDefinition);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_REACTIONS, parent.jjtGetChild(indexToPlace + 1));
    }

    private void writePorts(Diagram diagram, AstModel astModel) throws Exception
    {
        for( Node port : Util.getPorts(diagram).filter(n -> Util.isPublicPort(n)) )
            addPort(port, astModel);
    }

    protected void addPort(DiagramElement port, AstModel astModel) throws Exception
    {
        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        DynamicProperty dp = port.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR);
        if( dp != null )
        {
            String name = dp.getValue().toString();
            if( Util.isPropagatedPort(port) )
            {
                Node basePort = Util.getBasePort((Node)port);
                Compartment compartment = basePort.getCompartment();
                String variable = Util.getPortVariable(basePort);
                name = compartment.getCompleteNameInDiagram() + "." + variable;
            }
            if( port.getKernel() instanceof InputConnectionPort )
            {
                name = ">" + name;
            }
            else if( port.getKernel() instanceof OutputConnectionPort )
            {
                name = "<" + name;
            }
            symbol.setName(validAndNotReservedName(name));
            astModel.addParameter(symbol);
            link(port, symbol);
        }
    }

    private void writeFunction(Diagram diagram, biouml.plugins.antimony.astparser_v2.Node start)
    {
        EModel emodel = (EModel)diagram.getRole();
        if( emodel == null )
            return;

        Function[] functions = emodel.getFunctions();

        if( functions.length == 0 )
            return;

        addComment(start, AntimonyConstants.BLOCK_FUNCTIONS);
        for( Function function : emodel.getFunctions() )
        {
            addFunction(start, function, false);
        }

        for( int i = start.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( start.jjtGetChild(i) instanceof AstFunction )
            {
                blockLocations.put(AntimonyConstants.BLOCK_FUNCTIONS, start.jjtGetChild(i));
                break;
            }
    }

    protected void addFunction(biouml.plugins.antimony.astparser_v2.Node start, Function function, boolean updateBlockLocation)
    {
        AstFunction astFunction = new AstFunction(AntimonyNotationParser.JJTFUNCTION);
        DiagramElement de = function.getDiagramElement();

        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        createSpace(symbol);

        AstRegularFormulaElement leftBraket = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        leftBraket.setElement("(");

        AstRegularFormulaElement rightBraket = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        rightBraket.setElement(")");

        AstEquation newAstEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);

        astFunction.addAsLast(symbol);
        symbol.addAsLast(leftBraket);
        symbol.addAsLast(rightBraket);
        astFunction.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));

        astFunction.addAsLast(newAstEquation);
        astFunction.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        astFunction.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));

        setFormulaToAstFunction(function.getFormula(), astFunction);

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_FUNCTIONS) )
            index = start.findIndex(blockLocations.get(AntimonyConstants.BLOCK_FUNCTIONS)) + 2;
        else
            index = start.jjtGetNumChildren();

        if( ! ( start.jjtGetChild(index) instanceof AstEOL ) )
        {
            start.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index);
            index++;
        }

        start.addWithDisplacement(astFunction, index);
        start.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(de, astFunction);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_FUNCTIONS, start.jjtGetChild(index));
    }

    private void writeConnections(Diagram diagram, AstModel modelNode) throws Exception
    {
        List<DiagramElement> connectionEdges = AntimonyUtility.getPortConnectionEdges(diagram);

        if( connectionEdges.size() == 0 )
            return;

        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, AntimonyConstants.BLOCK_CONNECTIONS);

        for( DiagramElement connection : connectionEdges )
            addConnection((Edge)connection, modelNode, false);

        for( int i = modelNode.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isSynchronizationsSymbol(modelNode.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_CONNECTIONS, modelNode.jjtGetChild(i));
                break;
            }
    }

    protected void addComment(biouml.plugins.antimony.astparser_v2.Node modelNode, String comment)
    {
        AstComment astComment = new AstComment(comment);
        createIndent(astComment);
        modelNode.addAsLast(astComment);
        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
    }

    protected void addCommentWithDisplacement(biouml.plugins.antimony.astparser_v2.Node modelNode, String comment, int i)
    {
        AstComment astComment = new AstComment(comment);
        createIndent(astComment);
        modelNode.addWithDisplacement(astComment, i);
        modelNode.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), i + 1);
    }

    protected void addConnection(Edge connection, AstModel model, boolean updateBlockLocation) throws Exception
    {
        Node outputNode = connection.getOutput();
        Node inputNode = connection.getInput();

        // antimony doesn't contain intermediate connections of propagated ports
        if( Util.isPropagatedPort(inputNode) && Util.isPropagatedPort(outputNode)
                && ! ( inputNode.getParent() instanceof SubDiagram && outputNode.getParent() instanceof SubDiagram ) )
            return;

        AstSymbol replacedVarSymbol;
        AstSymbol mainVarSymbol;

        if( connection.getRole() instanceof UndirectedConnection && UndirectedConnection.MainVariableType.OUTPUT
                .equals(connection.getRole(UndirectedConnection.class).getMainVariableType()) )
        {
            replacedVarSymbol = createSubSymbol(inputNode);
            mainVarSymbol = createSubSymbol(outputNode);
        }
        else
        {
            replacedVarSymbol = createSubSymbol(outputNode);
            mainVarSymbol = createSubSymbol(inputNode);
        }

        createIndent(replacedVarSymbol);
        replacedVarSymbol.setTypeDeclaration(AstSymbol.SYNCHRONIZATIONS);

        DynamicProperty dp = connection.getAttributes().getProperty("conversionFactor");
        if( dp != null )
        {
            AstConnectionConversionFactor factor = new AstConnectionConversionFactor(AntimonyNotationParser.JJTCONNECTIONCONVERSIONFACTOR);
            createSpace(factor);
            AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            symbol.setName(AntimonyUtility.getAntimonyValidName(dp.getValue().toString()));
            createSpace(symbol);
            factor.addAsLast(symbol);
            replacedVarSymbol.addAsLast(factor);
        }

        AstIs is = new AstIs(AntimonyNotationParser.JJTIS);
        createSpace(mainVarSymbol);
        is.addAsLast(mainVarSymbol);
        createSpace(is);
        AstSemicolon semocolon = new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON);
        replacedVarSymbol.addAsLast(is);
        replacedVarSymbol.addAsLast(semocolon);

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_CONNECTIONS) )
            index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_CONNECTIONS)) + 2;
        else
            index = model.jjtGetNumChildren();

        model.addWithDisplacement(replacedVarSymbol, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_CONNECTIONS, model.jjtGetChild(index));

        link(connection, replacedVarSymbol);
    }

    private void writeBuses(Diagram diagram, AstModel modelNode)
    {
        List<VariableRole> buses = DiagramUtility.getBuses(diagram).map(b -> b.getRole(VariableRole.class)).distinct().toList();
        AstSymbolType symbolType;
        if( buses.size() != 0 )
        {
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
            addComment(modelNode, "Buses");
            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);
            symbolType.setType(AstSymbolType.BUS);
            addSymbols(buses, symbolType);
            createIndent(symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            modelNode.addAsLast(symbolType);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }
    }

    private void writeCompartmentsAndSpecies(Diagram diagram, AstModel modelNode)
    {
        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        List<Node> compartmentNodes = AntimonyUtility.getCompartmentNodes(diagram);
        List<Variable> parameters = AntimonyUtility.getParameters(diagram);
        List<Node> specieConcentrationNodes = AntimonyUtility.getSpecieNodes(diagram, diagram.getType() instanceof SbgnDiagramType).stream()
                .filter(n -> VariableRole.CONCENTRATION_TYPE == n.getRole(VariableRole.class).getQuantityType())
                .collect(Collectors.toList());
        List<Node> specieAmountNodes = AntimonyUtility.getSpecieNodes(diagram, diagram.getType() instanceof SbgnDiagramType).stream()
                .filter(n -> VariableRole.AMOUNT_TYPE == n.getRole(VariableRole.class).getQuantityType()).collect(Collectors.toList());
        List<Node> geneConcentrationNodes = AntimonyUtility.getGeneNode(diagram, diagram.getType() instanceof SbgnDiagramType).stream()
                .filter(n -> VariableRole.CONCENTRATION_TYPE == n.getRole(VariableRole.class).getQuantityType())
                .collect(Collectors.toList());
        List<Node> geneAmountNodes = AntimonyUtility.getGeneNode(diagram, diagram.getType() instanceof SbgnDiagramType).stream()
                .filter(n -> VariableRole.AMOUNT_TYPE == n.getRole(VariableRole.class).getQuantityType()).collect(Collectors.toList());

        if( compartmentNodes.size() != 0 || specieConcentrationNodes.size() != 0 || parameters.size() != 0
                || geneConcentrationNodes.size() != 0 || specieAmountNodes.size() != 0 || geneAmountNodes.size() != 0 )
        {
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        AstSymbolType symbolType;
        if( compartmentNodes.size() != 0 )
        {
            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);
            symbolType.setType(AstSymbolType.COMPARTMENT);
            addSymbols(compartmentNodes, symbolType);


            createIndent(symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            modelNode.addAsLast(symbolType);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        if( specieConcentrationNodes.size() != 0 )
        {
            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);

            symbolType.setType(AstSymbolType.SPECIES);
            addSymbols(specieConcentrationNodes, symbolType);

            createIndent(symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            modelNode.addAsLast(symbolType);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        if( specieAmountNodes.size() != 0 )
        {
            AstSubstanceOnly substanceOnly = new AstSubstanceOnly(AntimonyNotationParser.JJTSUBSTANCEONLY);

            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);

            symbolType.setType(AstSymbolType.SPECIES);
            createSpace(symbolType);
            addSymbols(specieAmountNodes, symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

            createIndent(substanceOnly);

            substanceOnly.addAsLast(symbolType);
            modelNode.addAsLast(substanceOnly);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        if( geneConcentrationNodes.size() != 0 )
        {
            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);
            symbolType.setType(AstSymbolType.GENE);
            addSymbols(geneConcentrationNodes, symbolType);

            createIndent(symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            modelNode.addAsLast(symbolType);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        if( geneAmountNodes.size() != 0 )
        {
            AstSubstanceOnly substanceOnly = new AstSubstanceOnly(AntimonyNotationParser.JJTSUBSTANCEONLY);

            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);

            symbolType.setType(AstSymbolType.GENE);
            createSpace(symbolType);
            addSymbols(geneAmountNodes, symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

            createIndent(substanceOnly);

            substanceOnly.addAsLast(symbolType);
            modelNode.addAsLast(substanceOnly);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }

        if( parameters.size() != 0 )
        {
            symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);

            symbolType.setType(AstSymbolType.PARAMETERS);
            addSymbols(parameters, symbolType);

            createIndent(symbolType);
            symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            modelNode.addAsLast(symbolType);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        }
    }

    private void writeSubtypes(Diagram diagram, AstModel modelNode)
    {
        List<Edge> eqEdges = AntimonyUtility.getEquivalenceEdges(diagram);

        if( eqEdges.size() == 0 )
            return;
        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, AntimonyConstants.BLOCK_SUBTYPES);

        for( Edge edge : eqEdges )
        {
            addSubtypes(edge.getInput(), edge.getOutput(), modelNode, false);
        }

        for( int i = modelNode.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isSubtypeAstSymbol(modelNode.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_SUBTYPES, modelNode.jjtGetChild(i));
                break;
            }
    }

    protected void addSubtypes(Node eqNode, Node superNode, AstModel model, boolean updateBlockLocation)
    {
        List<biouml.plugins.antimony.astparser_v2.Node> subtypes = new ArrayList<biouml.plugins.antimony.astparser_v2.Node>();
        List<Node> subtypeNodes = StreamEx.of(eqNode.getEdges()).filter(edge -> !SbgnUtil.isEquivalenceNode(edge.getInput()))
                .map(edge -> edge.getInput()).toList();
        for( Node node : subtypeNodes )
        {
            AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            AstSubtype subtype = new AstSubtype(AntimonyNotationParser.JJTSUBTYPE);
            AstSymbol superName = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);

            symbol.setName(validAndNotReservedName(node.getName()));
            createIndent(symbol);
            symbol.setTypeDeclaration(AstSymbol.SUBTYPE);

            superName.setName(superNode.getName());
            createSpace(superName);

            subtype.addAsLast(superName);
            createSpace(subtype);

            AstSemicolon semicolon = new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON);
            symbol.addAsLast(subtype);
            symbol.addAsLast(semicolon);

            link(node, symbol);
            link(superNode, symbol);
            link(eqNode, symbol);

            subtypes.add(symbol);
        }

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_SUBTYPES) )
            index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_SUBTYPES)) + 2;
        else
            index = model.jjtGetNumChildren();

        for( biouml.plugins.antimony.astparser_v2.Node subtype : subtypes )
        {
            model.addWithDisplacement(subtype, index);
            model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);
            index += 2;
        }

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_SUBTYPES, model.jjtGetChild(index - 2));
    }

    private void writeReactions(Diagram diagram, AstModel modelNode) throws Exception
    {
        List<Node> reactionNodes = DiagramUtility.getReactionNodes(diagram);
        List<Node> phenotypeNodes = AntimonyUtility.getPhenotypeNodes(diagram);
        List<Node> logicalNodes = AntimonyUtility.getLogicalNodes(diagram);
        if( reactionNodes.size() == 0 && phenotypeNodes.size() == 0 && logicalNodes.size() == 0 )
            return;
        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, AntimonyConstants.BLOCK_REACTIONS);

        for( Node reaction : reactionNodes )
        {
            addReaction(reaction, modelNode, true, false);
        }

        for( Node phenotype : phenotypeNodes )
        {
            addPhenotypeReaction(phenotype, modelNode, false);
            addModifier(phenotype, modelNode, phenotype.getEdges()[0], false);
        }

        for( Node node : logicalNodes )
        {
            AstSymbol modReaction = null;
            for( Edge elem : node.getEdges() )
                if( SbgnUtil.isRegulationEdge(elem) )
                {
                    Node reaction = null;
                    if( elem.getInput().getKernel() instanceof Reaction )
                        reaction = elem.getInput();
                    else
                        reaction = elem.getOutput();

                    modReaction = addModifier(reaction, modelNode, elem, false);

                }

            if( modReaction != null )
            {
                link(node, modReaction);
                for( Edge edge : node.getEdges() )
                    link(edge, modReaction);
            }

        }

        for( int i = modelNode.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isReactionAstSymbol(modelNode.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_REACTIONS, modelNode.jjtGetChild(i));
                break;
            }
    }

    private void writeInitializations(Diagram diagram, AstModel modelNode)
    {
        EModel emodel = (EModel)diagram.getRole();

        if( emodel == null || emodel.getVariables().isEmpty() )
            return;

        Collection<Variable> initVariables = new ArrayList<>();

        for( Variable var : emodel.getVariables() )
        {
            String name = var.getName();
            if( !name.equals("time") && !name.startsWith("$$") && var.getInitialValue() != 0 )
                initVariables.add(var);
        }

        if( initVariables.size() == 0 )
            return;

        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, AntimonyConstants.BLOCK_INITIALIZATIONS);

        for( Variable var : initVariables )
        {
            String name = var.getName();
            String unit = var.getUnits();

            if( !unit.equals(Unit.UNDEFINED) && !Unit.getBaseUnitsList().contains(unit) && emodel.getUnits().get(unit) == null )
            {
                log.log(Level.WARNING, "List of units doesn't contain " + unit);
                continue;
            }

            AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            symbol.setTypeDeclaration(AstSymbol.INIT);
            symbol.setName(validAndNotReservedName(name));

            AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
            AstEquation equation = new AstEquation(AntimonyNotationParser.JJTEQUATION);

            String compartmentName = null;

            if( var instanceof VariableRole && ( (VariableRole)var ).getInitialQuantityType() == VariableRole.AMOUNT_TYPE )
            {
                DiagramElement de = ( (VariableRole)var ).getDiagramElement();
                if( de.getParent() instanceof Compartment
                        && ( (Compartment)de.getParent() ).getKernel() instanceof biouml.standard.type.Compartment )
                    compartmentName = ( (Compartment)de.getParent() ).getName();
            }

            equation = createInitializingEquation(var.getInitialValue(), unit, compartmentName);

            createSpace(equal);
            symbol.addAsLast(equal);
            symbol.addAsLast(equation);

            createIndent(symbol);
            symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            modelNode.addAsLast(symbol);
            modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));

            if( var instanceof VariableRole )
            {
                Node varNode = (Node) ( (VariableRole)var ).getDiagramElement();
                symbol.setName(validAndNotReservedName(varNode.getName()));
                linkWithAssociatedDiagramElements(varNode, symbol);
            }
            else
            {
                link(var.getAttributes(), symbol);
            }

            if( !unit.equals(Unit.UNDEFINED) && !Unit.getBaseUnitsList().contains(unit) )
            {
                link(emodel.getUnits().get(unit).getAttributes(), symbol);
            }
        }

        for( int i = modelNode.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isInitAstSymbol(modelNode.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_INITIALIZATIONS, modelNode.jjtGetChild(i));
                break;
            }
    }

    public AstEquation createInitializingEquation(double value, String units, String compartmentName)
    {
        AstEquation result = new AstEquation(AntimonyNotationParser.JJTEQUATION);
        AstRegularFormulaElement number = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);

        number.setNumber(true);
        number.setElement(String.valueOf(value));
        createSpace(number);

        result.addAsLast(number);

        if( !units.equals(Unit.UNDEFINED) )
        {
            AstRegularFormulaElement unit = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            unit.setString(true);
            unit.setElement(units);
            createSpace(unit);
            result.addAsLast(unit);
        }

        if( compartmentName != null )
        {
            AstRegularFormulaElement div = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            div.setElement("/");
            createSpace(div);
            result.addAsLast(div);

            AstRegularFormulaElement comp = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            comp.setString(true);
            comp.setElement(compartmentName);
            createSpace(comp);
            result.addAsLast(comp);
        }

        return result;
    }

    /**
     * create AstEquation with one AstFormulaElement
     * @param value in AstFormulaElement
     * @return
     */
    public static AstEquation createNumber(double value)
    {
        AstEquation result = new AstEquation(AntimonyNotationParser.JJTEQUATION);
        AstRegularFormulaElement element = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        element.setNumber(true);
        element.setElement(String.valueOf(value));
        createSpace(element);
        result.addAsLast(element);
        return result;
    }

    private void writeConstant(Diagram diagram, AstModel modelNode) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);

        Collection<Variable> constVariables = emodel.getVariables().stream()
                .filter(var -> !"time".equals(var.getName()) && !var.getName().startsWith("$$") && var.isConstant())
                .collect(Collectors.toList());

        if( constVariables.size() == 0 )
            return;

        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, "Constants");

        AstVarOrConst astConst = new AstVarOrConst(AntimonyNotationParser.JJTVARORCONST);
        astConst.setType(AstVarOrConst.CONST);
        createIndent(astConst);
        addConstantSymbols(constVariables, astConst);

        astConst.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        modelNode.addAsLast(astConst);
        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
    }

    private void writeEquations(Diagram diagram, AstModel modelNode)
    {
        EModel emodel = (EModel)diagram.getRole();
        if( emodel == null )
            return;

        List<Equation> equations = emodel.getEquations(new MathEquationFilter()).toList();
        if( equations.size() == 0 )
            return;
        equations.sort(new EquationComparator());
        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, AntimonyConstants.BLOCK_EQUATIONS);

        String type = "";
        for( Equation eq : equations )
        {
            if( !eq.getType().equals(type) )
            {
                if( !type.isEmpty() )
                    modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
                type = eq.getType();
            }
            addEquation(eq, modelNode, false);
        }

        for( int i = modelNode.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isEquationAstSymbol(modelNode.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_EQUATIONS, modelNode.jjtGetChild(i));
                break;
            }
    }

    private void writeEvents(Diagram diagram, AstModel modelNode)
    {
        EModel emodel = (EModel)diagram.getRole();
        if( emodel == null )
            return;

        Event[] events = emodel.getEvents();
        if( events.length == 0 )
            return;

        modelNode.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(modelNode, AntimonyConstants.BLOCK_EVENTS);

        for( Event eq : events )
        {
            addEvent(eq, modelNode, false);
        }

        for( int i = modelNode.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isEventAstSymbol(modelNode.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_EVENTS, modelNode.jjtGetChild(i));
                break;
            }
    }

    protected void addEquation(Equation eq, AstModel modelNode, boolean updateBlockLocation)
    {

        SimpleNode equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);

        AstEquation equation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
        setFormulaToAstEquation(eq.getFormula(), equation);

        if( eq.getType().equals(Equation.TYPE_ALGEBRAIC) )
        {
            symbol.setTypeDeclaration(AstSymbol.ALGEBRAIC);
            equal = new AstEqualZero(AntimonyNotationParser.JJTEQUALZERO);
            symbol.setName(validAndNotReservedName("unknown"));
            createIndent(equal);
            symbol.addAsLast(equal);
            symbol.addAsLast(equation);

        }
        else
        {
            if( eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) )
                symbol.setTypeDeclaration(AstSymbol.INIT);
            else if( eq.getType().equals(Equation.TYPE_SCALAR) )
            {
                symbol.setTypeDeclaration(AstSymbol.RULE);
                equal = new AstColonEqual(AntimonyNotationParser.JJTCOLONEQUAL);
            }
            else if( eq.getType().equals(Equation.TYPE_RATE) )
            {
                symbol.setTypeDeclaration(AstSymbol.RATE);
                equal = new AstRateEqual(AntimonyNotationParser.JJTRATEEQUAL);
            }
            symbol.setName(validAndNotReservedName(eq.getVariable()));
            createSpace(equal);
            symbol.addAsLast(equal);
            symbol.addAsLast(equation);
            createIndent(symbol);
        }
        if( equation.jjtGetChild(0) != null )
            createSpace((SimpleNode)equation.jjtGetChild(0));

        symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_EQUATIONS) )
            index = modelNode.findIndex(blockLocations.get(AntimonyConstants.BLOCK_EQUATIONS)) + 2;
        else
            index = modelNode.jjtGetNumChildren();

        modelNode.addWithDisplacement(symbol, index);
        modelNode.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        Node eqNode = (Node)eq.getDiagramElement();
        link(eqNode, symbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_EQUATIONS, modelNode.jjtGetChild(index));
    }

    protected void addEvent(Event ev, AstModel modelNode, boolean updateBlockLocation)
    {
        Node eventNode = (Node)ev.getDiagramElement();

        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        symbol.setName(validAndNotReservedName(eventNode.getName()));
        symbol.setTypeDeclaration(AstSymbol.EVENT);

        AstColon colon = new AstColon(AntimonyNotationParser.JJTCOLON);
        AstAt at = new AstAt(AntimonyNotationParser.JJTAT);
        createSpace(at);

        if( ev.getDelay() != null && !ev.getDelay().equals("0") )
        {
            AstEquation delay = new AstEquation(AntimonyNotationParser.JJTEQUATION);
            setFormulaToAstEquation(ev.getDelay(), delay);
            if( delay.jjtGetChild(0) != null )
                createSpace((SimpleNode)delay.jjtGetChild(0));
            AstAfter after = new AstAfter(AntimonyNotationParser.JJTAFTER);
            createSpace(after);
            at.addAsLast(delay);
            at.addAsLast(after);
        }

        AstEquation equation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
        setFormulaToAstEquation(ev.getTrigger(), equation);

        if( equation.jjtGetChild(0) != null )
            createSpace((SimpleNode)equation.jjtGetChild(0));

        symbol.addAsLast(colon);
        symbol.addAsLast(at);
        at.addAsLast(equation);

        if( ev.getPriority() != null && !ev.getPriority().equals("") )
        {
            AstPriority priority = new AstPriority(AntimonyNotationParser.JJTPRIORITY);
            AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
            createSpace(equal);
            AstEquation priorityEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
            setFormulaToAstEquation(ev.getPriority(), priorityEquation);
            if( priorityEquation.jjtGetChild(0) != null )
                createSpace((SimpleNode)priorityEquation.jjtGetChild(0));
            AstComma comma = new AstComma(AntimonyNotationParser.JJTCOMMA);
            symbol.addAsLast(comma);
            symbol.addAsLast(priority);
            priority.addAsLast(equal);
            priority.addAsLast(priorityEquation);
        }
        if( ev.isTriggerInitialValue() )
        {
            AstTriggerInitialValue triggerInitialValue = new AstTriggerInitialValue(AntimonyNotationParser.JJTTRIGGERINITIALVALUE);
            addDefaultEventAttribute(symbol, triggerInitialValue);
        }

        if( ev.isTriggerPersistent() )
        {
            AstPersistent persistent = new AstPersistent(AntimonyNotationParser.JJTPERSISTENT);
            addDefaultEventAttribute(symbol, persistent);
        }

        if( !ev.isUseValuesFromTriggerTime() )
        {
            AstUseValuesFromTriggerTime useValuesFromTriggerTime = new AstUseValuesFromTriggerTime(
                    AntimonyNotationParser.JJTUSEVALUESFROMTRIGGERTIME);
            addDefaultEventAttribute(symbol, useValuesFromTriggerTime);
        }

        Assignment[] assinments = ev.getEventAssignment();
        for( Assignment as : assinments )
        {
            colon = new AstColon(AntimonyNotationParser.JJTCOLON);
            createSpace(colon);
            AstSymbol assignmentSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            createSpace(assignmentSymbol);
            assignmentSymbol.setName(validAndNotReservedName(as.getVariable()));
            assignmentSymbol.setTypeDeclaration(AstSymbol.INIT);
            AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
            createSpace(equal);
            equation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
            setFormulaToAstEquation(as.getMath(), equation);
            if( equation.jjtGetChild(0) != null )
                createSpace((SimpleNode)equation.jjtGetChild(0));

            assignmentSymbol.addAsLast(equal);
            assignmentSymbol.addAsLast(equation);
            symbol.addAsLast(colon);
            symbol.addAsLast(assignmentSymbol);
        }

        createIndent(symbol);
        symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_EVENTS) )
            index = modelNode.findIndex(blockLocations.get(AntimonyConstants.BLOCK_EVENTS)) + 2;
        else
            index = modelNode.jjtGetNumChildren();

        modelNode.addWithDisplacement(symbol, index);
        modelNode.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(eventNode, symbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_EVENTS, modelNode.jjtGetChild(index));
    }

    protected void addDefaultEventAttribute(AstSymbol symbol, SimpleNode eventAttribute)
    {
        createSpace(eventAttribute);
        AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
        createSpace(equal);
        AstRegularFormulaElement value = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        value.setElement("true");
        createSpace(value);
        AstComma comma = new AstComma(AntimonyNotationParser.JJTCOMMA);
        symbol.addAsLast(comma);
        symbol.addAsLast(eventAttribute);
        eventAttribute.addAsLast(equal);
        eventAttribute.addAsLast(value);
    }

    private void writeDisplayNames(Diagram diagram, AstModel model)
    {
        List<Node> nodes = allNodes(diagram).filter(this::hasDisplayName).toList();

        if( nodes.size() == 0 )
            return;

        model.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
        addComment(model, AntimonyConstants.BLOCK_TITLES);
        nodes.forEach(node -> addDisplayName(node, model, false));

        for( int i = model.jjtGetNumChildren() - 1; i >= 0; i-- )
            if( AntimonyUtility.isDisplayNameAstSymbol(model.jjtGetChild(i)) )
            {
                blockLocations.put(AntimonyConstants.BLOCK_TITLES, model.jjtGetChild(i));
                break;
            }
    }

    private boolean hasDisplayName(Node node)
    {
        return node.getRole() instanceof VariableRole && !Util.isBus(node)
                && ! ( node.getAttributes().getProperty(SBGNPropertyConstants.SBGN_CLONE_MARKER) != null
                        && AntimonyAnnotationImporter.isPropertyImported(SBGNPropertyConstants.SBGN_CLONE_MARKER) )
                && !validAndNotReservedName(node.getName()).equals(node.getTitle()) && AntimonyUtility.isEmptyCompartment(node);
    }

    protected void addDisplayName(Node node, AstModel model, boolean updateBlockLocation)
    {
        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        symbol.setName(validAndNotReservedName(node.getName()));
        createIndent(symbol);
        symbol.setTypeDeclaration(AstSymbol.DISPLAY_NAME);
        AstIs is = new AstIs(AntimonyNotationParser.JJTIS);
        AstText text = new AstText(AntimonyNotationParser.JJTTEXT);
        createSpace(text);
        text.setText("\"" + node.getTitle() + "\"");
        is.addAsLast(text);
        createSpace(is);
        AstSemicolon semocolon = new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON);
        symbol.addAsLast(is);
        symbol.addAsLast(semocolon);

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_TITLES) )
            index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_TITLES)) + 2;
        else
            index = model.jjtGetNumChildren();

        model.addWithDisplacement(symbol, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(node, symbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_TITLES, model.jjtGetChild(index));
    }

    private void writeProperties(Diagram diagram, AstModel model) throws Exception
    {
        List<Node> speciesList = AntimonyUtility.getSpecieNodes(diagram, diagram.getType() instanceof SbgnDiagramType);
        List<Node> phenotypesList = AntimonyUtility.getPhenotypes(diagram);
        List<Node> reactionsList = DiagramUtility.getReactionNodes(diagram);
        List<Edge> modEdgesList = AntimonyUtility.getModifierEdges(diagram);
        List<Node> logicalNodesList = AntimonyUtility.getLogicalNodes(diagram);
        List<Node> portsList = AntimonyUtility.getPortNodes(diagram, true);
        List<Node> buses = DiagramUtility.getBuses(diagram).toList();
        List<Node> notes = DiagramUtility.getNotes(diagram).toList();
        List<Node> tables = DiagramUtility.getTables(diagram).toList();

        for( Node node : speciesList )
        {
            addProperties(node, getPropertiesAsMap(node), model, false);
            addComplexElementsProperty(node, model, false);

            if( SbgnUtil.isClone(node) )
                for( DiagramElement clone : node.getRole(VariableRole.class).getAssociatedElements() )

                    addCloneProperty(clone, model, false);
        }

        for( Node node : phenotypesList )
            addProperties(node, getPropertiesAsMap(node), model, false);

        writeBusProperties(buses, model);

        for( Node node : reactionsList )
            addProperties(node, getPropertiesAsMap(node), model, false);

        for( Edge edge : modEdgesList )
            addProperties(edge, getPropertiesAsMap(edge), model, false);


        for( Node node : logicalNodesList )
        {
            AstProperty prop = null;
            for( Edge edge : node.getEdges() )
                if( SbgnUtil.isRegulationEdge(edge) )
                {
                    addProperties(edge, getPropertiesAsMap(edge), model, false);
                    Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(edge);
                    if( astNodes == null )
                        return;
                    for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                        if( AntimonyUtility.isModifierAstProperty(astNode) )
                        {
                            link(node, prop);
                            for( Edge ed : node.getEdges() )
                                link(ed, prop);
                        }
                }
        }

        for( Node node : portsList )
        {
            addProperties(node, getPropertiesAsMap(node), model, false);
        }

        for( Node node : notes )
        {
            addNoteProperty(node, model);
        }

        for( Node node : tables )
        {
            addTableProperty(node, model);
        }

    }

    protected void addTableProperty(Node node, AstModel model)
    {
        if( !AntimonyAnnotationImporter.isAnnotationImported(AntimonyConstants.ANNOTATION_BIOUML) )
            return;

        if( ! ( node.getRole() instanceof SimpleTableElement ) )
            return;

        SimpleTableElement role = (SimpleTableElement)node.getRole();

        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("path", role.getTablePath().toString());

        Map<String, String> argColumnProps = new HashMap<String, String>();
        VarColumn argCol = role.getArgColumn();

        if( !argCol.getColumn().isEmpty() )
            argColumnProps.put("name", argCol.getColumn());

        if( !argCol.getVariable().isEmpty() )
            argColumnProps.put("variable", argCol.getVariable());

        if( !argColumnProps.isEmpty() )
            properties.put("argColumn", argColumnProps);

        Set<Map<String, String>> columnsProps = new HashSet<Map<String, String>>();

        for( VarColumn col : role.getColumns() )
        {
            Map<String, String> colProps = new HashMap<String, String>();

            if( !col.getColumn().isEmpty() )
                colProps.put("name", col.getColumn());

            if( !col.getVariable().isEmpty() )
                colProps.put("variable", col.getVariable());

            if( !colProps.isEmpty() )
                columnsProps.add(colProps);
        }

        if( !columnsProps.isEmpty() )
            properties.put("columns", columnsProps);

        AstProperty property = createPropertyWithDeclaration(node, AstProperty.TABLE, properties);
        property.setNotationType(AntimonyConstants.ANNOTATION_BIOUML);

        if( !blockLocations.containsKey(AntimonyConstants.BLOCK_PROPERTIES) )
            createBlock(AntimonyConstants.BLOCK_PROPERTIES, model);
        int index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_PROPERTIES)) + 2;


        model.addWithDisplacement(property, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(node, property);

        blockLocations.put(AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild(index));
    }

    protected void addNoteProperty(Node node, AstModel model)
    {
        if( !AntimonyAnnotationImporter.isAnnotationImported(AntimonyConstants.ANNOTATION_SBGN) )
            return;

        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> nodes = new ArrayList<String>();
        List<Edge> edges = new ArrayList<Edge>();

        for( Edge edge : node.getEdges() )
        {
            if( !edge.getOutput().equals(node) )
                nodes.add(edge.getOutput().getName());
            else
                nodes.add(edge.getInput().getName());

            edges.add(edge);
        }

        properties.put("text", node.getTitle());
        properties.put("nodes", nodes);

        AstProperty property = createPropertyWithDeclaration(node, AstProperty.NOTE, properties);
        property.setNotationType(AntimonyConstants.ANNOTATION_SBGN);

        if( !blockLocations.containsKey(AntimonyConstants.BLOCK_PROPERTIES) )
            createBlock(AntimonyConstants.BLOCK_PROPERTIES, model);
        int index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_PROPERTIES)) + 2;


        model.addWithDisplacement(property, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(node, property);

        AstSingleProperty sprop = property.getSinglePropety("nodes");

        for( biouml.plugins.antimony.astparser_v2.Node child : sprop.getValueNode().getChildren() )
        {
            if( child instanceof AstSymbol )
            {
                AstSymbol symbol = (AstSymbol)child;
                int ind = nodes.indexOf(symbol.getName());
                if( ind != -1 )
                {
                    link(diagram.findNode(symbol.getName()), symbol);
                    link(edges.get(ind), symbol);
                }
            }
        }


        blockLocations.put(AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild(index));
    }

    /**
     * Used only for properties with declaration: 
     * <br>
     * <i>@annotation_type declaration_name node_name = ... </i>
     * @param node
     * @param declarationType 
     * @param value - map of single properties
     */
    private AstProperty createPropertyWithDeclaration(Node node, String declarationType, Map<String, Object> value)
    {
        AstProperty property = new AstProperty(AntimonyNotationParser.JJTPROPERTY);

        createIndent(property);

        property.setDeclarationType(declarationType);

        property.addChainName(node.getName());

        property.createValueNode(value);

        property.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        return property;
    }

    private void writeBusProperties(List<Node> busNodes, AstModel model)
    {
        if( !AntimonyAnnotationImporter.isPropertyImported(AntimonyConstants.BIOUML_BUS) )
            return;

        for( Node busNode : busNodes )
            addBusProperties(busNode, model, false);

    }

    protected void addBusProperties(Node busNode, AstModel model, boolean updateBlockLocation)
    {
        if( !AntimonyAnnotationImporter.isPropertyImported(AntimonyConstants.BIOUML_BUS) )
            return;

        HashMap<String, Object> busProperties = new HashMap<String, Object>();
        Map<String, Edge> portToEdge = new HashMap<String, Edge>();
        List<String> portList = new ArrayList<String>();

        Bus bus = (Bus)busNode.getRole();

        String varName = bus.getName();//node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_CLONE_MARKER);

        busProperties.put("title", busNode.getTitle());

        for( Edge edge : busNode.getEdges() )
        {
            if( Util.isPort(edge.getInput()) )
            {
                String name = edge.getInput().getCompleteNameInDiagram();
                portList.add(name);
                portToEdge.put(name, edge);
            }
            else if( Util.isPort(edge.getOutput()) )
            {
                String name = edge.getOutput().getCompleteNameInDiagram();
                portList.add(name);
                portToEdge.put(name, edge);
            }

        }

        busProperties.put("ports", portList);

        if( bus.isDirected() )
            busProperties.put("directed", true);
        AstProperty property = new AstProperty(AntimonyNotationParser.JJTPROPERTY);
        property.setNotationType("biouml");
        property.addChainName(varName);
        createIndent(property);

        Map<String, Object> value = new HashMap<String, Object>();
        value.put("bus", busProperties);
        property.createValueNode(value);

        AstSingleProperty sprop = property.getSinglePropety("bus");

        for( biouml.plugins.antimony.astparser_v2.Node n : sprop.getValueNode().getChildren() )
            if( n instanceof AstSingleProperty && ( (AstSingleProperty)n ).getPropertyName().equals("ports") )
            {
                for( biouml.plugins.antimony.astparser_v2.Node child : ( (AstSingleProperty)n ).getValueNode().getChildren() )
                {
                    if( child instanceof AstSymbol && portToEdge.get( ( (AstSymbol)child ).getName()) != null )
                        link(portToEdge.get( ( (AstSymbol)child ).getName()), (AstSymbol)child);

                }
                break;

            }

        property.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        property.setDotNeeded(true);

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_PROPERTIES) )
            index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_PROPERTIES)) + 2;
        else
            index = model.jjtGetNumChildren();

        if( ! ( model.jjtGetChild(index - 1) instanceof AstEOL ) )
            model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index++);

        model.addWithDisplacement(property, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(busNode, property);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild(index));
    }

    protected void addCloneProperty(DiagramElement de, AstModel model, boolean updateBlockLocation)
    {
        if( ! ( de instanceof Node ) )
            return;
        Node clone = (Node)de;

        if( !AntimonyAnnotationImporter.isPropertyImported(SBGNPropertyConstants.SBGN_CLONE_MARKER) || !AntimonyUtility.isClone(clone) )
            return;


        HashMap<String, Object> clonesProperties = new HashMap<String, Object>();
        List<String> reactionList = new ArrayList<String>();

        String realName = ( (VariableRole)clone.getRole() ).getShortName();//node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_CLONE_MARKER);
        clonesProperties.put("title", clone.getTitle());

        for( Edge edge : clone.getEdges() )
        {
            if( ! ( edge.getKernel() instanceof SpecieReference ) )
                continue;
            SpecieReference sr = (SpecieReference)edge.getKernel();
            if( realName.equals(sr.getSpecie()) && sr.getOrigin() instanceof Reaction )
                reactionList.add( ( (Reaction)sr.getOrigin() ).getName());
        }

        clonesProperties.put("reactions", reactionList);

        AstProperty property = new AstProperty(AntimonyNotationParser.JJTPROPERTY);
        property.setNotationType("sbgn");
        property.addChainName(realName);
        createIndent(property);

        Map<String, Object> value = new HashMap<String, Object>();
        value.put(AntimonyConstants.SBGN_CLONE, clonesProperties);
        property.createValueNode(value);

        AstSingleProperty sprop = property.getSinglePropety(AntimonyConstants.SBGN_CLONE);

        for( biouml.plugins.antimony.astparser_v2.Node n : sprop.getValueNode().getChildren() )
            if( n instanceof AstSingleProperty && ( (AstSingleProperty)n ).getPropertyName().equals("reactions") )
            {
                for( biouml.plugins.antimony.astparser_v2.Node child : ( (AstSingleProperty)n ).getValueNode().getChildren() )
                {
                    if( child instanceof AstSymbol && reactionList.contains( ( (AstSymbol)child ).getName()) )
                    {
                        AstSymbol symbol = (AstSymbol)child;
                        link(diagram.findNode(symbol.getName()), symbol);
                    }

                }
                break;

            }

        property.addAsLast(sprop);
        property.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        property.setDotNeeded(true);

        if( !blockLocations.containsKey(AntimonyConstants.BLOCK_PROPERTIES) )
            createBlock(AntimonyConstants.BLOCK_PROPERTIES, model);
        int index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_PROPERTIES)) + 2;

        model.addWithDisplacement(property, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        link(clone, property);

        blockLocations.put(AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild(index));
    }

    protected void addProperties(DiagramElement de, Map<String, HashMap<String, Object>> properties, AstModel model,
            boolean updateBlockLocation)
    {
        if( properties.isEmpty() )
            return;

        for( Map.Entry<String, HashMap<String, Object>> prop : properties.entrySet() )
            addProperty(de, prop.getKey(), prop.getValue(), model);
    }


    private void createBlock(String name, AstModel model)
    {
        int astModelLen = model.jjtGetNumChildren();
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), astModelLen);
        addCommentWithDisplacement(model, AntimonyConstants.BLOCK_PROPERTIES, astModelLen + 1);
        blockLocations.put(AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild(astModelLen + 1));
        //                for( int i = model.jjtGetNumChildren() - 1; i >= 0; i-- )
        //                    if( model.jjtGetChild( i ) instanceof AstProperty )
        //                    {
        //                        blockLocations.put( AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild( i ) );
        //                    }
    }

    protected void addProperty(DiagramElement de, String notationType, HashMap<String, Object> value, AstModel model)
    {
        AstProperty property = createProperty(de, value);
        property.setNotationType(notationType);

        if( !blockLocations.containsKey(AntimonyConstants.BLOCK_PROPERTIES) )
            createBlock(AntimonyConstants.BLOCK_PROPERTIES, model);
        int index = model.findIndex(blockLocations.get(AntimonyConstants.BLOCK_PROPERTIES)) + 2;

        model.addWithDisplacement(property, index);
        model.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        linkWithAssociatedDiagramElements(de, property);

        blockLocations.put(AntimonyConstants.BLOCK_PROPERTIES, model.jjtGetChild(index));
    }

    private AstProperty createProperty(DiagramElement de, Map<String, Object> value)
    {
        AstProperty property = new AstProperty(AntimonyNotationParser.JJTPROPERTY);
        String name;
        List<String> names = new ArrayList<String>();
        if( de instanceof Node && SbgnUtil.isClone((Node)de) )
            name = de.getRole(VariableRole.class).getShortName();
        else
        {
            if( de instanceof Node )
            {
                Node parent = (Node)de;
                while( parent.getParent() instanceof Compartment && ( (Compartment)parent.getParent() ).getKernel() instanceof Specie )
                {
                    parent = (Node)parent.getParent();
                    if( SbgnUtil.isComplex(parent) && parent.getParent() instanceof Node && SbgnUtil.isComplex((Node)parent.getParent()) )
                    {
                        int i = 1;
                        for( DiagramElement d : (Compartment)parent.getParent() )
                        {
                            if( d.getName().equals(parent.getName()) )
                                break;
                            else if( d instanceof Node && SbgnUtil.isComplex((Node)d) )
                                i++;
                        }

                        names.add(0, "__sub_" + i + "__");
                    }
                    else
                        names.add(0, parent.getName());
                }
            }
            name = validAndNotReservedName(de.getName());
            names.add(name);
        }

        if( Util.isPort(de) )
        {
            try
            {
                AstLocateFunction locate = new AstLocateFunction(AntimonyNotationParser.JJTLOCATEFUNCTION);
                AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                lb.setElement("(");
                AstSymbol variable = createSubSymbol((Node)de);
                AstComma comma = new AstComma(AntimonyNotationParser.JJTCOMMA);
                AstText portType = new AstText(AntimonyNotationParser.JJTTEXT);
                String type = Util.isPropagatedPort2(de) ? ConnectionPort.PROPAGATED : Util.getAccessType((Node)de);
                portType.setText("\"" + type + " port\"");
                createSpace(portType);
                AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                rb.setElement(")");

                locate.addProperty("name", variable.getName());
                locate.addProperty("type", type + " port");

                createSpace(locate);
                locate.addAsLast(lb);
                locate.addAsLast(variable);
                locate.addAsLast(comma);
                locate.addAsLast(portType);
                locate.addAsLast(rb);
                property.addAsLast(locate);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can't create property: " + e.getMessage());
            }

        }
        else
            names.forEach(n -> property.addChainName(n));


        createIndent(property);

        property.createValueNode(value);

        property.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        return property;
    }

    protected Map<String, HashMap<String, Object>> getPropertiesAsMap(DiagramElement de) throws Exception
    {
        if( AntimonyAnnotationImporter.annotations == null )
            return null;

        Map<String, HashMap<String, Object>> propertiesWithAnnotationType = new HashMap<String, HashMap<String, Object>>();

        String value;


        for( Map.Entry<String, Map<String, Object>> annotation : AntimonyAnnotationImporter.annotations.entrySet() )
        {
            String annotationName = annotation.getKey();
            HashMap<String, Object> properties = new HashMap<String, Object>();

            if( annotationName.equals("sbgn") )
            {
                for( Map.Entry<String, String> SBGNprop : getSBGNProperties(annotation.getValue(), de).entrySet() )
                    properties.put(SBGNprop.getKey(), SBGNprop.getValue());
            }
            else if( annotationName.equals("glycan") )
            {
                for( Map.Entry<String, Object> prop : annotation.getValue().entrySet() )
                {
                    String propertyName = prop.getKey();
                    DynamicProperty dp = de.getAttributes()
                            .getProperty(annotationName + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1));
                    if( dp == null )
                        continue;
                    value = dp.getValue().toString();

                    properties.put(propertyName, value);
                }

            }
            else
            {
                for( Map.Entry<String, Object> prop : annotation.getValue().entrySet() )
                {
                    String propertyName = prop.getKey();

                    DynamicProperty dp = de.getAttributes().getProperty(propertyName);
                    if( dp == null )
                        continue;
                    value = dp.getValue().toString();

                    if( prop.getValue() instanceof List && ! ( (List<?>)prop.getValue() ).contains(value) )
                        throw new Exception("Property value \"" + value + "\" is invalid");

                    properties.put(propertyName, value);
                }
            }

            if( !properties.isEmpty() )
                propertiesWithAnnotationType.put(annotationName, properties);
        }

        return propertiesWithAnnotationType;
    }

    private Map<String, String> getSBGNProperties(Map<String, Object> properties, DiagramElement de) throws Exception
    {
        Map<String, String> sbgnProps = new HashMap<String, String>();
        boolean noMultimer = properties.containsKey("structure");
        boolean isElementOfComplex = de.getParent() instanceof Node && SbgnUtil.isComplex((Node)de.getParent());
        boolean isSpecie = de instanceof Node && ( de.getKernel() instanceof Specie || SbgnUtil.isPhenotype((Node)de) );
        boolean isReaction = de.getKernel() instanceof Reaction;
        boolean isPort = Util.isPort(de);
        for( Map.Entry<String, Object> property : properties.entrySet() )
        {
            String value;
            String key = property.getKey();


            if( isSpecie )
            {
                // for complex elements
                if( isElementOfComplex && !key.equals("type") )
                    continue;

                switch( key )
                {
                    case ( "type" ):
                    {
                        value = de.getKernel().getType();
                        if( property.getValue() instanceof List && ! ( (List<?>)property.getValue() ).contains(value) )
                            throw new Exception("Property value \"" + value + "\" is invalid");

                        if( value.equals("macromolecule") )
                            continue;

                        sbgnProps.put(key, value);
                        break;
                    }
                    case ( "structure" ):
                    {
                        if( hasDisplayName((Node)de) )
                            continue;

                        value = SbgnComplexStructureManager.constructSBGNViewTitle((Node)de);
                        // Check whether it is complex or entity with modifiers, not subelement
                        if( ! ( value.isEmpty() || value.equals(de.getName()) || ( SbgnUtil.isComplex((Node)de)
                                && de.getParent() instanceof Node && SbgnUtil.isComplex((Node)de.getParent()) ) ) )
                        {
                            sbgnProps.put(key, value);
                        }

                        break;
                    }
                    case ( "multimer" ):
                    {
                        if( noMultimer )
                            continue;

                        DynamicProperty dp = de.getAttributes().getProperty(SBGNPropertyConstants.SBGN_MULTIMER);
                        if( dp == null )
                            continue;
                        value = dp.getValue().toString();

                        sbgnProps.put(key, value);
                        break;
                    }
                }
            }
            else if( isReaction )
            {
                if( key.equals("reactionType") )
                {
                    DynamicProperty dp = de.getAttributes().getProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE);
                    if( dp == null )
                        continue;
                    value = dp.getValue().toString();

                    if( property.getValue() instanceof List && ! ( (List<?>)property.getValue() ).contains(value) )
                        throw new Exception("Property value \"" + value + "\" is invalid");

                    if( value.equals("process") )
                        continue;

                    sbgnProps.put(key, value);
                    break;
                }
            }
            else if( isPort )
            {
                if( key.equals("title") && !de.getName().equals(de.getTitle()) )
                {
                    value = de.getTitle();
                    sbgnProps.put(key, value);
                }

            }
            else if( de instanceof Edge )
            {
                if( de.getKernel() instanceof SpecieReference )
                {
                    SpecieReference sr = (SpecieReference)de.getKernel();
                    String modifierAction = sr.getModifierAction();
                    if( SpecieReference.ACTION_NECCESSARY_STIMULATION.equals(modifierAction)
                            || SpecieReference.ACTION_STIMULATION.equals(modifierAction) )
                    {
                        sbgnProps.put("edgeType", modifierAction);
                    }
                }

                DynamicProperty dp = de.getAttributes().getProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE);
                if( dp == null )
                    continue;
                value = dp.getValue().toString();

                if( property.getValue() instanceof List && ! ( (List<?>)property.getValue() ).contains(value) )
                    throw new Exception("Property value \"" + value + "\" is invalid");

                if( value.equals("catalysis") )
                    continue;

                sbgnProps.put("edgeType", value);
                break;
            }
        }

        return sbgnProps;
    }


    protected void addComplexElementsProperty(Node node, AstModel model, boolean updateBlockLocation) throws Exception
    {
        if( node instanceof Compartment )
            for( Node child : ( (Compartment)node ).getNodes() )
            {
                if( SbgnUtil.isNotComplexEntity(child) )
                {
                    addProperties(child, getPropertiesAsMap(child), model, updateBlockLocation);
                }

                addComplexElementsProperty(child, model, updateBlockLocation);
            }
    }


    protected void addPhenotypeReaction(Node de, AstModel astModel, boolean updateBlockLocation) throws Exception
    {
        if( !SbgnUtil.isPhenotype(de) )
            throw new Exception("Node isn't a phenotype");

        AstSymbol reactionSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        reactionSymbol.setTypeDeclaration(AstSymbol.REACTION_DEFINITION);
        reactionSymbol.setName(validAndNotReservedName(de.getName()));

        reactionSymbol.addAsLast(new AstColon(AntimonyNotationParser.JJTCOLON));
        reactionSymbol.addAsLast(addReactionTitle(null, false));

        createIndent(reactionSymbol);
        reactionSymbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_REACTIONS) )
            index = astModel.findIndex(blockLocations.get(AntimonyConstants.BLOCK_REACTIONS)) + 2;
        else
            index = astModel.jjtGetNumChildren();

        astModel.addWithDisplacement(reactionSymbol, index);
        astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        DynamicProperty dp = de.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp != null )
            de.getAttributes().remove(AntimonyConstants.ANTIMONY_LINK);
        link(de, reactionSymbol);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_REACTIONS, astModel.jjtGetChild(index));
    }



    private static StreamEx<Node> allNodes(Compartment diagram)
    {
        return StreamEx.<Node> ofTree(diagram,
                node -> node.getKernel() instanceof DiagramInfo || node.getKernel() instanceof biouml.standard.type.Compartment
                        ? ( (Compartment)node ).stream(Node.class) : null);
    }

    private void addConstantSymbols(Collection<Variable> collection, biouml.plugins.antimony.astparser_v2.Node parent)
    {
        Iterator<Variable> iter = collection.iterator();
        while( iter.hasNext() )
        {
            Variable var = iter.next();

            String name;
            if( var instanceof VariableRole )
                name = ( (VariableRole)var ).getDiagramElement().getName();
            else
                name = var.getName();
            AstSymbol symbol = createSymbol(name);
            createSpace(symbol);
            parent.addAsLast(symbol);
            if( iter.hasNext() )
            {
                ( (SimpleNode)parent ).addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
            }

            if( var instanceof VariableRole )
                link( ( (VariableRole)var ).getDiagramElement(), symbol);
            else
                link(var.getAttributes(), symbol);
        }
    }

    private void addSymbols(Collection<?> collection, biouml.plugins.antimony.astparser_v2.Node parent)
    {
        Iterator<?> iter = collection.iterator();

        while( iter.hasNext() )
        {
            Object obj = iter.next();
            if( obj instanceof Node )
                addSymbol(parent, (Node)obj);
            else if( obj instanceof Variable )
            {
                AstSymbol symbol = createSymbol( ( (Variable)obj ).getName());
                createSpace(symbol);
                parent.addAsLast(symbol);
            }
            if( iter.hasNext() )
            {
                ( (SimpleNode)parent ).addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
            }
        }
    }

    protected AstSymbol addSymbol(biouml.plugins.antimony.astparser_v2.Node parent, DataElement de)
    {
        AstSymbol symbol = createSymbol(de.getName());
        createSpace(symbol);
        ( (SimpleNode)parent ).addAsLast(symbol);

        if( de instanceof Node )
        {
            Node node = (Node)de;
            Option compartment = node.getParent();
            if( compartment instanceof Compartment && ! ( compartment instanceof Diagram )
                    && AntimonyUtility.isCompartment((Node)compartment) )
            {
                AstIn astIn = new AstIn(AntimonyNotationParser.JJTIN);
                AstSymbol symbolParent = createSymbol( ( (Compartment)compartment ).getName());
                createSpace(symbolParent);
                astIn.addAsLast(symbolParent);
                createSpace(astIn);
                symbol.addAsLast(astIn);
            }
            linkWithAssociatedDiagramElements(node, symbol);
        }
        else if( de instanceof Variable )
            link( ( (Variable)de ).getAttributes(), symbol);

        return symbol;
    }

    /**
     * To clean all old links in diagram
     * @param diagram
     * @throws Exception
     */
    private static void cleanLink(Diagram diagram)
    {
        EModel emodel = (EModel)diagram.getRole();
        if( emodel != null )
            emodel.getVariables().stream().filter(var -> ! ( var instanceof VariableRole ))
                    .forEach(v -> v.getAttributes().remove(AntimonyConstants.ANTIMONY_LINK));
        diagram.recursiveStream().forEach(de -> de.getAttributes().remove(AntimonyConstants.ANTIMONY_LINK));
    }

    protected void addReaction(Node reaction, AstModel modelNode, boolean takeAccountEdges, boolean updateBlockLocation) throws Exception
    {
        if( ! ( reaction.getKernel() instanceof Reaction ) )
            throw new Exception("Kernel isn't reaction");
        Reaction kernel = ( (Reaction)reaction.getKernel() );
        AstSymbol reactionSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        reactionSymbol.setTypeDeclaration(AstSymbol.REACTION_DEFINITION);
        reactionSymbol.setName(validAndNotReservedName(reaction.getName()));

        reactionSymbol.addAsLast(new AstColon(AntimonyNotationParser.JJTCOLON));

        //takeAccountEdges is false when reaction has not edges
        if( takeAccountEdges | reaction.getEdges() != null )
            reactionSymbol.addAsLast(addReactionTitle(reaction.getEdges(), kernel.isReversible()));
        else
            reactionSymbol.addAsLast(addReactionTitle(null, kernel.isReversible()));

        createIndent(reactionSymbol);

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_REACTIONS) )
            index = modelNode.findIndex(blockLocations.get(AntimonyConstants.BLOCK_REACTIONS)) + 2;
        else
            index = modelNode.jjtGetNumChildren();

        modelNode.addWithDisplacement(reactionSymbol, index);
        modelNode.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        reactionSymbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

        // add formula
        String formula = ( (Reaction)reaction.getKernel() ).getFormula();
        if( !formula.isEmpty() )
        {
            AstEquation astFormula = new AstEquation(AntimonyNotationParser.JJTEQUATION);

            setFormulaToAstEquation(formula, astFormula);
            reactionSymbol.addAsLast(astFormula);
            if( astFormula.jjtGetChild(0) != null )
                createSpace((SimpleNode)astFormula.jjtGetChild(0));
            reactionSymbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        }
        DynamicProperty dp = reaction.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp != null )
            reaction.getAttributes().remove(AntimonyConstants.ANTIMONY_LINK);
        link(reaction, reactionSymbol);

        if( takeAccountEdges )
            addModifiers(reaction, modelNode, updateBlockLocation);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_REACTIONS, modelNode.jjtGetChild(index));
    }

    protected void addModifiers(Node node, biouml.plugins.antimony.astparser_v2.Node modelNode, boolean updateBlockLocation)
    {
        for( Edge edge : node.getEdges() )
        {
            if( ! ( edge.getKernel() instanceof SpecieReference ) )
                continue;

            if( ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                continue;

            addModifier(node, modelNode, edge, updateBlockLocation);
        }

    }

    protected AstSymbol addModifier(Node node, biouml.plugins.antimony.astparser_v2.Node modelNode, Edge edge, boolean updateBlockLocation)
    {
        AstSymbol modifierSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        createIndent(modifierSymbol);
        modifierSymbol.setTypeDeclaration(AstSymbol.REACTION_UNKNOWN_MODIFIER);
        modifierSymbol.setName(validAndNotReservedName(edge.getName()));

        AstReactionTitle reactionTitle = new AstReactionTitle(AntimonyNotationParser.JJTREACTIONTITLE);
        AstReactionType reactionType = new AstReactionType(AntimonyNotationParser.JJTREACTIONTYPE);

        if( SbgnUtil.isRegulationEdge(edge) )
        {
            if( SbgnUtil.isPhenotype(node) )
            {
                AstReactant reactant = new AstReactant(AntimonyNotationParser.JJTREACTANT);

                AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                createSpace(symbol);
                symbol.setName(validAndNotReservedName(edge.getInput().getRole().getDiagramElement().getName()));
                link(edge.getInput(), symbol);

                reactant.addAsLast(symbol);
                reactionTitle.addAsLast(reactant);

                // TODO: check attributes
                reactionType.setType(AstReactionType.UNKNOWN_MODIFIER);
            }
            else
            {
                Node logicalNode = SbgnUtil.isLogical(edge.getInput()) ? edge.getInput() : edge.getOutput();

                AstRegularFormulaElement oper = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                DynamicProperty dp = logicalNode.getAttributes().getProperty(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR);
                switch( dp.getValue().toString() )
                {
                    case "And":
                        oper.setElement("&");
                        break;
                    case "Or":
                        oper.setElement("|");
                        break;
                    case "Not":
                        oper.setElement("NOT");
                        break;
                }
                createSpace(oper);

                Edge[] logEdges = logicalNode.getEdges();
                boolean needBraces = logEdges.length > 2;
                for( int i = 0; i < logEdges.length; i++ )
                {
                    Edge elem = logEdges[i];
                    AstReactant reactant = new AstReactant(AntimonyNotationParser.JJTREACTANT);
                    if( elem.getKernel() instanceof SpecieReference )
                    {
                        SpecieReference sr = (SpecieReference)elem.getKernel();
                        AstStoichiometry stoichiometry = new AstStoichiometry(AntimonyNotationParser.JJTSTOICHIOMETRY);
                        stoichiometry.setStoichiometry(sr.getStoichiometry());
                        AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                        createSpace(symbol);
                        symbol.setName(validAndNotReservedName(elem.getInput().getName()));
                        link(edge.getInput(), symbol);

                        reactant.addAsLast(stoichiometry);
                        reactant.addAsLast(symbol);
                        reactionTitle.addAsLast(reactant);

                        if( needBraces && i != logEdges.length - 2 )
                            reactionTitle.addAsLast(oper);
                    }
                }

                reactionType.setType(AstReactionType.UNKNOWN_MODIFIER);
                link(logicalNode, modifierSymbol);
            }
        }

        else if( edge.getKernel() instanceof SpecieReference )
        {
            SpecieReference sr = (SpecieReference)edge.getKernel();
            AstStoichiometry stoichiometry = new AstStoichiometry(AntimonyNotationParser.JJTSTOICHIOMETRY);
            stoichiometry.setStoichiometry(sr.getStoichiometry());
            AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            createSpace(symbol);
            symbol.setName(validAndNotReservedName(edge.getInput().getRole().getDiagramElement().getName()));
            link(edge.getInput(), symbol);
            AstReactant reactant = new AstReactant(AntimonyNotationParser.JJTREACTANT);
            reactant.addAsLast(stoichiometry);
            reactant.addAsLast(symbol);
            reactionTitle.addAsLast(reactant);
            if( SpecieReference.ACTION_INHIBITION.equals(sr.getModifierAction()) )
                reactionType.setType(AstReactionType.INHIBITOR);
            else if( SpecieReference.ACTION_CATALYSIS.equals(sr.getModifierAction()) )
                reactionType.setType(AstReactionType.ACTIVATOR);
            else if( SpecieReference.ACTION_STIMULATION.equals(sr.getModifierAction()) )
                reactionType.setType(AstReactionType.ACTIVATOR);
            else if( SpecieReference.ACTION_NECCESSARY_STIMULATION.equals(sr.getModifierAction()) )
                reactionType.setType(AstReactionType.ACTIVATOR);
            else
                reactionType.setType(AstReactionType.UNKNOWN_MODIFIER);
        }


        AstSymbol reactionSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        createSpace(reactionSymbol);
        reactionSymbol.setName(validAndNotReservedName(node.getName()));
        link(node, reactionSymbol);
        AstProduct product = new AstProduct(AntimonyNotationParser.JJTPRODUCT);
        product.addAsLast(reactionSymbol);


        reactionTitle.addAsLast(reactionType);
        reactionTitle.addAsLast(product);

        modifierSymbol.addAsLast(new AstColon(AntimonyNotationParser.JJTCOLON));
        modifierSymbol.addAsLast(reactionTitle);
        modifierSymbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
        link(node, modifierSymbol);
        link(edge, modifierSymbol);

        int index;
        if( updateBlockLocation && blockLocations.containsKey(AntimonyConstants.BLOCK_REACTIONS) )
            index = modelNode.findIndex(blockLocations.get(AntimonyConstants.BLOCK_REACTIONS)) + 2;
        else
            index = modelNode.jjtGetNumChildren();

        modelNode.addWithDisplacement(modifierSymbol, index);
        modelNode.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

        if( updateBlockLocation )
            blockLocations.put(AntimonyConstants.BLOCK_REACTIONS, modelNode.jjtGetChild(index));

        return modifierSymbol;
    }

    private AstReactionTitle addReactionTitle(Edge[] edges, boolean isReversible)
    {
        AstReactionTitle reactionTitle = new AstReactionTitle(AntimonyNotationParser.JJTREACTIONTITLE);
        AstReactionType reactionType = new AstReactionType(AntimonyNotationParser.JJTREACTIONTYPE);
        ArrayList<AstReactant> reactants = new ArrayList<>();
        ArrayList<AstProduct> products = new ArrayList<>();

        // put all reactants and products in collections
        if( edges != null )
            for( Edge edge : edges )
            {
                AstStoichiometry stoichiometry = new AstStoichiometry(AntimonyNotationParser.JJTSTOICHIOMETRY);
                if( ! ( edge.getKernel() instanceof SpecieReference ) )
                    continue;
                SpecieReference sr = (SpecieReference)edge.getKernel();
                stoichiometry.setStoichiometry(sr.getStoichiometry());
                AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                if( sr.isReactant() )
                {
                    Node input = edge.getInput();
                    link(input, symbol);
                    symbol.setName(validAndNotReservedName(input.getRole().getDiagramElement().getName()));
                    AstReactant reactant = new AstReactant(AntimonyNotationParser.JJTREACTANT);
                    reactant.addAsLast(stoichiometry);

                    reactant.addAsLast(symbol);
                    reactants.add(reactant);
                }
                else if( sr.isProduct() )
                {
                    Node output = edge.getOutput();
                    link(output, symbol);
                    symbol.setName(validAndNotReservedName(output.getRole().getDiagramElement().getName()));
                    AstProduct product = new AstProduct(AntimonyNotationParser.JJTPRODUCT);
                    product.addAsLast(stoichiometry);

                    product.addAsLast(symbol);
                    products.add(product);
                }
            }

        //add reactants and products in reaction title
        for( int i = 0; i < reactants.size(); i++ )
        {
            AstReactant astReactant = reactants.get(i);
            if( i > 0 )
            {
                AstPlus plus = new AstPlus(AntimonyNotationParser.JJTPLUS);
                createSpace(plus);
                reactionTitle.addAsLast(plus);
            }
            if( astReactant.jjtGetChild(0) != null )
                createSpace((SimpleNode)astReactant.jjtGetChild(0));

            reactionTitle.addAsLast(astReactant);
        }
        if( isReversible )
            reactionType.setType(AstReactionType.REVERSIBLE);
        else
            reactionType.setType(AstReactionType.IRREVERSIBLE);
        reactionTitle.addAsLast(reactionType);
        createSpace(reactionType);
        for( int i = 0; i < products.size(); i++ )
        {
            AstProduct astProduct = products.get(i);
            if( i > 0 )
            {
                AstPlus plus = new AstPlus(AntimonyNotationParser.JJTPLUS);
                createSpace(plus);
                reactionTitle.addAsLast(plus);
            }
            if( astProduct.jjtGetChild(0) != null )
                createSpace((SimpleNode)astProduct.jjtGetChild(0));
            reactionTitle.addAsLast(astProduct);
        }
        return reactionTitle;
    }


    /**
     * add space before astNode
     */
    public static void createSpace(SimpleNode astNode)
    {
        if( astNode.jjtGetFirstToken() == null )
            astNode.jjtSetFirstToken(new Token());
        Token token = astNode.jjtGetFirstToken();
        //        while( token.specialToken != null && token.next != null )
        //            token = token.next;
        token.specialToken = new Token();
        token.specialToken.image = " ";
    }

    /**
     * add indent before astNode
     */
    public static void createIndent(SimpleNode astNode)
    {
        if( astNode.jjtGetFirstToken() == null )
            astNode.jjtSetFirstToken(new Token());
        Token token = astNode.jjtGetFirstToken();
        //        while( token.next != null )
        //            token = token.next;
        token.specialToken = new Token();
        token.specialToken.image = "   ";
    }

    protected AstSymbol createSymbol(String name)
    {
        AstSymbol result = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
        result.setName(validAndNotReservedName(name));
        return result;
    }

    /**
     * name must be validated ( example: "name1.name2.name3" )
     * @param name
     * @return
     */
    protected AstSymbol createSubSymbol(String name)
    {
        AstSubSymbol result = new AstSubSymbol(AntimonyNotationParser.JJTSUBSYMBOL);
        result.setName(name);
        return result;
    }

    /**
     * create SubSymbol if element is port. If element is public port, astSymbol name will variable name
     * @param element
     * @return
     * @throws Exception
     */
    protected AstSymbol createSubSymbol(Node element) throws Exception
    {
        String name;
        if( Util.isPropagatedPort2(element) )
        {
            StringBuilder nameBuilder = new StringBuilder();
            Node propagatedPort = element;
            while( Util.isPropagatedPort2(propagatedPort) )
            {
                if( propagatedPort.getParent() instanceof SubDiagram )
                {
                    SubDiagram subDiagram = (SubDiagram)propagatedPort.getParent();
                    if( nameBuilder.length() == 0 )
                        nameBuilder.append(validAndNotReservedName(subDiagram.getName()));
                    else
                        nameBuilder.append(".").append(validAndNotReservedName(subDiagram.getName()));
                    DynamicProperty dp = propagatedPort.getAttributes().getProperty(SubDiagram.ORIGINAL_PORT_ATTR);
                    if( dp == null )
                        throw new Exception("Incorrect propagated port " + propagatedPort.getName() + ", can't find original port");
                    Node originalPort = subDiagram.getDiagram().findNode(dp.getValue().toString());
                    propagatedPort = Util.getBasePort(originalPort);
                }
                else
                    throw new Exception("Incorrect propogated port " + propagatedPort.getName() + ", port must be have SubDiagram parent");
            }

            if( propagatedPort.getParent() instanceof SubDiagram )
                nameBuilder.append(".").append(validAndNotReservedName( ( (SubDiagram)propagatedPort.getParent() ).getName()));
            nameBuilder.append(".").append(validAndNotReservedName(Util.getPortVariable(propagatedPort)));
            return createSubSymbol(nameBuilder.toString());
        }
        else if( Util.isPublicPort(element) || Util.isPrivatePort(element) )
        {
            name = validAndNotReservedName(Util.getPortVariable(element));
            if( element.getParent() instanceof SubDiagram )
            {
                Node subDiagram = (Node)element.getParent();
                name = validAndNotReservedName(subDiagram.getName()) + "." + name;
                return createSubSymbol(name);
            }
        }
        else if( Util.isBus(element) )
        {
            name = validAndNotReservedName(element.getRole(VariableRole.class).getName());
        }
        else
            name = validAndNotReservedName(element.getName());

        return createSymbol(name);
    }


    /**
     * to link only diagram element used in parameters
     */
    public static void link(DiagramElement de, biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        link(de.getAttributes(), astNode);
    }

    /**
     * to link all diagram elements with the same variable role
     */
    public static void linkWithAssociatedDiagramElements(DiagramElement de, biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        if( de instanceof Node && SbgnUtil.isClone((Node)de) )
        {
            for( DiagramElement assDe : ( (Node)de ).getRole(VariableRole.class).getAssociatedElements() )
                link(assDe.getAttributes(), astNode);
        }
        else
            link(de.getAttributes(), astNode);
    }

    public static void link(DynamicPropertySet dps, biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> set;
        DynamicProperty dp = dps.getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp == null )
        {
            set = new HashSet<>();
            dps.add(DPSUtils.createHiddenReadOnlyTransient(AntimonyConstants.ANTIMONY_LINK, Set.class, set));
        }
        else
            set = (Set<biouml.plugins.antimony.astparser_v2.Node>)dp.getValue();

        set.add(astNode);
    }

    public static void removeLink(DiagramElement de, biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        removeLink(de.getAttributes(), astNode);
    }

    public static void removeLink(DynamicPropertySet dps, biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        DynamicProperty dp = dps.getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp == null || dp.getValue() == null )
            return;

        Set<biouml.plugins.antimony.astparser_v2.Node> set = (Set<biouml.plugins.antimony.astparser_v2.Node>)dp.getValue();
        set.remove(astNode);
    }

    Map<String, String> changedNames = new HashMap<>();
    public String validAndNotReservedName(String name)
    {
        name = AntimonyUtility.getAntimonyValidName(name);
        if( changedNames.containsKey(name) )
            return changedNames.get(name);
        if( !AntimonyUtility.getReservedNameCollection().contains(name) && !changedNames.containsValue(name) )
            return name;
        //to try changing name
        StringBuilder sb = new StringBuilder(name);
        try
        {
            String oldName = name;
            sb.append("_");
            EModel emodel = diagram.getRole(EModel.class);
            while( diagram.findNode(sb.toString()) != null || emodel.getVariable(sb.toString()) != null )
                sb.append("_");
            changedNames.put(oldName, sb.toString());
        }
        catch( Exception e )
        {
            log.log(Level.WARNING, "Can't change name: " + name, e);
        }
        return sb.toString();
    }

    protected String replaceCompliteNames(String formula)
    {
        StringBuffer sb = new StringBuffer();
        String delimiters = " =<>()[]+-/%*^|&;,";
        StringTokenizer tokens = new StringTokenizer(formula.trim(), delimiters, true);
        while( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            if( token.equals(" ") )
                continue;

            if( delimiters.indexOf(token) >= 0 )
                sb.append(token);
            else
                try
                {
                    Double.parseDouble(token);
                    sb.append(token);
                }
                catch( NumberFormatException e )
                {
                    sb.append(validAndNotReservedName(token));
                }
        }
        return sb.toString();
    }

    protected void setFormulaToAstEquation(String formula, AstEquation equation)
    {
        // create valid name for formula elements (example: comp.s1 to s1)
        formula = replaceCompliteNames(formula);
        try
        {
            AntimonyNotationParser parser = new AntimonyNotationParser();
            AstEquation standartFormatEq = parser.parseFormule(new StringReader(formula));
            translateToAntimonyFormat(standartFormatEq);
            equation.setChildren(standartFormatEq.getChildren());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't generate equation: " + formula, e);
        }
    }

    protected void setFormulaToUnitFormula(String formula, AstUnitFormula equation)
    {
        try
        {
            AntimonyNotationParser parser = new AntimonyNotationParser();
            AstUnitFormula eqWithChildren = parser.parseUnitFormule(new StringReader(formula));
            equation.setChildren(eqWithChildren.getChildren());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't generate formula for unit: " + formula, e);
        }
    }


    private void translateToAntimonyFormat(SimpleNode parent)
    {
        // create valid piecewise
        if( parent instanceof AstSpecialFormula && ( (AstSpecialFormula)parent ).getType().equals(AstSpecialFormula.PIECEWISE) )
        {
            for( int i = 0; i < parent.jjtGetNumChildren(); i++ )
            {
                SimpleNode child = (SimpleNode)parent.jjtGetChild(i);
                if( child instanceof AstRegularFormulaElement && child.toString().equals(AstSpecialFormula.PIECEWISE_ARROW) )
                {
                    SimpleNode firstChild = (SimpleNode)parent.jjtGetChild(i - 1);
                    SimpleNode secondChild = (SimpleNode)parent.jjtGetChild(i + 1);
                    parent.jjtAddChild(secondChild, i - 1);
                    parent.jjtAddChild(firstChild, i + 1);
                    ( (AstRegularFormulaElement)child ).setElement(",");
                }
            }
        }

        for( int i = 0; i < parent.jjtGetNumChildren(); i++ )
        {
            SimpleNode child = (SimpleNode)parent.jjtGetChild(i);

            if( child instanceof AstSpecialFormula )
                translateToAntimonyFormat(child);
        }
    }

    public void setFormulaToAstFunction(String formula, AstFunction astFunction)
    {
        astFunction.cleanParametrs();
        final int name = 1;
        final int parameter = 2;
        final int value = 3;
        int current = 1;
        String delimiters = " (),=";
        StringTokenizer tokens = new StringTokenizer(formula.trim(), delimiters, true);
        while( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            if( token.equals(" ") || token.equals("=") || token.equals("function") )
                continue;
            else if( token.equals("(") && current != 3 )
            {
                current++;
                continue;
            }
            else if( token.equals(")") )
            {
                current++;
                continue;
            }

            switch( current )
            {
                case name:
                    astFunction.setFunctionName(token);
                    break;
                case parameter:
                    astFunction.addParameter(token);
                    break;
                case value:
                    StringBuilder sb = new StringBuilder(token);
                    while( tokens.hasMoreTokens() )
                        sb.append(tokens.nextToken());
                    setFormulaToAstEquation(sb.toString(), astFunction.getEquation());
                    return;
                default:
                    return;
            }
        }
    }

    public static class MathEquationFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            String type = de.getKernel().getType();
            return ( type.equals(Type.MATH_EQUATION) || type.equals("equation") );
        }
    }

    /**
     * Order equations by the names of their type and variables for user convenience
     */
    public class EquationComparator implements Comparator<Equation>
    {
        @Override
        public int compare(Equation eq1, Equation eq2)
        {
            int p1 = getEquationPriority(eq1);
            int p2 = getEquationPriority(eq2);

            if( p1 != p2 )
                return p1 - p2;

            String v1 = prepareVariable(eq1.getVariable());
            String v2 = prepareVariable(eq2.getVariable());
            return v1.compareTo(v2);
        }
    }

    /**
     * Priority is needed to order equations by type
     */
    private int getEquationPriority(Equation eq)
    {
        if( eq.getType().equals(Equation.TYPE_RATE) )
            return 0;
        else if( eq.getType().equals(Equation.TYPE_ALGEBRAIC) )
            return 1;
        else if( eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) )
            return 2;
        return 3;
    }

    /**
     * In antimony compartment prefixes are not presented so when we sort equations we should strip them 
     */
    private String prepareVariable(String var)
    {
        String v = var.substring(var.indexOf(".") + 1);
        if( v.startsWith("$") )
            v = v.substring(1);
        return v;
    }

    protected Set<biouml.plugins.antimony.astparser_v2.Node> getLinkedAstNode(DynamicProperty dp)
    {
        if( dp == null )
            return null;
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = (Set<biouml.plugins.antimony.astparser_v2.Node>)dp.getValue();
        return astNodes;
    }

    protected Set<biouml.plugins.antimony.astparser_v2.Node> getLinkedAstNode(DiagramElement de)
    {
        DynamicProperty dp = de.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp == null )
            return null;

        return getLinkedAstNode(dp);
    }
}
