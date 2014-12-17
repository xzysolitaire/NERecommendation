package wse.ne.reco;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.Header;

/**
 * @author Yasser Ganjisaffar <lastname at gmail dot com>
 */
public class BasicCrawler extends WebCrawler {

  private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
      + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

  /**
   * You should implement this function to specify whether the given url
   * should be crawled or not (based on your crawling logic).
   */
  public boolean shouldVisit(Page page, WebURL url) {
    String href = url.getURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith("http://www.ics.uci.edu/");
  }

  /**
   * This function is called when a page is fetched and ready to be processed
   * by your program.
   */
  @Override
  public void visit(Page page) {
    int docid = page.getWebURL().getDocid();
    String url = page.getWebURL().getURL();
    String domain = page.getWebURL().getDomain();
    String path = page.getWebURL().getPath();
    String subDomain = page.getWebURL().getSubDomain();
    String parentUrl = page.getWebURL().getParentUrl();
    String anchor = page.getWebURL().getAnchor();

    System.out.println("Docid: " + docid);
    System.out.println("URL: " + url);
    System.out.println("Domain: '" + domain + "'");
    System.out.println("Sub-domain: '" + subDomain + "'");
    System.out.println("Path: '" + path + "'");
    System.out.println("Parent page: " + parentUrl);
    System.out.println("Anchor text: " + anchor);

    File file =new File("data/crawler/urls");
    FileWriter fw;
    try {
      fw = new FileWriter(file,true);

      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(url);
      bw.write('\n');
      bw.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (page.getParseData() instanceof HtmlParseData) {
      System.out.println("Enter the if. ");
      HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
      String text = htmlParseData.getText();
      String html = htmlParseData.getHtml();
      Set<WebURL> links = (Set<WebURL>) htmlParseData.getOutgoingUrls();
      System.out.println("After links initialization. ");
      System.out.println("Text length: " + text.length());
      System.out.println("Html length: " + html.length());
      System.out.println("Number of outgoing links: " + links.size());
    }

    Header[] responseHeaders = page.getFetchResponseHeaders();
    if (responseHeaders != null) {
      System.out.println("Response headers:");
      for (Header header : responseHeaders) {
        System.out.println("\t" + header.getName() + ": " + header.getValue());
      }
    }else {
      System.out.println("Response headers is null. ");
    }

    System.out.println("=============");
  }
}