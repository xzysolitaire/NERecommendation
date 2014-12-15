package wse.ne.reco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.parser.ParseException;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

/*
 * In this indexer, we build the name entity dictionary based on the Freebase
 */

public class IndexerKB implements Serializable {
  
  private static final String sourceDir = "data/testdata";
  private int numResults;
  private boolean isChanged;  //record whether the current index has been modified
  
  //the classifier file 
  private static final String serializedClassifier = 
      "data/english.conll.4class.distsim.crf.ser.gz";
  private AbstractSequenceClassifier<CoreLabel> classifier;
  
  //Name entity dictionary
  private static Map<String, Integer> NEDict = new HashMap<String, Integer>();
  private static Map<Integer, String> NEIndex = new HashMap<Integer, String>();
  
  //the map of recording the co-occurrence relation between name entities 
  private static Map<Integer, Map<Integer, Integer>> NECooccur =
      new HashMap<Integer, Map<Integer, Integer>>();
  
  //provide a link from each word of a name to a full name
  //used when there is perfect match for the name entity in the query
  private static Map<String, Set<Integer>> nameLink =
      new HashMap<String, Set<Integer>>();
  
  //get name entities from Freebase 
  private GetFreebaseNE KB = new GetFreebaseNE(); 
  
  public void constructIndex() throws IOException {
    String indexFile = "index/final.idx";
    ObjectOutputStream writer = 
        new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();
    System.out.println("Store index to: " + indexFile);
  }

  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = "index/kb.final.idx";
    System.out.println("Load index from: " + indexFile);
    ObjectInputStream reader = 
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerKB loaded = (IndexerKB) reader.readObject();

    this.NEDict = loaded.NEDict;
    this.NEIndex = loaded.NEIndex;
    this.NECooccur = loaded.NECooccur;
    this.nameLink = loaded.nameLink;
    this.KB = loaded.KB;
    reader.close();
    isChanged = false;
  }

  /*
   * Return all files under a certain directory
   */
  private List<File> getFilesUnderDirectory(String directoryPath)
      throws IOException {
    File root = new File(directoryPath);
    List<File> files = new ArrayList<File>();
    if (!root.isDirectory()) {
      throw new IOException("The corpus path " + directoryPath
          + " is not a directory!");
    } else {
      File[] subfiles = root.listFiles();
      for (File f : subfiles) {
        if (!f.isDirectory()) {
          files.add(f);
        } else {
          files.addAll(getFilesUnderDirectory(f.getAbsolutePath()));
        }
      }
    }
    return files;
  }
  
  /*
   * Build the Co-occurrence index for the source texts
   */
  private void buildIndex() throws IOException, ParseException, InterruptedException {
    List<File> files = new ArrayList<File>();
    
    //string set for each document 
    List<Set<String>> docNE = new ArrayList<Set<String>>();
    try {
      files = getFilesUnderDirectory(sourceDir);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    int files_size = files.size();
    System.out.println("Total Document Size " + files_size);

    for (int i = 0; i < files.size(); i++) {
      System.out.println("Processing document:" + files.get(i).getName());
      BuildOneDoc(files.get(i).getPath(), docNE);
    }
    
    //cross-document resolution
    crossResolution(docNE);
    
    for (Set<String> entities: docNE) {
      Set<Integer> temp = new HashSet<Integer>();
      for (String entity: entities) {
        if (NEDict.containsKey(entity)) {
          temp.add(NEDict.get(entity)); 
        }
      }
      processCooccurrence(temp);
    }
    
    isChanged = true;
  }
  
  /*
   * Process the given document and update the index
   */
  private void BuildOneDoc(String fileLoc, List<Set<String>> docNE) throws IOException, ParseException, InterruptedException {
    BufferedReader br = new BufferedReader(new FileReader(fileLoc));
    Set<String> entities = new HashSet<String>();
    Set<String> formalNE = new HashSet<String>();
    String line;

    while ((line = br.readLine()) != null) {
      String taggedtext = classifier.classifyWithInlineXML(line);
      entities.addAll(getEntities(taggedtext));
    }
    
    //update the NEDict and NEIndex based on Freebase, add the nameLink
    for (String entity: entities) {
      if (!NEDict.containsKey(entity)) {
        if (KB.isFormalNE(entity)) {
          //Thread.sleep(100);
          NEDict.put(entity, NEDict.size());
          NEIndex.put(NEDict.size() - 1, entity);
          formalNE.add(entity);
          
          //build the inverted index from terms to NE
          if (entity.split(" ").length > 1) {
            String[] temp = entity.split(" ");
            int index = NEDict.get(entity);
            for (int i = 0; i < temp.length; i++) {
              if (nameLink.containsKey(temp[i])) {
                nameLink.get(temp[i]).add(index);
              } else {
                Set<Integer> s = new HashSet<Integer>();
                s.add(index);
                nameLink.put(temp[i], s);
              }
            }
          }
        }
      }
    }
    
    //in-document resolution based on the textual feature
    Set<String> toRemove = new HashSet<String>();
    for (String entity: entities) {
      for (String ne: formalNE) {
        if (ne.contains(entity) && !ne.equals(entity)) {
          int index = NEDict.get(ne);
          toRemove.add(entity);
          
          String[] temp = entity.split(" ");
          for (int i = 0; i < temp.length; i++) {
            if (nameLink.containsKey(temp[i])) {
              nameLink.get(temp[i]).add(index);
            } else {
              Set<Integer> s = new HashSet<Integer>();
              s.add(index);
              nameLink.put(temp[i], s);
            }      
          }      
        }
      }
    }
    
    for (String entity: toRemove) {
      entities.remove(entity);
    }
    docNE.add(entities);
  }
  
  /*
   * Get all the name entities in the text
   */
  public Set<String> getEntities(String text) {
    Set<String> entityset = new HashSet<String>();

    Pattern p = Pattern.compile("<[A-Z]+>.+?</[A-Z]+>");
    Pattern rmTag = Pattern.compile("</?[A-Z]+>");
    Matcher m = p.matcher(text);

    while (m.find()) {
      Matcher tm = rmTag.matcher(m.group()); // remove tags
      String temp = tm.replaceAll("");

      if (!entityset.contains(temp.toLowerCase())) {
        entityset.add(temp.toLowerCase());
      }
    }

    return entityset;
  }

  /*
   * Shallow cross-document resolution
   * if the term only links to one name entity in the corpus,
   *   then merge these two maps
   */
  void crossResolution(List<Set<String>> docNE) throws IOException, ParseException, InterruptedException {
    for (Set<String> entities: docNE) {
      Set<String> toRemove = new HashSet<String>();
      for (String entity: entities) {
        if (!NEDict.containsKey(entity)) {
          toRemove.add(entity);
          String KBne = KB.getNE(entity);
          //Thread.sleep(100);
          if (NEDict.containsKey(KBne)) { 
            if (!entities.contains(KBne)) {
              entities.add(KBne);
            }
          }
        }
      }
      
      for (String rm: toRemove) {
        entities.remove(rm);
      }
    }
  }
  
  /*
   * Given the name entities, process the relation of cooccurrence in the same
   * text
   */
  private void processCooccurrence(Set<Integer> entities) {
    List<Integer> l = new ArrayList<Integer>();
    for (Integer entity : entities) {
      l.add(entity);
      if (!NECooccur.containsKey(entity)) {
        Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
        NECooccur.put(entity, temp);
      }
    }

    // update the co-occurrence map
    for (int i = 0; i < l.size() - 1; i++) {
      for (int j = i + 1; j < l.size(); j++) {
        int n_i = l.get(i);
        int n_j = l.get(j);
        if (NECooccur.get(n_i).containsKey(n_j)) {
          int occur = NECooccur.get(n_i).get(n_j) + 1;
          NECooccur.get(n_i).put(n_j, occur);
        } else {
          NECooccur.get(n_i).put(n_j, 1);
        }

        if (NECooccur.get(n_j).containsKey(n_i)) {
          int occur = NECooccur.get(n_j).get(n_i) + 1;
          NECooccur.get(n_j).put(n_i, occur);
        } else {
          NECooccur.get(n_j).put(n_i, 1);
        }
      }
    }
  }
  
  /*
   * provide recommendation results due to given query
   */
  public List<Integer> entityRecommend(String query) {
    List<Integer> recoResults = new ArrayList<Integer>();
    query = query.toLowerCase();

    // if it is in the NECooccur map, directly return the most co-occurred
    // entities
    if (NEDict.containsKey(query)) {
      int queryid = NEDict.get(query);
      return TopKMap.sortMap(numResults, NECooccur.get(queryid));
    } else {
      //if there is a ne link in the corpus for the query
      if (nameLink.containsKey(query)) {
        if (nameLink.get(query).size() == 1) {
          Integer ne = null;
          for (Integer e: nameLink.get(query)) {
            ne = e;
          }
          return entityRecommend(NEIndex.get(ne));
        } else {
          for (Integer index: nameLink.get(query)) {
            recoResults.add(index);
          }
          return recoResults;
        }
      }
      
      if (query.split(" ").length > 1) { 
        int j = 0;
        List<Integer> r = new ArrayList<Integer>();
        String[] temp = query.split(" ");
        for (int i = 0; i < temp.length; i++) {
       // get the intersection of all the entities'. name links
          if (nameLink.containsKey(temp[i])) { 
            for (Integer term : nameLink.get(temp[i])) {
              r.add(term);
            }
            j = i;
            break;
          }
        }

        for (int i = j + 1; i < temp.length; i++) {
          if (nameLink.containsKey(temp[i])) {
            r = getIntersection(r, nameLink.get(temp[i]));
          }
        }

        for (int i = 0; i < temp.length; i++) {
          if (NECooccur.containsKey(temp[i])) {
            r.add(NEDict.get(temp[i]));
          }
        }

        return r;
      } else {
        if (nameLink.containsKey(query)) { // if there exists a name link
          if (nameLink.get(query).size() == 1) { // if there only exists one
                                                 // link
            Integer link = 0;
            for (Integer term : nameLink.get(query)) {
              link = term;
            }
            return TopKMap.sortMap(numResults, NECooccur.get(link));
          } else {
            for (Integer ne : nameLink.get(query)) {
              recoResults.add(ne);
            }
            return recoResults;
          }
        } else { // if not, return an empty list
          return recoResults;
        }
      }
    }
  }
  
  /*
   * Get the intersection of a list and a set
   */
  private List<Integer> getIntersection(List<Integer> list, Set<Integer> set) {
    List<Integer> r = new ArrayList<Integer>();

    for (Integer term : list) {
      if (set.contains(term)) {
        r.add(term);
      }
    }

    return r;
  }
  
  /*
   * Predefine some name entities
   */
  void predefineNE() {
    NEDict.put("nba", 0);
    NEIndex.put(0, "nba");
  }
  
  public IndexerKB() {
    try {
      classifier = CRFClassifier.getClassifier(serializedClassifier);
      this.numResults = 20;
    } catch (ClassCastException | ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public IndexerKB(int numResults) {
    try {
      classifier = CRFClassifier.getClassifier(serializedClassifier);
      this.numResults = numResults;
      this.isChanged = true;
    } catch (ClassCastException | ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void main(String[] args) throws IOException, ParseException, InterruptedException {
    IndexerKB indexer = new IndexerKB(20);
    indexer.buildIndex();
    List<Integer> results = new ArrayList<Integer>();
    
//    for (int i = 0; i < NEIndex.size(); i++) {
//      System.out.println(NEIndex.get(i));
//    }
//    System.out.println(NEDict.containsKey("NBA"));
//    System.out.println(nameLink.containsKey("Los Angeles"));
//    for (Integer ne: nameLink.get("james")) {
//      System.out.println(NEDict.get(ne));
//    }
//    
    System.out.println(NECooccur.size());
    System.out.println(NECooccur.get(NEDict.get("chicago bulls")).keySet().size());
    for (Integer index: NECooccur.get(NEDict.get("chicago bulls")).keySet()) {
      System.out.println(NEIndex.get(index) + "\t" + NECooccur.get(NEDict.get("chicago bulls")).get(index));
    }
    System.out.println(nameLink.get("bulls").size());
    System.out.println("\n\nTEST CASE FOR: Chicago Bulls");
    results = indexer.entityRecommend("chicago Bulls");
    for (Integer r: results) {
      System.out.println(NEIndex.get(r));
    }

    System.out.println("\n\nTEST CASE FOR: Bulls");
    results = indexer.entityRecommend("Bulls");
    for (Integer r: results) {
      System.out.println(NEIndex.get(r));
    }
//    
//    System.out.println("\n\nTEST CASE FOR: Damian Lillard");
//    results = indexer.entityRecommend("Damian Lillard");
//    for (Integer r: results) {
//      System.out.println(NEIndex.get(r));
//    }
//    
//    System.out.println("\n\nTEST CASE FOR: Los Angeles Lakers");
//    results = indexer.entityRecommend("Los Angeles Lakers");
//    for (Integer r: results) {
//      System.out.println(NEIndex.get(r));
//    }
//    
//    System.out.println("\n\nTEST CASE FOR: Houston Rockets");
//    results = indexer.entityRecommend("Houston Rockets");
//    for (Integer r: results) {
//      System.out.println(NEIndex.get(r));
//    }
//    
//    System.out.println("\n\nTEST CASE FOR: lebron james harden");
//    results = indexer.entityRecommend("lebron james harden");
//    for (Integer r: results) {
//      System.out.println(NEIndex.get(r));
//    }
//
//    System.out.println("\n\nTEST CASE FOR: Lakers");
//    results = indexer.entityRecommend("Lakers");
//    for (Integer r: results) {
//      System.out.println(NEIndex.get(r));
//    }
//
//    System.out.println("\n\nTEST CASE FOR: Los Angeles");
//    results = indexer.entityRecommend("Los Angeles");
//    for (Integer r: results) {
//      System.out.println(NEIndex.get(r));
//    }
  }
}
