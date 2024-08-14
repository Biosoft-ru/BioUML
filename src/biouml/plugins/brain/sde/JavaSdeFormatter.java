package biouml.plugins.brain.sde;

import java.util.Map;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.JavaFormatter;
import ru.biosoft.math.model.Node;

public class JavaSdeFormatter extends JavaFormatter 
{
    private final String STOCHASTIC_FUNCTION_NAME = SdeEModel.STOCHASTIC_FUNCTION_NAME;
    
    protected Map<String, Integer> stochasticIndexes;
    protected Map<String, Integer> nameStochasticIndexes;
    public JavaSdeFormatter(Map<String, Integer> historicalIndexes, Map<String, Integer> stochasticIndexes, Map<String, Integer> nameStochasticIndexes)
    {
        super(historicalIndexes);
        this.stochasticIndexes = stochasticIndexes;
        this.nameStochasticIndexes = nameStochasticIndexes;
    }
    
    @Override
    protected String getFunctionName(String name)
    {
        // Override of standard stochastic functions to fix the seed
        switch (name)
        {
            case "normal":
                return "normal";
            case "uniform":
                return "uniform";
            case "possin":
                return "poisson";
            case "exponential":
                return "exponential";
            default:
                return super.getFunctionName(name);
        }
    }
    
    @Override
    protected void processFunction(AstFunNode node)
    {
    	Function function = node.getFunction();
        String functionName = function.getName();
        Node first = (node.jjtGetNumChildren() > 0) ? node.jjtGetChild(0) : null;
        Node second = (node.jjtGetNumChildren() > 1) ? node.jjtGetChild(1) : null;
        Node third = (node.jjtGetNumChildren() > 2) ? node.jjtGetChild(2) : null;
        Node fourth = (node.jjtGetNumChildren() > 3) ? node.jjtGetChild(3) : null;
        
        if (function.getPriority() == Function.FUNCTION_PRIORITY) 
        {
            /**
             * After StochasticPreprocessor has converted expressions "x = stochastic(distrType, loc, scale)" 
             * into two "x = AUX_stochastic_i" and "AUX_stochastic_i = stochastic4(distrType, loc, scale, AUX_stochastic_i)",
             * JavaSdeFormatter additionally transforms
             * "stochastic4(distrType, loc, scale, AUX_stochastic_i)" into "stochastic(auxVarIndex, distrType, loc, scale, time)" 
             */
            if ((STOCHASTIC_FUNCTION_NAME + "4").equals(functionName))
            {
                if (nameStochasticIndexes != null)
                {                    
                	Integer index = nameStochasticIndexes.get(((AstConstant)fourth).getValue());
                	if (index != null) //TODO: check why this can be null
                	{
                		append(STOCHASTIC_FUNCTION_NAME + "(" + index + ", ");
                		processNode(first);
                		append(", ");
                		processNode(second);
                		append(", ");
                		processNode(third);
                		//append(")");
                		append(", time)");
                	}
                }
            }
            else 
            {
            	super.processFunction(node);
            }
            return;
        }
        
        super.processFunction(node);
    }

}
