package com.bbn.akbc.common;

import java.io.File;

public class Util {

  public static String normalizedEntityId(String entityId) {
    return entityId.replace(":", "").replace("/", "_").replace("\\", "");
  }

  public static String make_sub_directories_for_entities(String fileVisualDir) {
    String fileDirEntities = fileVisualDir + "/entities/";

    File fDirEntities = new File(fileDirEntities);
    if (!fDirEntities.exists()) {
      fDirEntities.mkdir();
    }

    return fileDirEntities;
  }
}
