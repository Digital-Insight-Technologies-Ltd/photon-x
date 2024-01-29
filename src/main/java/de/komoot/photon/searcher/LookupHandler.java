package de.komoot.photon.searcher;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import java.io.IOException;

public interface LookupHandler {
    PhotonResult lookup(String lookupRequest, Tracer tracer, Span parentSpan) throws IOException;
}
