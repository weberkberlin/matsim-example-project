package org.matsim.analysis;

import org.apache.commons.math3.random.RandomGenerator;
import org.geotools.data.shapefile.files.ShpFiles;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class RunTravelTimeAnalysis {


    private static final String CONFIG_PATH = "scenarios/equil/config.xml";
    private static final String NETWORK_CHANGE_EVENTS = "";
    private static final String SHAPEFILE_PATH  = "C:/Users/konra/tubCloud/Uni/Bachelorarbeit/Hamburg/hamburg_hvv/hamburg_hvv_one_geom.shp";

    private static final String OUTPUT_EVENTS_PATH = "";

    private static final String OUTPUT_DIRECTORY = "";
    private static final Integer NUMBER_OF_AGENTS = 10;


    private static final String ACT_TYPE = "analysis";

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig(CONFIG_PATH);

        //create network change events
        String outputEvents = config.controler().getOutputDirectory() + "/" +  config.controler().getRunId() + ".output_events.xml.gz";
        //new CreateNetworkChangeEvents(config.network().getInputFile(), outputEvents, NETWORK_CHANGE_EVENTS).run();

        //set input paths and output dir
        config.network().setTimeVariantNetwork(true);
        config.network().setChangeEventsInputFile(NETWORK_CHANGE_EVENTS);
        config.controler().setOutputDirectory(OUTPUT_DIRECTORY);

        //set dummy population, will be overridden anyways so we do not load the original scenario population which would a) take long and b) possibly does not fit into Konrad's RAM (?)
        String pathToDummyPlansFile = Paths.get("scenarios/equil/plans100.xml").toAbsolutePath().toString();
        config.plans().setInputFile(pathToDummyPlansFile);

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

        Collection<SimpleFeature> listOfFeatures = ShapeFileReader.getAllFeatures(IOUtils.resolveFileOrResource(SHAPEFILE_PATH));
        if (listOfFeatures.size() > 1){
            throw new IllegalArgumentException("too many features in shapefile! Only 1 allowed.");
        }
        SimpleFeature shapefile = listOfFeatures.stream().findAny().orElseThrow();


        for (int i = 1; i < NUMBER_OF_AGENTS; i++) {

            Person carPerson = factory.createPerson(Id.createPersonId("person_" + i + "_CAR"));
            Person ptPerson = factory.createPerson(Id.createPersonId("person_" + i + "_PT"));

            Plan carPlan = factory.createPlan();
            Plan ptPlan = factory.createPlan();

            Tuple<Coord, Coord> relation = getRandomCoordRelationInNetwork(shapefile);

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
    private static Tuple<Coord,Coord> getRandomCoordRelationInNetwork(SimpleFeature shapefile) {

        //double[] bbox = NetworkUtils.getBoundingBox(network.getNodes().values());
        //List<Geometry> shape = ShpGeometryUtils.loadGeometries(IOUtils.resolveFileOrResource("123"));
        //ShpGeometryUtils.isCoordInGeometries();

        //RandomGenerator randGen = RandomUtils.getLocalGenerator();
        Random random = MatsimRandom.getLocalInstance();

        //Collection<SimpleFeature> listOfFeatures = ShapeFileReader.getAllFeatures(IOUtils.resolveFileOrResource("C:/Users/konra/tubCloud/Uni/Bachelorarbeit/Hamburg/hamburg_hvv/hamburg_hvv_one_geom.shp"));
        //if (listOfFeatures.size() > 1){
        //    throw new IllegalArgumentException("too many features in shapefile! Only 1 allowed.");
        //}

        System.out.println(shapefile.toString());


        //Point start = GeometryUtils.getRandomPointInFeature(random, listOfFeatures.stream().findAny().orElseThrow());
        //Point end = GeometryUtils.getRandomPointInFeature(random, listOfFeatures.stream().findAny().orElseThrow());

        Coord startCoord = null;
        Coord endCoord = null;

        return new Tuple<>(startCoord, endCoord);
    }



}
