package gov.nih.nci.evs.restapi.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Properties;

/*
Sample config.properties:
##############################################################
#                   Windows Configurations                   #
##############################################################
# The dir. where temporary files will be created.
tempDirForWindows = c:/temp
# Where is your dot program located? It will be called externally.
dotForWindows="C:/graphviz-2.38/bin/dot.exe"
*/

public class GraphViz
{
    public static String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}

    public static String getConfigurationFile() {
		String workingDir = System.getProperty("user.dir");
		return workingDir + "/" + "config.properties";
	}

    private final static String osName = "Windows";//System.getProperty("os.name").replaceAll("\\s","");
    //private final static String osName = System.getProperty("os.name").replaceAll("\\s","");

    private final static String cfgProp = getConfigurationFile();


    private final static Properties configFile = new Properties() {
        private final static long serialVersionUID = 1L; {
            try {
				System.out.println("loading config.properties...");
                load(new FileInputStream(cfgProp));
                System.out.println("Done loading config.properties...");
            } catch (Exception e) {}
        }
    };


    private static String TEMP_DIR = "c://temp";

    private static String DOT = configFile.getProperty("dotFor" + osName);

    private int[] dpiSizes = {46, 51, 57, 63, 70, 78, 86, 96, 106, 116, 128, 141, 155, 170, 187, 206, 226, 249};

    private int currentDpiPos = 7;

    public void increaseDpi() {
        if ( this.currentDpiPos < (this.dpiSizes.length - 1) ) {
            ++this.currentDpiPos;
        }
    }

    public void decreaseDpi() {
        if (this.currentDpiPos > 0) {
            --this.currentDpiPos;
        }
    }

    public int getImageDpi() {
        return this.dpiSizes[this.currentDpiPos];
    }

    private StringBuilder graph = new StringBuilder();

    public GraphViz() {
		String os = System.getProperty("os.name");
		System.out.println(os);
    }

    public String getDotSource() {
        return this.graph.toString();
    }

    public void add(String line) {
        this.graph.append(line);
    }

    public void addln(String line) {
        this.graph.append(line + "\n");
    }

    public void addln() {
        this.graph.append('\n');
    }

    public void clearGraph(){
        this.graph = new StringBuilder();
    }

    public byte[] getGraph(String dot_source, String type)
    {
        File dot;
        byte[] img_stream = null;

        try {
			System.out.println("writeDotSourceToFile..." + dot_source);
            dot = writeDotSourceToFile(dot_source);
            if (dot != null)
            {
                img_stream = get_img_stream(dot, type);
                if (dot.delete() == false)
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) { return null; }
    }


    public byte[] getGraph(String dot_source, String type, String dotsourcefile) {
        File dot;
        byte[] img_stream = null;

        try {
			System.out.println("writeDotSourceToFile..." + dot_source);
            dot = writeDotSourceToFile(dot_source, dotsourcefile);
            if (dot != null)
            {
                img_stream = get_img_stream(dot, type);
                if (dot.delete() == false)
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) { return null; }
    }

    public int writeGraphToFile(byte[] img, String file)
    {
        File to = new File(file);
        return writeGraphToFile(img, to);
    }

    public int writeGraphToFile(byte[] img, File to)
    {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            fos.write(img);
            fos.close();
        } catch (java.io.IOException ioe) { return -1; }
        return 1;
    }

    private byte[] get_img_stream(File dot, String type)
    {
        File img;
        byte[] img_stream = null;

        try {
            img = File.createTempFile("graph_", "."+type, new File(GraphViz.TEMP_DIR));
            Runtime rt = Runtime.getRuntime();
            String[] args = {DOT, "-T"+type, "-Gdpi="+dpiSizes[this.currentDpiPos], dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);
            p.waitFor();
            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            if( in != null ) in.close();
            if (img.delete() == false)
                System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
        }
        catch (java.io.IOException ioe) {
            System.err.println("Error:    in I/O processing of tempfile in dir " + GraphViz.TEMP_DIR+"\n");
            System.err.println("       or in calling external command");
            ioe.printStackTrace();
        }
        catch (java.lang.InterruptedException ie) {
            System.err.println("Error: the execution of the external program was interrupted");
            ie.printStackTrace();
        }
        return img_stream;
    }

    private File writeDotSourceToFile(String str, String dotfilename) throws java.io.IOException
    {
        File temp;
        try {
            temp = File.createTempFile("dorrr",".dot", new File(GraphViz.TEMP_DIR));
            System.out.println(temp.getName());
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
                       BufferedWriter br=new BufferedWriter(new FileWriter(dotfilename));
                       br.write(str);
                       br.flush();
                       br.close();
            fout.close();
        }
        catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return temp;
    }

    private File writeDotSourceToFile(String str) throws java.io.IOException
    {
        File temp;
        try {
            temp = File.createTempFile("dorrr",".dot", new File(GraphViz.TEMP_DIR));
            System.out.println(temp.getName());
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
                       BufferedWriter br=new BufferedWriter(new FileWriter("dotsource.dot"));
                       br.write(str);
                       br.flush();
                       br.close();
            fout.close();
        }
        catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return temp;
    }

    public String start_graph() {
        return "digraph G {";
    }

    public String end_graph() {
        return "}";
    }

    public String start_subgraph(int clusterid) {
        return "subgraph cluster_" + clusterid + " {";
    }

    public String end_subgraph() {
        return "}";
    }

    public void readSource(String input)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            FileInputStream fis = new FileInputStream(input);
            DataInputStream dis = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dis));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            dis.close();
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        this.graph = sb;
    }
}
