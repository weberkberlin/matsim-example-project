package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import java.util.List;

public class RunTravelTimeAnalysis {


    private static final String CONFIG_PATH = "";
    private static final String NETWORK_CHANGE_EVENTS = "";

    private static final String OUTPUT_EVENTS_PATH = "";

    private static final String OUTPUT_DIRECTORY = "";
    private static final Integer NUMBER_OF_AGENTS = 10_000;


    private static final String ACT_TYPE = "analysis";

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig(CONFIG_PATH);

        //create network change events
        String outputEvents = config.controler().getOutputDirectory() + "/" +  config.controler().getRunId() + ".output_events.xml.gz";
        new CreateNetworkChangeEvents(config.network().getInputFile(), outputEvents, NETWORK_CHANGE_EVENTS).run();

        //set input paths and output dir
        config.network().setTimeVariantNetwork(true);
        config.network().setChangeEventsInputFile(NETWORK_CHANGE_EVENTS);
        config.controler().setOutputDirectory(OUTPUT_DIRECTORY);


        //register new activity type
        PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams();
        params.setActivityType(ACT_TYPE);
        params.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(params);

        //load scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        //modify scenario (pop building)
        overridePopulation(scenario);

        Controler controler = new Controler(scenario);

        RoutingModule router = controler.getInjector().getInstance(RoutingModule.class);

        controler.run();
    }

    /**
     * first deletes all agents from the population and then (randomly)
     * generates new agents that have one leg each with variyng distances and relations spread over the entire network
     * @param scenario
     */
    private static void overridePopulation(Scenario scenario){
        // delete original population
        scenario.getPopulation().getPersons().clear();

        PopulationFactory factory = scenario.getPopulation().getFactory();


        for (int i = 1; i < NUMBER_OF_AGENTS; i++) {

            Person carPerson = factory.createPerson(Id.createPersonId("person_" + i + "_CAR"));
            Person ptPerson = factory.createPerson(Id.createPersonId("person_" + i + "_PT"));

            Plan carPlan = factory.createPlan();
            Plan ptPlan = factory.createPlan();

            Tuple<Coord, Coord> relation = getRandomCoordRelationInNetwork(scenario.getNetwork());

            Activity originAct = factory.createActivityFromCoord(ACT_TYPE, relation.getFirst());
            originAct.setEndTime( 8*3600 ); //TODO

            Activity destinationAct = factory.createActivityFromCoord(ACT_TYPE, relation.getSecond());

            carPlan.addActivity(originAct);
            ptPlan.addActivity(originAct);

            carPlan.addLeg(factory.createLeg(TransportMode.car));
            ptPlan.addLeg(factory.createLeg(TransportMode.pt));

            carPlan.addActivity(destinationAct);
            ptPlan.addActivity(destinationAct);

            carPerson.addPlan(carPlan);
            ptPerson.addPlan(ptPlan);

            scenario.getPopulation().addPerson(carPerson);
            scenario.getPopulation().addPerson(ptPerson);
        }

    }

    //TODO
    private static Tuple<Coord,Coord> getRandomCoordRelationInNetwork(Network network) {
//        NetworkUtils.getBoundingBox()
        //...
        return null;
    }



}
