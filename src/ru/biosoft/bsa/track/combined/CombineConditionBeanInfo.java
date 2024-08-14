package ru.biosoft.bsa.track.combined;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CombineConditionBeanInfo extends BeanInfoEx2<CombineCondition>
{
    public CombineConditionBeanInfo()
    {
        super( CombineCondition.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        addWithTags( "conditionType", CombineCondition.CONDITION_TYPES );
        addReadOnly( "formula" );
        add( "distance" );
    }

}
