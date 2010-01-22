package org.apache.lucene.search.didyoumean.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.index.facade.IndexWriterFacade;
import org.apache.lucene.search.didyoumean.AprioriCorpusFactory;
import org.apache.lucene.search.didyoumean.Suggester;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;
import org.apache.lucene.search.didyoumean.dictionary.SuggestionList;

import java.io.IOException;
import java.util.*;
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


/**
 * @author karl wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-okt-19
 * Time: 02:13:00
 */
public class DefaultAprioriCorpusFactory implements AprioriCorpusFactory {

  public void factory(Dictionary dictionary, Suggester suggester, IndexFacade aprioriIndex, String aprioriIndexField, Analyzer aprioriAnalyzer, IndexWriter.MaxFieldLength mfl) throws IOException {
    // create an a priori index based on the inverted dictionary

    System.out.println("Inverting index...");
    Map<String, SuggestionList> inverted = dictionary.inverted();

    IndexWriterFacade aprioriWriter = aprioriIndex.indexWriterFactory(aprioriAnalyzer, true, mfl);

//    int i=0;
//    int i2=0;
    System.out.println("Extracting most commonly misspelled words and phrases...");
    for (Map.Entry<String, SuggestionList> e : inverted.entrySet()) {
      if (e.getValue().size() > 1) {
        String suggested = suggester.didYouMean(dictionary, e.getKey());
        if (suggested != null && suggested.equalsIgnoreCase(e.getKey())) {
          Document d = new Document();
          d.add(new Field(aprioriIndexField, e.getKey(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
          aprioriWriter.addDocument(d);
//          i2++;
//          System.out.println(i + "\t" + i2);
        }
      }
//      i++;
    }
    aprioriWriter.close();
  }
}
