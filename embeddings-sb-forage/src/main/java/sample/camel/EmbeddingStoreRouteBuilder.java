
package sample.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreAction;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreHeaders;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.milvus.MilvusContainer;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;

@Component
public class EmbeddingStoreRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingStoreRouteBuilder.class);

    public static MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.3.1").withStartupAttempts(3).waitingFor(Wait.defaultWaitStrategy());

	public EmbeddingStoreRouteBuilder() throws IOException {
        GutenbergDownloader gd = new GutenbergDownloader();
        String testFile = gd.download("https://www.gutenberg.org/cache/epub/1513/pg1513.txt");
        String fileName = new String("src/main/resources/pg1513.txt");

        milvus.start();
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

    @Bean
    AllMiniLmL6V2EmbeddingModel allmini() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

	@Override
	public void configure() throws Exception {
        from("stream:in")
        .to("langchain4j-embeddings:bean:allmini")
        .setHeader(LangChain4jEmbeddingStoreHeaders.ACTION).constant(LangChain4jEmbeddingStoreAction.SEARCH)
        .to("langchain4j-embeddingstore:milvus?embeddingStoreFactory=#class:org.apache.camel.forage.vectordb.DefaultEmbeddingStoreFactory")
        .to("log:input");
	}
}
