package com.couggi.javagraphviz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;


/**
 * Graphviz engine to generate of graph output
 *
 * @author Everton Cardoso
 *
 */
public class GraphvizEngine {

	private static final Logger log = Logger.getLogger("net.javagraphviz.GraphvizEngine");
	
	private static final String PATH = "PATH";
	
	private Map<String,OutputType> type;
	private Graph graph;
	private String layoutManager;

	/**
	 * directory path where the dot command will be executed.
	 */
	private String directoryPathExecute = ".";

	/**
	 * create the engine. type defualt = xdot.
	 */
	public GraphvizEngine(Graph graph) {
		this.graph = graph;
		this.type = new HashMap<String,OutputType>();
		this.type.put("png",new OutputType("png"));
		this.layoutManager = "dot";
	}

	/**
	 * generate the output file
	 *
	 */
	public void output() {


		String dotContent = graph.output();

		try {
			String prog = findExecutable(layoutManager);
			File tmpDot = createDotFileTemp("in",dotContent);

			ArrayList<String> params = new ArrayList<>();
			params.add(prog);

//			StringBuilder outputTypes = new StringBuilder();
			for (OutputType type : this.type.values()){
				params.add("-T" + type.name());
				params.add("-o" + type.filePath());
//				outputTypes.append(" -T")
//						   .append(type.name())
//						   .append(" -o")
//						   .append(type.filePath());
			}

			params.add(tmpDot.getPath());
			String[] dotCommand = params.toArray(new String[0]);

			Process process = Runtime.getRuntime().exec(dotCommand,null,new File(directoryPathExecute));

			//@SuppressWarnings("unused")
			int exitVal = process.waitFor();
			if(exitVal != 0) {
				log.log(Level.SEVERE, IOUtils.toString(process.getErrorStream()));
			}
		} catch (IOException e) {

			if (log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE,"command error",e);
			}
			throw new GraphvizOutputException(e.getMessage(),e);

		} catch (InterruptedException e) {

			if (log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE,"command error",e);
			}
			throw new GraphvizOutputException(e.getMessage(),e);
		}

	}

	private String pathEnvName() {
		String result = null;
		// find path env name
		for(String k : System.getenv().keySet()) {
			if(k.equalsIgnoreCase(PATH)) {
				result = k;
				break;
			}
		}
		
		if(result == null)
			throw new GraphvizEngineException("Path environment variable not found");
		
		return result;
	}
	
	private String findExecutable(String prog) {
		String PATH = System.getenv().get(pathEnvName());
		if(PATH == null)
			throw new GraphvizEngineException(prog + " program not found.");
		
		String[] paths = PATH.split(File.pathSeparator);
	    for (String path : paths) {
	    	String file = (path == null) ? prog : (path + File.separator + prog);
			if (new File(file).canExecute() && !new File(file).isDirectory()) {
				  return file;
			}

			file = (path == null) ? prog : new StringBuilder().append(path).append(File.separator).append(prog).append(".exe").toString();
			if ((new File(file).canExecute()) && (!(new File(file).isDirectory()))) {
				return file;
			}
	    }
	    throw new GraphvizEngineException(prog + " program not found.");
	}

	/**
	 * create a file temp with the content of the dot.
	 *
	 * @param dotContent
	 * @return
	 */
	private File createDotFileTemp(String suffix, String dotContent) {
		try {
			File temp = File.createTempFile("graph",suffix);
			if (dotContent != null) {
				BufferedWriter out = new BufferedWriter(new FileWriter(temp));
				out.write(dotContent);
				out.close();
			}
		    return temp;
		} catch (IOException e) {
			throw new GraphvizOutputException(e.getMessage(),e);
		}
	}

	/**
	 * type of output
	 */
	public List<OutputType> types() {
		return new ArrayList<OutputType>(type.values());
	}

	/**
	 * define where the dot command will be executed.
	 *
	 * @param dir
	 * @return
	 */
	public GraphvizEngine fromDirectoryPath(String path) {
		this.directoryPathExecute = path;
		return this;
	}

	/**
	 * set or add a output type.
	 */
	public OutputType addType(String name) {
		OutputType output = type.get(name);
		if (output == null) {
			output = new OutputType(name);
			type.put(name,output);
		}

		return this.type.get(name);
	}

	/**
	 * remove a output type.
	 */
	public GraphvizEngine removeType(String name) {
		if (type.size() == 1) {
			throw new IllegalStateException("must be a type defined.");
		}

		type.remove(name);

		return this;
	}

	/**
	 * Set the layout manager. Available options are: dot, neato, fdp, sfdp, twopi, circo
	 */
	public GraphvizEngine layout(String layoutManager) {
		this.layoutManager = layoutManager;
		return this;
	}

	/**
	 * set filePath of the output type. only used method when exist a output type.
	 *
	 * @param filePath
	 */
	public GraphvizEngine toFilePath(String filePath) {
		if (this.type.size() > 1) {
			throw new IllegalStateException("there was more of a type defined.");
		}

		this.type.values().iterator().next().toFilePath(filePath);

		return this;
	}



}
