package com.bbn.akbc.actor;

import com.bbn.akbc.common.FileUtil;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * Created by bmin on 7/6/16.
 */
public class ActorID2entityType {
  ImmutableMap<Integer, String> actorId2entType;

  public ActorID2entityType() {
    ImmutableMap.Builder<Integer, String> mapBuilder = ImmutableMap.builder();

    List<String> lines = FileUtil.readLinesIntoList("/nfs/mercury-04/u42/bmin/repositories/svn/source/Active/Projects/AWAKE.bak.20160404/experiments/run_awake_pipeline/sequences/bmin_freebase_cs2015_full_corpus_v1.after_run_awake.sqlite.table_actor.tabs");
    for(String line : lines) {
      if(line.trim().isEmpty())
        continue;

      String [] items = line.trim().split("\t");

      int actorId = Integer.parseInt(items[0]);
      String entityType = items[2];
      mapBuilder.put(actorId, entityType);
    }

    actorId2entType = mapBuilder.build();
  }

  public Optional<String> getEntType(int actorId) {
    if(!actorId2entType.containsKey(actorId))
      return Optional.absent();
    else
      return Optional.of(actorId2entType.get(actorId));
  }

  public Optional<String> getEntType(long actorId) {
    return getEntType((int) actorId);
  }
}
