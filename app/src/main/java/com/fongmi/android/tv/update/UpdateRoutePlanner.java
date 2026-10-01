package com.fongmi.android.tv.update;

import java.util.ArrayList;
import java.util.List;

public final class UpdateRoutePlanner {

    private UpdateRoutePlanner() {
    }

    public static List<UpdateTarget> plan(String source, String githubUrl, OciArtifact artifact, List<GithubProxy.Config> githubProxy, String ociEndpoint) {
        List<UpdateTarget> routes = new ArrayList<>();
        String normalized = UpdateSource.normalize(source);
        if (UpdateSource.GITHUB.equals(normalized)) {
            addGithub(routes, githubUrl, githubProxy);
            addOci(routes, artifact, ociEndpoint);
        } else {
            addOci(routes, artifact, ociEndpoint);
            addGithub(routes, githubUrl, githubProxy);
        }
        return routes;
    }

    private static void addGithub(List<UpdateTarget> routes, String githubUrl, List<GithubProxy.Config> proxy) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) return;
        for (GithubProxy.Config config : proxy) {
            try {
                String url = config.rewrite(githubUrl);
                if (!routes.stream().anyMatch(item -> url.equals(item.url))) routes.add(UpdateTarget.github(url));
            } catch (Exception ignored) {
            }
        }
    }

    private static void addOci(List<UpdateTarget> routes, OciArtifact artifact, String endpoint) {
        if (artifact == null || !artifact.isValid() || endpoint == null || endpoint.trim().isEmpty()) return;
        try {
            routes.add(UpdateTarget.oci(endpoint, artifact));
        } catch (Exception ignored) {
        }
    }
}
