package biouml.plugins.antimony.web;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.Antimony;
import biouml.plugins.antimony.AntimonyConstants;
import biouml.plugins.antimony.AntimonyDiagramListener;
import biouml.plugins.antimony.AntimonyUtility;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebProviderSupport;

public class AntimonyWebProvider extends WebProviderSupport
{
    private static final String GET_TEXT = "getText";
    private static final String UPDATE_TEXT = "updateText";
    private static final String CAN_EXPLORE = "canExplore";
    private static final String SET_AUTO_MODE = "autoMode";
    private static final String SET_MANUAL_MODE = "manualMode";

    @Override
    public void process(BiosoftWebRequest req, BiosoftWebResponse resp) throws Exception
    {
        String action = req.getAction();
        JSONResponse response = new JSONResponse(resp);
        if( CAN_EXPLORE.equals(action) )
        {
            String pathStr = req.get("diagram");
            boolean result = AntimonyUtility.checkDiagramType(WebDiagramsProvider.getDiagram(pathStr, false));
            response.sendString(result + "");
        }
        else if( GET_TEXT.equals(action) )
        {
            String pathStr = req.get("diagram");
            Diagram diagram = WebDiagramsProvider.getDiagram(pathStr, false);
            String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            String text;
            //If attribute is not null, consider mode as manual and init diagram listener
            if( antimonyText != null )
            {
                text = antimonyText;
                Antimony antimony = new Antimony(diagram);
                Diagram newDiagram = antimony.generateDiagram(text, true);
                applyAntimony(diagram, newDiagram);
                Object listenerObj = diagram.getAttributes().getValue(AntimonyDiagramListener.LISTENER_ATTR);
                AntimonyDiagramListener listener;
                if( listenerObj == null || ! ( listenerObj instanceof AntimonyDiagramListener ) )
                {
                    listener = new AntimonyDiagramListener();
                    listener.register(diagram);
                }
                else
                    listener = (AntimonyDiagramListener)listenerObj;
                listener.setAntimony(antimony);
            }
            else
            {
                Antimony antimony = new Antimony(diagram);
                antimony.createAst();
                text = antimony.generateText();
            }
            if( text != null )
                response.sendString(text);
            else
                response.error("Can not generate antimony text from given diagram");
        }
        else if( SET_MANUAL_MODE.equals(action) )
        {
            String pathStr = req.get("diagram");
            String text = req.get("text");
            Diagram diagram = WebDiagramsProvider.getDiagram(pathStr, false);
            //Add listener and store text to diagram attribute
            StringBuffer errorLog = new StringBuffer();
            Writer writer = new Writer()
            {
                StringBuffer buffer = new StringBuffer();

                @Override
                public void close() throws IOException
                {
                }

                @Override
                public void flush() throws IOException
                {
                    errorLog.append(buffer.toString());
                    buffer = new StringBuffer();
                }

                @Override
                public void write(char[] bytes, int offset, int len) throws IOException
                {
                    buffer.append(bytes, offset, len);
                }
            };
            Handler webLogHandler = new WriterHandler(writer, new PatternFormatter("%4$s - %5$s%n"));
            webLogHandler.setLevel(Level.INFO);

            WebDiagramsProvider.performTransaction(diagram, "Apply antimony", () -> {
                try
                {
                    diagram.getAttributes().remove(AntimonyConstants.ANTIMONY_TEXT_ATTR);
                    //check that text is correct
                    Antimony antimony = new Antimony(diagram);
                    antimony.setLogHandler(webLogHandler);
                    //TODO: rework antimony logging and exceptions throwing
                    Diagram newDiagram = antimony.generateDiagram(text, true);
                    applyAntimony(diagram, newDiagram);
                    AntimonyUtility.setAntimonyAttribute(diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR);
                    Object listenerObj = diagram.getAttributes().getValue(AntimonyDiagramListener.LISTENER_ATTR);
                    AntimonyDiagramListener listener;
                    if( listenerObj == null || ! ( listenerObj instanceof AntimonyDiagramListener ) )
                    {
                        listener = new AntimonyDiagramListener();
                        listener.register(diagram);
                    }
                    else
                        listener = (AntimonyDiagramListener)listenerObj;
                    listener.setAntimony(antimony);
                }
                catch( Exception e )
                {
                    if( errorLog.length() == 0 )
                        errorLog.append("Unknown error");
                }

            });
            if( errorLog.length() > 0 )
                response.error("Text is incorrect: " + errorLog.toString());
            else
                response.sendString("ok");

        }
        else if( SET_AUTO_MODE.equals(action) )
        {
            String pathStr = req.get("diagram");
            Diagram diagram = WebDiagramsProvider.getDiagram(pathStr, false);
            //remove listener and antimony property
            Object listenerObj = diagram.getAttributes().getValue(AntimonyDiagramListener.LISTENER_ATTR);
            if( listenerObj != null && ( listenerObj instanceof AntimonyDiagramListener ) )
            {
                AntimonyDiagramListener listener = (AntimonyDiagramListener)listenerObj;
                listener.release(diagram);
            }
            diagram.getAttributes().remove(AntimonyConstants.ANTIMONY_TEXT_ATTR);

            Antimony antimony = new Antimony(diagram);
            antimony.createAst();
            String text = antimony.generateText();
            if( text != null )
                response.sendString(text);
            else
                response.error("Can not generate antimony text from given diagram");
        }
        else if( UPDATE_TEXT.equals(action) )
        {
            String pathStr = req.get("diagram");
            Diagram diagram = WebDiagramsProvider.getDiagram(pathStr, false);
            //If no antimony listener is set, auto mode is on, generate antimony each time from diagram
            //If listener is set, it will store updated antimony in diagram property
            String text = "";
            if( diagram.getAttributes().getProperty(AntimonyDiagramListener.LISTENER_ATTR) == null )
            {
                diagram.getAttributes().remove(AntimonyConstants.ANTIMONY_TEXT_ATTR);
                Antimony antimony = new Antimony(diagram);
                antimony.createAst();
                text = antimony.generateText();
            }
            else
            {
                text = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            }
            if( text != null )
                response.sendString(text);
            else
                response.error("Can not generate antimony text from given diagram");
        }
        else
        {
            response.error("Unexpected antimony action '" + action + "'.");
        }
    }

    public static void applyAntimony(Diagram diagram, Diagram changedDiagram) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        EModel changedEmodel = changedDiagram.getRole(EModel.class);
        DataCollection<Variable> variables = emodel.getVariables();

        boolean notificationEnabled = diagram.isNotificationEnabled();
        boolean propagationEnabled = diagram.isPropagationEnabled();
        boolean emodelNotificationEnabled = variables.isNotificationEnabled();
        boolean emodelPropagationEnabled = variables.isPropagationEnabled();

        diagram.setNotificationEnabled(false);
        diagram.setPropagationEnabled(false);
        variables.setNotificationEnabled(false);
        variables.setPropagationEnabled(false);

        for( DiagramElement de : diagram.recursiveStream().remove(de -> de instanceof Diagram).toSet() )
            diagram.remove(de.getName());

        for( Variable v : variables.stream().filter(v -> ! ( v instanceof VariableRole )).collect(Collectors.toSet()) )
            variables.remove(v.getName());


        for( Variable v : changedEmodel.getVariables().stream().filter(v -> ! ( v instanceof VariableRole )).collect(Collectors.toSet()) )
        {
            Variable var = new Variable(v.getName(), emodel, variables);
            var.setComment(v.getComment());
            var.setConstant(v.isConstant());
            var.setInitialValue(v.getInitialValue());
            var.setUnits(v.getUnits());
            for( DynamicProperty dp : v.getAttributes() )
                var.getAttributes().add(new DynamicProperty(dp.getName(), dp.getType(), dp.getValue()));

            emodel.put(var);
        }

        changedDiagram.recursiveStream().remove(de -> de instanceof Diagram).select(Node.class)
                .forEachOrdered(n -> copyDiagramElement(diagram, n));

        //Dirty copying of new variables since notifications are off
        //TODO: refactor
        List<VariableRole> missedVariables = new ArrayList<>();
        for( Variable v : changedEmodel.getVariables().stream().collect(Collectors.toSet()) )
        {
            if( v instanceof VariableRole )
            {
                Variable oldVar = variables.get(v.getName());
                DiagramElement changedDe = ( (VariableRole)v ).getDiagramElement();
                Compartment parent = (Compartment)diagram.findNode(changedDe.getCompartment().getName());
                if( parent == null )
                    parent = diagram;
                DiagramElement de = parent.get(changedDe.getName());
                if( oldVar == null )
                {
                    if( de != null && de.getRole() instanceof VariableRole )
                    {
                        VariableRole newVar = (VariableRole)de.getRole();
                        missedVariables.add(newVar);
                    }
                }
                else
                {
                    //TODO: change variable
                }
            }
        }

        changedDiagram.recursiveStream().select(Edge.class).forEach(e -> copyDiagramElement(diagram, e));

        variables.setPropagationEnabled(emodelPropagationEnabled);
        variables.setNotificationEnabled(emodelNotificationEnabled);
        for( VariableRole missed : missedVariables )
        {
            variables.put(missed);
        }
        emodel.removeNotUsedParameters();
        diagram.setPropagationEnabled(propagationEnabled);
        diagram.setNotificationEnabled(notificationEnabled);
    }

    private static void copyDiagramElement(Diagram diagram, DiagramElement de)
    {
        Compartment parent = (Compartment)diagram.findNode(de.getCompartment().getName());
        if( parent == null )
            parent = diagram;
        parent.put(de.clone(parent, de.getName()));
    }

}
