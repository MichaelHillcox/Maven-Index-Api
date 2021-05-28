package pro.mikey;
import io.javalin.Javalin;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MavenApi {
    public static void main(String[] args) {

        Path mavenRoot = Paths.get("/Users/michael/Documents/GitHub/maven-index-api/maven");
        System.out.println(seekMavenMeta(mavenRoot));
//        Javalin app = Javalin.create().start(7000);
//        app.get("/", ctx -> ctx.result("Hello World"));
    }

    private static List<MavenMeta> seekMavenMeta(Path root) {
        try {
            return Files.walk(root)
                    .filter(e -> e.getFileName().toString().equals("maven-metadata.xml"))
                    .map(MavenApi::transformXml)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Nullable
    private static MavenMeta transformXml(Path file) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file.toFile());
            return new MavenMeta(document.getElementsByTagName("artifactId").item(0).getTextContent(), file.resolve("../"));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }
}
