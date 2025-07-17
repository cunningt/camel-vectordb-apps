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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.testcontainers.weaviate.WeaviateContainer;

import com.google.gson.internal.LinkedTreeMap;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.async.graphql.GraphQL;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.builder.GetBuilder;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.v1.graphql.query.fields.Fields;
import io.weaviate.client.v1.schema.model.WeaviateClass;

@SpringBootApplication
public class LoaderApplication {

	private static final String MNIST_TEST_FILENAME = "mnist_test.csv";
	private static final String MNIST_FILENAME = "mnist_train.csv";
	private static final String INDEX_NAME = "MnistTraining";

	public static WeaviateContainer weaviate = new WeaviateContainer("cr.weaviate.io/semitechnologies/weaviate:1.25.5");
	public static WeaviateClient client;
	public static WeaviateClass wc;

	public LoaderApplication() {
	}

	public void setup() throws InterruptedException, ExecutionException {
		weaviate.start();

		Config config = new Config("http", weaviate.getHttpHostAddress());
		client =new WeaviateClient(config); 

		wc = WeaviateClass.builder().className(INDEX_NAME).build();

		Result<Boolean> result = client.schema().classCreator()
	    	.withClass(wc)
    		.run();

		System.out.println("Result of creation of " + INDEX_NAME + " " + result);

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

	public void logWebPage(GraphQLResponse gql, int counter) throws IOException {
		File webpage = new File("target" + File.separator + "log-" + counter + ".html");
		FileWriter fw = new FileWriter(webpage);
		fw.write("<html><body>\n");
 
		String imgName = new String("testimage-" + counter + ".png");

		fw.write("<h1>Original image : " + counter + " <img src=\"" + imgName + "\"/><br>\n");

		LinkedTreeMap ltm = (LinkedTreeMap) gql.getData();

		String regex = "imageid=(\\d+)\\.";
		Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ltm.toString());
        while (matcher.find()) {
            String extractedNumber = matcher.group(1);

			String fileName = new String("mnistimage-" + extractedNumber + ".png");

			fw.write("<br><img src=\"" + fileName + "\"/> <br>\n");
		}
 		fw.write("</body></html>\n");
		fw.flush();
		fw.close();
	}

	public void prompt() {
		Scanner scanner = new Scanner(System.in);
        System.out.print("Enter text (press Enter to finish): ");
        String input = scanner.nextLine();
        System.out.println("You entered: " + input);
        scanner.close();
	}

	public static void main(String[] args) {
		MnistDownloader md = new MnistDownloader();
        try {
            md.downloadTestFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

		LoaderApplication la = new LoaderApplication();
		try {
			ArrayList<String> mnistList = (ArrayList) la.loadMNist(MNIST_FILENAME);

			int counter = 0;
			la.setup();
			for (String mnistImage : mnistList) {

				String fileName = new String("target" + File.separator + "mnistimage-" + counter + ".png");
				la.createImage(fileName, mnistImage);
				ArrayList<Float> mnistVector = (ArrayList) la.convert(mnistImage);

				// convert to float primitive
				Float[] floatVector = new Float[mnistVector.size()];
				for (int i = 0; i < mnistVector.size(); i++) {
					floatVector[i] = mnistVector.get(i);
				}

				HashMap props = new HashMap<String,Object>();
				props.put("imageid", Integer.valueOf(counter));
					
				Result<WeaviateObject> result = client.data().creator()
					.withClassName(INDEX_NAME)
					.withVector(floatVector)
					.withProperties(props)
					.run();
				counter++;
			}

			la.prompt();

			ArrayList<String> mnistTestList = (ArrayList) la.loadMNist(MNIST_TEST_FILENAME);
			counter = 0;
			for (String mnistImage : mnistTestList) {

				String fileName = new String("target" + File.separator + "testimage-" + counter + ".png");
				la.createImage(fileName, mnistImage);
			
				ArrayList<Float> mnistVector = (ArrayList) la.convert(mnistImage);

				Float[] floatVector = mnistVector.toArray(new Float[mnistVector.size()]);

				NearVectorArgument nearVector = NearVectorArgument.builder()
				.vector(floatVector)
				.distance(0.8f)
				.build();

				Field imageid = Field.builder().name("imageid").build();

				String query = GetBuilder.builder()
				.className(INDEX_NAME)
				.fields(Fields.builder().fields(new Field[]{imageid}).build())
				.withNearVectorFilter(nearVector)
				.limit(5)
				.build()
				.buildQuery();

				Result<GraphQLResponse> result = client.graphQL().raw().withQuery(query).run();

				la.logWebPage(result.getResult(), counter);
				
				counter++;

			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SpringApplication.run(LoaderApplication.class, args);
	}

}
