package ru.biosoft.galaxy.validators;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 *
 */
public class ValidatorFactory
{
    private static final Map<String, Class<? extends Validator>> validators = new HashMap<>();
    
    static
    {
        // TODO: ExpressionValidator
        validators.put("in_range", InRangeValidator.class);
        validators.put("length", LengthValidator.class);
        validators.put("empty_field", EmptyTextfieldValidator.class);
        validators.put("no_options", NoOptionsValidator.class);
        validators.put("dataset_ok_validator", DatasetOkValidator.class);
        validators.put("unspecified_build", UnspecifiedBuildValidator.class);
        validators.put("dataset_metadata_in_file", MetadataInFileColumnValidator.class);
        validators.put("metadata", MetadataValidator.class);
    }
    
    public static Validator createValidator(Element element, Parameter parameter)
    {
        String type = element.getAttribute("type");
        Validator validator = null;
        if(validators.containsKey(type))
        {
            try
            {
                validator = validators.get(type).newInstance();
            }
            catch( Exception e )
            {
            }
        }
        if(validator != null)
            validator.init(element, parameter);
        return validator;
    }
}
