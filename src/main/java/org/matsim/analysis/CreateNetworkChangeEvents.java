/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.ArrayList;
import java.util.List;





/**
 * This class is copied from matsim-code-examples and shows how to generate Network Change Events based on the speeds measured in an existing simulation.
 */
public class CreateNetworkChangeEvents{

	//settings
	private static final int ENDTIME = 36 * 3600;
	private static final int TIMESTEP = 15 * 60;

	//input files
	private static final String NETWORKFILE = "C:/Users/konra/tubCloud/Uni/Bachelorarbeit/Hamburg/hamburg-v2.2-10pct-base.output_network.xml";
	private static final String SIMULATION_EVENTS_FILE = "C:/Users/konra/tubCloud/Uni/Bachelorarbeit/Hamburg/hamburg-v2.2-10pct-base.output_events.xml";
	static final String OUTPUT_CHANGE_EVENTS_FILE = "";
	private static final double MINIMUMFREESPEED = 3;


	private final String networkFile;
	private final String simulationEvents;
	private final String outputEvents;

	CreateNetworkChangeEvents(String inputNetworkFile, String inputEventsFile, String outputFile) {
		networkFile = inputNetworkFile;
		simulationEvents = inputEventsFile;
		outputEvents = outputFile;
	}

	public static void main(String[] args) {
		CreateNetworkChangeEvents ncg = new CreateNetworkChangeEvents(NETWORKFILE, SIMULATION_EVENTS_FILE, OUTPUT_CHANGE_EVENTS_FILE);
		ncg.run();
	}

	void run() {
		Network network = NetworkUtils.createNetwork() ;
		new MatsimNetworkReader(network).readFile(this.networkFile);
		TravelTimeCalculator tcc = readEventsIntoTravelTimeCalculator( network );
		List<NetworkChangeEvent> networkChangeEvents = createNetworkChangeEvents( network, tcc );
		new NetworkChangeEventsWriter().write(this.outputEvents, networkChangeEvents);
	}

	private List<NetworkChangeEvent> createNetworkChangeEvents( Network network, TravelTimeCalculator tcc ) {
		List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>() ;

		for (Link l : network.getLinks().values()) {

//			if (l.getId().toString().startsWith("pt")) continue;

			double length = l.getLength();
			double previousTravelTime = l.getLength() / l.getFreespeed();

			for (double time = 0; time < ENDTIME; time = time + TIMESTEP) {

				double newTravelTime = tcc.getLinkTravelTimes().getLinkTravelTime(l, time, null, null);
				if (newTravelTime != previousTravelTime) {

					NetworkChangeEvent nce = new NetworkChangeEvent(time);
					nce.addLink(l);
					double newFreespeed = length / newTravelTime;
					if (newFreespeed < MINIMUMFREESPEED) newFreespeed = MINIMUMFREESPEED;
					ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, newFreespeed);
					nce.setFreespeedChange(freespeedChange);

					networkChangeEvents.add(nce);
					previousTravelTime = newTravelTime;
				}
			}
		}
		return networkChangeEvents ;
	}

	private TravelTimeCalculator readEventsIntoTravelTimeCalculator( Network network ) {
		EventsManager manager = EventsUtils.createEventsManager();
		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder( network );
		TravelTimeCalculator tcc = builder.build();
		manager.addHandler(tcc);
		manager.initProcessing();
		new MatsimEventsReader(manager).readFile(this.simulationEvents);
		manager.finishProcessing();
		return tcc ;
	}

}