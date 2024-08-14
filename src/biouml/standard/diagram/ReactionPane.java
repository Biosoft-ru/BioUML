package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.DefaultRowModel;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;

public class ReactionPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(ReactionEditPane.class.getName());
    protected ReactionInitialProperties properties = null;
    protected Diagram diagram;
    protected Compartment compartment;
    protected Reaction reactionTemplate;
    protected Point point;
    protected ViewEditorPane viewEditor;
    protected ViewPaneAdapter adapter;
    protected JTextField titleField;
    protected JTextField reactionName;
    protected JTextField reactionTitle;
    protected JTextField componentName;
    protected JTextField variableName;

    /**
     * Panel to edit reaction kinetik law. It is only needed if diagram is simulatable (has EModel). Otherwise it is null;
     */
    protected ReactionEditPane reactionEditPane = null;
    protected JComboBox<String> componentRole;
    protected ResourceBundle resources = ResourceBundle.getBundle(MessageBundle.class.getName());
    protected DefaultRowModel components;
    protected TabularPropertyInspector componentsTable;

    private JTabbedPane tabbedPane;
    private JButton addButton;
    private JButton removeButton;

    /**
     * Indicates whether we edit existing reaction or create new one 
     */
    protected boolean newReaction;
    private boolean simulatable;

    public ReactionPane(Reaction reaction, Diagram diagram, Compartment compartment, Point point, ViewEditorPane viewEditor)
    {
        this(reaction, diagram, compartment, point, viewEditor, null);
    }

    public ReactionPane(Diagram diagram, Compartment compartment, Point point, ViewEditorPane viewEditor)
    {
        this(null, diagram, compartment, point, viewEditor, null);
    }

    public ReactionPane(Diagram diagram, Compartment compartment, Point point, ViewEditorPane viewEditor,
            ReactionInitialProperties properties)
    {
        this(null, diagram, compartment, point, viewEditor, properties);
    }

    public ReactionPane(Reaction reaction, Diagram diagram, Compartment compartment, Point point, ViewEditorPane viewEditor,
            ReactionInitialProperties properties)
    {
        super(new BorderLayout());

        this.properties = properties;
        this.diagram = diagram;
        this.compartment = compartment;
        this.point = point;
        this.viewEditor = viewEditor;
        this.newReaction = reaction == null;
        simulatable = diagram.getRole() instanceof EModel;
        String reactionName = ReactionInitialProperties.generateReactionName(diagram);
        this.reactionTemplate = newReaction ? new Reaction(null, reactionName) : reaction;
        this.tabbedPane = new JTabbedPane();
        this.components = new DefaultRowModel();
        this.componentsTable = new TabularPropertyInspector();

        viewEditor.setSelectionEnabled(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createReactionNamePanel(), BorderLayout.NORTH);
        Node reactionNode = Util.findNode(diagram, reactionTemplate);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createEditComponentsPanel(), BorderLayout.NORTH);
        mainPanel.add(createComponentListPanel(components, componentsTable), BorderLayout.CENTER);

        if( simulatable ) //if diagram is not simulatable then we do not need reaction edit pane
        {
            tabbedPane.addTab("Components", mainPanel);
            reactionEditPane = new ReactionEditPane(reactionTemplate, reactionNode != null ? reactionNode.getRole() : null, diagram,
                    components, newReaction, true);
            tabbedPane.addTab("Edit formula", reactionEditPane);
            add(tabbedPane, BorderLayout.CENTER);
        }
        else
        {
            add(mainPanel, BorderLayout.CENTER);
        }

        //--- Events processing -----------------------------------------------/
        addButton.addActionListener(e -> {
            addComponent(componentName.getText(), (String)componentRole.getSelectedItem());
        });
        removeButton.addActionListener(e -> {
            removeComponent(componentsTable.getTable().getSelectedRow());
        });

        componentsTable.getTable().getSelectionModel()
                .addListSelectionListener(e -> removeButton.setEnabled(componentsTable.getTable().getSelectedRow() >= 0));

        adapter = ( new ViewPaneAdapter()
        {
            @Override
            public void mousePressed(ViewPaneEvent e)
            {
                Object model = e.getViewSource().getModel();

                if( model instanceof Role )
                    model = ( (Role)model ).getDiagramElement();

                if( model instanceof Node )
                {
                    Node node = (Node)model;
                    //TODO: create fine criteria for reaction participants
                    //rolled back for non-simulation diagrams without variable roles but with reactions
                    if( ! ( diagram.getType().getSemanticController().isAcceptableForReaction(node) ) )
                        return;

                    componentName.setText(CollectionFactory.getRelativeName(node, ReactionPane.this.diagram));
                    addButton.setEnabled(true);

                    if( simulatable )
                    {
                        String variable = "";
                        if( node.getRole() instanceof VariableRole )
                        {
                            variable = node.getRole(VariableRole.class).getName();
                            int mode = diagram.getViewOptions().getVarNameCode();
                            variable = diagram.getRole( EModel.class ).getQualifiedName( variable, node, mode ); //get titled variable
                        }
                        variableName.setText(variable);
                    }
                }
            }
        } );
        viewEditor.addViewPaneListener(adapter);
    }

    protected void removeComponent(int index)
    {
        if( index >= 0 && index < components.size() )
        {
            components.remove(index);
            buildReactionTitle();
            if( simulatable )
                reactionEditPane.changeComponents();
        }
    }

    private void buildReactionTitle()
    {
        titleField.setText(DiagramUtility.generateReactionTitle(getComponentList()));
    }

    /**
     * Get reaction component list
     */
    public List<SpecieReference> getComponentList()
    {
        List<SpecieReference> componentsList = new ArrayList<>();
        for( int i = 0; i < components.size(); i++ )
            componentsList.add((SpecieReference)components.getBean(i));
        return componentsList;
    }

    public void release()
    {
        viewEditor.setSelectionEnabled(true);
        viewEditor.removeViewPaneListener(adapter);
    }

    //TODO: do something more elegant with exception
    public boolean okPressed()
    {
        try
        {
            boolean result = newReaction ? createReaction() : modifyReaction();
            if( !result )
                return false;
        }
        catch( CreateReactionException ex )
        {
            JOptionPane.showMessageDialog(ReactionPane.this, ex.getMessage(), resources.getString("REACTION_ERROR_TITLE"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        catch( Exception ex )
        {
            String dialogTitle = resources.getString("REACTION_ERROR_TITLE");
            String message = resources.getString("REACTION_ERROR_MESSAGE") + ex.getMessage();
            JOptionPane.showMessageDialog(ReactionPane.this, message, dialogTitle, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        release();
        return true;
    }

    protected void cancelPressed()
    {
        release();
    }

    /**
     * Add new reaction component
     * @param reactionTemplate 
     */
    public void addComponent(String name, String role)
    {
        // check whether such component is already added
        for( int i = 0; i < components.size(); i++ )
        {
            SpecieReference component = (SpecieReference)components.getBean(i);
            if( component.getName().equals(name) && component.getRole().equals(role) )
            {
                String title = resources.getString("REACTION_ERROR_TITLE");
                String message = resources.getString("REACTION_COMPONENT_DUPLICATED");
                message = MessageFormat.format(message, new Object[] {name, role});
                JOptionPane.showMessageDialog(ReactionPane.this, message, title, JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        //TODO: use SpecieReference wrapper class with bean info to avoid recreating when reaction name and specie role changes
        try
        {
            SpecieReference component = new SpecieReference(reactionTemplate, name, role);
            boolean notificationEnabled = component.isNotificationEnabled();
            component.setNotificationEnabled(false);

            // set title as a kernel title
            DiagramElement de = (DiagramElement)CollectionFactory.getDataElement(component.getName(), diagram);
            if( de != null )
            {
                component.setTitle(SpecieReference.generateSpecieReferenceName(reactionTemplate.getName(), de.getKernel().getName(), role));
                //TODO: Think about it: in SBGN diagrams module is no needed, in some cases diagram is not in the module
                component.setSpecie(CollectionFactory.getRelativeName(de, Module.getModule(de)));
            }
            else
            {
                component.setTitle(SpecieReference.generateSpecieReferenceName(reactionTemplate.getName(), name, role));
                component.setSpecie(name);
            }

            component.setInitialized(false);

            if( notificationEnabled )
                component.setNotificationEnabled(true);
            component.addPropertyChangeListener(l -> {
                if( l.getPropertyName().equals("role") )
                    buildReactionTitle();
            });

            components.add(component);
            if( simulatable )
                reactionEditPane.changeComponents();
            buildReactionTitle();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not add reaction component " + name + " as " + role, t);
        }
    }

    public static class CreateReactionException extends Exception
    {
        public CreateReactionException(String str)
        {
            super(str);
        }
    }

    /**
     * Creates reaction, if reaction can no be created returns error message
     */
    protected boolean createReaction() throws Exception
    {
        if( !isReactionValid() )
        {
            String message = resources.getString("REACTION_IS_EMPTY");
            int res = JOptionPane.showConfirmDialog(this, message, "", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if( res != JOptionPane.YES_OPTION )
                return false;
        }
        viewEditor.startTransaction("Create reaction");
        List<SpecieReference> componentsList = getComponentList();
        componentsList.stream().forEach(sr -> sr.setInitialized(true));
        if( properties != null )
        {
            properties.setReactionName(reactionName.getText());
            if( simulatable )
                properties.setKineticlaw(new KineticLaw(reactionEditPane.getFormula()));
            properties.setReactionTitle(getReactionTitle());
            properties.setSpecieReferences(componentsList);
            DiagramElementGroup group = properties.createElements(compartment, point, viewEditor);
            properties.putResults(group);
        }
        else
        {
            String formula = simulatable ? reactionEditPane.getFormula() : "";
            DiagramUtility.createReactionNode(diagram, compartment, reactionTemplate, componentsList, formula, point, null);
        }
        viewEditor.completeTransaction();
        return true;
    }

    protected boolean modifyReaction() throws Exception
    {
        if( !isReactionValid() )
        {
            String message = resources.getString("REACTION_IS_EMPTY");
            int res = JOptionPane.showConfirmDialog(this, message, "", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if( res != JOptionPane.YES_OPTION )
                return false;
        }

        Node reactionNode = Util.findNode(diagram, reactionTemplate);

        reactionTemplate.setTitle(titleField.getText());
        if( simulatable )
            reactionTemplate.setKineticLaw(new KineticLaw(reactionEditPane.getFormula()));

        List<SpecieReference> componentsList = getComponentList();

        Map<String, SpecieReference> oldComponents = new HashMap<>();
        reactionTemplate.stream().forEach(sr -> oldComponents.put(sr.getName(), sr));

        ReactionInitialProperties props = (ReactionInitialProperties)diagram.getType().getSemanticController().getPropertiesByType(diagram,
                Reaction.class, null);
        DiagramElementGroup result = new DiagramElementGroup();
        //We assume existed specie references were edited directly in interface and all corresponding fields are already changed
        Map<String, SpecieReference> specieToRole = new HashMap<>();
        viewEditor.startTransaction("Modify reaction");
        for( SpecieReference prototype : componentsList )
        {
            //check if element with the same specie and role was already processed, just increase stoichiometry in this case
            String key = prototype.getSpecie() + prototype.getRole();
            if( specieToRole.containsKey(key) )
            {
                SpecieReference ref = specieToRole.get(key);
                String stoichiometry = ref.getStoichiometry();
                if( !stoichiometry.startsWith("-") )
                    stoichiometry = "+" + stoichiometry;
                ref.setStoichiometry(prototype.getStoichiometry() + stoichiometry);
                continue;
            }

            String specieReferenceName = prototype.getName();
            Node specieNode = null;
            DiagramElement de = diagram.findDiagramElement(DiagramUtility.toDiagramPath(specieReferenceName));
            if( de == null || ! ( de instanceof Node ) )
                de = diagram.findNode(DiagramUtility.toDiagramPath(prototype.getSpecie()));
            if( de != null && de instanceof Node )
                specieNode = (Node)de;

            if( specieNode == null )
                continue;

            SpecieReference real = null;
            if( reactionTemplate.contains(specieReferenceName) )
            {
                SpecieReference old = reactionTemplate.get(specieReferenceName);

                if( prototype.equals(old) )
                    real = old;
                else //new component with same name
                {
                    reactionTemplate.remove(specieReferenceName);
                    real = prototype.clone(reactionTemplate, specieReferenceName);
                    String specieLink = ( ReactionInitialProperties.getReactionOrigin(diagram) != null )
                            ? CollectionFactory.getRelativeName(specieNode.getKernel(), Module.getModule(diagram))
                            : specieNode.getCompleteNameInDiagram();
                    real.setSpecie(specieLink);
                    reactionTemplate.put(real);
                }
                //Recreate edge
                Edge oldEdge = reactionNode.edges().findFirst(e -> e.getKernel().equals(old)).orElse(null);
                Path path = null;
                if( oldEdge != null )
                {
                    path = oldEdge.getPath();
                    try
                    {
                        oldEdge.getInput().removeEdge(oldEdge);
                        oldEdge.getOutput().removeEdge(oldEdge);
                        oldEdge.getOrigin().remove(oldEdge.getName());
                    }
                    catch( Exception e1 )
                    {
                    }
                }
                Edge edge = props.createEdge(real, reactionNode, specieNode);
                if( path != null )
                    edge.setPath(path);
                result.add(edge);
                oldComponents.remove(specieReferenceName);
            }
            else
            {
                real = prototype.clone(reactionTemplate, specieReferenceName);
                String specieLink = ( ReactionInitialProperties.getReactionOrigin(diagram) != null )
                        ? CollectionFactory.getRelativeName(specieNode.getKernel(), Module.getModule(diagram))
                        : specieNode.getCompleteNameInDiagram();
                real.setSpecie(specieLink);
                reactionTemplate.put(real);
                Edge edge = props.createEdge(real, reactionNode, specieNode);
                result.add(edge);
            }
            specieToRole.put(key, real);
        }
        result.putToCompartment();
        Set<Edge> toRemove = new HashSet<>();
        for( SpecieReference removed : oldComponents.values() )
        {
            reactionNode.edges().filter(e -> e.getKernel().equals(removed)).forEach(e -> toRemove.add(e));
            reactionTemplate.remove(removed.getName());
        }
        for( Edge oldEdge : toRemove )
        {
            oldEdge.getInput().removeEdge(oldEdge);
            oldEdge.getOutput().removeEdge(oldEdge);
            oldEdge.getOrigin().remove(oldEdge.getName());
        }
        DiagramUtility.generateRoles(diagram, reactionNode);
        viewEditor.completeTransaction();
        return true;
    }

    /** Checks whether the reaction is valid */
    protected boolean isReactionValid()
    {
        return getComponentList().stream().filter(c -> c.isReactantOrProduct()).count() > 0; // check whether reaction contains at least one reactant or product
    }

    private JPanel createReactionNamePanel()
    {
        reactionName = new JTextField(30);
        DataCollection<Reaction> module = ReactionInitialProperties.getReactionOrigin(diagram);
        if( !newReaction || module != null )
            reactionName.setEditable(false);
        else
            reactionName.setEditable(true);
        reactionName.setText(reactionTemplate.getName());
        reactionName.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                String newName = diagram.getType().getSemanticController().validateName(reactionName.getText());
                reactionName.setText(newName);
            }
        });
        JPanel reactionNamePanel = new JPanel(new BorderLayout());
        reactionNamePanel.setBorder(new EmptyBorder(new Insets(10, 0, 10, 10)));
        reactionNamePanel.add(new JLabel(resources.getString("REACTION_NAME")), BorderLayout.LINE_START);
        reactionNamePanel.add(reactionName, BorderLayout.LINE_END);

        titleField = new JTextField(30);
        titleField.setText(newReaction ? "" : reactionTemplate.getTitle());
        JPanel reactionTitlePanel = new JPanel(new BorderLayout());
        reactionTitlePanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 0)));
        reactionTitlePanel.add(new JLabel(resources.getString("REACTION_TITLE")), BorderLayout.LINE_START);
        reactionTitlePanel.add(titleField, BorderLayout.LINE_END);

        JPanel result = new JPanel(new BorderLayout());
        result.add(reactionNamePanel, BorderLayout.LINE_START);
        result.add(reactionTitlePanel, BorderLayout.LINE_END);

        result.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        return result;
    }

    private JPanel createComponentListPanel(DefaultRowModel components, TabularPropertyInspector componentsTable)
    {
        JPanel componentsPanel = new JPanel(new BorderLayout(10, 10));
        componentsPanel.setBorder(new TitledBorder(new EmptyBorder(10, 10, 10, 10), resources.getString("REACTION_COMPONENTS_PANEL")));
        Dimension size = new Dimension(600, 100);
        componentsTable.setMinimumSize(size);
        componentsTable.setPreferredSize(size);
        componentsTable.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        componentsTable.explore(components, new SpecieReference(null, "template"), PropertyInspector.SHOW_USUAL);
        componentsTable.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        componentsPanel.add(new JScrollPane(componentsTable), BorderLayout.CENTER);
        return componentsPanel;
    }

    private JPanel createEditComponentsPanel()
    {
        JLabel componentNameLabel = new JLabel(resources.getString("REACTION_COMPONENT_NAME"));
        componentName = new JTextField(30);
        componentName.setEditable(false);

        JLabel variableLabel = new JLabel(resources.getString("REACTION_VARIABLE_NAME"));
        variableName = new JTextField(30);
        variableName.setEditable(false);
        componentNameLabel.setPreferredSize(variableLabel.getPreferredSize());

        JLabel componentRoleLabel = new JLabel(resources.getString("REACTION_COMPONENT_ROLE"));
        componentRole = new JComboBox<>(new String[] {SpecieReference.REACTANT, SpecieReference.PRODUCT, SpecieReference.MODIFIER});

        addButton = new JButton(resources.getString("REACTION_ADD_COMPONENT"));
        addButton.setEnabled(false);
        removeButton = new JButton(resources.getString("REACTION_REMOVE_COMPONENT"));
        removeButton.setEnabled(false);

        JPanel componentPanel = new JPanel(new GridBagLayout());
        componentPanel.setBorder(new TitledBorder(new EmptyBorder(10, 10, 10, 10), resources.getString("REACTION_COMPONENT_PANEL")));

        JPanel componentNamePanel = new JPanel(new BorderLayout(5, 5));
        componentNamePanel.add(componentNameLabel, BorderLayout.WEST);
        componentNamePanel.add(componentName, BorderLayout.CENTER);

        JPanel componentRolePanel = new JPanel(new BorderLayout(5, 5));
        componentRolePanel.add(componentRoleLabel, BorderLayout.WEST);
        componentRolePanel.add(componentRole, BorderLayout.CENTER);

        componentPanel.add(componentNamePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        componentPanel.add(componentNameLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        componentPanel.add(componentName, new GridBagConstraints(1, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));

        componentPanel.add(componentRolePanel, new GridBagConstraints(3, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));

        componentPanel.add(addButton, new GridBagConstraints(4, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 10, 0, 0), 0, 0));

        componentPanel.add(removeButton, new GridBagConstraints(5, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 10, 0, 0), 0, 0));

        componentPanel.add(variableLabel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        componentPanel.add(variableName, new GridBagConstraints(1, 1, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));

        componentPanel.add(new JLabel(resources.getString("REACTION_HELP_MESSAGE")), new GridBagConstraints(3, 1, 3, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 10, 0, 0), 0, 0));
        return componentPanel;
    }

    /**
     * Get reaction title
     */
    public String getReactionTitle()
    {
        return titleField.getText();
    }
}
