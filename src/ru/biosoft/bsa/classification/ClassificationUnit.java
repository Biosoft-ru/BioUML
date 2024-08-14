package ru.biosoft.bsa.classification;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

@ClassIcon ( "resources/classification.gif" )
public interface ClassificationUnit extends DataCollection<ClassificationUnit>
{
    public String getClassName();
    public String getClassNumber();

    public String getLevel();
    public String getDescription();
    
    public DynamicPropertySet getAttributes();

    public ClassificationUnit   getParent();
    public ClassificationUnit   getChild(int i);

    public boolean              isAncestor(ClassificationUnit unit);
}


