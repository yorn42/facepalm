package util;

import java.io.StringWriter;
import java.util.HashMap;

import org.gephi.graph.api.*;
import org.gephi.io.database.drivers.SQLiteDriver;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.CharacterExporter;
import org.gephi.io.exporter.spi.Exporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.plugin.database.EdgeListDatabaseImpl;
import org.gephi.io.importer.plugin.database.ImporterEdgeList;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.spi.Layout;
import org.gephi.project.api.*;
import org.openide.util.Lookup;

/**
 * 
 * @author yorn Helper class to create a graph from an sql database. Uses gephi.
 * 
 */
public class GraphCreator {
	private EdgeListDatabaseImpl db;
	private Layout layout = null;
	private String path = "";

	public GraphCreator(SQLiteHelper todb, String path) {
		this.path = path;
		db = new EdgeListDatabaseImpl();
		db.setSQLDriver(new SQLiteDriver(todb));
		db.setNodeQuery("SELECT nodes.id AS id, nodes.label AS label, nodes.url, nodes.sex, nodes.single FROM nodes");
		db.setEdgeQuery("SELECT edges.source AS source, edges.target AS target, edges.name AS label, edges.weight AS weight FROM edges");
	}

	/**
	 * Create graph from database.
	 * 
	 * @return String contains the gexf
	 */
	public String createGraphFromSQL() {
		if (db == null) {
			System.out.println("[X] DB not configured!");
			return "";
		}
		ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();

		// get controllers and models
		ImportController importController = Lookup.getDefault().lookup(
				ImportController.class);
		GraphModel graphModel = Lookup.getDefault()
				.lookup(GraphController.class).getModel();

		ImporterEdgeList edgeListImporter = new ImporterEdgeList();
		Container container = importController.importDatabase(db,
				edgeListImporter);
		container.setAllowAutoNode(false); // don't create missing nodes

		// force UNDIRECTED
		container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED);

		// append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);
		NodeIterator ni = graphModel.getGraph().getNodes().iterator();
		Integer male = 0;
		Integer female = 0;
		Integer neutral = 0;
		while (ni.hasNext()) {
			Node foo = ni.next();
			String sex = ((String) foo.getNodeData().getAttributes()
					.getValue("sex"));
			if (sex.compareTo("M") == 0) {
				foo.getNodeData().setColor(0, 0, 1);
				male++;
			}

			else if (sex.compareTo("W") == 0) {
				foo.getNodeData().setColor(1, 0, 1);
				female++;
			} else {
				neutral++;
			}
		}
		
		// Write stats to files.
		HashMap<String, String> mp = new HashMap<String, String>();
		mp.put("Name:", graphModel.getGraph().getNode(1).getNodeData().getLabel());
		mp.put("male:", male.toString());
		mp.put("female:", female.toString());
		mp.put("neutral:", neutral.toString());
		WriteToFile.writeStatsToFile(mp, path);
		
		// layout - 100 Yifan Hu passes
		layout = new YifanHuLayout(null, new StepDisplacement(1f));
		layout.setGraphModel(graphModel);
		layout.resetPropertiesValues();
		for (int i = 0; i < 100 && layout.canAlgo(); i++) {
			layout.goAlgo();
		}
		
		// export to gexf
		ExportController ec = Lookup.getDefault()
				.lookup(ExportController.class);
		Exporter exporter = ec.getExporter("gexf");
		CharacterExporter characterExporter = (CharacterExporter) exporter;
		StringWriter stringWriter = new StringWriter();
		ec.exportWriter(stringWriter, characterExporter);
		String result = stringWriter.toString();
		// The swf Explorer cannot use "for", so we need to use "id"
		result = result.replaceAll("for=\"url\"", "id=\"url\"");
		return result;
	}

}
