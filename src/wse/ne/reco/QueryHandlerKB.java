package wse.ne.reco;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import wse.ne.reco.QueryHandler.CgiArguments;
import wse.ne.reco.QueryHandler.CgiArguments.OutputFormat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class QueryHandlerKB implements HttpHandler {

  /**
   * CGI arguments provided by the user through the URL. This will determine
   * which Ranker to use and what output format to adopt. For simplicity, all
   * arguments are publicly accessible.
   */
  public static class CgiArguments {
    // The raw user query
    public String _query = "";
    // How many results to return
    private int _numResults = 10;

    // The output format.
    public enum OutputFormat {
      TEXT, HTML,
    }

    public OutputFormat _outputFormat = OutputFormat.TEXT;

    public CgiArguments(String uriQuery) {
      String[] params = uriQuery.split("&");
      for (String param : params) {
        String[] keyval = param.split("=", 2);
        if (keyval.length < 2) {
          continue;
        }
        String key = keyval[0].toLowerCase();
        String val = keyval[1];
        if (key.equals("query")) {
          _query = val;
        } else if (key.equals("num")) {
          try {
            _numResults = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("format")) {
          try {
            _outputFormat = OutputFormat.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        }
      } // End of iterating over params
    }
  }

  private IndexerKB indexer;

  public QueryHandlerKB(IndexerKB indexer) {
    this.indexer = indexer;
  }

  private void respondWithMsg(HttpExchange exchange, final String message)
      throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
    responseBody.close();
  }

  private void constructHtmlOutput(StringBuffer response, String query,
      List<String> results) throws IOException {
    BufferedReader reader = new BufferedReader(
        new FileReader("web/result.html"));
    String line = null;
    StringBuilder sb = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      sb.append(line);
      sb.append("\n");
    }
    reader.close();
    response.append(sb.toString() + "\n");
    response
        .append("<input type='text' id='tags' class='search-bar' name='query' value='"
            + query + "'></input>\n");
    response
        .append("<button type=\"button\" class=\"btn btn-default\" id=\"submit\">\n");
    response
        .append("<span class=\"glyphicon glyphicon-search\" aria-hidden=\"true\"></span>Search\n");
    response.append("</button>\n");
    response.append("</div></div>\n");
    // response.append("<h3>Related Entity</h3>");
    response.append("<table class=\"table table-striped\" >\n");
    if (results.size() == 0) {
      response.append("<caption>No Related Entity</caption><tbody>\n");
    } else {
      response.append("<caption>Related Entity</caption><tbody>\n");

      for (String r : results) {
        String res = r;
        if (r != null) { 
          if(r.split(" ").length>1){
            res = "";
            String[] temp = r.split(" ");
            for(String t: temp){
              String f = t.substring(0,1);
              String te = t.substring(1);
              f = f.toUpperCase() + te + " ";
              res += f;
            }
          } else {
            String head = res.substring(0,1);
            String tail = res.substring(1);
            res = "";
            res += head.toUpperCase() + tail;
          }
          res = res.trim();
          response.append("<tr><td><a href=\"/search?query=" + res + "&format=html\">").append(res).append("</a></td></tr>\n");
        }
      }
    }
    response.append("</tbody></table></body></html>");
  }

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
    }
    System.out.println();

    // Validate the incoming request.
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    if (uriPath == null || uriQuery == null) {
      respondWithMsg(exchange, "Something wrong with the URI!");
      return;
    }
    System.out.println(uriPath);
    if (uriPath.equals("/search") || uriPath.equals("/prf")) {
      CgiArguments cgiArgs = new CgiArguments(uriQuery);
      if (cgiArgs._query.isEmpty()) {
        respondWithMsg(exchange, "No query is given!");
      }
      System.out.println("Query: " + uriQuery);

      if (uriPath.equals("/search")) {

        StringBuffer response = new StringBuffer();
        List<Integer> results = indexer.entityRecommend(cgiArgs._query);
        List<String> newResults = indexer.test(cgiArgs._query);
        for (Integer r : results) {
          System.out.println(r);
        }
        switch (cgiArgs._outputFormat) {
        case TEXT:
          break;
        case HTML:
          // @CS2580: Plug in your HTML output
          constructHtmlOutput(response, cgiArgs._query, newResults);
          break;
        default:
          // nothing
        }
        respondWithMsg(exchange, response.toString());
        System.out.println("Finished query: " + cgiArgs._query);
      }
    } else {
      respondWithMsg(exchange, "Only /search or /prf is handled!");
    }

  }
}
