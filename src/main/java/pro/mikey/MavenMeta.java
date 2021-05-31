package pro.mikey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.StringJoiner;

@JsonSerialize
@JsonIgnoreProperties({"home"})
public record MavenMeta(
        Path home,
        String artifact,
        String groupId,
        String release,
        String latest,
        Instant updated,
        String[] versions
) {
    @JsonSerialize
    public String location() {
        return this.groupId.replace(".", "/") + "/" + this.artifact;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MavenMeta.class.getSimpleName() + "[", "]")
            .add("home=" + home)
            .add("location=" + location())
            .add("artifact='" + artifact + "'")
            .add("groupId='" + groupId + "'")
            .add("release='" + release + "'")
            .add("latest='" + latest + "'")
            .add("updated=" + updated)
            .add("versions=" + Arrays.toString(versions))
            .toString();
    }
}
