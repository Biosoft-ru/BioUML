package ru.biosoft.plugins.javascript;

/**
 * Contains JavaScript command. BeanInfo designed to show special button as editor with running script by click.
 */
public class JSCommand
{
    private CommandGenerator commandGenerator;

    public JSCommand(CommandGenerator commandGenerator)
    {
        this.commandGenerator = commandGenerator;
    }

    public String getCommand()
    {
        return commandGenerator.generateCommand();
    }

    public boolean isEmpty()
    {
        return commandGenerator.isEmpty();
    }

    public static interface CommandGenerator
    {
        public boolean isEmpty();
        public String generateCommand();
    }
}
