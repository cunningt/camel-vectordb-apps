package demo.baseball.loader;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.testcontainers.qdrant.QdrantContainer;

import com.google.common.math.Quantiles;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.HnswConfigDiff;
import io.qdrant.client.grpc.Collections.HnswConfigDiffOrBuilder;
import io.qdrant.client.grpc.Collections.QuantizationConfig;
import io.qdrant.client.grpc.Collections.QuantizationType;
import io.qdrant.client.grpc.Collections.ScalarQuantization;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.Collections.QuantizationConfig.QuantizationCase;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.QuantizationSearchParams;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchParams;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpdateResult;
import static io.qdrant.client.PointIdFactory.id;

@SpringBootApplication
public class LoaderApplication {

	private static final String MNIST_FILENAME = "mnist_train.csv";
	private static final String MNIST_TEST_FILENAME = "mnist_test.csv";
	private static final String INDEX_NAME = "mnist-training";

	public static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest");
	public static QdrantClient client;

	public LoaderApplication() {
	}

	public void setup() throws InterruptedException, ExecutionException {
		qdrant.start();

		client =new QdrantClient(
                      QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false)
                              .build()); 

		HnswConfigDiff hnsw = HnswConfigDiff.newBuilder()
			.setM(128)
			.setEfConstruct(1000)
			.setOnDisk(true).build();
		client.createCollectionAsync(
			CreateCollection.newBuilder()
				.setCollectionName(INDEX_NAME)
				.setHnswConfig(hnsw)
				.setVectorsConfig(
					VectorsConfig.newBuilder()

						.setParams(
							VectorParams.newBuilder()
								.setDistance(Distance.Dot)
								.setSize(5)
								.setOnDisk(true)
								.build())
						.build())
			.setQuantizationConfig(
				QuantizationConfig.newBuilder()
					.setScalar(
						ScalarQuantization.newBuilder()
							.setQuantile(.9999999f)
							.setType(QuantizationType.Int8)
							.setAlwaysRam(false)
							.build())
			).build())
		.get();
	}

    public List<HistoricalPlayer> loadHistoricalPlayers(String file) {
        List<HistoricalPlayer> results = null;

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
            //for (HistoricalPlayer p : results) {
            //    System.out.println(p.getName() + " " + p.toSelectArray());
            //}
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
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
            //for (FangraphsPlayer p : results) {
            //    System.out.println(p.getName() + " " + p.toSelectArray());
            //}
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

	public void logWebPage(List<ScoredPoint> points, ArrayList<HistoricalPlayer> historicals, FangraphsPlayer fg, int counter) throws IOException {
		System.out.println("logWebPage : " + fg.toSelectArray());
		File webpage = new File("target" + File.separator + "log-" + counter + ".html");
		FileWriter fw = new FileWriter(webpage);
		fw.write("<html><body>\n");
 
		fw.write("<h1>Player : " + fg.getName() + " Age: " + fg.getAge() + " Vector: " + fg.toVector() + "<br><br>\n"); 

		for (ScoredPoint sp : points) {

			System.out.println(sp);

			String playerID = sp.getId().getNum() + "";
			HistoricalPlayer hp = historicals.get(Integer.parseInt(playerID));

			fw.write("<br>" + playerID + " " + hp.getName() + " " + hp.getYear() + " " + hp.toVector()
			+  " " +  ", score : " + sp.getScore() + "<br>\n");
			System.out.println(sp);
		}
		fw.write("</body></html>\n");
		fw.flush();
		fw.close();
	}

	public static void main(String[] args) throws IOException {
		LoaderApplication la = new LoaderApplication();
		try {
			ArrayList<FangraphsPlayer> fangraphs = (ArrayList<FangraphsPlayer>) la.loadFangraphsPlayers("src/main/resources/fangraphs-leaders-aaa.csv");

			ArrayList<HistoricalPlayer> historicals = (ArrayList<HistoricalPlayer>)
			la.loadHistoricalPlayers("src/main/resources/aaa.csv");

			int counter = 0;
			la.setup();
			for (HistoricalPlayer hist : historicals) {
				if (hist.getAge() > 40) {
					historicals.remove(hist);
					break;
				}
				List<Float> hVector = hist.toVector();

				List<PointStruct> points =
					List.of(
						PointStruct.newBuilder()
							.setId(id(counter++))
							.setVectors(VectorsFactory.vectors(hVector))
							.build());
				UpdateResult updateResult = client.upsertAsync(INDEX_NAME, points).get();
			}

			counter = 0;
			System.out.println("FG SIZE " + fangraphs.size());
			for (FangraphsPlayer fg : fangraphs) {
			
				List<Float> fVector = fg.toVector();

				QuantizationSearchParams qsp = QuantizationSearchParams.newBuilder()
					.setRescore(true)
					.setIgnore(false)
					.build();
				SearchParams searchParams = SearchParams.newBuilder()
						    .setHnswEf(128) 
						    .setExact(true)
							.setQuantization(qsp)
							.build();

				SearchPoints searchRequest = SearchPoints.newBuilder()
					.setCollectionName(INDEX_NAME)
					.addAllVector(fVector)
					.setLimit(10)
					.setParams(searchParams)
					.build();

				List<ScoredPoint> points = client.searchAsync(searchRequest).get();

				la.logWebPage(points, historicals, fg, counter);
				
				counter++;

			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SpringApplication.run(LoaderApplication.class, args);
	}

}
