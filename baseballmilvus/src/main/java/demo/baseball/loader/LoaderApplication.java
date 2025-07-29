package demo.baseball.loader;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

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
                .metricType(IndexParam.MetricType.L2)
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

	public float logWebPage(SearchResp searchR, HashMap<String,HistoricalPlayer> historicals, FangraphsPlayer fg) throws IOException {

		File webpage = new File("target" + File.separator + "log-" + fg.getPlayerID()
		 + ".html");
		FileWriter fw = new FileWriter(webpage);
		fw.write("<html><body>\n");
 
		fw.write("<h1>Player : " + fg.getName() + " Age: " + fg.getAge() + " Vector: " + fg.toVector() + "<br><br>\n"); 

		float scoredTotal = 0;
		List<List<SearchResp.SearchResult>> searchResults = searchR.getSearchResults();
		for (List<SearchResp.SearchResult> results : searchResults) {
			for (SearchResp.SearchResult result : results) {

				String playerID = (String) result.getId().toString();

				HistoricalPlayer hp = historicals.get(playerID);

				float warScored = result.getScore() * hp.getWaroff();
				fw.write("<br>" + playerID + " " + hp.getName() + " " + hp.getYear() + " " + hp.toVector() +  " " +  ", war: " + hp.getWaroff() + " score : " + result.getScore() + " scored war : " + warScored + "<br>\n");

				fw.write("<br><br>Score : " + scoredTotal);
				scoredTotal += warScored;
			}
		}

		fw.write("</body></html>\n");
		fw.flush();
		fw.close();

		return scoredTotal;
	}

	public static void main(String[] args) throws IOException {
		LoaderApplication la = new LoaderApplication();
		try {
			ArrayList<FangraphsPlayer> fangraphs = (ArrayList<FangraphsPlayer>) la.loadFangraphsPlayers("src/main/resources/fangraphs-leaders-aaa.csv");

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

			TreeMap<Float,FangraphsPlayer> scores = new TreeMap<Float,FangraphsPlayer>();
			for (FangraphsPlayer fg : fangraphs) {
				List<Float> fVector = fg.toVector();
				SearchResp searchR = client.search(SearchReq.builder()
						.collectionName(INDEX_NAME)
						.data(Collections.singletonList(new FloatVec(fg.toVector())))
						.limit(20)
						.outputFields(Collections.singletonList("*"))
						.build());

				float score = la.logWebPage(searchR, historicals, fg);
				scores.put(Float.valueOf(score), fg);

				counter++;
			}

			for (Map.Entry<Float, FangraphsPlayer> entry : scores.entrySet()) { 
				FangraphsPlayer player = entry.getValue();
            	System.out.println("Player = " + player.getName() + " " 
					+ player.getAge() + " " + " Score = " + entry.getKey());      
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SpringApplication.run(LoaderApplication.class, args);
	}

}
