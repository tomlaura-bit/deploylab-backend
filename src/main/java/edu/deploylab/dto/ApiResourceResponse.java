package edu.deploylab.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** Stable API response that keeps database rows out of controller contracts and adds navigation metadata. */
public final class ApiResourceResponse {
    private final Map<String,Object> fields;
    private final Map<String,LinkResponse> links;

    public ApiResourceResponse(Map<String,Object> fields,String self) {
        this.fields=Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        this.links=Map.of("self",new LinkResponse(self));
    }

    @JsonAnyGetter public Map<String,Object> fields() { return fields; }
    @JsonProperty("_links") public Map<String,LinkResponse> links() { return links; }
}
