package biouml.workbench.diagram;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import biouml.model.Diagram;

public abstract class AbstractDiagramTextRepresentation implements DiagramTextRepresentation
{
    protected final Diagram diagram;
    private boolean updating = false;
    
    public AbstractDiagramTextRepresentation(Diagram diagram)
    {
        this.diagram = diagram;
    }

    @Override
    public void setContent(String text)
    {
        updating = true;
        try
        {
            doSetContent(text);
        }
        finally
        {
            updating = false;
        }
    }

    protected abstract void doSetContent(String text);

    @Override
    public void addTextChangeListener(PropertyChangeListener l)
    {
        DiagramChangeListener listener = new DiagramChangeListener( l );
        diagram.addDataCollectionListener( listener );
        diagram.addPropertyChangeListener( listener );
    }

    private class DiagramChangeListener implements PropertyChangeListener, DataCollectionListener
    {
        private final WeakReference<PropertyChangeListener> delegateRef;
        private String text;
        
        public DiagramChangeListener(PropertyChangeListener delegate)
        {
            this.delegateRef = new WeakReference<>( delegate );
            this.text = getContent();
        }

        @Override
        public void elementAdded(DataCollectionEvent e)
        {
            handleTextChange();
        }

        @Override
        public void elementWillAdd(DataCollectionEvent e)
        {
        }

        @Override
        public void elementChanged(DataCollectionEvent e)
        {
            handleTextChange();
        }

        @Override
        public void elementWillChange(DataCollectionEvent e)
        {
        }

        @Override
        public void elementRemoved(DataCollectionEvent e)
        {
            handleTextChange();
        }

        @Override
        public void elementWillRemove(DataCollectionEvent e)
        {
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            handleTextChange();
        }

        private void handleTextChange()
        {
            PropertyChangeListener delegate = delegateRef.get();
            if(delegate == null)
            {
                diagram.removeDataCollectionListener( this );
                diagram.removePropertyChangeListener( this );
                return;
            }
            if(updating)
            {
                return;
            }
            String newText = getContent();
            if(!text.equals( newText ))
            {
                PropertyChangeEvent pce = new PropertyChangeEvent( diagram, "text", text, newText );
                this.text = newText;
                delegate.propertyChange( pce );
            }
        }
    }
}
