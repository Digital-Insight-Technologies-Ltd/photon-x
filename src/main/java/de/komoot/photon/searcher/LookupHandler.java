package de.komoot.photon.searcher;

import io.opentelemetry.api.trace.Span;

import java.io.IOException;

public interface LookupHandler {
    PhotonResult lookup(String lookupRequest, Span parentSpan) throws IOException;
}
