package ru.biosoft.plugins.javascript;

import one.util.streamex.StreamEx;

import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Registry for JavaScript preprocessors
 */
public class PreprocessorRegistry
{
    protected static final String PREPROCESSOR_NAME = "name";

    private static final ObjectExtensionRegistry<Preprocessor> registry = new ObjectExtensionRegistry<>(
            "ru.biosoft.plugins.javascript.preprocessor", PREPROCESSOR_NAME, Preprocessor.class);

    public static StreamEx<Preprocessor> preprocessors()
    {
        return registry.stream();
    }
}
