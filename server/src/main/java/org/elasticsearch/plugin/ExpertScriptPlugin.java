/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.plugin;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScoreScript.LeafFactory;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

/**
 * An example script plugin that adds a {@link ScriptEngine} implementing expert scoring.
 */
public class ExpertScriptPlugin extends Plugin implements ScriptPlugin {
    protected static final Logger logger = LogManager.getLogger(ExpertScriptPlugin.class);

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new MyExpertScriptEngine();
    }

    /** An example {@link ScriptEngine} that uses Lucene segment details to implement pure document frequency scoring. */
    // tag::expert_engine
    private static class MyExpertScriptEngine implements ScriptEngine {
        @Override
        public String getType() {
            return "expert_scripts";
        }

        @Override
        public <T> T compile(String scriptName, String scriptSource,
                ScriptContext<T> context, Map<String, String> params) {
            if (context.equals(ScoreScript.CONTEXT) == false) {
                throw new IllegalArgumentException(getType()
                        + " scripts cannot be used for context ["
                        + context.name + "]");
            }
            // we use the script "source" as the script identifier
            if ("pure_df".equals(scriptSource)) {
                ScoreScript.Factory factory = PureDfLeafFactory::new;
                return context.factoryClazz.cast(factory);
            }
            throw new IllegalArgumentException("Unknown script name "
                    + scriptSource);
        }

        @Override
        public void close() {
            // optionally close resources
        }

        private static class PureDfLeafFactory implements LeafFactory {
            private final Map<String, Object> params;
            private final SearchLookup lookup;
            private final String field;
            private final String term;

            private PureDfLeafFactory(
                        Map<String, Object> params, SearchLookup lookup) {
                if (params.containsKey("field") == false) {
                    throw new IllegalArgumentException(
                            "Missing parameter [field]");
                }
                if (params.containsKey("term") == false) {
                    throw new IllegalArgumentException(
                            "Missing parameter [term]");
                }

                this.params = params;
                this.lookup = lookup;
                field = params.get("field").toString();
                term = params.get("term").toString();
            }

            @Override
            public boolean needs_score() {
                return false;  // Return true if the script needs the score
            }

            @Override
            public ScoreScript newInstance(LeafReaderContext context)
                    throws IOException {
                LeafReader reader = context.reader();
                PostingsEnum postings = reader.postings(
                        new Term(field, term));
                if (postings == null) {
                    /*
                     * the field and/or term don't exist in this segment,
                     * so always return 0
                     */
                    return new ScoreScript(params, lookup, context) {
                        @Override
                        public double execute() {
                            return 0.0d;
                        }
                    };
                }
                return new ScoreScript(params, lookup, context) {
                    int currentDocid = -1;
                    @Override
                    public void setDocument(int docid) {
                        /*
                         * advance has undefined behavior calling with
                         * a docid <= its current docid
                         */
                        if (postings.docID() < docid) {
                            try {
                                postings.advance(docid);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                        currentDocid = docid;
                    }
                    @Override
                    public double execute() {

                        if (postings.docID() != currentDocid) {
                            /*
                             * advance moved past the current doc, so this doc
                             * has no occurrences of the term
                             */
                            return 0.0d;
                        }
                        try {
                            //打印原文档
                            Document document = reader.document(currentDocid);
                            String doc=new String(document.getBinaryValue("_source").bytes);
                            JSONObject jsonObject=JSONObject.parseObject(doc);
                            String value=jsonObject.getString(field);
                            logger.info(term+" | "+value);
//                            System.out.println(currentDocid+" "+ doc);
                            //打印分出来的term
                            Terms terms = reader.terms(field);
                            TermsEnum iterator = terms.iterator();
                            for (int i = 0; i < terms.size(); i++) {
                                BytesRef next = iterator.next();
                                if (next==null){
                                    break;
                                }
//                                System.out.println(currentDocid+" "+new String(next.bytes)+ " "+iterator.docFreq());
                            }
                            return 1;
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                };
            }
        }
    }
    // end::expert_engine
}
