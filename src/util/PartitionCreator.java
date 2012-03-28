package util;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.CharacterExporter;
import org.gephi.io.exporter.spi.Exporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.Modularity;
import org.openide.util.Lookup;

public class PartitionCreator {
	/**
	 * 
	 * @author yorn Helper class to create stats from a gexf file. Uses gephi.
	 * 
	 */
	public PartitionCreator() {
	}

	/**
	 * Create stats graph from gexf.
	 * 
	 * @return String contains the stats gexf
	 */
	public String createPartitions(String path) {
		HashMap<String, String> mp = new HashMap<String, String>();
		ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		GraphModel graphModel = Lookup.getDefault()
				.lookup(GraphController.class).getModel();
		Workspace workspace = pc.getCurrentWorkspace();
		AttributeModel attributeModel = Lookup.getDefault()
				.lookup(AttributeController.class).getModel();
		// Import file
		ImportController importController = Lookup.getDefault().lookup(
				ImportController.class);
		Container container;
		try {
			File file = new File(path + "/fb.gexf");
			container = importController.importFile(file);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
		// Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);

		PartitionController partitionController = Lookup.getDefault().lookup(
				PartitionController.class);
		// See if graph is well imported
		DirectedGraph graph = graphModel.getDirectedGraph();
		mp.put("Friends:", new Integer(graph.getNodeCount()).toString());
		mp.put("Connections:", new Integer(graph.getEdgeCount()).toString());
		
		// Run modularity algorithm - community detection
		Modularity modularity = new Modularity();
		modularity.execute(graphModel, attributeModel);

		// Partition with 'modularity_class', just created by Modularity
		// algorithm
		AttributeColumn modColumn = attributeModel.getNodeTable().getColumn(
				Modularity.MODULARITY_CLASS);
		Partition<?> p2 = partitionController.buildPartition(modColumn, graph);
		System.out.println("[!] " + p2.getPartsCount() + " partitions found!");
		mp.put("Communities:", new Integer(p2.getPartsCount()).toString());
		
		NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
		nodeColorTransformer2.randomizeColors(p2);
		partitionController.transform(p2, nodeColorTransformer2);
		
		// export to gexf
		ExportController ec = Lookup.getDefault()
				.lookup(ExportController.class);
		Exporter exporter = ec.getExporter("gexf");
		CharacterExporter characterExporter = (CharacterExporter) exporter;
		StringWriter stringWriter = new StringWriter();
		ec.exportWriter(stringWriter, characterExporter);
		String result = stringWriter.toString();
		
		// write stats to files
		WriteToFile.writeStatsToFile(mp, path);
		return result;
	}

}
