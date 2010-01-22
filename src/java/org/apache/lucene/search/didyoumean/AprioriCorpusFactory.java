package org.apache.lucene.search.didyoumean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;
import org.apache.lucene.search.didyoumean.dictionary.QueryException;

import java.io.IOException;
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
 * Extracts an a priori corpus from a dictionary.
 *
 * @author karl wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-okt-19
 * Time: 02:07:34
 */
public interface AprioriCorpusFactory {

  /**
   * Initializes and populates an a priori index with documents whos text is known to be correct.
   *
   * @param dictionary dictionary to extract data from.
   * @param suggester suggester used to navigate the dictionary.
   * @param aprioriIndex lucene index, will be created/reinitialized.
   * @param aprioriIndexField index field used to store the a priori text.
   * @param aprioriAnalyzer analyzer used to tokenize a priori text.
   * @throws IOException
   * @throws QueryException
   */
  public abstract void factory(Dictionary dictionary, Suggester suggester, IndexFacade aprioriIndex, String aprioriIndexField, Analyzer aprioriAnalyzer, IndexWriter.MaxFieldLength mfl) throws IOException, QueryException;

}
