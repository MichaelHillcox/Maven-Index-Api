package pro.mikey;

import java.nio.file.Path;

public record MavenMeta(
        String artifact,
        Path home
) {
}
