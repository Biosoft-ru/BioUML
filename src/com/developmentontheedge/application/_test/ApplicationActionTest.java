package com.developmentontheedge.application._test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.developmentontheedge.application.action.ApplicationAction;

/**
 *
 */
public class ApplicationActionTest extends TestCase
{
    /** Standart JUnit constructor */
    public ApplicationActionTest( String name )
    {
        super( name );
    }

    int actionPerformed;
    public void testListenerRemoving()
    {
        ApplicationAction action = new ApplicationAction("testAction", "testAction");
        ActionListener listener1 = new TestListener();
        ActionListener listener2 = new TestListener();
        action.addActionListener( listener1 );
        action.addActionListener( listener2 );

        actionPerformed = 0;
        action.actionPerformed( new ActionEvent(this,0,"test") );
        assertEquals( "Not all actions performed.",2,actionPerformed );

        actionPerformed = 0;
        action.removeActionListeners( TestListener.class );
        action.actionPerformed( new ActionEvent(this,0,"test") );
        assertEquals( "Not all listeners removed.",0,actionPerformed );
    }

    public class TestListener implements ActionListener
    {
        @Override
        public void actionPerformed( ActionEvent evt )
        {
            actionPerformed ++;
        }
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite( ApplicationActionTest.class.getName() );
        suite.addTestSuite( ApplicationActionTest.class );
        return suite;
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main( String[] args )
    {
        if ( args != null && args.length>0 && args[0].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( ApplicationActionTest.class ); }
    }
}// end of ApplicationActionTest