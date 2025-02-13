package dev.burak.rlversionchecker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Release(List<Asset> assets, @JsonProperty("tag_name") String tagName) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asset(String name, @JsonProperty("browser_download_url") String browserDownloadUrl) {
    }
}
