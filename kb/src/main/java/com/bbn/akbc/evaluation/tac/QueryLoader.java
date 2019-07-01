package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.evaluation.tac.io.QueryReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryLoader {
  public static Map<String, CSInitQuery> id2queryEntities;

  public static Map<String, CSInitQuery> load(String fileQuery) throws IOException {
    id2queryEntities = new HashMap<String, CSInitQuery>();

    List<Query> queries = QueryReader.readQueriesFromFile(fileQuery, false);
    for (Query query : queries) {
      if (query instanceof CSInitQuery) {
        id2queryEntities.put(query.id, (CSInitQuery) query);
      }
    }

    return id2queryEntities;
  }
}
