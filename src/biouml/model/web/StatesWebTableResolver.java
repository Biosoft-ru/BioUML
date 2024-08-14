package biouml.model.web;

import java.util.List;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.standard.state.State;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.util.bean.BeanInfoEx2;

public class StatesWebTableResolver extends TableResolver
{

    public static final String TYPE_PARAMETER = "tabletype";

    protected String type;
    protected String subDiagram;
    protected String stateName;

    public StatesWebTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        this.type = arguments.getString(TYPE_PARAMETER);
        this.subDiagram = arguments.get("subDiagram");
        this.stateName = arguments.get("stateName");
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        Diagram diagram = de.cast(Diagram.class);
        if ( "states".equals(type) )
        {
            VectorDataCollection<State> result = new VectorDataCollection<>("States", State.class, null);
            diagram.states().forEach(result::put);
            return result;
        }
        else if ( "changes".equals(type) )
        {
            VectorDataCollection<UndoableEditWrapper> result = new VectorDataCollection<>("Changes", UndoableEditWrapper.class, null);
            if ( stateName != null )
            {
                State state = diagram.getState(stateName);
                if ( state != null )
                {
                    List<UndoableEdit> edits = state.getStateUndoManager().getEdits();
                    for ( int i = 0; i < edits.size(); i++ )
                    {
                        result.put(new UndoableEditWrapper(String.valueOf(i), edits.get(i)));
                    }
                }
            }
            return result;
        }
        return null;
    }

    public static class UndoableEditWrapper implements DataElement
    {
        String name;
        UndoableEdit ue;

        public UndoableEditWrapper()
        {
            this.name = "Stub";
            this.ue = new StubUndo();
        }

        public UndoableEditWrapper(String name, UndoableEdit ue)
        {
            this.name = name;
            this.ue = ue;
        }


        @PropertyName("Transaction order")
        @Override
        public String getName()
        {
            return name;
        }

        public void setName(String newName)
        {
            this.name = newName;
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @PropertyName("Transaction name")
        public String getTitle()
        {
            return ue.getPresentationName();
        }

        public void setTitle(String title)
        {
        }

    }

    public static class UndoableEditWrapperBeanInfo extends BeanInfoEx2<UndoableEditWrapper>
    {
        public UndoableEditWrapperBeanInfo()
        {
            super(UndoableEditWrapper.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            addReadOnly("name");
            add("title");
        }
    }

    public static class StubUndo implements UndoableEdit
    {

        @Override
        public void undo() throws CannotUndoException
        {
        }

        @Override
        public boolean canUndo()
        {
            return false;
        }

        @Override
        public void redo() throws CannotRedoException
        {

        }

        @Override
        public boolean canRedo()
        {
            return false;
        }

        @Override
        public void die()
        {

        }

        @Override
        public boolean addEdit(UndoableEdit anEdit)
        {
            return false;
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit)
        {
            return false;
        }

        @Override
        public boolean isSignificant()
        {
            return false;
        }

        @Override
        public String getPresentationName()
        {
            return "stub name";
        }

        @Override
        public String getUndoPresentationName()
        {
            return "stub undo";
        }

        @Override
        public String getRedoPresentationName()
        {
            return "stub redo";
        }

    }
}
