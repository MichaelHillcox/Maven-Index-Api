package pro.mikey;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public class MavenApi {
    private static final MetaMavenFetcher MAVEN_FETCHER = new MetaMavenFetcher();

    public static void main(String[] args) {
        Path mavenRoot = Paths.get("./maven");
        Javalin app = Javalin.create().start(7000);

        JavalinJackson.configure(new ObjectMapper().registerModule(new JavaTimeModule()));

        app.get("/", ctx -> {
            HashSet<MavenMeta> repos = MAVEN_FETCHER.getRepos(mavenRoot);
            ctx.json(repos);
        });

        app.get("/repo/:name/files", ctx -> {
            HashSet<MavenMeta> repos = MAVEN_FETCHER.getRepos(mavenRoot);
            Optional<MavenMeta> name = repos.stream().filter(e -> e.artifact().equals(ctx.pathParam("name"))).findFirst();

            name.ifPresentOrElse((e) -> {
                HashMap<String, String[]> files = new HashMap<>();
                for (String version : e.versions()) {
                    files.put(version, e.home().resolve(version).toFile().list());
                }

                ctx.json(new MavenFiles(
                    e.latest(),
                    e.release(),
                    e.updated(),
                    files
                ));
            }, () -> ctx.status(404).json(new ResError("Nothing found")));
        });
    }

    static record ResError(String message) {}
    static record MavenFiles(
        String latest,
        String release,
        Instant updated,
        HashMap<String, String[]> files
    ) {}
}
