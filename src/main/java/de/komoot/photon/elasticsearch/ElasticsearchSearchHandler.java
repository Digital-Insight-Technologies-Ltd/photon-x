package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.logging.PhotonLogger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class ElasticsearchSearchHandler implements SearchHandler {
    private final ElasticsearchClient client;
    private final String[] supportedLanguages;
    private final Tracer tracer;
    private boolean lastLenient = false;

    public ElasticsearchSearchHandler(ElasticsearchClient client, String[] languages, OpenTelemetry otel) {
        this.client = client;
        this.supportedLanguages = languages;
        this.tracer = otel.getTracer(ElasticsearchSearchHandler.class.getName());
    }

    public ElasticsearchSearchHandler(ElasticsearchClient client, String[] languages) {
        this(client, languages, OpenTelemetry.noop());
    }

    @Override
    public List<PhotonResult> search(PhotonRequest photonRequest, Span parentSpan) throws IOException {
        Span buildQuerySpan = tracer.spanBuilder("buildQuery")
                .setParent(Context.current().with(parentSpan))
                .startSpan();
        PhotonQueryBuilder queryBuilder;
        int limit, extLimit;
        try (Scope scope = buildQuerySpan.makeCurrent()){
            queryBuilder = buildQuery(photonRequest, false);
            // for the case of deduplication we need a bit more results, #300
            limit = photonRequest.getLimit();
            extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;
        } catch (Exception e) {
            buildQuerySpan.recordException(e);
            throw e;
        } finally {
            buildQuerySpan.end();
        }

        Span sendQuerySpan = tracer.spanBuilder("sendQuery")
                .setParent(Context.current().with(parentSpan))
                .startSpan();
        SearchResponse<ObjectNode> results;
        try (Scope scope = sendQuerySpan.makeCurrent()){
            results = sendQuery(queryBuilder.buildQuery(), extLimit);
        } catch (Exception e) {
            sendQuerySpan.recordException(e);
            throw e;
        } finally {
            sendQuerySpan.end();
        }

        if (results.hits().hits().isEmpty()) {
            Span sendLenientQuerySpan = tracer.spanBuilder("sendLenientQuery")
                    .setParent(Context.current().with(parentSpan))
                    .startSpan();
            try (Scope scope = sendLenientQuerySpan.makeCurrent()){
                results = sendQuery(buildQuery(photonRequest, true).buildQuery(), extLimit);
            } catch (Exception e) {
                sendLenientQuerySpan.recordException(e);
                throw e;
            } finally {
                sendLenientQuerySpan.end();
            }
        }

        List<PhotonResult> ret = new ArrayList<>();

        for (Hit<ObjectNode> hit : results.hits().hits()) {
            ret.add(new ElasticResult(hit));
        }

        return ret;
    }

    public String dumpQuery(PhotonRequest photonRequest) {
        return buildQuery(photonRequest, lastLenient).buildQuery().toString().substring(7);
    }

   public PhotonQueryBuilder buildQuery(PhotonRequest photonRequest, boolean lenient) {
       lastLenient = lenient;
       return PhotonQueryBuilder
               .builder(
                       photonRequest.getQuery(),
                       photonRequest.getLanguage(),
                       supportedLanguages,
                       lenient
               )
               .withOsmTagFilters(photonRequest.getOsmTagFilters())
               .withLayerFilters(photonRequest.getLayerFilters())
               .withLocationBias(
                       photonRequest.getLocationForBias(),
                       photonRequest.getScaleForBias(),
                       photonRequest.getZoomForBias()
               )
               .withBoundingBox(photonRequest.getBbox());
    }

    private SearchResponse<ObjectNode> sendQuery(Query query, Integer limit) throws IOException {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(Constants.PHOTON_INDEX)
                .searchType(SearchType.QueryThenFetch)
                .query(query)
                .size(limit)
                .timeout(String.format("%ss", 7));

        SearchRequest request = builder.build();
        return client.search(request, ObjectNode.class);
    }
}
