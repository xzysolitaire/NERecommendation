package wse.ne.reco;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;


// get text from espn, exclude all html format, 
// exclude insider section (http://insider.espn.go.com/)
// shell command: 
// cat allStories | grep -v 'http://insider.espn.go.com/' > new1
public class getAllHTML {

  static void genAllHTML (String urlPath) throws IOException {

    BufferedReader in = new BufferedReader
        ( new FileReader("data/crawler/"+urlPath) );
    String line;
    int count = 0; // count the lines
    while ((line = in.readLine()) != null) {  // line is one url
      if ( ! line.contains("/story/_")) {
        // not a news url
        continue;
      }
      count++;
      String[] url = line.split("/");
      Document doc = Jsoup.connect(line).
          timeout(30000).userAgent("Mozilla/17.0").get();
      String html = doc.html(); 
      File file =new File
          ("data/crawler/allHTML/"+url[url.length-1]);
      FileWriter fw;
      fw = new FileWriter(file,true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(html);
      bw.flush();
      bw.close();
      System.out.println("Processing No. "+count);
      System.out.println("get HTML: "+url[url.length-1]);
    }
    in.close();

    System.out.println("Done with getting all HTML. ");

  }

  static void genStories() throws IOException {

    File folder = new File("data/crawler/allHTML/");
    File[] listOfFiles = folder.listFiles();

    for (File file : listOfFiles) {
      if (file.isFile()) {
        BufferedReader br = new BufferedReader
            ( new FileReader(file.getAbsolutePath()) );
        String lineFile;
        String storyBody = "";
        int flag = 0;
        //        int citeFlag=0;
        //        int inline_imageFlag=0;
        while ((lineFile = br.readLine()) != null) {  // line is one url

          //          if (lineFile.contains("<p><div class=\"mod-inline image image-right ") ) {
          //            // to remove all inline image information
          //            System.out.println("remove all inline image information");
          //            System.out.println(lineFile);
          //            inline_imageFlag=1;
          //            continue;
          //          }
          //          if (inline_imageFlag==1 && lineFile.contains("<p><div class=\"mod-inline image image-right ") ) {
          //            // to remove all inline image information
          //            System.out.println("remove all inline image information");
          //            System.out.println(lineFile);
          //            inline_imageFlag=1;
          //            continue;
          //          }
          if (flag == 1) {
            storyBody+=lineFile;
            storyBody+='\n';
          }
          if (lineFile.contains("<!-- begin story body -->") ) {
            // begin of story body
            flag = 1;
            continue;
          }
          if (lineFile.contains("<!-- end story body -->") ) {
            // end of story body
            flag = 0;
            break; 
          }

          if (lineFile.contains("<!-- begin inline") ) {
            // remove other inline contents
            flag = 0;
            continue;
          }
          if (lineFile.contains("<!-- end inline") ) {
            // remove other inline contents
            flag = 1;
            continue;
          }

        }
        Document oneStory = Jsoup.parse(storyBody);

        Elements elements1 = oneStory.select("#page-actions-bottom");
        for (Element element : elements1) {
          element.replaceWith(new Element(Tag.valueOf("div"), ""));
        }
        Elements elements2 = oneStory.select("i");
        for (Element element : elements2) {
          element.replaceWith(new Element(Tag.valueOf("div"), ""));
        }
        Elements elements4 = oneStory.select("em");
        for (Element element : elements4) {
          element.replaceWith(new Element(Tag.valueOf("div"), ""));
        }
        Elements elements3 = oneStory.select("cite");
        for (Element element : elements3) {
          element.replaceWith(new Element(Tag.valueOf("div"), ""));
        }
        Elements elements5 = oneStory.select("h4");
        for (Element element : elements5) {
          element.replaceWith(new Element(Tag.valueOf("div"), ""));
        }
        Elements elements9 = oneStory.select("[class]");
        for (Element element : elements9) {
          element.replaceWith(new Element(Tag.valueOf("div"), ""));
        }

        String text = oneStory.body().text(); 
        System.out.println(file.getName());
        System.out.println(text);
        File final_out =new File
            ("data/stories/"+file.getName());
        FileWriter fw;
        fw = new FileWriter(final_out,true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(text);
        bw.flush();
        bw.close();
      }
    }


  }
  public static void main (String[] args) throws IOException {
    genAllHTML("urls");
    genStories();
  }

}
