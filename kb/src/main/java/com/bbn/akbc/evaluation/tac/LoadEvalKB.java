package com.bbn.akbc.evaluation.tac;

import java.io.IOException;


public class LoadEvalKB {

  public static void main(String[] argv) throws IOException {
    String fileQuery = argv[0];
    String fileAssessment = argv[1];
    String fileSysKbAligned = argv[2];
    String evalLog = argv[3];
    String sysKb = argv[4];

    EvalKB evalKB = new EvalKB(fileQuery, fileAssessment, fileSysKbAligned, evalLog, sysKb);
    evalKB.print();
  }


}

