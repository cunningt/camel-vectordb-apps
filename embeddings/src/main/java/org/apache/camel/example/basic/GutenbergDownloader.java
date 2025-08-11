package org.apache.camel.example.basic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class GutenbergDownloader {


    private static final int BYTE_SIZE = 2048;

    public GutenbergDownloader() {
    }

    public String download(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);

        File outputFile = new File(url.getFile());
        outputFile = new File("src/main/resources" + File.separator + outputFile.getName());
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

    public static void main (String args[]) {
        GutenbergDownloader md = new GutenbergDownloader();
        try {
            md.download("https://www.gutenberg.org/cache/epub/1513/pg1513.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
