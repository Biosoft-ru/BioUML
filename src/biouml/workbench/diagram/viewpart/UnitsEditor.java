package biouml.workbench.diagram.viewpart;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Unit;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;

public class UnitsEditor extends EditorPartSupport implements PropertyChangeListener
{
    protected EModel executableModel;
    private final PropertyInspectorEx inspector = new PropertyInspectorEx();
    
    public UnitsEditor(Object model)
    {
        explore( model, null );
    }
    
    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ((Diagram)model).getRole() instanceof EModel;
    }

    @Override
    public JComponent getView()
    {
        return inspector;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( executableModel != null )
            executableModel.removePropertyChangeListener(this);

        executableModel = ( (Diagram)model ).getRole(EModel.class);
        executableModel.addPropertyChangeListener(this);

        try
        {
            Units units = new Units(executableModel);
            inspector.explore(units);
        }
        catch( Exception e )
        {
            System.out.println("Can not explore units for diagram " + model + ", error: " + e);
        }
    }

    public static class Units extends Option
    {
        EModel emodel;
        public Units(EModel emodel)
        {
            this.emodel = emodel;
        }

        @PropertyName("Model units")
        public Unit[] getUnits()
        {
            Map<String, Unit> modelUnits = emodel.getUnits();
            return modelUnits.values().toArray(new Unit[modelUnits.size()]);
        }

        public void setUnits(Unit[] units)
        {
            Unit[] oldValue = getUnits();
            Set<String> oldNames = new HashSet<>(emodel.getUnits().keySet());
            for (Unit unit: units)
            {
                emodel.addUnit(unit);
                oldNames.remove(unit.getName());
            }

            for (String oldName: oldNames)
                emodel.removeUnit(oldName);

            this.firePropertyChange("units", oldValue, units);
        }
        
        public String calcUnitName(Integer index, Object unit)
        {
            return ( (Unit)unit ).getTitle();
        }
    }

    public static class UnitsBeanInfo extends BeanInfoEx
    {
        public UnitsBeanInfo()
        {
            super( Units.class, true );
            this.setSubstituteByChild(true);            
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("units", beanClass);
            pde.setChildDisplayName(beanClass.getMethod("calcUnitName", new Class[] {Integer.class, Object.class}));
            add(pde);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals("units") )
        {
            Units units = new Units(executableModel);
            inspector.explore(units);
        }
    }
}