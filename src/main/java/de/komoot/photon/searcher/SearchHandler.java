package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import java.io.IOException;
import java.util.List;

public interface SearchHandler {

    List<PhotonResult> search(PhotonRequest photonRequest, Tracer tracer, Span parentSpan) throws IOException;

    String dumpQuery(PhotonRequest photonRequest);
}
