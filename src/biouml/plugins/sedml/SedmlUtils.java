package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jlibsedml.AbstractTask;
import org.jlibsedml.AddXML;
import org.jlibsedml.Change;
import org.jlibsedml.ChangeAttribute;
import org.jlibsedml.ChangeXML;
import org.jlibsedml.ComputeChange;
import org.jlibsedml.Model;
import org.jlibsedml.RemoveXML;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import org.jlibsedml.SubTask;

import ru.biosoft.access.core.DataElement;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;

/**
 * @author lan
 *
 */
public class SedmlUtils
{

    /**
     * @param model
     * @param modelDOM
     * @throws JDOMException
     * @throws Exception
     */
    public static void applySedMlChanges(Model model, Document modelDOM) throws JDOMException, Exception
    {
        if(model.hasChanges())
        {
            for(Change change: model.getListOfChanges())
            {
                if(change instanceof ChangeAttribute)
                    applyChange((ChangeAttribute)change, modelDOM);
                else if(change instanceof RemoveXML)
                    applyChange((RemoveXML)change, modelDOM);
                else if(change instanceof ChangeXML)
                    applyChange((ChangeXML)change, modelDOM);
                else if(change instanceof AddXML)
                    applyChange((AddXML)change, modelDOM);
                else if(change instanceof ComputeChange)
                {
                    //will be processed latter in ModelBuilder
                }
                else throw new Exception("Unsupported change type "+change.getClass().getSimpleName());
            }
        }
    }
    
    private static XPath getXPath(Change change, org.jdom.Document modelDOM) throws JDOMException
    {
        XPath xPath = XPath.newInstance(change.getTargetXPath().getTargetAsString());
        // TODO: support non-sbml namespaces
        xPath.addNamespace("sbml", modelDOM.getRootElement().getNamespaceURI());
        xPath.addNamespace("cellml", modelDOM.getRootElement().getNamespaceURI());
        return xPath;
    }

    private static void applyChange(AddXML change, org.jdom.Document modelDOM) throws JDOMException
    {
        XPath xPath = getXPath(change, modelDOM);
        for(Object node: xPath.selectNodes(modelDOM))
        {
            if(node instanceof Element)
            {
                ((Element)node).addContent((Element)change.getNewXML().getXml());
            }
        }
    }

    private static void applyChange(ChangeXML change, org.jdom.Document modelDOM) throws JDOMException
    {
        XPath xPath = getXPath(change, modelDOM);
        for(Object node: xPath.selectNodes(modelDOM))
        {
            if(node instanceof Content)
            {
                int i = ((Content)node).getParent().indexOf((Content)node);
                
                List<Element> elements = change.getNewXML().getXml();
                elements = StreamEx.of( elements ).map( e->(Element)e.clone() ).toList();
                ( (((Content)node).getParentElement()) ).setContent(i, elements );
            }
        }
    }

    private static void applyChange(RemoveXML change, org.jdom.Document modelDOM) throws JDOMException
    {
        XPath xPath = getXPath(change, modelDOM);
        for(Object node: xPath.selectNodes(modelDOM))
        {
            if(node instanceof Content)
            {
                ((Content)node).getParent().removeContent(((Content)node));
            }
        }
    }

    private static void applyChange(ChangeAttribute change, org.jdom.Document modelDOM) throws JDOMException
    {
        XPath xPath = getXPath(change, modelDOM);
        for(Object node: xPath.selectNodes(modelDOM))
        {
            if(node instanceof Attribute)
            {
                ((Attribute)node).setValue(change.getNewValue());
            }
        }
    }
    
    public static String getIdFromXPath(String target)
    {
        if(target == null || target.equals( "time" ))
            return "time";
        Matcher matcher = Pattern.compile("(id|name)=[\\\'\\\"]([^\\\'\\\"]+)[\\\'\\\"]\\](/@\\w*)?$").matcher(target);
        if(matcher.find())
            return matcher.group(2);
        throw new IllegalArgumentException("Invalid xpath " + target);
    }
    
    public static String resolveVariableName(Diagram model, String target)
    {
        //TODO: support symbol attributes
        String name = getIdFromXPath( target );
        EModel emodel = model.getRole( EModel.class );
        for( ru.biosoft.access.core.DataElement var : emodel.getVariables() )
        {
            String varName = var.getName();
            String cutName = varName.substring( varName.lastIndexOf( "." ) + 1 );
            if( cutName.startsWith( "$" ) )
                cutName = cutName.substring( 1 );
            if( cutName.equals( name ) )
                return varName;
        }
        throw new RuntimeException( "Can not resolve " + name + " variable" );
    }
    
    public static String unresolveVariableName(String varName)
    {
        int idx = varName.lastIndexOf( '.' );
        if( idx != -1 )
            varName = varName.substring( idx + 1, varName.length() );
        if(varName.startsWith( "$" ))
            varName = varName.substring( 1 );
        return varName;
    }

    public static String getModelReference(RepeatedTask repeatedTask, SedML sedml)
    {
        for(SubTask subTask : repeatedTask.getSubTasks().values())
        {
            AbstractTask task = sedml.getTaskWithId( subTask.getTaskId() );
            String result;
            if(task instanceof RepeatedTask)
                result = getModelReference((RepeatedTask)task, sedml);
            else
                result = task.getModelReference();
            if(result != null && ! result.isEmpty())
                return result;
        }
        throw new IllegalArgumentException("No model reference for " + repeatedTask.getId() );
    }
    
    public static List<SubTask> getSubTasks(RepeatedTask task)
    {
        ArrayList<SubTask> subTasks = new ArrayList<>( task.getSubTasks().values() );
        //TODO: support subTask order without order attribute (in given order), jlibsedml doesn't support this
        if(subTasks.stream().allMatch( x -> x.getOrder() != null ))
            subTasks.sort( Comparator.comparingInt( x -> Integer.parseInt(x.getOrder()) ) );
        return subTasks;
    }


}
