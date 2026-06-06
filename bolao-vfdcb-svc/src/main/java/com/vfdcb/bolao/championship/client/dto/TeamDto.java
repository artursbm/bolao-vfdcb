package com.vfdcb.bolao.championship.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamDto(Long id, String name, String tla, String crest) {

}
