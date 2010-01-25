package org.apache.lucene.search.didyoumean.dictionary;

import org.apache.lucene.search.didyoumean.Suggester;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link Dictionary} backed by an in memory map
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 22, 2010
 */
public class MemoryDictionary extends Dictionary {

  private Map<String,SuggestionList> store = new HashMap<String,SuggestionList>();

  @Override
  public SuggestionList getSuggestions(String query) {
    return store.get(keyFormatter(query));
  }

  @Override
  public void close() {
    store.clear();
  }

  @Override
  public void put(SuggestionList suggestions) {
    store.put(suggestions.getQuery(), suggestions);
  }

  @Override
  public void optimize(Suggester suggester) throws IOException {
    // TODO
  }

  @Override
  public void prune(int maxSize) throws IOException {
    for (SuggestionList suggestionList : this) {
      if (suggestionList.size() > maxSize) {
        for (int i = maxSize; i < suggestionList.size(); i++) {
          suggestionList.getSuggestions().remove(i);
        }
      }
    }
  }

  @Override
  public int size() {
    return store.size();
  }

  public Iterator<SuggestionList> iterator() {
    return store.values().iterator();
  }
}
