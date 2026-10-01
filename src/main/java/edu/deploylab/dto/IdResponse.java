package edu.deploylab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

public record IdResponse(UUID id,@JsonProperty("_links") Map<String,LinkResponse> links) {
    public static IdResponse of(UUID id,String self) { return new IdResponse(id,Map.of("self",new LinkResponse(self))); }
}
