package biouml.plugins.agentmodeling.covid19.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;
import one.util.streamex.DoubleStreamEx;

public class HealthCare extends SimulationAgent
    {
        private List<String> names = new ArrayList<>();
        private Set<String> sharedVariables = new HashSet<>();
        private List<Double> values = new ArrayList<>();
        private AgentBasedModel model;
        private int nextSpanIndex = 1;
        private Map<String, Integer> nameToIndex = new HashMap<>();
        Queue<MortalProducerAgent> qeue = new PriorityQueue<>();
        private int todayTests = 0;
        
        public int contactLevels = 3;
        private int testsLimit = 3;
        
        public static int TEST_NO_TESTING = 0;
        public static int TEST_ONLY_SEVERE_SYMPTOMS = 1;
        public static int TEST_ALL_WITH_SYMPTOMS = 2;
        public static int TEST_ALL_WITH_SYMPTOMS_CT = 3;

        private int testingMode = TEST_ONLY_SEVERE_SYMPTOMS;

        private Scenario scenario;
        private int scenarioIndex = 0;

        @Override
        public void init() throws Exception
        {
            super.init();
            this.testingMode = (int)getCurrentValue( "testingMode" );
        }
        
        public double calculateR()
        {
            double result = 0;
            if( model.getAgents().isEmpty() )
                return result;
            for( SimulationAgent agent : model.getAgents() )
            {
                if( agent instanceof MortalProducerAgent )
                {
                    int size = ( (MortalProducerAgent)agent ).getCreated().size();
                    result += size;
                }
            }
            return result;
        }

        public void addGlobalVariable(String name, double value, boolean shared)
        {
            names.add( name );
            values.add( value );
            nameToIndex.put( name, names.size() - 1 );
            if( shared )
                sharedVariables.add( name );
        }

        public double getPriority()
        {
            return OBSERVER_AGENT_PRIORITY;
        }

        public HealthCare(AgentBasedModel model, String name, Span span)
        {
            super( name, span );
            this.model = model;
            //calculated variables
            addGlobalVariable( "testsToday", 0, false );
            addGlobalVariable( "in Qeue", 0, false );
            addGlobalVariable( "testsTotal", 0, false );
            addGlobalVariable( "R", 0, false );
            todayTests = 0;
        }
        
        public void setScenario(Scenario scenario)
        {
            this.scenario = scenario;
        }

        private void performTesting()// throws Exception
        {
            try
            {
                for( SimulationAgent agent : model.getAgents() )
                {
                    if( ! ( agent instanceof MortalProducerAgent ) )
                        continue;
                    if( agent.isAlive() && agent.getCurrentValue( "Seek_Testing" ) > 0 && agent.getCurrentValue( "Detected" ) == 0
                            && agent.getCurrentValue( "Qeued" ) == 0 )
                    {
                        if( shouldBeTested( agent ) )
                            qeueForTesting( agent );
                    }
                }

                runTests();
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }

        private void qeueForTesting(SimulationAgent agent) throws Exception
        {
            agent.setCurrentValue( "Qeued", 1.0 );
            qeue.add( (MortalProducerAgent)agent );
        }

        private boolean shouldBeTested(SimulationAgent agent) throws Exception
        {
            if( testingMode == TEST_ALL_WITH_SYMPTOMS || testingMode == TEST_ALL_WITH_SYMPTOMS_CT )
                return true;
            else if( testingMode == TEST_ONLY_SEVERE_SYMPTOMS )
                return (int)agent.getCurrentValue( "Symptoms" ) >= 2;
            else
            {
                double rand = Math.random();
                return rand < 0.02; //some people gets testing anyway
            }
        }

        private void performContactTracing(MortalProducerAgent agent) throws Exception
        {
            //find who infected this agent and who was infected by him
            //TODO: introduce some type of error here
            Set<MortalProducerAgent> contacts = new HashSet<>();
            contacts.addAll( agent.getCreated() );
            if( agent.getCreator() != null )
                contacts.add( agent.getCreator() );

            //trace three levels
            for( int i = 0; i < contactLevels; i++ )
            {
                Set<MortalProducerAgent> nextContacts = new HashSet<>();
                for( MortalProducerAgent contact : contacts )
                {
                    if( !contact.isAlive() || contact.getCurrentValue( "Detected" ) != 0 || contact.getCurrentValue( "Qeued" ) == 1 )
                        continue;

                    qeueForTesting( contact );

                    nextContacts.addAll( contact.getCreated() );
                    if( contact.getCreator() != null )
                    nextContacts.add( contact.getCreator() );
                }
                contacts = nextContacts;
            }
        }
        
        private List<String> testTodayList;

        private void runTests() throws Exception
        {
            todayTests = 0;
            testTodayList = new ArrayList<>();
            List<MortalProducerAgent> detected = new ArrayList<>();
            int todayLimit = testsLimit + (int)((1 - 2*Math.random())*(testsLimit/10.0));
            while( !qeue.isEmpty() && todayTests < todayLimit )
            {
                MortalProducerAgent agent = qeue.poll();
                boolean isDetected = detect( agent );
                if( isDetected )
                {
                    agent.setCurrentValue( "Detected", 1.0 );
                    agent.setCurrentValue( "Seek_Testing", 0.0 );
                    detected.add( agent );
//                    
//                    if( testingMode == TEST_ALL_WITH_SYMPTOMS_CT )
//                    {
//                        for( MortalProducerAgent detectedAgent : detected )
//                            performContactTracing( detectedAgent );
//                    }
                }
            }
            
            if( testingMode == TEST_ALL_WITH_SYMPTOMS_CT )
            {
                for( MortalProducerAgent agent : detected )
                    performContactTracing( agent );


                while( !qeue.isEmpty() && todayTests < todayLimit )
                {
                    MortalProducerAgent agent = qeue.poll();
                    boolean isDetected = detect( agent );
                    if( isDetected )
                    {
                        agent.setCurrentValue( "Detected", 1.0 );
                        agent.setCurrentValue( "Seek_Testing", 0.0 );
                        performContactTracing( agent );
                    }
                }
            }
            detected.clear();
            
            this.setCurrentValue( "testsToday", todayTests );
            this.setCurrentValue( "testsTotal", getCurrentValue( "testsTotal" ) + todayTests );
            this.setCurrentValue( "in Qeue", qeue.size());
//            System.out.println( "Today limit "+ todayLimit );
//            System.out.println( currentTime + " Tested total: "+ getCurrentValue( "testsTotal" ) );
//            System.out.println( currentTime + " Tested today: ("+ todayTests+ ") "+ StreamEx.of( testTodayList ).joining( "," ));
//            System.out.println( currentTime + " Test await: "+ qeue.size() +" : " +StreamEx.of(qeue).map( agent->agent.name ).joining(",") );
//            qeue.clear();
        }

        private boolean detect(SimulationAgent agent) throws Exception
        {
            todayTests++;            
            boolean isDetected = agent.getCurrentValue( "Infectious" ) > 0;
            testTodayList.add( agent.getName() +" ["+(isDetected?"+":"-")+"]");
            return isDetected;
        }

        @Override
        public double getCurrentValue(String name)
        {
            return values.get( nameToIndex.get( name ) );
        }

        @Override
        public void setCurrentValue(String name, double newValue)
        {
            values.set( nameToIndex.get( name ), newValue );
        }

        @Override
        public double[] getCurrentValues() throws Exception
        {
            return DoubleStreamEx.of( values ).toArray();
        }

        @Override
        public String[] getVariableNames()
        {
            return names.stream().toArray( String[]::new );
        }

        public Set<String> getSharedVariables()
        {
            return sharedVariables;
        }

        @Override
        public void iterate()
        {
            this.currentTime = this.span.getTime( nextSpanIndex );    
            this.testingMode = (int)getCurrentValue( "testingMode" );
//            this.setCurrentValue( "R", calculateR() );
            
            if( scenario != null )
            {
                if( scenarioIndex <scenario.times.length && Math.abs( currentTime - scenario.times[scenarioIndex] ) < 0.1 )
                {
                    applyScenario(scenario, scenarioIndex);
                    scenarioIndex++;
                }
            }        
            performTesting();
            nextSpanIndex++;
        }
        
        private void applyScenario(Scenario scenario, int index)
        {
            this.setCurrentValue( "testingMode" ,(int)scenario.testMode[index]);
            this.setCurrentValue( "mobility_limit", scenario.mobilityLimit[index] );
            this.setCurrentValue( "available_beds", getCurrentValue( "available_beds" ) + scenario.newBeds[index] );
            this.setCurrentValue( "available_ICU", getCurrentValue( "available_ICU" ) + scenario.newICU[index] );
            this.setCurrentValue("limit_mass_gathering", scenario.limit_mass_gathering[index]);
            this.testsLimit = (int)scenario.testLimits[index];
        }

        @Override
        public double[] getUpdatedValues() throws Exception
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void setUpdated()
        {
            // TODO Auto-generated method stub
        }
    }