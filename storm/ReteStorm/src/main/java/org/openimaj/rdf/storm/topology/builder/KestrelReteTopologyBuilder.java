package org.openimaj.rdf.storm.topology.builder;

import org.openimaj.kestrel.KestrelServerSpec;
import org.openimaj.rdf.storm.topology.bolt.KestrelReteConflictSetBolt;
import org.openimaj.rdf.storm.topology.bolt.ReteConflictSetBolt;
import org.openimaj.rdf.storm.topology.bolt.ReteFilterBolt;
import org.openimaj.rdf.storm.topology.builder.ReteTopologyBuilder.ReteTopologyBuilderContext;

import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.IRichBolt;


/**
 * The {@link KestrelReteTopologyBuilder} is mostly the same as a {@link SimpleReteTopologyBuilder} 
 * but allows for integration and use of kestrel queues for output of triples fed to the network as well
 * as infered triples.
 * 
 * This culminates in the replacement of the {@link ReteConflictSetBolt} with the {@link KestrelReteConflictSetBolt}
 * and a slight variation to the default connection of {@link ReteFilterBolt} instances 
 * @author Jon Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class KestrelReteTopologyBuilder extends SimpleReteTopologyBuilder {
	private KestrelServerSpec spec;

	/**
	 * @param spec the kestrel server to connect to
	 * @param outputQueue the output queue
	 */
	public KestrelReteTopologyBuilder(KestrelServerSpec spec, String outputQueue) {
		this.spec = spec;
	}

	@Override
	public ReteConflictSetBolt constructConflictSetBolt(ReteTopologyBuilderContext context) {
		return new KestrelReteConflictSetBolt();
	}
	
	@Override
	public void connectFilterBolt(ReteTopologyBuilderContext context,String name, IRichBolt bolt) {
		BoltDeclarer midBuild = context.builder.setBolt(name, bolt);
		// Just add it to the source, it doesn't need to be attached to the conflict set instance directly
		midBuild.shuffleGrouping(context.source);
	}
}
