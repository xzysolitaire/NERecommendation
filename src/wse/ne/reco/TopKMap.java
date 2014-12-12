package wse.ne.reco;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/*
 * return the top k results of a map
 */

public class TopKMap {
  private static class keypair {
    int key, value;
    
    public keypair(int k, int v) {
      key = k;
      value = v;
    }
  }
  
  /*
   * Provide the top k results of the map
   */
  public static List<Integer> sortMap(int k, Map<Integer, Integer> map) {
    List<Integer> r = new ArrayList<Integer>();
    PriorityQueue<keypair> pq = new PriorityQueue<keypair>(k, new Comparator<keypair>(){
      @Override
      public int compare(keypair arg0, keypair arg1) {
        if (arg0.value < arg1.value) {
          return -1;
        } else {
          return 1;
        }
      }
    });
    
    if (k >= map.keySet().size()) {
      for (Integer key: map.keySet()) {
        pq.add(new keypair(key, map.get(key)));
      }      
    } else {
      for (Integer key: map.keySet()) {
        if (pq.size() < k) {
          pq.add(new keypair(key, map.get(key)));
        } else {
          if (pq.peek().value < map.get(key)) {
            pq.poll();
            pq.add(new keypair(key, map.get(key)));
          }
        }
      }
    }
    
    while (pq.peek() != null) {
      keypair temp = pq.poll();
      r.add(temp.key);
    }
    
    return r;
  }
}
