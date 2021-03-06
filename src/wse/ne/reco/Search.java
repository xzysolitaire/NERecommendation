package wse.ne.reco;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.json.simple.parser.ParseException;

import com.sun.net.httpserver.HttpServer;

public class Search {
  /**
   * Prints {@code msg} and exits the program if {@code condition} is false.
   */
  public static void Check(boolean condition, String msg) {
    if (!condition) {
      System.err.println("Fatal error: " + msg);
      System.exit(-1);
    }
  }
  
  public static Options OPTIONS = null;
  
  public static class Options {
    
    public String _corpusPrefix = null;

    public String _indexPrefix = null;
  
    public Options(String optionsFile) throws IOException {
      // Read options from the file.
      BufferedReader reader = new BufferedReader(new FileReader(optionsFile));
      Map<String, String> options = new HashMap<String, String>();
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        String[] vals = line.split(":", 2);
        if (vals.length < 2) {
          reader.close();
          Check(false, "Wrong option: " + line);
        }
        options.put(vals[0].trim(), vals[1].trim());
      }
      reader.close();

      // Populate global options.
      _corpusPrefix = options.get("corpus_prefix");
      Check(_corpusPrefix != null, "Missing option: corpus_prefix!");
      _indexPrefix = options.get("index_prefix");
      Check(_indexPrefix != null, "Missing option: index_prefix!");
    }
  }

  /**
   * Running mode of the search engine.
   */
  public static enum Mode {
    NONE, MINING, INDEX, SERVE,
  };

  public static Mode MODE = Mode.NONE;

  public static int PORT = -1;

  private static void parseCommandLine(String[] args) throws IOException,
      NumberFormatException {
    for (String arg : args) {
      String[] vals = arg.split("=", 2);
      String key = vals[0].trim();
      String value = vals[1].trim();
      if (key.equals("--mode") || key.equals("-mode")) {
        try {
          MODE = Mode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
          // Ignored, error msg will be printed below.
        }
      } else if (key.equals("--port") || key.equals("-port")) {
        PORT = Integer.parseInt(value);
      } else if (key.equals("--options") || key.equals("-options")) {
        OPTIONS = new Options(value);
      }
    }
    Check(MODE == Mode.SERVE || MODE == Mode.INDEX || MODE == Mode.MINING,
        "Must provide a valid mode: serve or index or mining!");
    Check(MODE != Mode.SERVE || PORT != -1,
        "Must provide a valid port number (258XX) in serve mode!");
    Check(OPTIONS != null, "Must provide options!");
  }

  private static void startIndexing() throws IOException, ClassCastException, ClassNotFoundException, ParseException, InterruptedException {
    IndexerKB indexer = new IndexerKB();
    indexer.buildIndex();
    indexer.constructIndex();
  }

  private static void startServing() throws IOException, ClassNotFoundException {
    // Create the handler and its associated indexer.
    IndexerKB indexer = new IndexerKB();  
    indexer.loadIndex();
    QueryHandlerKB handler = new QueryHandlerKB(indexer);
    MainpageHandler indexHander = new MainpageHandler();
    MWebHandler webFileHandler = new MWebHandler();
    // Establish the serving environment
    InetSocketAddress addr = new InetSocketAddress(Search.PORT);
    HttpServer server = HttpServer.create(addr, -1);

    server.createContext("/search", handler);
    server.createContext("/index", indexHander);
    server.createContext("/web", webFileHandler);
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Listening on port: "
        + Integer.toString(Search.PORT));
  }

  public static void main(String[] args) {
    try {
      Search.parseCommandLine(args);
      switch (Search.MODE) {
      case INDEX:
        startIndexing();
        break;
      case SERVE:
        startServing();
        break;
      default:
        Check(false, "Wrong mode for SearchEngine!");
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
