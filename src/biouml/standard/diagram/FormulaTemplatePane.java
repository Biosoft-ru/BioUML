package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.swing.table.DefaultRowModel;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.type.SpecieReference;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.math.Expression;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Parser;
import ru.biosoft.math.model.Utils;

@SuppressWarnings ( "serial" )
public class FormulaTemplatePane extends JPanel
{
    public static final String SBO_PATH = "databases/SBO";
    public static final String SBO_RATE_LAW = "SBO:0000001";

    public static final String PANE_NAME = "SBO template";

    public static final String NAME = "Name";
    public static final String VALUE = "Value";
    public static final String COMMENT = "Comment";
    public static final String[] COLUMN_NAMES = new String[] {NAME, VALUE, COMMENT};

    public static final String REACTANT_CONCENTRATION ="concentration of reactant";
    public static final String SUBSTRATE_CONCENTRATION ="concentration of substrate";
    public static final String PRODUCT_CONCENTRATION ="concentration of product";
    public static final String ENZYME_CONCENTRATION ="concentration of enzyme";
    public static final String MODIFIER_CONCENTRATION ="concentration of modifier";
    public static final String ACTIVATOR_CONCENTRATION ="concentration of activator";

    //for old version of SBO
    public static final String ENZYME ="enzyme";
    public static final String MODIFIER ="modifier";
    public static final String REACTANT = "reactant";
    public static final String SUBSTRATE ="substrate";
    public static final String PRODUCT ="product";

    public static final Set<String> SBO_REACTANTS = StreamEx.of( REACTANT_CONCENTRATION, SUBSTRATE_CONCENTRATION, REACTANT, PRODUCT ).toSet();
    public static final Set<String> SBO_PRODUCTS = StreamEx.of( PRODUCT_CONCENTRATION, PRODUCT, SUBSTRATE ).toSet();
    public static final Set<String> SBO_MODIFIERS = StreamEx.of( ENZYME_CONCENTRATION, MODIFIER_CONCENTRATION, ACTIVATOR_CONCENTRATION, ENZYME, MODIFIER ).toSet();

    private JComboBox<String> formulas = new JComboBox<>();
    private JTable paramsTable = new JTable();
    private JTextField targetText;
    private JEditorPane description = new JEditorPane();
    private List<VariableDescription> currentParams = null;
    private List<FunctionDescription> functionDescriptionList;
    private Expression expression = new Expression(null, "0");

    private DefaultRowModel reactionComponents;
    private static final LinearFormatter linearFormatter = new LinearFormatter();
    private static final Parser mathMLParser = new ru.biosoft.math.xml.MathMLParser();
    private Diagram diagram;

    public FormulaTemplatePane(JTextField targetText, DefaultRowModel reactionComponents, Diagram diagram)
    {
        super(new BorderLayout());
        this.targetText = targetText;
        this.reactionComponents = reactionComponents;
        this.diagram = diagram;

        initMathList();

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.add(formulas, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(5, 0, 0, 0), 0, 0));

        formulas.addItemListener(e -> selectFunctionAction());

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        paramsTable.setModel(new ParamsTableModel());
        paramsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        paramsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        paramsTable.getColumnModel().getColumn(2).setPreferredWidth(600);
        paramsTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(paramsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(0, 100));
        bottomPanel.add(scrollPane, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        description.setEditable(false);
        JScrollPane scrollPane2 = new JScrollPane(description);
        scrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane2.setPreferredSize(new Dimension(0, 90));
        bottomPanel.add(scrollPane2, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);

        selectFunctionAction();
    }

    DataCollection<?> sboCollection;
    protected void initMathList()
    {
        List<FormulaDescription> mathList = null;
        //        DataCollection<?> sboCollection = null;
        try
        {
            sboCollection = DataElementPath.create(SBO_PATH).getDataElement(Module.class).getPrimaryCollection();
            Method m = sboCollection.getClass().getMethod("getFormulas", String.class);
            mathList = (List<FormulaDescription>)m.invoke(sboCollection, SBO_RATE_LAW);
        }
        catch( Exception e )
        {
            mathList = new ArrayList<>();
        }

        functionDescriptionList = new ArrayList<>();
        for(FormulaDescription formula : mathList)
        {
            FunctionDescription fd = createFunctionDescription( formula );

            if( fd == null || fd.math.contains( "selector" ) )
                continue;

            if( !functionDescriptionList.contains( fd ) )
                functionDescriptionList.add(fd);
        }
    }

    private String formatMath(FunctionDescription fd, DataCollection<?> ontology)
    {
        if( fd.functionsString != null )
            return fd.functionsString;
        String math = fd.math;
        mathMLParser.setDeclareUndefinedVariables( false );
        mathMLParser.setContext( expression.getParserContext() );
        int status = mathMLParser.parse( math );

        if( ( status & Parser.STATUS_FATAL_ERROR ) == 0 )
            expression.setAstStart( mathMLParser.getStartNode() );

        String function = linearFormatter.format( expression.getAstStart() )[1];
        function = function.substring( function.indexOf( "=" ) + 1 ).trim();

        function = function.replace( "(", "" ).replace( ")", "" ); //zeroth order kinetic laws are strange: "k()" and "c()"
        fd.setFunctionsString( function );

        Node node = expression.getAstStart().jjtGetChild( 0 );
        if( node instanceof AstFunctionDeclaration )
            fd.setVariables( getFunctionParameters( (AstFunctionDeclaration)node, ontology ) );

        return function;
    }

    protected FunctionDescription createFunctionDescription(FormulaDescription formula)
    {
        FunctionDescription fd = new FunctionDescription();
        try
        {
            
            fd.math = formula.getMath();
            //            fd.setFunctionsString(function);
            fd.setName(formula.getName());
            fd.setDescription(formula.getDescription());
        }
        catch( Exception e )
        {
            return null;
        }

        return fd;
    }

    protected List<VariableDescription> getFunctionParameters(AstFunctionDeclaration function, DataCollection<?> ontology)
    {
        return function.getParameters().map(v -> new VariableDescription(v.getName(), v.getName(), getVariableDescription(v, ontology))).toList();
    }

    protected String getVariableDescription(AstVarNode var, DataCollection<?> ontology)
    {
        String description = var.getDefinitionUrl();
        if( ontology != null && description.contains("#") )
        {
            description = description.substring(description.indexOf("#") + 1);
            try
            {
                Method m = ontology.getClass().getMethod("getTermDescription", new Class[] {String.class});
                description = (String)m.invoke(ontology, new Object[] {description});
            }
            catch( Exception e )
            {
            }
        }
        return description.trim();
    }

    protected void refreshTable()
    {
        paramsTable.setModel(new ParamsTableModel());
        paramsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        paramsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        paramsTable.getColumnModel().getColumn(2).setPreferredWidth(600);
    }

    protected void applyAction()
    {
        if( currentParams == null )
            return;

        String functionName = (String)formulas.getSelectedItem();
        StreamEx.of(functionDescriptionList).findFirst(fd -> functionName.equals(fd.getName())).ifPresent(fd -> {
            String functionString = formatMath( fd, sboCollection );//.getFunctionsString();           
            targetText.setText( substituteVars(functionString, currentParams) );
        } );
    }
    
    private static String substituteVars(String formula, List<VariableDescription> vars)
    {       
        ru.biosoft.math.parser.Parser parser = new  ru.biosoft.math.parser.Parser();
        parser.parse( formula );
        AstStart node = parser.getStartNode();
        Map<String, String> substitution = new HashMap<>();
        for( VariableDescription var : vars )
            substitution.put( var.getName(), var.getValue() );
        node = Utils.substituteVars( node, substitution );      
        return new LinearFormatter().format( node )[1];
    }

    public void selectFunctionAction()
    {
        String targetString = (String)formulas.getSelectedItem();
        StreamEx.of(functionDescriptionList).findFirst( fd -> fd.getName().equals( targetString ) )
            .ifPresent( fd -> {
                    formatMath( fd, sboCollection );
                refreshVariablesValues(fd);
                currentParams = fd.getVariables();
                description.setText(fd.getDescription());
                refreshTable();
                applyAction();
            });
    }

    public void changeComponents()
    {
        Map<String, Integer> roleCounts = IntStreamEx.range( reactionComponents.size() ).mapToObj( reactionComponents::getBean )
                .select( SpecieReference.class ).map( SpecieReference::getRole )
                .groupingBy( Function.identity(), Collectors.reducing( 0, e -> 1, Integer::sum ) );
        int reactant = roleCounts.getOrDefault( SpecieReference.REACTANT, 0 );
        int modifier = roleCounts.getOrDefault( SpecieReference.MODIFIER, 0 );
        int product = roleCounts.getOrDefault(SpecieReference.PRODUCT, 0);

        formulas.removeAllItems();
        List<FunctionDescription> allowed = StreamEx.of(functionDescriptionList)
                .filter(fd -> checkRoleCount(fd, reactant, modifier, product)).toList();
        StreamEx.of(allowed).map(d -> d.getName()).sorted().forEach(formulas::addItem);
        //        StreamEx.of(allowed).sorted(Comparator.comparingInt(fd -> fd.getFunctionsString().length())).findFirst()
        //                .ifPresent(d -> formulas.setSelectedItem(d.getName()));
        StreamEx.of( allowed ).findFirst().ifPresent( d -> formulas.setSelectedItem( d.getName() ) );
    }

    public void refreshVariablesValues(FunctionDescription fd)
    {
        formatMath( fd, sboCollection );
        List<VariableDescription> vars = new ArrayList<>(fd.getVariables());
        for( int i = 0; i < reactionComponents.size(); i++ )
        {
            SpecieReference component = (SpecieReference)reactionComponents.getBean(i);
            String role = component.getRole();

            VariableDescription var = vars.stream().filter(v -> checkRole(v.getDescription(), role)).findAny().orElse(null);
            if( var == null )
                continue;
            vars.remove(var);
            var.setValue(component.getSpecieVariable(diagram));
        }
    }

    private boolean checkRole(String description, String role)
    {
        if( description == null )
            return false;
        if( SBO_REACTANTS.contains(description) && role.equals(SpecieReference.REACTANT) )
            return true;
        else if( SBO_MODIFIERS.contains(description) && role.equals(SpecieReference.MODIFIER) )
            return true;
        else if( SBO_PRODUCTS.contains(description) )
            return true;
        return false;
    }

    private boolean checkRoleCount(FunctionDescription fd, int reactant, int modifier, int product)
    {
        formatMath( fd, sboCollection );
        boolean hasProduct = false;
        for( VariableDescription var : fd.getVariables() )
        {
            String description = var.getDescription();
            if( SBO_REACTANTS.contains(description))
            {
                reactant--;
            }
            else if( SBO_MODIFIERS.contains(description))
            {
                modifier--;
            }
            else if( SBO_PRODUCTS.contains(description))
            {
                product--;
                hasProduct = true;
            }
        }
        return ( ( reactant == 0 ) && ( modifier == 0 ) && ( ( product == 0 ) || ( !hasProduct ) ) );
    }

    public static class FunctionDescription
    {
        private String functionsString;
        private String name;
        private List<VariableDescription> variables;
        private String description;
        public String math;

        public String getFunctionsString()
        {
            return functionsString;
        }
        public void setFunctionsString(String functionsString)
        {
            this.functionsString = functionsString;
        }

        public List<VariableDescription> getVariables()
        {
            if( variables == null )
                variables = new ArrayList<>();
            return variables;
        }
        public void setVariables(List<VariableDescription> variables)
        {
            this.variables = variables;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof FunctionDescription &&  ( (FunctionDescription)obj ).getName().equals(name);
        }

        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }

        public String getDescription()
        {
            return description;
        }
        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class VariableDescription
    {
        private String name;
        private String value;
        private String description;

        public VariableDescription(String name, String value, String description)
        {
            this.name = name;
            this.value = value;
            this.description = description;
        }
        public String getDescription()
        {
            return description;
        }
        public void setDescription(String description)
        {
            this.description = description;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getValue()
        {
            return value;
        }
        public void setValue(String value)
        {
            this.value = value;
        }
    }

    private class ParamsTableModel extends AbstractTableModel
    {
        @Override
        public int getColumnCount()
        {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount()
        {
            return currentParams != null ? currentParams.size() : 0;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            if( currentParams == null )
                return "";

            switch( col )
            {
                case 0:
                    return currentParams.get(row).getName();
                case 1:
                    return currentParams.get(row).getValue();
                case 2:
                    return currentParams.get(row).getDescription();
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            if( isCellEditable(row, col) )
                currentParams.get(row).setValue((String)value);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == 1;
        }

        @Override
        public String getColumnName(int col)
        {
            return col > 0 && col < COLUMN_NAMES.length? COLUMN_NAMES[col]: "";
        }

    }
}
