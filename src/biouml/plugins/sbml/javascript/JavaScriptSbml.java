package biouml.plugins.sbml.javascript;

import java.awt.Point;
import java.io.File;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import java.util.logging.Logger;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.javascript.JavaScriptModel;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.SbmlDiagramTransformer;
import biouml.plugins.sbml.SbmlDiagramType;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.SbmlModelWriter;
import biouml.plugins.sbml.extensions.SBGNExtension;
import biouml.plugins.sbml.extensions.SbmlAnnotationRegistry;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.util.ApplicationUtils;

public class JavaScriptSbml extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger(JavaScriptSbml.class.getName());

    /**
     * 
     * @return the array of supported SBML files formats.
     */
    public String[] formats()
    {
        return SbmlConstants.SBML_SUPPORTED_FORMATS;
    }

    /**
     * 
     * @return the array of supported extensions (used in annotation tag)
     */
    public String[] extensions()
    {
        Set<String> extensionsKey = SbmlAnnotationRegistry.getNamespaces();
        String[] extensions = new String[extensionsKey.size()];

        Iterator<String> it = extensionsKey.iterator();
        int i = 0;
        while( it.hasNext() )
        {
            String key = it.next();
            if( "".equals(key) )
            {
                extensions[i] = SBGNExtension.SBGN_ELEMENT;
            }
            else
            {
                extensions[i] = key;
            }
            i++;
        }
        return extensions;
    }

    /**
     * Loads SBML file by the specified path.
     * 
     * @param path the path to SBML file to be loaded.
     * @return the loaded diagram on succes or null otherwise.
     */
    public Diagram load(String path)
    {
        if( !new File(path).exists() )
        {
            log.log(Level.SEVERE, "Incorrect file path: " + path);
            return null;
        }

        File file = new File(path);
        String name = ApplicationUtils.getFileNameWithoutExtension(file.getName());
        Diagram diagram = null;
        try
        {
            diagram = SbmlModelFactory.readDiagram(file, null, name);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not read the diagram '" + name + "'.");
        }
        return diagram;
    }

    /**
     * Saves SBML file with the specified path
     * 
     * @param diagram the diagram to be stored in SBML format.
     * @param path the path for SBML file to store the model.
     * @param extensions SBML extensions to save extra information, for example diagram layout.
     * See extensions function for further details.
     */
    public void save(Diagram diagram, String path, String[] extensions)
    {
        if( diagram == null )
        {
            log.log(Level.SEVERE, "Can not save incorrect diagram.");
            return;
        }
        if( path == null || path.equals("") )
        {
            log.log(Level.SEVERE, "Incorrect path for the diagram saving.");
        }
        try
        {
            SbmlDiagramTransformer transformer = new SbmlDiagramTransformer();
            transformer.init(null, diagram.getOrigin());
            Diagram diagramToWrite = transformer.getDiagramToWrite(diagram);

            if( diagramToWrite.getType() instanceof SbmlDiagramType )
            {
                SbmlModelWriter writer = SbmlModelFactory.getWriter(diagramToWrite);
                writer.validateAnnotationsExtensions(getNamespaces(extensions));

                File file = new File(path);

                SbmlModelFactory.writeDiagram(file, diagramToWrite, writer);
            }
            else
            {
                log.log(Level.SEVERE, "Can not save diagram '" + diagram.getName() + "'. Unknown diagram type: '"
                        + diagramToWrite.getType().getClass().getName() + "'.");
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not save the diagram: '" + diagram.getName() + "'.", e);
        }
    }

    private Set<String> getNamespaces(String[] extensions)
    {
        Set<String> namespaces = new TreeSet<>();
        if( extensions != null )
        {
            for( int i = 0; i < extensions.length; ++i )
            {
                if( SBGNExtension.SBGN_ELEMENT.equals(extensions[i]) )
                {
                    namespaces.add("");
                }
                else if( SbmlAnnotationRegistry.getNamespaces().contains(extensions[i]) )
                {
                    namespaces.add(extensions[i]);
                }
                else
                {
                    log.info("Unknown extension '" + extensions[i] + "' will be removed.");
                }
            }
        }
        return namespaces;
    }
    
    /**
     * Creates diagram with given name in given collection
     */
    public Diagram createDiagram(DataCollection origin, String name) throws Exception
    {  
        Diagram result = new SbgnDiagramType().createDiagram(origin, name, new DiagramInfo(name));
        if( origin != null )
            result.save();
        return result;
    }
    
    public Diagram createDiagram(String name) throws Exception
    {  
        return createDiagram(null, name);
    }
    
    public VariableRole addSpecies(Diagram diagram, String name, double initialValue)
    {
        return addSpecies(diagram, null, name, initialValue);
    }
    
    /**
     * Adds new specie to the diagram.
     *
     * @param diagram the diagram to add specie to.
     * @param name name of the specie
     * @param initialValue initial value of the specie.
     */
    public VariableRole addSpecies(Diagram diagram, String compartmentName, String name, double initialValue)
    {
        try
        {
            if( diagram == null )
                throw new IllegalArgumentException("Diagram is null.");
            else if( name == null )
                throw new IllegalArgumentException("Name is null.");

            DiagramElement compartment;
            if( compartmentName == null )
                compartment = diagram;
            else
                compartment = diagram.get(compartmentName);

            if (!(compartment instanceof Compartment))
                throw new IllegalArgumentException("Can not create species inside compartment "+compartmentName );
            
            CreatorElementWithName controller = (CreatorElementWithName)diagram.getType().getSemanticController();
            DiagramElementGroup deg = controller.createInstance((Compartment)compartment, Specie.class, name, new Point(), null);
            DiagramElement de = deg.getElement();
            VariableRole role = de.getRole(VariableRole.class);
            role.setInitialValue(initialValue);
            deg.putToCompartment();
            return role;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add specie.", e);
            return null;
        }
    }
    
    public Variable addParameter(Diagram diagram, String name, double value)
    {
        diagram.getRole(EModel.class).declareVariable(name, value);
        return diagram.getRole(EModel.class).getVariable(name);
    }
    
    public Reaction addSimpleReaction(Diagram diagram, String name, String formula, String reactant, String product)
    {
        JavaScriptModel javaScriptModel = new JavaScriptModel();
        SpecieReference[] refs = new SpecieReference[2];
        refs[0] = javaScriptModel.createSpecieReference(diagram, reactant, "reactant");
        refs[1] = javaScriptModel.createSpecieReference(diagram, product, "product");
        return addReaction(diagram, name, formula, refs);
    }
    
    public Reaction addReaction(Diagram diagram, SpecieReference[] specieReferences)
    {
        return addReaction(diagram, specieReferences);
    }
    
    public Reaction addReaction(Diagram diagram, String name, SpecieReference[] specieReferences)
    {
        return addReaction(diagram, name, specieReferences);
    }
    
    public Reaction addReaction(Diagram diagram, String name, String formula, SpecieReference[] specieReferences)
    {
        try
        {
            if( diagram == null )
                throw new IllegalArgumentException("Cannot create reaction with no diagram specified.");
            else if( name == null )
                name = DefaultSemanticController.generateUniqueName(diagram, "Reaction");
            
            CreatorElementWithName controller = (CreatorElementWithName)diagram.getType().getSemanticController();
            Reaction prototype = new Reaction(null, name);
            prototype.setSpecieReferences(specieReferences);
            if( formula != null )
                prototype.setFormula(formula);
            DiagramElementGroup deg = controller.createInstance(diagram, Reaction.class, name, new Point(), prototype);
            DiagramElement de = deg.getElement();
            Reaction reaction = (Reaction)de.getKernel();
            deg.putToCompartment();
            return reaction;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add specie.", e);
            return null;
    }
        
    
    }
}
