package ru.biosoft.util.bean;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.stream.Stream;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.OptionEx;

/**
 * @author lan
 *
 */
public class BeanInfoEx2<T> extends BeanInfoEx
{
    public static final String STRUCTURE_CHANGING_PROPERTY = "structureChangingProperty";
    public static final String FIXED_LENGTH_PROPERTY = "fixedLengthProperty";
    public static final String IMPLICIT_PROPERTY = "implicitProperty";

    protected BeanInfoEx2()
    {
        super();
    }

    /**
     * @param beanClass
     * @param resourceBundleName
     */
    public BeanInfoEx2(Class<? extends T> beanClass, String resourceBundleName)
    {
        super(beanClass, resourceBundleName);
    }

    /**
     * @param beanClass
     */
    public BeanInfoEx2(Class<? extends T> beanClass)
    {
        super(beanClass, true);
    }

    public void addReadOnly(String name)
    {
        property( name ).readOnly().add();
    }

    public void addWithTags(String name, Function<T, Stream<String>> tagsSupplier)
    {
        property( name ).tags( tagsSupplier ).add();
    }

    public void addWithTags(PropertyDescriptorEx pd, Function<T, Stream<String>> tagsSupplier)
    {
        property( pd ).tags( tagsSupplier ).add();
    }

    public void addWithoutChildren(String name, Class<?> editor)
    {
        property( name ).editor(editor).hideChildren().add();
    }

    @Override
    public PropertyDescriptorBuilder2 property(String name)
    {
        return new PropertyDescriptorBuilder2( name );
    }

    @Override
    public PropertyDescriptorBuilder2 property(PropertyDescriptorEx pd)
    {
        return new PropertyDescriptorBuilder2( pd );
    }

    public class PropertyDescriptorBuilder2 extends PropertyDescriptorBuilder
    {
        public PropertyDescriptorBuilder2(String name)
        {
            super(name);
        }

        public PropertyDescriptorBuilder2(PropertyDescriptorEx pd)
        {
            super(pd);
        }

        @Override
        public void add()
        {
            BeanInfoEx2.this.add(pd);
        }

        public void add(int i)
        {
            BeanInfoEx2.this.add(i, pd);
        }

        @SuppressWarnings ( "unchecked" )
        public PropertyDescriptorBuilder2 tags(Function<T, Stream<String>> tagsSupplier)
        {
            tagsFunction( (Function<Object, Stream<String>>)tagsSupplier );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 tags(String... tags)
        {
            return tags( bean -> Stream.of(tags) );
        }

        @Override
        public PropertyDescriptorBuilder2 editor(Class<?> editor)
        {
            pd.setPropertyEditorClass( editor );
            return this;
        }

        public PropertyDescriptorBuilder2 inputElement(Class<? extends DataElement> elementType)
        {
            DataElementPathEditor.registerInput( pd, elementType );
            return this;
        }

        public PropertyDescriptorBuilder2 outputElement(Class<? extends DataElement> elementType)
        {
            DataElementPathEditor.registerOutput( pd, elementType );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 title(String title)
        {
            pd.setDisplayName( getResourceString( title ) );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 description(String description)
        {
            pd.setShortDescription( getResourceString( description ) );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 titleRaw(String title)
        {
            pd.setDisplayName(title);
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 descriptionRaw(String description)
        {
            pd.setShortDescription( description );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 numberFormat(String pattern)
        {
            pd.setNumberFormat( pattern );
            return this;
        }

        public PropertyDescriptorBuilder2 structureChanging()
        {
            pd.setValue( STRUCTURE_CHANGING_PROPERTY, Boolean.TRUE );
            return this;
        }

        public PropertyDescriptorBuilder2 fixedLength()
        {
            pd.setValue(FIXED_LENGTH_PROPERTY, Boolean.TRUE);
            return this;
        }

        public PropertyDescriptorBuilder2 implicit()
        {
            pd.setValue( IMPLICIT_PROPERTY, Boolean.TRUE );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 hidden()
        {
            pd.setHidden(true);
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 expert()
        {
            pd.setExpert( true );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 readOnly()
        {
            pd.setReadOnly( true );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 canBeNull()
        {
            pd.setCanBeNull( true );
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 hidden(String methodName)
        {
            try
            {
                pd.setHidden(beanClass.getMethod( methodName ));
            }
            catch( NoSuchMethodException | SecurityException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 readOnly(String methodName)
        {
            try
            {
                pd.setReadOnly( beanClass.getMethod( methodName ) );
            }
            catch( NoSuchMethodException | SecurityException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return this;
        }

        public PropertyDescriptorBuilder2 canBeNull(String methodName)
        {
            try
            {
                pd.setCanBeNull( beanClass.getMethod( methodName ) );
            }
            catch( NoSuchMethodException | SecurityException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return this;
        }

        public PropertyDescriptorBuilder2 auto(String template)
        {
            OptionEx.makeAutoProperty( pd, template );
            return this;
        }

        public PropertyDescriptorBuilder2 hideChildren()
        {
            pd.setHideChildren( true );
            return this;
        }
        
        public PropertyDescriptorBuilder2 childDisplayName(Method method)
        {
            pd.setValue(BeanInfoEx.CHILD_DISPLAY_NAME, method);
            return this;
        }

        @Override
        public PropertyDescriptorBuilder2 simple()
        {
            pd.setSimple( true );
            return this;
        }

        public PropertyDescriptorBuilder2 htmlDisplayName(String name)
        {
            HtmlPropertyInspector.setDisplayName(pd, name);
            return this;
        }

        public PropertyDescriptorBuilder2 value(String attributeName, Object value)
        {
            pd.setValue(attributeName, value);
            return this;
        }

        public PropertyDescriptorBuilder2 referenceType(Class<? extends ReferenceType> referenceTypeClass)
        {
            pd.setValue( DataElementPathEditor.REFERENCE_TYPE, referenceTypeClass );
            return this;
        }
    }
}
