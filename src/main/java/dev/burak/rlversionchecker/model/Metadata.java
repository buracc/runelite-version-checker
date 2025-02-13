package dev.burak.rlversionchecker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonRootName("metadata")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Metadata(Versioning versioning) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Versioning(List<SnapshotVersion> snapshotVersions) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SnapshotVersion(String extension, String value) {}
    }
}
