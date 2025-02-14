package dev.burak.rlversionchecker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.burak.rlversionchecker.model.Bootstrap;
import dev.burak.rlversionchecker.model.Cache;
import dev.burak.rlversionchecker.model.Metadata;
import dev.burak.rlversionchecker.model.Project;
import dev.burak.rlversionchecker.model.Release;
import dev.burak.rlversionchecker.model.WorkflowDispatch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        var githubToken = System.getenv("GITHUB_TOKEN");
        if (githubToken == null) {
            throw new IOException("GITHUB_TOKEN environment variable not set");
        }

        var workflowId = System.getenv("TRIGGER_WORKFLOW_ID");
        if (workflowId == null) {
            throw new IOException("TRIGGER_WORKFLOW_ID environment variable not set");
        }

        var repoUri = System.getenv("REPO_URI");
        if (repoUri == null) {
            throw new IOException("REPO_URI environment variable not set");
        }

        var repoRef = System.getenv("REPO_REF");
        if (repoRef == null) {
            throw new IOException("REPO_REF environment variable not set");
        }

        Cache cache = null;
        var file = new File(System.getenv("CACHE_FILE"));

        var objectMapper = new ObjectMapper();
        if (file.exists()) {
            cache = objectMapper.readValue(file, Cache.class);
            log.info("Cache file found, version: {}, snapshot version: {}", cache.version(), cache.snapshotVersion());
        } else {
            log.info("Cache file not found");
        }

        var snapshotVersion = getLatestSnapshotVersion();
        log.info("Snapshot version: {}", snapshotVersion);

        var snapshotJarVersion = getLatestSnapshotJarVersion(snapshotVersion);
        log.info("Snapshot Jar version: {}", snapshotJarVersion);

        var stableVersion = getLatestStableVersion();
        log.info("Stable version: {}", stableVersion);


        var launcherRelease = getLauncherRelease(githubToken);
        var launcherVersion = launcherRelease.tagName();
        log.info("Launcher version: {}", launcherVersion);

        var shouldDownloadStable = cache == null || !cache.version().equals(stableVersion);
        var shouldDownloadSnapshot = cache == null || !cache.snapshotVersion().equals(snapshotJarVersion);
        var shouldDownloadLauncher = cache == null || !Objects.equals(cache.launcherVersion(), launcherVersion);

        var toDownload = new ArrayList<String>();
        if (shouldDownloadSnapshot) {
            toDownload.add("snapshot");
        }

        if (shouldDownloadStable) {
            toDownload.add("stable");
        }

        if (shouldDownloadLauncher) {
            toDownload.add("launcher");
        }

        if (shouldDownloadStable || shouldDownloadSnapshot || shouldDownloadLauncher) {
            log.info("Downloading: {}", toDownload);
            var dispatch = new WorkflowDispatch(repoRef, Map.of("download", String.join(",", toDownload)));
            triggerGithubPipeline(repoUri, githubToken, workflowId, dispatch);
        }

        cache = new Cache(stableVersion, snapshotJarVersion, launcherVersion);
        objectMapper.writeValue(file, cache);
    }

    private static String getLatestSnapshotVersion() throws IOException {
        var response = new OkHttpClient()
                .newCall(new Request.Builder()
                        .url("https://raw.githubusercontent.com/runelite/runelite/master/pom.xml")
                        .build())
                .execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to get RuneLite version");
        }

        var body = response.body();
        if (body == null) {
            throw new RuntimeException("Empty response");
        }

        var mapper = new XmlMapper();
        var pom = mapper.readValue(body.string(), Project.class);
        response.close();
        return pom.version();
    }

    private static String getLatestStableVersion() throws IOException {
        var response = new OkHttpClient()
                .newCall(new Request.Builder()
                        .url("https://static.runelite.net/bootstrap.json")
                        .build())
                .execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to get latest stable version");
        }

        var body = response.body();
        if (body == null) {
            throw new RuntimeException("Empty response");
        }

        var mapper = new ObjectMapper();
        var bootstrap = mapper.readValue(body.string(), Bootstrap.class);

        return bootstrap.version();
    }

    private static String getLatestSnapshotJarVersion(String version) throws IOException {
        var response = new OkHttpClient()
                .newCall(new Request.Builder()
                        .url("https://repo.runelite.net/net/runelite/injected-client/" + version + "/maven-metadata.xml")
                        .build())
                .execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed to get snapshot version");
        }

        var body = response.body();
        if (body == null) {
            throw new RuntimeException("Empty response");
        }

        var mapper = new XmlMapper();
        var metadata = mapper.readValue(body.string(), Metadata.class);
        response.close();
        return metadata.versioning().snapshotVersions().getFirst().value();
    }

    private static Release getLauncherRelease(String gitHubToken) throws IOException {
        var latestUrl = "https://api.github.com/repos/runelite/launcher/releases/latest";
        var request = new Request.Builder()
                .url(latestUrl)
                .header("Authorization", "token " + gitHubToken)
                .build();
        var client = new OkHttpClient();
        var call = client.newCall(request);

        try (var response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to get latest launcher version");
            }

            var body = response.body();
            if (body == null) {
                throw new RuntimeException("Empty response");
            }

            var mapper = new ObjectMapper();
            return mapper.readValue(body.string(), Release.class);
        }
    }

    private static void triggerGithubPipeline(String repoUri, String token, String workflowId, WorkflowDispatch body) throws IOException {
        var url = "https://api.github.com/repos/" + repoUri + "/actions/workflows/" + workflowId + "/dispatches";
        var objectMapper = new ObjectMapper();
        var json = objectMapper.writeValueAsString(body);
        var request = new Request.Builder()
                .url(url)
                .header("Authorization", "token " + token)
                .post(RequestBody.create(MediaType.get("application/json"), json))
                .build();
        var client = new OkHttpClient();
        var call = client.newCall(request);

        try (var response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to trigger GitHub pipeline");
            }
        }
    }
}
