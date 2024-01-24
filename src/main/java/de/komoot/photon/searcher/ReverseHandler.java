package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseRequest;
import io.opentelemetry.api.trace.Span;

import java.io.IOException;
import java.util.List;

public interface ReverseHandler {

    List<PhotonResult> reverse(ReverseRequest photonRequest, Span parentSpan) throws IOException;

    String dumpQuery(ReverseRequest photonRequest);
}
