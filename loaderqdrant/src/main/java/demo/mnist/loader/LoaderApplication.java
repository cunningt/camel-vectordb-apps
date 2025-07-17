package demo.mnist.loader;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.testcontainers.qdrant.QdrantContainer;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
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

		client.createCollectionAsync(INDEX_NAME,
			VectorParams.newBuilder()
			.setDistance(Distance.Cosine)
			.setSize(28 * 28 + 1)
			.build())
		.get();
	}

	public void createImage(String filename, String mnistImageArray) {
		String[] imageArray = mnistImageArray.split(",");

		BufferedImage image = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
		for (int y = 0; y < 28; y++) {
			for (int x = 0; x < 28; x++) {
				// Assuming mnistArray is a 1D array of 784 elements
				int pixelValue = Integer.parseInt(imageArray[y * 28 + x]); 
				raster.setSample(x, y, 0, pixelValue); 
			}
		}

		try {
			System.out.println("== Generating image for " + filename);
			File outputFile = new File(filename);
			ImageIO.write(image, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public List<String> loadMNist(String file) throws IOException, InterruptedException {
		ArrayList<String> al = new ArrayList<String>();
		InputStream is = getClass().getClassLoader()
                         .getResourceAsStream(file);		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = br.readLine()) != null) {
				al.add(line);
			}
		}
		return al;
	}

	/*
	 * converts a mnist image representation (comma delimited list of numbers between 0..255) to
	 * vector	
	*/
	public List<Float> convert(String mnistImage) {
		ArrayList<Float> vector = new ArrayList<Float>();
		String[] numbers = mnistImage.split(",");
		for (String number : numbers) {
			float point = (float) Integer.parseInt(number) / 255;
			vector.add(new Float(point));
		}

		return vector;
	}

	public void logWebPage(List<ScoredPoint> points, int counter) throws IOException {
		File webpage = new File("target" + File.separator + "log-" + counter + ".html");
		FileWriter fw = new FileWriter(webpage);
		fw.write("<html><body>\n");
 
		String imgName = new String("testimage-" + counter + ".png");

		fw.write("<h1>Original image : " + counter + " <img src=\"" + imgName + "\"/><br>\n");

		for (ScoredPoint sp : points) {

			String fileName = new String("mnistimage-" + sp.getId().getNum() + ".png");

			fw.write("<br><img src=\"" + fileName + "\"/> , score : " + sp.getScore() + "<br>\n");
			System.out.println(sp);
		}
		fw.write("</body></html>\n");
		fw.flush();
		fw.close();
	}

	public static void main(String[] args) throws IOException {
		MnistDownloader md = new MnistDownloader();
		md.downloadTestFiles();

		LoaderApplication la = new LoaderApplication();
		try {
			ArrayList<String> mnistList = (ArrayList) la.loadMNist(MNIST_FILENAME);

			int counter = 0;
			la.setup();
			for (String mnistImage : mnistList) {
				String fileName = new String("target" + File.separator + "mnistimage-" + counter + ".png");
				la.createImage(fileName, mnistImage);
				ArrayList<Float> mnistVector = (ArrayList) la.convert(mnistImage);
				List<PointStruct> points =
					List.of(
						PointStruct.newBuilder()
							.setId(id(counter++))
							.setVectors(VectorsFactory.vectors(mnistVector))
							.build());
				UpdateResult updateResult = client.upsertAsync(INDEX_NAME, points).get();
			}

			ArrayList<String> mnistTestList = (ArrayList) la.loadMNist(MNIST_TEST_FILENAME);
			counter = 0;
			for (String mnistImage : mnistTestList) {

				String fileName = new String("target" + File.separator + "testimage-" + counter + ".png");
				la.createImage(fileName, mnistImage);
			
				ArrayList<Float> mnistVector = (ArrayList) la.convert(mnistImage);
				List<ScoredPoint> points = client.searchAsync(SearchPoints.newBuilder()
					.setCollectionName(INDEX_NAME)
					.addAllVector(mnistVector)
					.setLimit(5)
					.build()
					).get();

				la.logWebPage(points, counter);
				
				counter++;

			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SpringApplication.run(LoaderApplication.class, args);
	}

}
