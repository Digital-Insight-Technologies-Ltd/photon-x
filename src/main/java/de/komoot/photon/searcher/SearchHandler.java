package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import io.opentelemetry.api.trace.Span;

import java.io.IOException;
import java.util.List;

public interface SearchHandler {

    List<PhotonResult> search(PhotonRequest photonRequest, Span parentSpan) throws IOException;

    String dumpQuery(PhotonRequest photonRequest);
}
