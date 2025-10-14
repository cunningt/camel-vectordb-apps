/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStore;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreAction;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreComponent;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreHeaders;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.testcontainers.milvus.MilvusContainer;
/**
 * A basic example running as public static void main.
 */
public final class CamelEmbedding {

    public static MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.3.1");

    public void rewriteMilvusProperties() throws IOException {
        InputStream is = CamelEmbedding.class.getClassLoader().getResourceAsStream("forage-vectordb-milvus.properties");
        Properties properties = new Properties();
        properties.load(is);
        properties.setProperty("milvus.host", milvus.getHost());
        properties.setProperty("milvus.port", milvus.getMappedPort(19530).toString());
        properties.setProperty("milvus.uri", milvus.getEndpoint());
        FileOutputStream fos = new FileOutputStream("src/main/resources/forage-vectordb-milvus.properties");
        properties.store(fos, "Overwritten on " + System.currentTimeMillis());
        fos.close();
    }

    public void setup(String fileName) throws IOException {
        GutenbergDownloader gd = new GutenbergDownloader();
        String testFile = gd.download("https://www.gutenberg.org/cache/epub/1513/pg1513.txt");

        milvus.start();
        rewriteMilvusProperties();
        EmbeddingStore<TextSegment> milvusStore = MilvusEmbeddingStore.builder()
                    .uri(milvus.getEndpoint())
                    .collectionName("test_collection")
                    .dimension(384)
                    .build();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();


        int counter = 0;
        try {
            FileReader fr = new FileReader(new File(fileName));

            BufferedReader reader = new BufferedReader(fr);
            List<String> lines = (List<String>) reader.lines().toList();
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                String line = it.next();
                if (line != null && line.strip().length() > 0 ) {
                    TextSegment segment1 = TextSegment.from(line);
                    Embedding embedding1 = embeddingModel.embed(segment1).content();
                    milvusStore.add(embedding1, segment1);
                    counter++;
                }
            }
            System.out.println("Inserted " + counter + " lines from " + fileName + " into Milvus Vector DB");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        CamelEmbedding ce = new CamelEmbedding();
        ce.setup("src/main/resources/pg1513.txt");

        // create a CamelContext
        try (CamelContext camel = new DefaultCamelContext()) {
            camel.getRegistry().bind("allmini", new AllMiniLmL6V2EmbeddingModel());
            camel.addRoutes(createBasicRoute());

            camel.start();

            Thread.sleep(30_000);
            camel.stop();
        }
    }

    static RouteBuilder createBasicRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("stream:in")
                .to("langchain4j-embeddings:allmini")
                .setHeader(LangChain4jEmbeddingStoreHeaders.ACTION).constant(LangChain4jEmbeddingStoreAction.SEARCH)
                .to("langchain4j-embeddingstore:milvus?embeddingStoreFactory=#class:org.apache.camel.forage.vectordb.DefaultEmbeddingStoreFactory")
                .to("log:input");
            }
        };
    }
}
