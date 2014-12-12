package wse.ne.reco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ling.CoreLabel;

/*
 * Name entity linking problem:
 * 1. consult tothe knowledge base: Google Freebase API 
 * 2. name entity resolution in the same document
 * 3. query expansion --> name entity similarity
 */

public class Indexer {
  private static final String WORKDIR = ""; 
  private static final String sourceDir = "data/testdata";
  private int numResults;
  private boolean isChanged;  //record whether the current index has been modified
  
  //the classifier file 
  private static final String serializedClassifier = 
      "data/english.conll.4class.distsim.crf.ser.gz";
  private AbstractSequenceClassifier<CoreLabel> classifier;
  
  //Name entity dictionary
  private Map<String, Integer> NEDict = new HashMap<String, Integer>();
  private Map<Integer, String> NEIndex = new HashMap<Integer, String>();
  
  //the map of recording the co-occurrence relation between name entities 
  private static Map<Integer, Map<Integer, Integer>> NECooccur =
      new HashMap<Integer, Map<Integer, Integer>>();
  
  //provide a link from each word of a name to a full name
  //used when there is perfect match for the name entity in the query
  private Map<String, Set<Integer>> nameLink =
      new HashMap<String, Set<Integer>>();
  
  /*
   * Record the document ids where the name entity appears
   */
  private Map<String, List<Integer>> NEDoc = 
      new HashMap<String, List<Integer>>();
  
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
  private void buildIndex() throws IOException {
    List<File> files = new ArrayList<File>();
    try {
      files = getFilesUnderDirectory(sourceDir);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    int files_size = files.size();
    System.out.println("Total Document Size " + files_size);
    
    for (int i = 0; i < files.size(); i++) {
      System.out.println("Processing document:" + files.get(i).getName());
      BuildOneDoc(files.get(i).getPath());
    }
    
    isChanged = false;
  }
  
  /*
   * Load the index from the disk 
   */
  public void loadIndex() {
    
    isChanged = false;
  }
  
  /*
   * Write the index into disk 
   */
  public void writeIndexToFile() {
    if (isChanged) {
        
    }
    
    isChanged = false;
  }
  
  /*
   * Process the given document and update the index
   */
  private void BuildOneDoc(String fileLoc) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(fileLoc));
    Set<String> entities = new HashSet<String>();
    String line;
    
    while ((line = br.readLine()) != null) {
      String taggedtext = classifier.classifyWithInlineXML(line);
      entities.addAll(getEntities(taggedtext));
    }
    
    //entity linking by in-document resolution 
    Set<String> resolvedEntities = entityLinkingResolution(entities);
    
    //update the dictionary
    Set<Integer> entityIndexes = new HashSet<Integer>();
    for (String term: resolvedEntities) {
      if (!NEDict.containsKey(term)) {
        NEDict.put(term.toLowerCase(), NEDict.size());
        NEIndex.put(NEDict.size() - 1, term.toLowerCase());
      }
      
      entityIndexes.add(NEDict.get(term.toLowerCase()));
    }
    
    if (resolvedEntities.size() > 1) {
      processCooccurrence(entityIndexes);
    }
  }
  
  /*
   * Add one file to the whole text 
   */
  public void addFile(String fileLoc) throws IOException {
    BuildOneDoc(fileLoc);
    isChanged = true;
    
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
      Matcher tm = rmTag.matcher(m.group()); //remove tags
      String temp = tm.replaceAll("");
      
      if (!entityset.contains(temp.toLowerCase())) {
        entityset.add(temp.toLowerCase());
      }
    }
    
    return entityset;
  }
  
  /*
   * in-document resolution to remove duplicate entities
   */
  private Set<String> entityLinkingResolution(Set<String> entities) {
    Set<String> r = new HashSet<String>();
    
    //put name entities which has a name longer than 2
    for (String term: entities) {
      if (term.split(" ").length > 1) {
        if (!r.contains(term)) {
          r.add(term);
        }
        
        String[] temp = term.split(" ");
        for (int i = 0; i < temp.length; i++) {
          if (nameLink.containsKey(temp[i])) {
            if (!nameLink.get(temp[i].toLowerCase()).contains(NEDict.get(term))) {
              nameLink.get(temp[i].toLowerCase()).add(NEDict.get(term));
            }
          } else {
            Set<Integer> s = new HashSet<Integer>();
            s.add(NEDict.get(term));
            nameLink.put(temp[i].toLowerCase(), s);
          }
        }
      } 
    }
    
    for (String term: entities) {
      if (term.split(" ").length == 1) {
        if (!nameLink.containsKey(term)) {
          r.add(term);
          Set<Integer> temp = new HashSet<Integer>();
          temp.add(NEDict.get(term));
          nameLink.put(term, temp); //add a link to itself
        }
      }
    }
    
    return r;
  }
  
  /*
   * Given the name entities, process the relation of cooccurrence in the same text
   */
  private void processCooccurrence(Set<Integer> entities) {
    List<Integer> l = new ArrayList<Integer>();
    for (Integer name: entities) {
      l.add(name);
      
      if (!NECooccur.containsKey(name)) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        NECooccur.put(name, map);
      }
    }
    
    //update the co-occurrence map
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
    
    //if it is in the NECooccur map, directly return the most co-occurred entities
    if (NEDict.containsKey(query)) {
      int queryid = NEDict.get(query);
      return TopKMap.sortMap(numResults, NECooccur.get(queryid));
    } else {
      if (query.split(" ").length > 1) { //provide the intersection of the lists
        int j = 0;
        List<Integer> r = new ArrayList<Integer>();
        String[] temp = query.split(" ");
        for (int i = 0; i < temp.length; i++) {
          if (nameLink.containsKey(temp[i])) { //get the intersection of all the entities name links
            for (Integer term: nameLink.get(temp[i])) {
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
        if (nameLink.containsKey(query)) { //if there exists a name link
          if (nameLink.get(query).size() == 1) { //if there only exists one link
            Integer link = 0;
            for (Integer term: nameLink.get(query)) {
              link = term;
            }
            return TopKMap.sortMap(numResults, NECooccur.get(link));
          } else {
            for (Integer ne: nameLink.get(query)) {
              recoResults.add(ne);
            }
            return recoResults;
          }
        } else {   //if not, return an empty list
          return recoResults;
        }
      }
    }
  }
  
  /*
   * Show results
   */
  public void showResults(String query, List<Integer> results) {
    if (NEDict.containsKey(query)) { 
      System.out.println("You might also be insterested in...");
      for (Integer index: results) {
        System.out.println(NEIndex.get(index));
      }
    } else {
      if (results.size() == 0) {
        System.out.println("There is no such name entity.");
      } else {
        System.out.println("Do you mean by ...?");
        for (Integer index: results) {
          System.out.println(NEIndex.get(index));
        }
      }
    }
  }
  
  
  /*
   * Get the intersection of a list and a set
   */
  private List<Integer> getIntersection(List<Integer> list, Set<Integer> set) {
    List<Integer> r = new ArrayList<Integer>();
    
    for (Integer term: list) {
      if (set.contains(term)) {
        r.add(term);
      }
    }
    
    return r;
  }

  public Indexer() throws ClassCastException, ClassNotFoundException, IOException {
    classifier = CRFClassifier.getClassifier(serializedClassifier);
    this.numResults = 20; 
  }
  
  public Indexer(int numResults) throws ClassCastException, ClassNotFoundException, IOException {
    classifier = CRFClassifier.getClassifier(serializedClassifier);
    this.numResults = numResults; 
    this.isChanged = true;
  }
  
  public void listInformation() {
//    System.out.println("There are overall " + NECooccur.keySet().size() + " entities.");
//    for (String term: NECooccur.get("andrew wiggins").keySet()) {
//      System.out.println(term);
//      System.out.println(NECooccur.get("andrew wiggins").get(term));
//    }
//    for (String term: nameLink.get("hawk")) {
//      //System.out.println(term);
//    }
  }
  
  public static void main(String[] args) {
    try {
      Indexer indexer = new Indexer(20);
      indexer.buildIndex();
      indexer.listInformation();
      List<Integer> results = indexer.entityRecommend("indiana pacers");
      System.out.println(results.size());
      for (Integer s: results) {
        System.out.println(s);
        //System.out.println(NECooccur.get("Andrew Wiggins").get(s));
      }
    } catch (ClassCastException | ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }
  }
}
