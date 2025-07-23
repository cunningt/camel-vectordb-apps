package demo.mnist.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MnistDownloader {

    private static final String TEST_URL = "https://github.com/phoebetronic/mnist/raw/refs/heads/main/mnist_test.csv.zip";
    private static final String TRAIN_URL = "https://github.com/phoebetronic/mnist/raw/refs/heads/main/mnist_train.csv.zip";

    private static final int BYTE_SIZE = 2048;

    public MnistDownloader() {
    }

    private String download(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);

        File outputFile = new File(url.getFile());
        outputFile = new File(outputFile.getName());
        System.out.println("outputFile = " + outputFile);
        try (InputStream in = url.openStream();
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BYTE_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return outputFile.toString();
    }

    private void removeZipFile(String zipUrl) throws IOException {
        URL url = new URL(zipUrl);
        File file = new File(url.getFile());
        File localFile = new File(file.getName());

        if (localFile.exists()) {
            localFile.delete();
        } else { 
            throw new IOException("Could not find file " + file.getName() + " to delete");
        }

    }

    public void unzipFile(String zipFile) throws IOException {
        File inputFile = new File(zipFile);
        File targetDir = new File("src/main/resources");

        if (!targetDir.exists()) {
            throw new IOException("Can't find src/main/resources directory to unzip into");
        }

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            if (zipEntry.getName().startsWith("__MACOSX")) {
                break;
            }
            File destFile = new File(targetDir, zipEntry.getName());
            if (!zipEntry.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(destFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    public void downloadTestFiles() throws IOException {
        String testZip = download(TEST_URL);
        String trainZip = download(TRAIN_URL);

        unzipFile(testZip);
        unzipFile(trainZip);

        removeZipFile(TEST_URL);
        removeZipFile(TRAIN_URL);
    }

    public static void main (String args[]) {
        MnistDownloader md = new MnistDownloader();
        try {
            md.downloadTestFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
