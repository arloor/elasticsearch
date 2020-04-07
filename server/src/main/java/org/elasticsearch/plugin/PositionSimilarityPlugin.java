/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.plugin;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;


public class PositionSimilarityPlugin extends Plugin {
    public String name() {
        return "elasticsearch-position-similarity";
    }

    public String description() {
        return "Elasticsearch scoring plugin based on matching a term or a phrase relative to a position of the term in a searched field.";
    }

    public void onIndexModule(IndexModule indexModule) {
        indexModule.addSimilarity("position", (settings, version, scriptService)->{
            String DISCOUNT_OVERLAPS="discount_overlaps";
            // BM25的k1默认是1.2 b默认是0.75
            float k1 = settings.getAsFloat("k1", 1.4f);
            float b = settings.getAsFloat("b", 0.8f);
            boolean discountOverlaps = settings.getAsBoolean(DISCOUNT_OVERLAPS, true);

            BM25Similarity similarity = new BM25Similarity(k1, b);
            similarity.setDiscountOverlaps(discountOverlaps);
            return similarity;
        });
    }
}
