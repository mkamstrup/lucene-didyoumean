package org.apache.lucene.search.didyoumean.secondlevel.token;
/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;
import org.apache.lucene.util.Attribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * A layer on top of the single token suggesting {@link TokenSuggester} that enables muti token (phrase) suggestions.
 * <p/>
 * Pretty much the same thing as a SpanFuzzyQuery.
 * <p/>
 * Places a matrix of {@link org.apache.lucene.search.spans.SpanNearQuery} to find valid suggestions. If any of the
 * valid hits contains a {@link org.apache.lucene.index.TermPositionVector}, it will be analyzed and suggest the query
 * in the order of terms in the index.
 * todo: if term positions available and stored, suggest that for cosmetic reasons in case of stemming et c.)
 * todo: E.g. query "camel that broke the staw" is suggested with "straw that broke the camel"
 *
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-jan-30
 *         Time: 05:24:38
 */
public abstract class TokenPhraseSuggester {

  private TokenSuggester tokenSuggester;

  /**
   * this is a nasty hack due to MultiTokenSuggester using this code.
   * @return true if this implementation allows inspection of term vector to change order of query
   */
  protected abstract boolean isUpdateSuggestionsOrder();

  /**
   * @return the index used for checking that suggestions match something, inspecting term position vector- and term frequency inspection.
   * @see TokenPhraseSuggester#getAprioriIndexField()
   */
  protected abstract IndexFacade getAprioriIndex();

  protected abstract IndexReader getAprioriReader();

  protected abstract IndexSearcher getAprioriSearcher();

  /**
   * @param suggestions suggestion to be made in to a query
   * @return a query that much match in order for the suggestion to be suggested
   */
  protected abstract Query suggestionAprioriQueryFactory(Suggestion[] suggestions);

  /**
   * if true, the ngram suggester will only suggest words more frequent than the query word
   */
  private boolean defaultSuggestMorePopularTokensOnly;

  /**
   * used by the single token ngram spell checker when suggestMorePopularTokensOnly is true
   * in the apriori index reader to check frequency.
   */
  private String aprioriIndexField;


  /**
   * number of suggestion per token in phrase.
   * there will be (n^tokens in phrase) queries placed on the index to find the best suggestion.
   * e.g. "three token phrase" and n=5 might results in 243 queries on the apriori index.
   */
  private int defaultMaxSuggestionsPerToken = 3;
  private Analyzer queryAnalyzer;

  /**
   * @param tokenSuggester                the single token suggester that backs this phrase suggester
   * @param aprioriIndexField             the document field used for term frequency inspection at token suggestion level.
   * @param defaultSuggestMorePopularTokensOnly
   *                                      if true, at token suggestion level, only suggest tokens that are more popular than the one in the original query.
   * @param defaultMaxSuggestionsPerToken number of suggestion per token in phrase. there will be (n^tokens in phrase) queries placed on the index to find the best suggestion. e.g. "three token phrase" and n=5 might results in 243 queries on the apriori index.
   * @param queryAnalyzer                the analyzer used to tokenize phrases
   * @throws java.io.IOException if something goes wrong in either the ngram spell checker or in the apriori index
   */
  public TokenPhraseSuggester(TokenSuggester tokenSuggester, String aprioriIndexField, boolean defaultSuggestMorePopularTokensOnly, int defaultMaxSuggestionsPerToken, Analyzer queryAnalyzer) throws IOException {
    this.tokenSuggester = tokenSuggester;
    this.aprioriIndexField = aprioriIndexField;
    this.defaultSuggestMorePopularTokensOnly = defaultSuggestMorePopularTokensOnly;
    this.defaultMaxSuggestionsPerToken = defaultMaxSuggestionsPerToken;
    this.queryAnalyzer = queryAnalyzer;
  }

  public String didYouMean(String query) {
    SuggestionPriorityQueue suggestions = suggest(query, 1);
    if (suggestions.size() > 0) {
      return ((Suggestion) suggestions.top()).getSuggested();
    } else {
      return null;
    }
  }

  /**
   * If this returns true,
   * then didYouMean() will place this phrase as a query against the apriori index
   * to see if it is a good suggestion or not.
   *
   * @param phrase the suggested phrase
   * @param query  the user input that requests the suggestion
   * @return true if the phrase is known to the sub class
   */
  protected boolean isSuggestablePhrase(String phrase, String query) {
    return true;
  }


  public boolean suggestionArraysEquals(Suggestion[] suggestions, Suggestion[] suggestions1) {
    if (suggestions == suggestions1) {
      return true;
    }
    if (suggestions == null /*&& suggestions1 != null*/) {
      return false;
    }
    if (/*suggestions != null &&*/ suggestions1 == null) {
      return false;
    }
    if (suggestions.length != suggestions1.length) {
      return false;
    }
    for (int i = 0; i < suggestions.length; i++) {
      if (!suggestions[i].getSuggested().equals(suggestions1[i].getSuggested())) {
        return false;
      }
    }
    return true;
  }


  /**
   * @param query          user input
   * @param maxSuggestions will not return more than this many suggestions
   * @return suggestions found, in natural order
   */
  public SuggestionPriorityQueue suggest(String query, int maxSuggestions) {
    return suggest(query, maxSuggestions, getDefaultMaxSuggestionsPerToken(), isDefaultSuggestMorePopularTokensOnly());
  }

  /**
   * @param query                        user input
   * @param maxSuggestions               limits results to this many suggestions
   * @param maxSuggestionsPerToken       tokens in the phrase will contain max this many suggestions
   * @param suggestMorePopularTokensOnly true if the token suggester should suggest only more popular terms. not recommended.
   * @return suggestions found, in natural order
   */
  public SuggestionPriorityQueue suggest(String query, int maxSuggestions, int maxSuggestionsPerToken, boolean suggestMorePopularTokensOnly) {

    long ms = System.currentTimeMillis();

    // build a matrix with all suggestions per token in query.

    final List<Suggestion[]> matrix = new LinkedList<Suggestion[]>();

    TokenStream ts = getQueryAnalyzer().tokenStream(null, new StringReader(query));
    try {
      while (ts.incrementToken()) {
        try {
          Attribute term = ts.getAttribute(TermAttribute.class);
          if (!(term instanceof TermAttribute)) {
            continue; // Should we throw an Exception here?
          }
          String termString = ((TermAttribute)term).term();
          SuggestionPriorityQueue suggestions = tokenSuggester.suggest(termString, maxSuggestionsPerToken, true, getAprioriReader(), getAprioriIndexField(), suggestMorePopularTokensOnly);
          if (suggestions.size() == 0) {
            suggestions.add(new Suggestion(termString));
          }
          matrix.add(suggestions.toArray());

        } catch (IOException ioe) {
          throw new RuntimeException("Exception caught while looking for a suggestion to " + query, ioe);
        }
      }
    } catch (IOException ioe) {
      throw new RuntimeException("Error tokenizing " + query, ioe);
    }

    /*
    * iterate through all possible combinations of suggetions in the matrix
    * <pre>
    * the best game
    * tho rest fame
    *          lame
    *
    * tho best game
    * tho best fame
    * tho best lame
    * tho rest game
    * tho rest fame
    * tho rest lame
    * the best game
    * the best fame
    * the best lame
    * the rest game
    * the rest fame
    * the rest lame
    * </pre>
    */
    Iterator<Suggestion[]> itAllCombinations = new Iterator<Suggestion[]>() {
      private int[] counter = new int[matrix.size()];


      public void remove() {
        throw new IllegalStateException("not implemented");
      }

      public boolean hasNext() {
        int s = counter.length;
        return counter[s - 1] < matrix.get(s - 1).length;
      }

      public Suggestion[] next() {
        if (!hasNext()) {
          throw new NoSuchElementException("no more elements");
        }
        Suggestion[] ls = new Suggestion[counter.length];
        for (int i = 0; i < counter.length; i++) {
          ls[i] = (matrix.get(i)[counter[i]]);
        }
        incrementCounter();
        return ls;
      }

      private void incrementCounter() {
        for (int i = 0; i < counter.length; i++) {
          counter[i]++;
          if (counter[i] == matrix.get(i).length &&
              i < counter.length - 1) {
            counter[i] = 0;
          } else {
            break;
          }
        }
      }
    };

    SuggestionPriorityQueue queue = new SuggestionPriorityQueue(maxSuggestions);


    int queryCounter = 0;
    int currentHit = 0;
    TopDocs hits = null;
    Suggestion[] suggestions = null;
    while (hits != null || itAllCombinations.hasNext()) {
      Integer corpusQueryResults = null;

      if (hits == null) {
        suggestions = itAllCombinations.next();
        queryCounter++;

        StringBuilder sb = new StringBuilder(10 * matrix.size());
        for (Suggestion suggestion : suggestions) {
          if (sb.length() > 0) sb.append(' ');
          sb.append(suggestion.getSuggested());
        }
        String phrase = sb.toString();

        //System.out.println(cnt + "\t" + phrase);

        if (isSuggestablePhrase(phrase, query)) {
          Query nextQuery = suggestionAprioriQueryFactory(suggestions);
          try {

            hits = getAprioriSearcher().search(nextQuery, maxSuggestions);
            corpusQueryResults = hits.totalHits;
            if (corpusQueryResults == 0) {
              hits = null;
              continue;
            } else {
              currentHit = 0;
            }
          } catch (IOException ioe) {
            throw new RuntimeException("Exception caught while searching for " + nextQuery.toString(), ioe);
          }
        } else {
          continue;
        }
      } else {
        currentHit++;
      }


      if (isUpdateSuggestionsOrder()) {

        // todo refactor, this is a nasty hack due to MultiTokenSuggester using this code.


        if (currentHit < hits.scoreDocs.length) {

          // attempt to figure out the order of the tokens in the phrase

          try {
            TermFreqVector termFreqVector = getAprioriReader().getTermFreqVector(hits.scoreDocs[currentHit].doc, getAprioriIndexField());
            if (termFreqVector != null && termFreqVector instanceof TermPositionVector) {
              TermPositionVector termPosVector = (TermPositionVector) termFreqVector;
              final int[][] suggestionPositions = new int[suggestions.length][];

              for (int i = 0; i < suggestionPositions.length; i++) {
                int termIndex = Arrays.binarySearch(termFreqVector.getTerms(), suggestions[i].getSuggested());
                suggestionPositions[i] = termPosVector.getTermPositions(termIndex);
              }

              // todo: if pos vectors and stored data, then extract the suggestion as stored.
//              TermVectorOffsetInfo[] offsets = termPosVector.getOffsets(termIndex);
//              if (offsets != null) {
//                suggestionOffsets[i][0] = i;
//                suggestionOffsets[i][1] = i;
//              }

              // todo: find the actual position area that matched, rather than just looking at the first occurances.
              // todo: at least optimze!
              Map<Suggestion, Integer> positions = new HashMap<Suggestion, Integer>(suggestions.length);
              for (int i = 0; i < suggestions.length; i++) {
                positions.put(suggestions[i], suggestionPositions[i][0]);
              }
              Map.Entry<Suggestion, Integer>[] ordered = positions.entrySet().toArray((Map.Entry<Suggestion, Integer>[]) new Map.Entry[0]);
              Arrays.sort(ordered, new Comparator<Map.Entry<Suggestion, Integer>>() {

                public int compare(Map.Entry<Suggestion, Integer> entry, Map.Entry<Suggestion, Integer> entry1) {
                  return entry.getValue().compareTo(entry1.getValue());
                }
              });
              for (int i = 0; i < ordered.length; i++) {
                suggestions[i] = ordered[i].getKey();
              }

              // token order found

            } else {
              // to term vector. look for one in next hit.
              // todo some setting that skip this when score is lower than n or current hit greather than n.
              if (hits.scoreDocs.length - 1 == currentHit) {
                // fall back on user input
              } else {
                continue;
              }
            }
          } catch (IOException ioe) {
            //throw new RuntimeException("Exception caught when inspecting the term position vectors.", ioe);
          }
        }
      }

      StringBuilder stringBuilder = new StringBuilder(matrix.size() * 10);
      for (Suggestion suggestion : suggestions) {
        if (stringBuilder.length() > 0) {
          stringBuilder.append(' ');
        }
        stringBuilder.append(suggestion.getSuggested());
      }

      String suggested = stringBuilder.toString();

      double score = 0;
      for (Suggestion suggestion : suggestions) {
        score += suggestion.getScore();
      }
      score *= hits.totalHits;

      // Update the result set, spilling out any excess suggestions with too low score
      queue.insertWithOverflow(new Suggestion(suggested, score, corpusQueryResults));

      hits = null;

    }

    System.out.println("Built matrix with " + queryCounter + " queries and executed them in " + (System.currentTimeMillis() - ms) + " milliseconds.");

    return queue;
  }


  public TokenSuggester getTokenSuggester() {
    return tokenSuggester;
  }

  public void setTokenSuggester(TokenSuggester tokenSuggester) {
    this.tokenSuggester = tokenSuggester;
  }

  /**
   * @return the document field used for term frequency inspection at token suggestion level.
   */
  public String getAprioriIndexField() {
    return aprioriIndexField;
  }

  /**
   * @param aprioriIndexField the document field used for term frequency inspection at token suggestion level.
   */
  public void setAprioriIndexField(String aprioriIndexField) {
    this.aprioriIndexField = aprioriIndexField;
  }

  /**
   * // todo: consider if false will allow for "lost on translation" to suggest "lost in translation".
   * // todo: will single token phrases then start suggesting a lot of bad things?
   *
   * @return if true, at token suggestion level, only suggest tokens that are more popular than the one in the original query.
   */
  public boolean isDefaultSuggestMorePopularTokensOnly() {
    return defaultSuggestMorePopularTokensOnly;
  }

  public void setDefaultSuggestMorePopularTokensOnly(boolean defaultSuggestMorePopularTokensOnly) {
    this.defaultSuggestMorePopularTokensOnly = defaultSuggestMorePopularTokensOnly;
  }


  public int getDefaultMaxSuggestionsPerToken() {
    return defaultMaxSuggestionsPerToken;
  }

  public void setDefaultMaxSuggestionsPerToken(int defaultMaxSuggestionsPerToken) {
    this.defaultMaxSuggestionsPerToken = defaultMaxSuggestionsPerToken;
  }

  public Analyzer getQueryAnalyzer() {
    return queryAnalyzer;
  }

  public void setQueryAnalyzer(Analyzer queryAnalyzer) {
    this.queryAnalyzer = queryAnalyzer;
  }
}
