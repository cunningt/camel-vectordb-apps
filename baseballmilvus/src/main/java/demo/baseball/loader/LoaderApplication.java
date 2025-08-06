package demo.baseball.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.testcontainers.milvus.MilvusContainer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;

@SpringBootApplication
public class LoaderApplication {

	private static final String INDEX_NAME = "baseball";

	public static MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.3.9");
	public static MilvusClientV2 client;

	public LoaderApplication() {
	}

	public void setup() throws InterruptedException, ExecutionException {
		milvus.start();

        ConnectConfig config = ConnectConfig.builder()
                .uri(milvus.getEndpoint()) 
                .build();
    	client = new MilvusClientV2(config);

        client.dropCollection(DropCollectionReq.builder()
                .collectionName(INDEX_NAME)
                .build());

        List<IndexParam> indexes = new ArrayList<>();
        Map<String,Object> extraParams = new HashMap<>();
        extraParams.put("nlist",128);
		extraParams.put("m", 64);
		extraParams.put("nbits", 16);
        indexes.add(IndexParam.builder()
                .fieldName("vector")
                .indexName(INDEX_NAME)
                .indexType(IndexParam.IndexType.FLAT)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(extraParams)
                .build());
        client.createCollection(CreateCollectionReq.builder()
                .collectionName(INDEX_NAME)
				.indexParams(indexes)
				.consistencyLevel(ConsistencyLevel.STRONG)
                .dimension(5)
                .build());
	}

    public HashMap<String,HistoricalPlayer> loadHistoricalPlayers(String file) {
        List<HistoricalPlayer> results = null;
		HashMap<String,HistoricalPlayer> hash = new HashMap<String,HistoricalPlayer>();
        try {
            FileReader fr = new FileReader(new File(file));

            BufferedReader reader = new BufferedReader(fr);
            reader.readLine();
            CsvToBean<HistoricalPlayer> csvReader = new CsvToBeanBuilder(reader)
                    .withType(HistoricalPlayer.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            results = csvReader.parse();

			for (HistoricalPlayer hp: results) {
				hash.put(hp.getUid(), hp);
			}


        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hash;
    }

	public List<FangraphsPlayer> loadFangraphsPlayers(String file) {
        List<FangraphsPlayer> results = null;

        try {
            FileReader fr = new FileReader(new File(file));

            BufferedReader reader = new BufferedReader(fr);
            reader.readLine();
            CsvToBean<FangraphsPlayer> csvReader = new CsvToBeanBuilder(reader)
                    .withType(FangraphsPlayer.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            results = csvReader.parse();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

	public static void main(String[] args) throws IOException {
		LoaderApplication la = new LoaderApplication();
		WebpageCreator webpageCreator = new WebpageCreator("target/rankings");
		try {
			ArrayList<FangraphsPlayer> fangraphs = (ArrayList<FangraphsPlayer>) la.loadFangraphsPlayers("src/main/resources/fangraphs-minor-league-leaders-AAA.csv");

			HashMap<String,HistoricalPlayer> historicals = (HashMap<String,HistoricalPlayer>) la.loadHistoricalPlayers("src/main/resources/aaa.csv");

			int counter = 0;
			la.setup();
			for (HistoricalPlayer hist : historicals.values()) {
				if (hist.getAge() > 40) {
					historicals.remove(hist);
					break;
				}
				List<Float> hVector = hist.toVector();

		        List<JsonObject> rows = new ArrayList<>();
        		Gson gson = new Gson();
            	JsonObject row = new JsonObject();
				row.addProperty("id", hist.getUid());
            	row.addProperty("uid", hist.getUid());
				row.addProperty("age", hist.getAge());
				row.addProperty("name", hist.getLeague());
				row.addProperty("year", hist.getYear());
				row.addProperty("waroff", hist.getWaroff());

				row.add("vector", gson.toJsonTree(hist.toVector()));
	            rows.add(row);
		        InsertResp insertR = client.insert(InsertReq.builder()
        	        .collectionName(INDEX_NAME)
	                .data(rows)
   		            .build());
			}

			counter = 0;

			HashMap<FangraphsPlayer,BigDecimal> scores = new HashMap<FangraphsPlayer,BigDecimal>();
			for (FangraphsPlayer fg : fangraphs) {

				List<Float> fVector = fg.toVector();
				String filterString = new String("age > " + (fg.getAge()-1) + " and age < " + (fg.getAge()+1));
				SearchResp searchR = client.search(SearchReq.builder()
						.collectionName(INDEX_NAME)
						.data(Collections.singletonList(new FloatVec(fg.toVector())))
						.filter(filterString)
						.limit(20)
						.outputFields(Collections.singletonList("*"))
						.build());

				scores.put(fg, webpageCreator.logWebPage(searchR, historicals, fg));

				counter++;

			}

            LinkedHashMap<FangraphsPlayer, BigDecimal> sortedScores = scores.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

			
			File curfile = new File(".");
			for (Map.Entry<FangraphsPlayer,BigDecimal> entry : sortedScores.entrySet()) { 
				FangraphsPlayer player = entry.getKey();
				BigDecimal score = entry.getValue();

				char esc = 0x1B;
            	System.out.println(esc + "]8;;" + curfile.getCanonicalPath() + "/target/rankings/" + player.getPlayerID() + ".html" + esc + "\\" + player.getName() +  
					 esc + "]8;;" + esc + "\\ " + player.getAge() + "  " + " Score = " + score);      
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SpringApplication.run(LoaderApplication.class, args);
	}

}
