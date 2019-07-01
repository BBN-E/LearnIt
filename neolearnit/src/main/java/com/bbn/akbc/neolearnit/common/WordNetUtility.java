package com.bbn.akbc.neolearnit.common;

/**
 * Created by bmin on 6/4/15.
 */
public class WordNetUtility {
    /*
  public static final Symbol NN = Symbol.from("NN");
  public static final Symbol NNP = Symbol.from("NNP");
  public static final Symbol NNS = Symbol.from("NNS");
  public static final Symbol NNPS = Symbol.from("NNPS");

  static WordNet wordNet = null;

  private static void initializeWordNet() {
    try {
      String paramFile = "/nfs/mercury-04/u42/bmin/everything/projects/ere/wordnet.params";
      Parameters params = Parameters.loadSerifStyle(new File(paramFile));
      wordNet = WordNet.fromParameters(params);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static Optional<String> getSynsetID(String word, Symbol pos) {
    if(wordNet == null)
      initializeWordNet();

    Optional<ISynset> iSynsetOptional = wordNet.getFirstSynSetOfWord(Symbol.from(word), pos);
    if(iSynsetOptional.isPresent())
      return Optional.of(iSynsetOptional.get().getID().toString());

    return Optional.absent();
  }

  public static Optional<String> getHypernym(String word, Symbol pos) {
    if(wordNet == null)
      initializeWordNet();

    ImmutableList<Integer> hypernymChain = wordNet.getHypernymChainOfFirstSynset(Symbol.from(word),
        pos);
    if(hypernymChain.isEmpty())
      return Optional.absent();
    else {
      ISynset iSynset = wordNet.dictionary().getSynset(new SynsetID(hypernymChain.iterator().next(), POS.NOUN));
      List<IWord> iwords = iSynset.getWords();
      if(iwords.isEmpty())
        return Optional.absent();
      else
        return Optional.of(iwords.get(0).getLemma());
    }
  }

  public static void main(String [] argv) throws IOException {

    String word = "writers";
    Symbol pos = NN;

    Optional<String> synsetID = getSynsetID(word, pos);
    Optional<String> hypernym = getHypernym(word, pos);

    System.out.println("word=" + word + ", pos=" + pos.toString() + ", synsetID=" + synsetID + ", hypernym=" + hypernym);
  }


    public static void main2(String [] argv) throws IOException {
    String paramFile = "/nfs/mercury-04/u42/bmin/everything/projects/ere/wordnet.params";
    Parameters params = Parameters.loadSerifStyle(new File(paramFile));
    wordNet = WordNetUtility.fromParameters(params);

    Symbol word = Symbol.from("writers");
    Symbol pos = NN;

    Optional<ISynset> iSynsetOptional = wordNet.getFirstSynSetOfWord(word, pos);
    System.out.println("iSynsetOptional: " + getStringForISynset(iSynsetOptional.get()));

    System.out.println("a");

//  ImmutableList<Integer> hypernyms = wordNet.getHypernymsOfFirstSynset(word, pos);
    ImmutableList<Integer> hypernymChain = wordNet.getHypernymChainOfFirstSynset(word, pos);

    System.out.println("B");

    if(hypernymChain.isEmpty())
      System.out.println("no hypernymChain found");
    else {
      for (int hypernym : hypernymChain) {

        ISynset iSynset = wordNet.dictionary().getSynset(new SynsetID(hypernym, POS.NOUN));
        System.out.println("hypernym iSynset: " + getStringForISynset(iSynset));

      }
    }

    System.out.println("c");

  }


  public static String getStringForISynset(ISynset iSynset) {
    StringBuilder sb = new StringBuilder();

    sb.append("ID=" + iSynset.getID());
    sb.append(", POS=" + iSynset.getPOS());
    sb.append(", words=");

    for(IWord iWord : iSynset.getWords())
      sb.append(iWord.getLexicalID() + ":" + iWord.getLemma() + ",");

    return sb.toString();
  }
  */
}
