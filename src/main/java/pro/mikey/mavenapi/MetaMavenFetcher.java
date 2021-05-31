package pro.mikey.mavenapi;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MetaMavenFetcher {
    private final HashSet<MavenMeta> repos = new HashSet<>();
    private Instant lastRead;

    /**
     * Hold the maven meta paths for 30 minutes to reduce fs work.
     */
    public HashSet<MavenMeta> getRepos(Path target) {
        Instant now = Instant.now();

        if (this.lastRead == null || this.lastRead.isAfter(now.plus(30, ChronoUnit.MINUTES))) {
            this.lastRead = now;
            this.repos.clear();
            this.repos.addAll(this.seekMavenMeta(target));
            return this.repos;
        }

        return this.repos;
    }

    private List<MavenMeta> seekMavenMeta(Path root) {
        try {
            return Files.walk(root)
                .filter(e -> e.getFileName().toString().equals("maven-metadata.xml"))
                .map(this::transformXml)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Nullable
    private MavenMeta transformXml(Path file) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file.toFile());

            NodeList xmlVersion = document.getElementsByTagName("versions").item(0).getChildNodes();
            String[] versions = new String[xmlVersion.getLength()];
            for (int i = 0; i < xmlVersion.getLength(); i++) {
                versions[i] = xmlVersion.item(i).getTextContent();
            }

            String lastUpdated = document.getElementsByTagName("lastUpdated").item(0).getTextContent();
            return new MavenMeta(
                file.getParent(),
                document.getElementsByTagName("artifactId").item(0).getTextContent(),
                document.getElementsByTagName("groupId").item(0).getTextContent(),
                document.getElementsByTagName("release").item(0).getTextContent(),
                document.getElementsByTagName("latest").item(0).getTextContent(),
                LocalDateTime.parse(lastUpdated, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).toInstant(ZoneOffset.UTC),
                versions
            );
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }
}
