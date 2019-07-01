package com.bbn.akbc.evaluation.tac.io;

import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.evaluation.tac.CSInitQuery;
import com.bbn.akbc.evaluation.tac.Query;
import com.bbn.akbc.evaluation.tac.SFQuery;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.common.HTMLHelper;
import com.bbn.bue.common.xml.XMLUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class QueryReader {

  public static List<Query> readQueriesFromFile(String file) {
    return readQueriesFromFile(file, true);
  }

  public static List<Query> readQueriesFromFile(String file, boolean needAddDotSgmToFilePath) {
    return readQueriesFromFile(file, true, needAddDotSgmToFilePath);
  }

  public static List<Query> readQueriesFromFile(String file, boolean readHTMLForAnnotation,
      boolean needAddDotSgmToFilePath) {
    List<Query> queries = new ArrayList<Query>();

    try {
      String contents = Files.toString(new File(file), Charsets.UTF_8);
      final InputSource in = new InputSource(new StringReader(contents));

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      org.w3c.dom.Document xml = builder.parse(in);

      final Element root = xml.getDocumentElement();

      boolean isColdStartQueryFile = root.getTagName().equals("query_set");
      boolean isSlotFillingQueryFile = root.getTagName().equals("kbpslotfill");

      if (isColdStartQueryFile || isSlotFillingQueryFile) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
          if (child instanceof Element) {
//						targets.add(TargetFactory.fromElement((Element)child));
            if (isColdStartQueryFile) {
              Query queryRead = QueryReader.readColdStartInitQueryFromElement((Element) child);
              if (readHTMLForAnnotation) {
                queryRead.setTextForAnnotation(HTMLHelper
                    .getHTMLForAnnotation(queryRead, needAddDotSgmToFilePath)); // add HTML
              }
              queries.add(queryRead);
            } else if (isSlotFillingQueryFile) {
              queries.add(QueryReader.readSlotFillingQueryFromElement((Element) child));
            }
          }
        }

      } else {
        System.err.println(
            String.format("Don't know what to do with %s. Skipping file.", root.getTagName()));
      }
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return queries;
  }

  private static Query readSlotFillingQueryFromElement(Element query) {
    SFQuery.Builder builder = new SFQuery.Builder(XMLUtils.requiredAttribute(query, "id"));

    Optional<String> name = Optional.absent();
    Optional<String> docid = Optional.absent();
    Optional<String> enttype = Optional.absent();
    Optional<String> nodeid = Optional.absent();
    Optional<Integer> begin = Optional.absent();
    Optional<Integer> end = Optional.absent();

    for (Node child = query.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        String tag = ((Element) child).getTagName();
        String text = ((Element) child).getTextContent();
        if (tag.equals("name")) {
          name = Optional.of(text);
        } else if (tag.equals("docid")) {
          docid = Optional.of(text);
        } else if (tag.equals("beg")) {
          begin = Optional.of(Integer.parseInt(text));
        } else if (tag.equals("end")) {
          end = Optional.of(Integer.parseInt(text));
        } else if (tag.equals("enttype")) {
          enttype = Optional.of(text);
        } else if (tag.equals("nodeid")) {
          nodeid = Optional.of(text);
        } else if (tag.equals("ignore")) {
          String[] slots = text.trim().split(" ");
          for (String slot : slots) {
            builder.withAddedSlotToIgnore(SlotFactory.fromStringSlotName(slot));
          }
        }
      }
    }

    if (!name.isPresent() || !docid.isPresent() || !enttype.isPresent() // || !nodeid.isPresent() // we no longer have nodeID in SF2014 queries
        || !begin.isPresent() || !end.isPresent()) {
      System.err.println("Error reading query file");
      System.exit(-1);
    }

    TextMention mention = new TextMention(docid.get(), begin.get(), end.get());
    mention.setText(name.get());
    mention.setType(enttype.get());
    builder.withAddedMention(mention);

    return builder.build();
  }

  private static CSInitQuery readColdStartInitQueryFromElement(Element query) {
//		CSInitQuery.Builder builder = new CSInitQuery.Builder(XMLUtils.requiredAttribute(query, "id") + "_00"); // append hop info to initial query id
    CSInitQuery.Builder builder = new CSInitQuery.Builder(XMLUtils.requiredAttribute(query, "id"));

    List<String> names = new ArrayList<String>();
    List<String> docids = new ArrayList<String>();
    List<Integer> begins = new ArrayList<Integer>();
    List<Integer> ends = new ArrayList<Integer>();
    List<Integer> offsets = new ArrayList<Integer>();

    List<String> enttypes = new ArrayList<String>();

    for (Node child = query.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        String tag = ((Element) child).getTagName();
        String text = ((Element) child).getTextContent();
        if (tag.equals("name")) {
          names.add(text);
        } else if (tag.equals("docid")) {
          docids.add(text);
        } else if (tag.equals("beg")) {
          begins.add(Integer.parseInt(text));
        } else if (tag.equals("end")) {
          ends.add(Integer.parseInt(text));
        } else if (tag.equals("enttype")) {
          enttypes.add(text);
        } else if (tag.equals("offset")) {
          offsets.add(Integer.parseInt(text));
        } else if (tag.equals("slot0")) {
          // slot0 for queries coming from SF will contain a list of slots separated by " "
          List<String> slots = Arrays.asList(text.split(" "));
          for (String slot : slots) {
            builder.withAddedSlotHop0(SlotFactory.fromStringSlotName(slot));
          }
        } else if (tag.equals("slot1")) {
          builder.withAddedSlotHop1(SlotFactory.fromStringSlotName(text));
        }
      }
    }

    // make sure mentions have everything
    assert (names.size() == docids.size());
    assert (docids.size() == begins.size());
    assert (begins.size() == ends.size());
//		assert(ends.size()==offsets.size());

    for (int i = 0; i < names.size(); i++) {
      TextMention mention = new TextMention(docids.get(i), begins.get(i), ends.get(i));
      mention.setText(names.get(i));
      if (!offsets.isEmpty()) {
        mention.setAnchorOffset(offsets.get(i));
      }

      if(!enttypes.isEmpty()) {
        if(enttypes.size()>=i+1)
          mention.setType(enttypes.get(i));
        else
          mention.setText(enttypes.get(0));
      }

      builder.withAddedMention(mention);
    }

    return builder.build();
  }

  /*
   * Test
   */
  public static void main() {
    // Test 1: on CS2013 queries
    List<Query> queries = readQueriesFromFile(
        "/nfs/mercury-04/u10/KBP_ColdStart/2013/processed/query/tac_2013_kbp_english_cold_start_full_queries.xml");
    for (Query query : queries) {
      System.out.println(query.toString());
    }

    // Test 2: on SF2013 queries
    queries = readQueriesFromFile(
        "/nfs/mercury-04/u10/KBP_ColdStart/SF_resources/2013/ldc_release/LDC2013E77_TAC_2013_KBP_English_Regular_Slot_Filling_Evaluation_Queries_and_Annotations_V1.1/data/tac_2013_kbp_english_regular_slot_filling_evaluation_queries.xml");
    for (Query query : queries) {
      System.out.println(query.toString());
    }
  }
}
