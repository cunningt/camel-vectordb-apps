package demo.baseball.loader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import io.milvus.v2.service.vector.response.SearchResp;

public class WebpageCreator {

    File webpageDirectory;

    public WebpageCreator(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        Files.createDirectories(path);

        webpageDirectory = path.toFile();

        if (!webpageDirectory.exists()) {
            throw new IOException("Tried to create directory " + path.toAbsolutePath() + " but was unable to");
        }
    }

    private String header(String name) throws IOException {
        File headerFile = new File("src/main/resources/header.html");

        String result = Files.readString(headerFile.toPath());
        result = result.replaceAll("%NAME_TOKEN%", name);
        return result;

    }

    private String footer() throws IOException {
        File footerFile = new File("src/main/resources/footer.html");
        return Files.readString(footerFile.toPath());
    }

    public BigDecimal logWebPage(SearchResp searchR, HashMap<String,HistoricalPlayer> historicals, FangraphsPlayer fg) throws IOException {

		File webpage = new File(webpageDirectory, fg.getPlayerID() + ".html");
		FileWriter fw = new FileWriter(webpage);
		fw.write(header(fg.getName()));
 
        fw.write("\n\n  <tr class=\"highlighted-row\">");
        fw.write("      <td class=\"name-cell\">" + fg.getName() + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">" + fg.getAge() + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">2025</td>\n");
        fw.write("      <td><span class=\"level-pro\">" + fg.getLevel() + "</span></td>\n");
        fw.write("      <td class=\"numeric-cell\">---</td>\n");
        fw.write("      <td class=\"numeric-cell\">" + fg.getIso() + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">" + fg.getWoba() + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">" + fg.getBabip() + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">" + (fg.getBbPercentage() * 100) + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">" + (fg.getkPercentage() * 100) + "</td>\n");
        fw.write("      <td class=\"numeric-cell\">---</td>\n");
        fw.write("  </tr>\n");

		BigDecimal scoredTotal = new BigDecimal(0);
		List<List<SearchResp.SearchResult>> searchResults = searchR.getSearchResults();
		for (List<SearchResp.SearchResult> results : searchResults) {
			for (SearchResp.SearchResult result : results) {

				String playerID = (String) result.getId().toString();

				HistoricalPlayer hp = historicals.get(playerID);

                fw.write("\n\n  <tr>");
                fw.write("      <td class=\"name-cell\">" + hp.getName() + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + hp.getAge() + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + hp.getYear() + "</td>\n");
                fw.write("      <td><span class=\"level-pro\">" + hp.getLevel() + "</span></td>\n");
                fw.write("      <td class=\"numeric-cell\">" + result.getScore() + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + hp.getIso() + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + hp.getWOBA() + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + hp.getBabip() + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + (hp.getBbPercentage() * 100) + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + (hp.getkPercentage() * 100) + "</td>\n");
                fw.write("      <td class=\"numeric-cell\">" + hp.getWar() + "</td>\n");
                fw.write("  </tr>\n");

                BigDecimal warBigDecimal = new BigDecimal(hp.getWar());

				scoredTotal = scoredTotal.add(warBigDecimal);
			}
		}


        fw.write("  </tbody>\n");
        fw.write("</table>\n");
        fw.write("</div>\n");
        fw.write("<div class=\"stats-info\">\n");
        fw.write("<strong>Score: " + scoredTotal + "</strong><br><br>");
        fw.write("<strong>Stats Key:</strong> ISO = Isolated Power | wOBA = Weighted On-Base Average | BABIP = Batting Average on Balls in Play | BB% = Walk Rate | K% = Strikeout Rate | WAR = Wins Above Replacement\n");
        fw.write("</div>\n");
        fw.write("</div>\n");
		fw.write(footer());
		fw.flush();
		fw.close();

		return scoredTotal;
	}
}
