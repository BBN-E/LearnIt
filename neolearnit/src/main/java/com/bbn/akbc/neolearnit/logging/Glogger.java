package com.bbn.akbc.neolearnit.logging;


import org.apache.logging.log4j.*;


/**
 * Created by bmin on 6/18/15.
 */
public class Glogger {
  static boolean isInitialized = false;
  static Logger logger;

  public static Logger logger() {
    if(!isInitialized) {
      init();
      isInitialized = true;
    }

    return logger;
  };

  public static void init() {
    logger = LogManager.getLogger("GLOBAL");
    logger.entry();
  }
}
