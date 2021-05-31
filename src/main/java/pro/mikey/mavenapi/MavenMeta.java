package pro.mikey.mavenapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.file.Path;
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
            .add("home=" + this.home)
            .add("location=" + this.location())
            .add("artifact='" + this.artifact + "'")
            .add("groupId='" + this.groupId + "'")
            .add("release='" + this.release + "'")
            .add("latest='" + this.latest + "'")
            .add("updated=" + this.updated)
            .add("versions=" + Arrays.toString(this.versions))
            .toString();
    }
}
