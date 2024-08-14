package biouml.plugins.research.web;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.eclipsesource.json.Json;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramTypeSupport;
import biouml.model.Module;
import biouml.plugins.research.BaseResearchSemanticController;
import biouml.plugins.research.MessageBundle;
import biouml.plugins.research.ResearchModuleType;
import biouml.plugins.research.research.ResearchDiagramType;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.server.access.AccessProtocol;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.ViewEditorPaneStub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalProperties;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.tasks.TaskInfo;

/**
 * @author lan
 *
 */
public class JournalProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();

        if( "init".equals(action) )
        {
            JSONObject json = new JSONObject();
            String[] names = JournalRegistry.getJournalNames();
            if( names != null )
            {
                JSONArray namesJson = new JSONArray(names);
                json.put("names", namesJson);
                String journalName = JournalProperties.getCurrentJournal();
                if( journalName != null )
                {
                    json.put("current", journalName);
                    WebServicesServlet.getSessionCache().addObject( JournalRegistry.JOURNAL_NAME_KEY, journalName, true );
                }
            }
            response.sendJSON(json);
        }
        else if( "set".equals(action) )
        {
            String journalName = arguments.getString(JournalRegistry.JOURNAL_NAME_KEY);
            if( journalName.equals("-") )
            {
                WebServicesServlet.getSessionCache().addObject( JournalRegistry.JOURNAL_NAME_KEY, "", true );
            }
            else
            {
                JournalProperties.setCurrentJournal(journalName);
                WebServicesServlet.getSessionCache().addObject(JournalRegistry.JOURNAL_NAME_KEY, journalName, true);
            }
            response.send(new byte[0], 0);
        }
        else if( "remove".equals(action) )
        {
            DataElementPath dePath = arguments.getDataElementPath();
            String[] rows = arguments.optStrings("jsonrows");
            removeElementsFromJournal(dePath, rows, response);
        }
        else if( "add".equals(action) )
        {
            Diagram diagram = arguments.getDataElement(Diagram.class);
            String[] jsonRows = arguments.getStrings("jsonrows");
            Point pt = arguments.getPoint();
            String type = arguments.get("type");
            if( type != null && type.equals("json") )
            {
                addElementsToDiagram(diagram, jsonRows, pt, response, "json");
            }
            else
            {
                addElementsToDiagram(diagram, jsonRows, pt, response, null);
            }
        }
        else if( "module".equals(action) )
        {
            DataElementPath dePath = arguments.getDataElementPath();
            Journal journal = getJournal(dePath);
            if( journal != null )
            {
                response.send(new byte[0], 0);
            }
            else
            {
                response.error("Can not find journal");
            }
        }
        else if( "create".equals(action) )
        {
            DataCollection dc = arguments.getDataCollection("dc");
            String name = arguments.getString(AccessProtocol.KEY_DE);
            String type = arguments.getString("type");
            createDiagram(dc, name, type, response);
        }
        else if( "completed".equals( action ) )
        {
            Journal journal = JournalRegistry.getCurrentJournal();
            if( journal == null )
            {
                response.error( "No journal selected" );
            }
            else
            {
                DataElementPath source = arguments.getDataElementPath();
                boolean completed = StreamEx.of( journal.iterator() )
                        .filter( t->source.equals( t.getSource() ) )
                        .findAny( t->t.getEndTime() > 0 ).isPresent();
                response.sendJSON( Json.value( completed ) );
            }
        }
        else
        {
            response.error("Unknown action: " + action);
        }
    }

    public static void createDiagram(DataCollection parent, String diagramName, String type, JSONResponse response) throws Exception
    {
        DiagramTypeSupport dType = null;
        if(type.equals("workflow"))
        {
            dType = new WorkflowDiagramType();
        }
        else if(type.equals("research"))
        {
            dType = new ResearchDiagramType();
        }
        else
        {
            throw new IllegalArgumentException("Unknown diagram type");
        }
        Diagram diagram = dType.createDiagram(parent, diagramName, null);
        diagram.getInfo().setNodeImageLocation(MessageBundle.class, "resources/workflow.gif");
        parent.put(diagram);
        response.send(new byte[0], 0);
    }

    protected static void removeAllFromJournal(Journal journal)
    {
        Iterator<TaskInfo> iter = journal.iterator();
        List<TaskInfo> toRemove = new ArrayList<>();
        while( iter.hasNext() )
        {
            TaskInfo action = iter.next();
            toRemove.add(action);
        }
        iter = toRemove.iterator();
        while( iter.hasNext() )
        {
            TaskInfo action = iter.next();
            journal.removeAction(action);
        }
    }

    protected static void addElementsToDiagram(final Diagram diagram, String[] rows, final Point location, JSONResponse response,
            String responseType) throws Exception
    {
        Journal journal = getJournal(diagram.getCompletePath());
        if( journal != null )
        {
            Set<String> names = new HashSet<>();
            for( String row : rows )
            {
                names.add(DataElementPath.create(row).getName());
            }
            final List<TaskInfo> addList = new ArrayList<>();
            for( TaskInfo action : journal  )
            {
                if( names.contains(action.getName()) )
                {
                    addList.add(action);
                }
            }
            final DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
            //init stub for view editor pane
            final ViewEditorPane viewEditor = new ViewEditorPaneStub(helper, diagram);
            final List<DiagramElement> result = new ArrayList<>();
            WebDiagramsProvider.performTransaction(diagram, "Add elements from history", () -> {
                try
                {
                    Compartment parent = (Compartment)helper.getOrigin(location);
                    if( parent == null )
                        parent = diagram;
                    result.addAll( Arrays.asList( ( (BaseResearchSemanticController)diagram.getType().getSemanticController() )
                            .addTaskInfoItemsToDiagram( parent, addList.iterator(), location, viewEditor ) ) );
                }
                catch( Exception e )
                {
                    throw new RuntimeException("Unable to add elements to diagram: ", e);
                }
            });
            WebDiagramsProvider.sendDiagramChanges(diagram, response, responseType, result);
        }
    }

    protected static void removeElementsFromJournal(DataElementPath dePath, String[] rows, JSONResponse response) throws Exception
    {
        Journal journal = getJournal(dePath);
        if( journal != null )
        {
            if( rows == null )
            {
                removeAllFromJournal(journal);
            }
            else
            {
                removeFromJournal(journal, rows);
            }
            response.send(new byte[0], 0);
        }
    }

    protected static Journal getJournal(DataElementPath dePath) throws Exception
    {
        DataElement de = dePath.getDataElement();
        Module module = Module.optModule(de);
        if( ( module == null ) || ! ( module.getType() instanceof ResearchModuleType ) )
        {
            throw new Exception("Module not found for data element de=" + dePath);
        }
        return ( (ResearchModuleType)module.getType() ).getResearchJournal(module);
    }

    protected static void removeFromJournal(Journal journal, String[] rows) throws Exception
    {
        Set<String> names = new HashSet<>();
        for( String row : rows )
        {
            names.add(DataElementPath.create(row).getName());
        }
        List<TaskInfo> toRemove = new ArrayList<>();
        for(TaskInfo action : journal)
        {
            if( names.contains(action.getName()) )
            {
                toRemove.add(action);
            }
        }
        for(TaskInfo action: toRemove)
        {
            journal.removeAction(action);
        }
    }
}
