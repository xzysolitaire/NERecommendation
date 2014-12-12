package wse.ne.reco;

import java.util.HashMap;
import java.util.Map;

/*
 * Use the query expansion method on the name entities
 * Use the name entity similarity to experiment on the cross-document referrence resolution
 */

public class NEExpansion {
  
  private Map<Integer, Map<Integer, Integer>> NECooccur =
      new HashMap<Integer, Map<Integer, Integer>>();
  private Map<Integer, Map<Integer, Float>> NormNECooccur =
      new HashMap<Integer, Map<Integer, Float>>();
  
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
    
    return sum_up / (sum1 * sum2);
  }
  
  public NEExpansion(Map<Integer, Map<Integer, Integer>> nemap) {
    NECooccur = nemap;
  }
}
