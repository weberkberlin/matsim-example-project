package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunTravelTimeAnalysis {

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();

        //setze Input Files

        Scenario scenario = ScenarioUtils.loadScenario(config);

        //modify scenario (pop building)
        // mit https://github.com/matsim-scenarios/matsim-berlin/blob/ev/src/test/java/org/matsim/urbanEV/UrbanEVTests.java

        Controler controler = new Controler(scenario);

        controler.run();

    }


}
