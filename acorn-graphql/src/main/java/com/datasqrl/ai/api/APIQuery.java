package com.datasqrl.ai.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines the API query for a function
 */
public interface APIQuery {

  String query();

}
