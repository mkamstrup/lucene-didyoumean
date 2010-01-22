package org.apache.lucene.search.didyoumean;

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
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-17
 *         Time: 03:08:24
 */
public abstract class EditDistance {
  protected final char[] sa;

  public EditDistance(String sa) {
    this.sa = sa.toCharArray();
  }

  // private static Log log = LogFactory.getLog(EditDistance.class);
  // private static long serialVersionUID = 1l;

  /**
   * @param query string to measure distance to.
   * @return distance to query
   */
  public abstract int getDistance(String query);

  public double getNormalizedDistance(String query) {
    return
        1.0d - ((double) getDistance(query) / Math
            .min(query.length(), sa.length));

  }

  public char[] getSa() {
    return sa;
  }


}
