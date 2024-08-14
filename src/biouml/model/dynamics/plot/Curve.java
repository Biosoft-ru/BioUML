package biouml.model.dynamics.plot;

import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.EModel;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.Pen;

public class Curve extends PlotVariable implements DataElement
{
    public static final String TYPE_VALUE = "Value";

    private Pen pen = null;
    private String type = TYPE_VALUE;

    public Curve()
    {
    }

    public Curve(String path, String name, String title, EModel emodel)
    {
        super(path, name, title, emodel);
    }

    @PropertyName ( "Line spec" )
    public Pen getPen()
    {
        return pen;
    }
    public void setPen(Pen pen)
    {
        this.pen = pen;
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }

    public Stream<String> types()
    {
        return StreamEx.of(new String[] {TYPE_VALUE});
    }

    @Override
    public Curve clone(EModel emodel)
    {
        Curve result = new Curve(getPath(), getName(), getTitle(), emodel);
        result.setPen(getPen().clone());
        result.setType(getType());
        return result;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }
}