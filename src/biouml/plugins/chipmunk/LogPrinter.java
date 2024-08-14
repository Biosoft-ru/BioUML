package biouml.plugins.chipmunk;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.autosome.assist.IPrinter;

public class LogPrinter implements IPrinter
{
    private Logger log;
    private Level level;
    
    public LogPrinter(Logger log, Level level)
    {
        this.log = log;
        this.level = level;
    }

    @Override
    public void println(String message)
    {
        log.log(level, message);
    }
}