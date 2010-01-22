package org.apache.lucene.index.facade;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.IndexWriter;

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
 * @author karl wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-aug-17
 * Time: 00:18:25
 */
public class DirectoryIndexWriterFacade extends IndexWriterFacade {

  private IndexWriter indexWriter;

  public DirectoryIndexWriterFacade(Directory directory, Analyzer analyzer, boolean create, IndexWriter.MaxFieldLength mfl) throws IOException {
    indexWriter = new IndexWriter(directory, analyzer, create, mfl);
  }

  public void addDocument(Document document, Analyzer analyzer) throws IOException {
    indexWriter.addDocument(document, analyzer);
  }

  public void addDocument(Document document) throws IOException {
    indexWriter.addDocument(document);
  }

  public void close() throws IOException {
    indexWriter.close();
  }

  public void optimize() throws IOException {
    indexWriter.optimize();
  }
}
