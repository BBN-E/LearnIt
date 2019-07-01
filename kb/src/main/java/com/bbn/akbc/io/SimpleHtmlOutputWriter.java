package com.bbn.akbc.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class SimpleHtmlOutputWriter {

  private final Writer writer;
  private int collapseIdNum;

  private static final String headerContent =
      "\t<head>\n" +
          "\t<script src=\"http://code.jquery.com/jquery-1.11.0.min.js\"></script>\n" +
          "\t<style>\n" +
          "\t\t.collapse {\n" +
          "\t\t\tbox-shadow: 3px 3px 5px;\n" +
          "\t\t\tmargin: 5px;\n" +
          "\t\t\tborder: 1px solid black;\n" +
          "\t\t}\n" +
          "\t\t.collapsehead {\n" +
          "\t\t\tbackground-color: #9cf;\n" +
          "\t\t\tpadding: 2px;\n" +
          "\t\t\tcursor: pointer;\n" +
          "\t\t\tfont-weight: bolder;\n" +
          "\t\t\twhite-space: pre;\n" +
          "\t\t}\n" +
          "\t\t.collapsehead:hover {\n" +
          "\t\t\tbackground-color: #bdf\n" +
          "\t\t}\n" +
          "\t\t.collapsebody {\n" +
          "\t\t\tpadding: 2px;\n" +
          "\t\t\tmargin: 5px;\n" +
          "\t\t\tmargin-left: 20px;\n" +
          "\t\t}\n" +
          "\t\t.slot0 {\n" +
          "\t\t\tbackground-color: lightblue;\n" +
          "\t\t\tborder-bottom: 2px solid blue;\n" +
          "\t\t}\n" +
          "\t\t.slot1 {\n" +
          "\t\t\tbackground-color: lightgreen;\n" +
          "\t\t\tborder-top: 2px solid green;\n" +
          "\t\t}\n" +
          "\t</style>\n" +
          "\t</head>\n";


  public SimpleHtmlOutputWriter(File file)
      throws UnsupportedEncodingException, FileNotFoundException {
    this.writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
    this.collapseIdNum = 0;
  }

  public void start() throws IOException {
    writer.write("<html>\n\t" + headerContent + "\n\t<body>\n");
  }

  public void close() throws IOException {
    writer.write("\n\t</body>\n</html>");
    writer.close();
  }

  protected String sanitize(String input) {
    return input.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")
        .replace("\n", "<br />").replace("\t", "&nbsp;&nbsp;&nbsp;");
  }

  public void writeContent(String content) throws IOException {
    writer.write("<p>" + sanitize(content) + "</p>\n");
  }

  public void writeSlottedContent(String content) throws IOException {
    writer.write("<p>" + sanitize(content)
        .replace("&lt;SLOT0&gt;", "<span class=\"slot0\">")
        .replace("&lt;SLOT1&gt;", "<span class=\"slot1\">")
        .replace("&lt;/SLOT0&gt;", "</span>")
        .replace("&lt;/SLOT1&gt;", "</span>")
        + "</p>\n");
  }

  public void writeHtmlContent(String content) throws IOException {
    writer.write(content);
  }

  public void writeHeader(String text, int level) throws IOException {
    writer.write("<h" + level + ">" + sanitize(text) + "</h" + level + ">\n");
  }

  public void startCollapsibleSection(String header, boolean open) throws IOException {
    String closedText = open ? "" : "style=\"display: none\"";
    writer.write(String.format(
        "<div class=\"collapse\">\n\t" +
            "<div class=\"collapsehead\" onclick=\"$('#collapsebody%d').toggle('fast');\">%s</div>\n\t"
            +
            "<div id=\"collapsebody%d\" %s class=\"collapsebody\">\n",
        collapseIdNum, sanitize(header), collapseIdNum, closedText));

    collapseIdNum++;
  }

  public void endCollapsibleSection() throws IOException {
    writer.write("\n\t</div>\n</div>");
  }

}
