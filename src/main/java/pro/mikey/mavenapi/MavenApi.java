package pro.mikey.mavenapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

public class MavenApi {
    private static final MetaMavenFetcher MAVEN_FETCHER = new MetaMavenFetcher();

    public static void main(String[] args) {
        // Get the maven director from the args
        String configArg = Arrays.stream(args).filter(e -> e.contains("--config=")).findFirst().map(e -> e.replace("--config=", "")).orElse("./config.json");
        Path configPath = Path.of(configArg);

        ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

        Config config;
        try {
            config = mapper.readValue(configPath.toFile(), Config.class);
        } catch (IOException e) {
            System.out.println("Config file not found or not readable...");
            return;
        }

        Path mavenRoot = Paths.get(config.mavenDir());
        if (!Files.exists(mavenRoot) || !Files.isReadable(mavenRoot)) {
            System.out.println("Your maven root [" + config.mavenDir() + "] does not exist or is not readable");
            return;
        }

        // Configure Javalin
        Javalin app = Javalin.create();
        app.config.contextPath = config.contextPath() == null
            ? "/"
            : config.contextPath();
        app.start(7000);

        // Configure the json parser
        JavalinJackson.configure(mapper);

        // Displays a distinct list of groups found in our maven path
        app.get("/repos", ctx -> {
            HashSet<MavenMeta> repos = MAVEN_FETCHER.getRepos(mavenRoot);

            ctx.json(repos.stream().map(MavenMeta::groupId).distinct().collect(Collectors.toList()));
        });

        // Using the /repos endpoint you can now find the projects for a specific group / domain
        app.get("/repos/:group/projects", ctx -> {
            HashSet<MavenMeta> repos = MAVEN_FETCHER.getRepos(mavenRoot);
            String group = ctx.pathParam("group");
            if (repos.stream().noneMatch(e -> e.groupId().equals(group))) {
                ctx.status(404).json(new ResError("Not found"));
                return;
            }

            ctx.json(repos.stream().filter(e -> e.groupId().equals(group)).map(MavenMeta::artifact).collect(Collectors.toList()));
        });

        // Gives maven meta data for a specific project
        app.get("/repo/:group/:artifact", ctx -> findByIdentifier(mavenRoot, ctx.pathParam("group"), ctx.pathParam("artifact"))
            .ifPresentOrElse(ctx::json, () -> ctx.status(404).json(new ResError("Not found"))));


        // Physically walks the specific project files to return a paginated list of physical
        // disc files.
        app.get("/repo/:group/:artifact/files", ctx -> {
            findByIdentifier(mavenRoot, ctx.pathParam("group"), ctx.pathParam("artifact")).ifPresentOrElse((e) -> {
                String pageParam = ctx.queryParam("page");

                // Pagination
                int page = pageParam == null
                    ? 1
                    : Math.max(1, Integer.parseInt(pageParam));

                int results = e.versions().length;
                int limit = 50;
                int offset = (page - 1) * limit;
                int remaining = Math.max(0, results - (page * limit));

                HashMap<String, String[]> files = new HashMap<>();
                for (int i = offset; i < Math.min(page * limit, results); i++) {
                    files.put(e.versions()[i], e.home().resolve(e.versions()[i]).toFile().list());
                }

                ctx.json(new Pagination<>(e, files, new PageInfo(page, results, limit, offset, remaining)));
            }, () -> ctx.status(404).json(new ResError("Nothing found")));
        });
    }

    private static Optional<MavenMeta> findByIdentifier(Path path, String group, String artifact) {
        HashSet<MavenMeta> repos = MAVEN_FETCHER.getRepos(path);

        return repos.stream()
            .filter(e -> e.groupId().equals(group) && e.artifact().equals(artifact))
            .findFirst();
    }

    static record ResError(String message) {
    }

    static record Pagination<K, T>(
        K meta,
        T items,
        PageInfo page
    ) {
    }

    static record PageInfo(
        int page,
        int results,
        int limit,
        int offset,
        int remaining
    ) {
    }
}
