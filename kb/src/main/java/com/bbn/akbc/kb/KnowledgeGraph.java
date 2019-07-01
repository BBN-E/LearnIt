package com.bbn.akbc.kb;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Event;
import com.bbn.serif.theories.Event.Argument;
import com.bbn.serif.theories.Event.EntityArgument;
import com.bbn.serif.theories.Event.ValueArgument;
import com.bbn.serif.theories.Relation;

import java.io.File;
import java.io.IOException;

public class KnowledgeGraph {

  static void loadSerifXml(String strPathSerifXml) {
    DocTheory dt = null;
    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      File fileSerifXml = new File(strPathSerifXml);
      if (!fileSerifXml.exists()) {
        return;
      }
      dt = fromXML.loadFrom(fileSerifXml);
    } catch (IOException e) {
      e.printStackTrace();
    }
                /*
                String docText = dt.document().originalText().get().text();

		for(int i=0; i<dt.actorEntities().numActorEntities(); i++) {
			ActorEntity actorEntity = dt.actorEntities().actorEntity(i);
			for(int j=0; j<actorEntity.actorMentions().size(); j++) {
				ActorMention actorMention = actorEntity.actorMentions().get(j);
				actorEntity.
			}
		}
		*/

    for (int i = 0; i < dt.relations().numRelations(); i++) {
      Relation r = dt.relations().relation(i);

      // both arguments must have some name mention
      if (!r.leftEntity().hasNameMention() || !r.rightEntity().hasNameMention()) {
        continue;
      }

      String leftText =
          r.leftEntity().representativeMention().mention().atomicHead().span().tokenizedText().utf16CodeUnits();
      String rightText =
          r.rightEntity().representativeMention().mention().atomicHead().span().tokenizedText().utf16CodeUnits();
      leftText = normalizeText(leftText);
      rightText = normalizeText(rightText);

      System.out.println("reln" + "\t" + r.type().toString() + "\t" + leftText + "\t" + rightText);

/*
                        for(int j=0; j<r.relationMentions().size(); j++) {
				RelationMention rm = r.relationMentions().get(j);
				String leftText = rm.leftMention().atomicHead().span().tokenizedText();
				String rightText = rm.rightMention().atomicHead().span().tokenizedText();

				System.out.println("LEFT=" + leftText + ", RIGHT=" + rightText + ", TYPE=" + rm.type().toString());
			}
			System.out.println();
		*/
    }

    for (int i = 0; i < dt.events().numEvents(); i++) {
      Event e = dt.events().event(i);

      StringBuilder sb = new StringBuilder();
      sb.append("event" + "\t" + e.type().toString() + "\t" + e.tense().toString());

      // skip events that has no argument
      if (e.arguments().isEmpty()) {
        continue;
      }

      for (int j = 0; j < e.arguments().size(); j++) {
        Argument a = e.arguments().get(j);

        String bestName = "";
        if (a instanceof EntityArgument) {
          bestName = normalizeText(
              ((EntityArgument) a).entity().representativeMention().mention().atomicHead().span()
                  .tokenizedText().utf16CodeUnits());
        } else if (a instanceof ValueArgument) {
          bestName = normalizeText(((ValueArgument) a).value().span().tokenizedText().utf16CodeUnits());
        } else {
          System.err.println("event argument not recognized.");
          System.exit(-1);
        }

        sb.append("\t" + a.role() + ":" + bestName);
      }
      System.out.println(sb.toString());
    }
  }

  public static String normalizeText(String text) {
    return text.replace("\t", " ").replace("\r", " ").replace("\n", " ");
  }

  public static void main(String[] argv) {
    String strPathSerifXml = argv[0];
    loadSerifXml(strPathSerifXml);
  }
}
