/**
 * Copyright (c) ${year}, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.rdf.storm.bolt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openimaj.rdf.storm.bolt.StormGraphRouter.Action;
import org.openimaj.rdf.storm.bolt.StormSteMBolt.Component;

import com.hp.hpl.jena.graph.Graph;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author davidlmonks
 *
 */
public class StormEddyBolt implements IRichBolt {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1073714124183765931L;

	public static final String STREAM_TO_EDDY = "eddy stream";
	
	protected StormGraphRouter router;
	
	/**
	 * @param sgr
	 */
	public StormEddyBolt(StormGraphRouter sgr){
		this.router = sgr;
	}
	
	private Map<String,Object> conf;
	private TopologyContext context;
	private OutputCollector collector;
	
	@SuppressWarnings("unchecked")
	@Override
	public void prepare(@SuppressWarnings("rawtypes") Map stormConf,
						TopologyContext context,
						OutputCollector collector) {
		this.conf = stormConf;
		this.context = context;
		this.collector = collector;
		
		this.router.setOutputCollector(collector);
	}

	@Override
	public void execute(Tuple input) {
		this.router.routeGraph(
					 /*anchor*/input,
					 /*action*/(Action)input.getValueByField(Component.action.toString()),
					  /*isAdd*/input.getBooleanByField(StormSteMBolt.Component.isAdd.toString()),
					  /*graph*/(Graph)input.getValueByField(StormSteMBolt.Component.graph.toString()),
				  /*timestamp*/input.getLongByField(Component.timestamp.toString())
		);
	}
	
	@Override
	public void cleanup() {
		this.conf = null;
		this.context = null;
		this.collector = null;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		this.router.declareOutputFields(declarer);
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return conf;
	}
	
	// INNER CLASSES
	
	/**
	 * 
	 * @author David Monks <dm11g08@ecs.soton.ac.uk>
	 */
	public static class EddyStubStormGraphRouter extends StormGraphRouter {
		
		private final List<String> eddies;
		
		/**
		 * 
		 * @param eddies 
		 * 			The list of eddies this router's SteM is part of.
		 */
		public EddyStubStormGraphRouter(List<String> eddies){
			this.eddies = eddies;
		}
		
		@Override
		protected long routingTimestamp(long stamp1, long stamp2){
			return stamp1 > stamp2 ? stamp1 : -1;
		}
		
		@Override
		public void routeGraph(Tuple anchor, boolean isAdd, Graph g, long... timestamp) {
			// The default assumption is that Tuple's from SteMs are intended for probing (i.e. NOT building).
			routeGraph(anchor, Action.probe, isAdd, g, timestamp);
		}

		@Override
		public void routeGraph(Tuple anchor, Action action, boolean isAdd, Graph g,
							   long... timestamp) {
			Values vals = new Values();
			for (Component c : Component.values()) {
				switch (c) {
				case action:
					// set whether this Tuple is intended for probing or building into other SteMs
					vals.add(action);
					break;
				case isAdd:
					// insert this Tuple's value of isAdd to be passed onto subscribing Bolts.
					vals.add(isAdd);
					break;
				case graph:
					// insert the new graph into the array of Values
					vals.add(g);
					break;
				case timestamp:
					vals.add(timestamp);
					break;
				default:
					break;
				}
			}
			
			String source = anchor.getSourceComponent();
			if (this.eddies.contains(source))
				this.collector.emit(source, anchor, vals);
			else
				for (String eddy : this.eddies)
					this.collector.emit(eddy, anchor, vals);
			this.collector.ack(anchor);
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			for (String eddy : this.eddies)
				declarer.declareStream(eddy, new Fields( Arrays.asList( Component.strings() ) ) );
		}

	}

}