package wse.ne.reco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Use the query expansion method on the name entities
 * Use the name entity similarity to experiment on the cross-document referrence resolution
 */

public class NEExpansion {
  
  private Map<Integer, Map<Integer, Integer>> NECooccur;
  private Map<Integer, Map<Integer, Float>> NormNECooccur =
      new HashMap<Integer, Map<Integer, Float>>();
  private Map<Integer, String> NEIndex;
  private Map<String, Integer> NEDict;
  
  private class NEPair {
    public String ne1, ne2;
    
    NEPair(String ne1, String ne2) {
      this.ne1 = ne1;
      this.ne2 = ne2;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NEPair)) {
        return false;
      }
      if (obj == this) {
        return true;
      }

      NEPair pair = (NEPair) obj;
      return (pair.ne1.equals(this.ne1) && pair.ne2.equals(this.ne2)) ||
             (pair.ne1.equals(this.ne2) && pair.ne2.equals(this.ne1));
    }
  }
  
  private void normalize() {
    for (Integer key: NECooccur.keySet()) {
      int sum = 0;
      
      //normalize the co-occurrence vector
      for (Integer term: NECooccur.get(key).keySet()) {
        sum += NECooccur.get(key).get(term);
      }
      Map<Integer, Float> temp = new HashMap<Integer, Float>();
      for (Integer term: NECooccur.get(key).keySet()) {
        temp.put(term, (float)NECooccur.get(key).get(term) / sum);
      }
      NormNECooccur.put(key, temp);
    }
  }
  
  private Map<Integer, Float> getNormalizedMap(Map<Integer, Integer> map) {
    List<Integer> toRemove = new ArrayList<Integer>();
    Map<Integer, Float> r = new HashMap<Integer, Float>();
    int sum = 0;
    for (Integer i: map.keySet()) {
      if (map.get(i) < 10) {
        toRemove.add(i);
      } else {
        sum += map.get(i);
      }
    }
    
    for (Integer i: toRemove) {
      map.remove(i);
    }
    
    for (Integer i: map.keySet()) {
      r.put(i, (float)map.get(i) / (float)sum);
    }
    
    return r;
  }
  
  private float calculateSimilarity(Map<Integer, Float> map1, Map<Integer, Float> map2) {
    float sum_up = 0.0f, sum1 = 0.0f, sum2 = 0.0f;
    
    for (Integer ne1: map1.keySet()) {
      if (map2.containsKey(ne1)) {
        sum_up += map1.get(ne1) * map2.get(ne1);
      }
      sum1 += map1.get(ne1) * map1.get(ne1);
    }
    
    for (Integer ne2: map2.keySet()) {
      sum2 += map2.get(ne2) * map2.get(ne2);
    }
    
    return (float) (sum_up / (Math.sqrt(sum1) * Math.sqrt(sum2)));
  }
  
  public float getSimilarity(String ne1, String ne2) {
    int index1 = 0, index2 = 0;
    if (NEDict.containsKey(ne1.toLowerCase())) {
      index1 = NEDict.get(ne1.toLowerCase());
    } else {
      return 0.0f;
    }
    
    if (NEDict.containsKey(ne2.toLowerCase())) {
      index2 = NEDict.get(ne2.toLowerCase());
    } else {
      return 0.0f;
    }
    
    System.out.println("CALCULATE SIMILARITY:" + ne1 + " " + ne2);
    Map<Integer, Float> map1 = getNormalizedMap(NECooccur.get(index1));
    Map<Integer, Float> map2 = getNormalizedMap(NECooccur.get(index2));
    
    return calculateSimilarity(map1, map2);
  }
  
  public NEExpansion(Map<Integer, Map<Integer, Integer>> nemap,
                     Map<String, Integer> dict, 
                     Map<Integer, String> index) {
    NECooccur = nemap;
    NEDict = dict;
    NEIndex = index;
  }
}
