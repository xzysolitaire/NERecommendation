package wse.ne.reco;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.jayway.jsonpath.JsonPath;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * Get relative name entity list from the Freebase API
 */
public class GetFreebaseNE {
  private HttpTransport httpTransport = new NetHttpTransport();
  private HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
  private JSONParser parser = new JSONParser();
  public static Properties properties = new Properties();
  
  /*
   * return whether this name entity is one in the Freebase
   */
  public boolean isFormalNE(String query) throws IOException, ParseException, InterruptedException {
    if (getNE(query) == null) {
      return false;
    } else {
      return query.toLowerCase().equals(getNE(query).toLowerCase());
    }
  }
  
  /*
   * We only consider the most likely related name entity
   */
  public String getNE(String query) throws IOException, ParseException, InterruptedException {
    Thread.sleep(150);
    List<String> r = new ArrayList<String>();
    GenericUrl url = new GenericUrl("https://www.googleapis.com/freebase/v1/search");
    url.put("query", query);
    url.put("limit", "1");
    url.put("key", "AIzaSyAetx3nngxEG63CZqhfL8B1IHytxrG7w6E");
    HttpRequest request = requestFactory.buildGetRequest(url);
    HttpResponse httpResponse = request.execute();
    JSONObject response = (JSONObject)parser.parse(httpResponse.parseAsString());
    JSONArray results = (JSONArray)response.get("result");    
    
    for (Object result: results) {
      r.add(JsonPath.read(result, "$.name").toString());
    }
    
    if (r.size() == 0) {
      return null;
    } else {
      return r.get(0); 
    }
  }
  
  public static void main(String[] args) {
    try {
      GetFreebaseNE test = new GetFreebaseNE();
      System.out.println(test.getNE("channing frye"));
      //System.out.println(test.isFormalNE("Lakers"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
