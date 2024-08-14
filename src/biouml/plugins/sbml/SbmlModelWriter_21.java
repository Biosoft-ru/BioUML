package biouml.plugins.sbml;

import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.math.xml.MathMLFormatter;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.sbml.SbmlModelReader_21.MetaIdInfo;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;

public class SbmlModelWriter_21 extends SbmlModelWriter
{

    MathMLFormatter mathMLFormatter;

    public SbmlModelWriter_21()
    {
        mathMLFormatter = new MathMLFormatter();
        mathMLFormatter.declareCSymbol("time", "http://www.sbml.org/sbml/symbols/time");
        mathMLFormatter.declareCSymbol("delay", "http://www.sbml.org/sbml/symbols/delay");
        mathMLFormatter.declareCSymbol("avogadro", "http://www.sbml.org/sbml/symbols/avogadro");
    }

    @Override
    protected void initContext()
    {
        mathMLFormatter.setParserContext((EModel)diagram.getRole());
    }

    @Override
    protected Element createSpecieElement()
    {
        return document.createElement(SPECIE_ELEMENT);
    }

    @Override
    protected void setLevel(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_LEVEL_ATTR, SBML_LEVEL_VALUE_2);
    }

    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_1);
    }

    @Override
    protected void setId(Element element, String id)
    {
        element.setAttribute(ID_ATTR, id);
    }

    @Override
    protected void setTitle(Element element, String id)
    {
        element.setAttribute(NAME_ATTR, id);
    }

    @Override
    protected boolean validCompartmentList(List compartmentList)
    {
        return true;
    }

    @Override
    protected boolean validReaction(Node reaction)
    {
        int counter = 0;
        for( SpecieReference species : (Reaction)reaction.getKernel() )
        {
            if( species.isReactant() || species.isProduct() )
                counter++;
        }
        if( counter > 0 )
            return true;

        error("ERROR_REACTION_INVALID", new String[] {diagram.getName(), reaction.getName()});
        return false;
    }

    protected void appendMathChild(Element element, String formula, Role role)
    {
        try
        {
            EModel emodel = diagram.getRole(EModel.class);
            Parser oldParser = emodel.getParser();
            
//            if (role instanceof Function)
//                emodel.setParser( new FunctionParser( oldParser ) );
//            else
                emodel.setParser(new WriterParser(oldParser));

            String math = parseFormula(formula, (Node)role.getDiagramElement());
            AstStart astStart = emodel.readMath(math, role);
            emodel.setParser(oldParser);

            if( mathMLFormatter.getParserContext() != emodel )
                mathMLFormatter.setParserContext(emodel);
            mathMLFormatter.format(astStart, element, document);
        }
        catch( Throwable t )
        {
            error("ERROR_CREATING_MATH_ELEMENT", new String[] {diagram.getName(), formula, t.getMessage()});
        }
    }

    @Override
    protected void writeModelAttributes(Element model)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty(ID_ATTR);
        if( dp != null && dp.getType().equals(String.class) )
            setId(model, dp.getValue().toString());
        else
            setId(model, getSbmlId(diagram));//castStringToSId(diagram.getName()));

        dp = diagram.getAttributes().getProperty(METAID_ATTR);
        if( dp != null && dp.getType().equals(String.class) )
            model.setAttribute(METAID_ATTR, dp.getValue().toString());

        model.setAttribute(NAME_ATTR, diagram.getTitle());
    }

    @Override
    public Element writeSpecie(Element speciesListElement, Node species)
    {
        try
        {
            Element speciesElement = createSpecieElement();
            speciesListElement.appendChild(speciesElement);
            setId(speciesElement, getSbmlId(species));//castStringToSId( species.getCompleteNameInDiagram() ) );

            if( species.getTitle() != null && !species.getName().equals(species.getTitle()) )
                setTitle(speciesElement, species.getTitle());

            VariableRole role = species.getRole(VariableRole.class);

            if( role.isConstant() )
                speciesElement.setAttribute(CONSTANT_ATTR, Boolean.toString(role.isConstant()));

            if( role.isBoundaryCondition() )
                speciesElement.setAttribute(SPECIE_BOUNDARY_CONDITION_ATTR, Boolean.toString(role.isBoundaryCondition()));

            boolean hasOnlySubstanceUnits = role.getQuantityType() == VariableRole.AMOUNT_TYPE;
            speciesElement.setAttribute(SPECIE_HAS_ONLY_SUBSTANCE_UNITS_ATTR, String.valueOf(hasOnlySubstanceUnits));

            boolean initialSubstance = role.getInitialQuantityType() == VariableRole.AMOUNT_TYPE;

            if( initialSubstance )
                speciesElement.setAttribute(SPECIE_INITIAL_AMOUNT_ATTR, String.valueOf(role.getInitialValue()));
            else
                speciesElement.setAttribute(SPECIE_INITIAL_CONCENTRATION_ATTR, String.valueOf(role.getInitialValue()));

            if( role.getUnits() != null && !role.getUnits().isEmpty() )
                speciesElement.setAttribute(SUBSTANCE_UNITS_ATTR, role.getUnits());

            //type issues
            BaseSupport sType = (BaseSupport) ( ( species ).getAttributes().getValue(SPECIE_TYPE_ATTR) );
            if( sType != null )
                speciesElement.setAttribute(SPECIE_TYPE_ATTR, sType.getName());

            Compartment parent = (Compartment)species.getOrigin();
            String compartment = ( parent instanceof Diagram ) ? defaultCompartmentName : getSbmlId(parent);
            speciesElement.setAttribute(COMPARTMENT_ATTR, compartment);

            Specie speciesKernel = null;
            if( species.getKernel() instanceof Specie )
            {
                speciesKernel = (Specie)species.getKernel();
                if( speciesKernel.getCharge() != 0 )
                    speciesElement.setAttribute(SPECIE_CHARGE_ATTR, "" + speciesKernel.getCharge());
            }

            // write node info as BioUML extension
            Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
            writeAnnotation(annotationElement, species);

            if( writeBioUMLAnnotation )
            {
                Element bioumlElement = document.createElement(BIOUML_ELEMENT);
                bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);

                Element nodeInfoElement = document.createElement(BIOUML_NODE_INFO_ELEMENT);
                DiagramXmlWriter.writeNodeInfo(nodeInfoElement, species, document);
                nodeInfoElement.setAttribute("completeName", species.getCompleteNameInDiagram());
                bioumlElement.appendChild(nodeInfoElement);

                if( species.getKernel() != null && species.getKernel() instanceof Specie )
                {
                    speciesKernel = (Specie)species.getKernel();
                    if( !speciesKernel.getType().equals(Specie.TYPE_MOLECULE) )
                    {
                        Element speciesInfoElement = document.createElement(BIOUML_SPECIE_INFO_ELEMENT);
                        speciesInfoElement.setAttribute(BIOUML_SPECIE_TYPE_ATTR, speciesKernel.getType());
                        DiagramXmlWriter.serializeDPS(document, speciesInfoElement, speciesKernel.getAttributes(), null);
                        writeVariable(species.getRole(VariableRole.class), bioumlElement);
                        bioumlElement.appendChild(speciesInfoElement);

                    }
                }
                annotationElement.appendChild(bioumlElement);
            }
            if( annotationElement.hasChildNodes() )
                speciesElement.appendChild(annotationElement);

            return speciesElement;
        }
        catch( Throwable t )
        {
            error("ERROR_SPECIE_WRITING", new String[] {diagram.getName(), species.getName(), t.getMessage()});
        }
        return null;
    }

    @Override
    protected void writeFunctionDefinitionList(Element modelElement)
    {
        Element functionList = document.createElement(FUNCTION_LIST_ELEMENT);
        for( Function function : emodel.getFunctions() )
        {
            try
            {
                writeFunction(function, functionList);
            }
            catch( Throwable t )
            {
                error("ERROR_FUNCTION_DECLARATION_PROCESSING",
                        new String[] {diagram.getName(), function.getDiagramElement().getName(), t.getMessage()});
            }
        }
        if( functionList.hasChildNodes() )
            modelElement.appendChild(functionList);
    }

    @Override
    public Element writeFunction(Function function, Element functionListElement)
    {
        Element functionDefinitonElement = document.createElement(FUNCTION_DEFINITION_ELEMENT);
        Node node = (Node)function.getDiagramElement();
        functionDefinitonElement.setAttribute(ID_ATTR, function.getName());

        Element annotationElement = document.createElement(ANNOTATION_ELEMENT);
        writeAnnotation(annotationElement, node);
        if( writeBioUMLAnnotation )
        {
            Element bioumlElement = document.createElement(BIOUML_ELEMENT);
            bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);
            Element nodeInfoElement = document.createElement(BIOUML_NODE_INFO_ELEMENT);
            DiagramXmlWriter.writeNodeInfo(nodeInfoElement, node, document);
            nodeInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, node.getCompleteNameInDiagram());
            bioumlElement.appendChild(nodeInfoElement);
            annotationElement.appendChild(bioumlElement);
        }

        if( annotationElement.hasChildNodes() )
            functionDefinitonElement.appendChild(annotationElement);

        appendMathChild(functionDefinitonElement, function.getFormula(), function);
        functionListElement.appendChild(functionDefinitonElement);
        return functionDefinitonElement;
    }

    @Override
    protected Element writeCompartmentList(Element model)
    {
        Element compartmentListElement = super.writeCompartmentList(model);
        if( compartmentListElement.hasChildNodes() )
            model.appendChild(compartmentListElement);
        return compartmentListElement;
    }

    @Override
    public Element writeCompartment(Element compartmentListElement, Compartment compartment)
    {
        Element compartmentElement = super.writeCompartment(compartmentListElement, compartment);
        int dimension = ( (biouml.standard.type.Compartment)compartment.getKernel() ).getSpatialDimension();
        compartmentElement.setAttribute(COMPARTMENT_DIMENSION_ATTR, String.valueOf(dimension));
       
        if (!isAutoCreated( compartment ))
        {
            boolean isConstant = compartment.getRole( VariableRole.class ).isConstant();
            if (!isConstant)
                compartmentElement.setAttribute( CONSTANT_ATTR, Boolean.toString(isConstant) );
        }
        return compartmentElement;
    }

    @Override
    protected Element writeSpecieList(Element model)
    {
        Element speciesListElement = super.writeSpecieList(model);
        if( speciesListElement.hasChildNodes() )
            model.appendChild(speciesListElement);
        return speciesListElement;
    }

    @Override
    protected Element writeReactionList(Element model)
    {
        Element reactionListElement = document.createElement(REACTION_LIST_ELEMENT);
        writeReactionList(reactionListElement, diagram);
        if( reactionListElement.hasChildNodes() )
            model.appendChild(reactionListElement);
        return reactionListElement;
    }

    @Override
    protected void writeRuleList(Element modelElement)
    {
        Element ruleListElement = document.createElement(RULE_LIST_ELEMENT);

        for( Equation eq : emodel.getEquations(new EModel.NotInitialAssignmentsFilter()) )
        {
            try
            {
                writeRule(eq, ruleListElement);
            }
            catch( Throwable t )
            {
                error("ERROR_RULE_PROCESSING", new String[] {diagram.getName(), eq.getFormula(), t.getMessage()});
            }
        }
        if( ruleListElement.hasChildNodes() )
            modelElement.appendChild(ruleListElement);
    }

    @Override
    public Element writeRule(Equation equation, Element ruleListElement)
    {
        if( equation.getDiagramElement().getKernel().getType().equals(Type.MATH_EQUATION) )
        {
            Node ruleNode = (Node)equation.getDiagramElement();
            Element ruleElement = null;
            if( equation.getType().equals(Equation.TYPE_ALGEBRAIC) )
            {
                ruleElement = document.createElement(RULE_ALGEBRAIC_ELEMENT);
            }
            else
            {
                ruleElement = equation.getType().equals(Equation.TYPE_RATE) ? document.createElement(RULE_RATE_ELEMENT)
                        : equation.getType().equals(Equation.TYPE_SCALAR) ? document.createElement(RULE_ASSIGNEMENT_ELEMENT) : null;
                if( ruleElement == null )
                    throw new IllegalStateException("Invalid equation type: " + equation.getType() + " (equation is " + equation + ")");

                String variableName = castVariableNameToSId(equation.getVariable());
                ruleElement.setAttribute(RULE_VARIABLE_ATTR, variableName);
            }

            MetaIdInfo info = getMetaId( ruleNode, null );
            if( info != null )
                ruleElement.setAttribute(METAID_ATTR, info.id);
            
//            if( ruleNode.getName() != null )
//                ruleElement.setAttribute(METAID_ATTR, getSbmlId(ruleNode));

            Element annotationElement = document.createElement(ANNOTATION_ELEMENT);

            if( writeBioUMLAnnotation )
            {
                Element bioumlElement = document.createElement(BIOUML_ELEMENT);
                bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);
                Element nodeInfoElement = document.createElement(BIOUML_NODE_INFO_ELEMENT);
                DiagramXmlWriter.writeNodeInfo(nodeInfoElement, ruleNode, document);
                nodeInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, ruleNode.getCompleteNameInDiagram());
                bioumlElement.appendChild(nodeInfoElement);
                annotationElement.appendChild(bioumlElement);
            }
            if( annotationElement.hasChildNodes() )
                ruleElement.appendChild(annotationElement);

            appendMathChild(ruleElement, equation.getFormula(), equation);

            ruleListElement.appendChild(ruleElement);
            return ruleElement;
        }
        return null;
    }

    @Override
    protected void writeInitialAssignmentList(Element modelElement)
    {
        Element initialAssignmentsList = document.createElement(INITIAL_ASSIGNMENT_LIST_ELEMENT);

        for( Equation assignment : emodel.getInitialAssignments() )
        {
            try
            {
                writeInitialAssignment(assignment, initialAssignmentsList);
            }
            catch( Throwable t )
            {
                error("ERROR_INITIAL_ASSIGNMENT_PROCESSING", new String[] {diagram.getName(), assignment.getFormula(), t.getMessage()});
            }
        }
        if( initialAssignmentsList.hasChildNodes() )
            modelElement.appendChild(initialAssignmentsList);
    }
    @Override
    public Element writeInitialAssignment(Equation assignment, Element initialAssignmentsListElement)
    {
        if( assignment.getDiagramElement().getKernel().getType().equals(Type.MATH_EQUATION) )
        {
            Node assignmentNode = (Node)assignment.getDiagramElement();
            Element assignmentElement = document.createElement(INITIAL_ASSIGNMENT_ELEMENT);

            Element annotationElement = document.createElement(ANNOTATION_ELEMENT);

            if( assignmentNode.getName() != null )
                assignmentElement.setAttribute(METAID_ATTR, getSbmlId(assignmentNode));

            // write info as BioUML extension
            if( writeBioUMLAnnotation )
            {
                Element bioumlElement = document.createElement(BIOUML_ELEMENT);
                bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);

                Element nodeInfoElement = document.createElement(BIOUML_NODE_INFO_ELEMENT);
                DiagramXmlWriter.writeNodeInfo(nodeInfoElement, assignmentNode, document);
                nodeInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, assignmentNode.getCompleteNameInDiagram());
                bioumlElement.appendChild(nodeInfoElement);
                annotationElement.appendChild(bioumlElement);
            }
            if( annotationElement.hasChildNodes() )
                assignmentElement.appendChild(annotationElement);

            String variableName = castVariableNameToSId(assignment.getVariable());
            assignmentElement.setAttribute(INITIAL_ASSIGNMENT_VARIABLE_ATTR, variableName);

            appendMathChild(assignmentElement, assignment.getFormula(), assignment);
            initialAssignmentsListElement.appendChild(assignmentElement);
            return assignmentElement;
        }
        return null;
    }

    @Override
    protected void writeEventList(Element modelElement)
    {
        Element eventListElement = document.createElement(EVENT_LIST_ELEMENT);

        for( Event event : emodel.getEvents() )
        {
            try
            {
                writeEvent(event, eventListElement);
            }
            catch( Throwable t )
            {
                error("ERROR_PROCESSING_EVENT", new String[] {diagram.getName(), event.getDiagramElement().getName(), t.getMessage()});
            }
        }

        if( eventListElement.hasChildNodes() )
            modelElement.appendChild(eventListElement);
    }

    @Override
    public Element writeEvent(Event event, Element eventListElement)
    {
        Element eventElement = document.createElement(EVENT_ELEMENT);

        Node node = (Node)event.getDiagramElement();
        if( node.getName() != null )
            eventElement.setAttribute(ID_ATTR, getSbmlId(node));

        MetaIdInfo info = getMetaId(node, null);

        if( info != null )
            eventElement.setAttribute(METAID_ATTR, info.id);
        /**
         * @todo Check this out
         */
        if( node.getComment() != null && node.getComment().length() > 0 )
            eventElement.setAttribute(NAME_ATTR, node.getComment());

        if( event.getTimeUnits() != null && event.getTimeUnits().length() > 0 )
            eventElement.setAttribute(TIME_UNITS_ATTR, event.getTimeUnits());

        if( event.getTrigger() != null && event.getTrigger().length() > 0 )
        {
            Element triggerElement = document.createElement(TRIGGER_ELEMENT);
            appendMathChild(triggerElement, event.getTrigger(), event);
            eventElement.appendChild(triggerElement);

            info = getMetaId(node, "trigger");
            if( info != null )
                triggerElement.setAttribute(METAID_ATTR, info.id);
        }

        if( event.getDelay() != null && event.getDelay().length() > 0 && !"0".equals(event.getDelay()) )
        {
            Element delayElement = document.createElement(DELAY_ELEMENT);
            appendMathChild(delayElement, event.getDelay(), event);
            eventElement.appendChild(delayElement);

            info = getMetaId(node, "delay");
            if( info != null )
                delayElement.setAttribute(METAID_ATTR, info.id);
        }

        Element annotationElement = document.createElement(ANNOTATION_ELEMENT);

        if( writeBioUMLAnnotation )
        {
            Element bioumlElement = document.createElement(BIOUML_ELEMENT);
            bioumlElement.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);

            Element nodeInfoElement = document.createElement(BIOUML_NODE_INFO_ELEMENT);
            Node ruleNode = (Node)event.getDiagramElement();
            DiagramXmlWriter.writeNodeInfo(nodeInfoElement, ruleNode, document);
            nodeInfoElement.setAttribute(BIOUML_COMPLETE_NAME_ATTR, node.getCompleteNameInDiagram());
            bioumlElement.appendChild(nodeInfoElement);
            annotationElement.appendChild(bioumlElement);
        }
        if( annotationElement.hasChildNodes() )
            eventElement.appendChild(annotationElement);
        writeEventAssignmentList(event, eventElement);

        eventListElement.appendChild(eventElement);
        return eventElement;
    }

    protected void writeEventAssignmentList(Event event, Element eventElement)
    {
        Element eventAssignmentListElement = document.createElement(ASSIGNEMENT_LIST_ELEMENT);
        Node node = (Node)event.getDiagramElement();
        Assignment[] assignments = event.getEventAssignment();
        for( int i = 0; i < assignments.length; i++ )
        {
            try
            {
                Element eventAssignmentElement = writeAssignment(assignments[i], eventAssignmentListElement);
                eventAssignmentListElement.appendChild(eventAssignmentElement);

                MetaIdInfo info = getMetaId(node, "eventAssignment", i);
                if( info != null )
                    eventAssignmentElement.setAttribute(METAID_ATTR, info.id);
            }
            catch( Throwable t )
            {
                error("ERROR_PROCESSING_ASSIGNMENT", new String[] {diagram.getName(),
                        event.getDiagramElement() != null ? event.getDiagramElement().getName() : null, t.getMessage()});
            }
        }

        if( eventAssignmentListElement.hasChildNodes() )
            eventElement.appendChild(eventAssignmentListElement);
    }

    protected Element writeAssignment(Assignment assignment, Element eventAssignmentListElement)
    {
        Element eventAssignmentElement = document.createElement(ASSIGNEMENT_ELEMENT);
        String variableName = castVariableNameToSId(assignment.getVariable());
        eventAssignmentElement.setAttribute(ASSIGNEMENT_VARIABLE_ATTR, variableName);
        appendMathChild(eventAssignmentElement, assignment.getMath(), assignment.getRole());
        return eventAssignmentElement;
    }

    @Override
    protected void writeFormula(String formula, Node reaction, Element kineticLawElement)
    {
        appendMathChild(kineticLawElement, formula, reaction.getRole());
    }

    /**
     * @todo - use math formatter
     */
    @Override
    protected void writeSpeciesReferenceAttributes(Element specieReferenceElement, SpecieReference species, Node reaction)
    {
        String s = species.getStoichiometry();
        if( s == null )
            return;
        s = s.trim();
        if( s.isEmpty() )
            return;

        AstStart start = emodel.readMath(s, reaction.getRole());

        if( start.jjtGetNumChildren() == 1 && start.jjtGetChild(0) instanceof AstConstant )
            specieReferenceElement.setAttribute(STOICHIOMETRY_ATTR, "" + ( (AstConstant)start.jjtGetChild(0) ).getValue());
        else
        {
            Element stoichiometryMathElement = document.createElement(STOICHIOMETRY_MATH_ELEMENT);
            appendMathChild(stoichiometryMathElement, s, reaction.getRole());
            specieReferenceElement.appendChild(stoichiometryMathElement);
        }
    }

    @Override
    protected void writeConstraintList(Element model)
    {
        if( emodel.getConstraints().length > 0 )
            log.info("Constraints in the model " + diagram.getName() + " are not supported by target SBML level and will be ignored");
    }

    @Override
    protected void writeModifiers(Element reactionElement, Node reaction)
    {
        Element modifierListElement = document.createElement(MODIFIER_LIST_ELEMENT);
        writeSpecieReferences(modifierListElement, reaction, SpecieReference.MODIFIER, MODIFIER_SPECIE_REFERENCE_ELEMENT);
        if( modifierListElement.getFirstChild() != null )
            reactionElement.appendChild(modifierListElement);
    }

    @Override
    protected void writeReactants(Element reactionElement, Node reaction)
    {
        Element reactantListElement = document.createElement(REACTANT_LIST_ELEMENT);
        writeSpecieReferences(reactantListElement, reaction, SpecieReference.REACTANT);
        if( reactantListElement.hasChildNodes() )
            reactionElement.appendChild(reactantListElement);
    }

    @Override
    protected void writeProducts(Element reactionElement, Node reaction)
    {
        Element productListElement = document.createElement(PRODUCT_LIST_ELEMENT);
        writeSpecieReferences(productListElement, reaction, SpecieReference.PRODUCT);
        if( productListElement.hasChildNodes() )
            reactionElement.appendChild(productListElement);
    }

    protected String castVariableNameToSId(String variableName)
    {
        if( emodel.containsVariable(variableName) )
            return getSbmlId(emodel.getVariable(variableName));
        return castStringToSId(variableName);
    }

    /**
     * Checks if the model represented by diagram is representable
     * in SBML level 1 version 1 model.
     */
    public static boolean isValidModel(Diagram diagram)
    {
        EModel emodel = (EModel)diagram.getRole();

        if( emodel == null )
            return false;

        int modelType = emodel.getModelType();

        if( EModel.isOfType(modelType, EModel.STATE_TRANSITION_TYPE) )
            return false;

        return true;
    }

    @Override
    protected String getSbmlNamespace()
    {
        return SBML_LEVEL2_XMLNS_VALUE;
    }

    @Override
    protected String getUnitsAttr()
    {
        return SPECIE_SUBSTANCE_UNITS_ATTR;
    }

    @Override
    protected String getCompartmentVolumeAttr()
    {
        return COMPARTMENT_SIZE_ATTR;
    }

    class WriterParser extends Parser
    {
        public WriterParser(Parser prototype)
        {
            super();
            setContext(prototype.getContext());
            setDeclareUndefinedVariables(false);
        }
        @Override
        protected String processVariable(String varName)
        {
            return castVariableNameToSId(varName);
        }
    }
    
    class FunctionParser extends Parser
    {
        public FunctionParser(Parser prototype)
        {
            super();
            setContext(prototype.getContext());
            setDeclareUndefinedVariables(false);
        }
        @Override
        protected String processVariable(String varName)
        {
            return varName;
        }
    }

    public static MetaIdInfo getMetaId(Node node, String propertyName, int index)
    {
        DynamicProperty dp = node.getAttributes().getProperty(METAID_ATTR);
        if( dp == null )
            return null;

        List<MetaIdInfo> infos = (List<MetaIdInfo>)dp.getValue();

        for( MetaIdInfo info : infos )
        {
            if( propertyName != null && propertyName.equals(info.getProperty()) && index == info.index )
                return info;
        }
        return null;
    }

    public static MetaIdInfo getMetaId(Node node, String propertyName)
    {
        DynamicProperty dp = node.getAttributes().getProperty(METAID_ATTR);
        if( dp == null )
            return null;

        List<MetaIdInfo> infos = (List<MetaIdInfo>)dp.getValue();

        for( MetaIdInfo info : infos )
        {
            if( propertyName != null && propertyName.equals(info.getProperty()) )
                return info;
            else if( propertyName == null && info.getProperty() == null )
                return info;
        }
        return null;
    }
}
