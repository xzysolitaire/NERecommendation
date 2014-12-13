package wse.ne.reco;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class SearchEngine {

  /**
   * Prints {@code msg} and exits the program if {@code condition} is false.
   */
  public static void Check(boolean condition, String msg) {
    if (!condition) {
      System.err.println("Fatal error: " + msg);
      System.exit(-1);
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
        //OPTIONS = new Options(value);
      }
    }
    Check(MODE == Mode.SERVE || MODE == Mode.INDEX || MODE == Mode.MINING,
        "Must provide a valid mode: serve or index or mining!");
    Check(MODE != Mode.SERVE || PORT != -1,
        "Must provide a valid port number (258XX) in serve mode!");
    //Check(OPTIONS != null, "Must provide options!");
  }

  // /// Main functionalities start

  private static void startMining() throws IOException,
      NoSuchAlgorithmException {
    return;
  }

  private static void startIndexing() throws IOException, ClassCastException, ClassNotFoundException {
    Indexer indexer = new Indexer();
    //indexer.constructIndex();
  }

  private static void startServing() throws IOException, ClassNotFoundException {
    // Create the handler and its associated indexer.
    Indexer indexer = new Indexer();
    
    indexer.loadIndex();
    QueryHandler handler = new QueryHandler(indexer);
    MainpageHandler indexHander = new MainpageHandler();
    MWebHandler webFileHandler = new MWebHandler();
    // Establish the serving environment
    InetSocketAddress addr = new InetSocketAddress(SearchEngine.PORT);
    HttpServer server = HttpServer.create(addr, -1);

    server.createContext("/search", handler);
    server.createContext("/index", indexHander);
    server.createContext("/web", webFileHandler);
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Listening on port: "
        + Integer.toString(SearchEngine.PORT));
  }

  public static void main(String[] args) {
    try {
      SearchEngine.parseCommandLine(args);
      switch (SearchEngine.MODE) {
      case MINING:
        startMining();
        break;
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
