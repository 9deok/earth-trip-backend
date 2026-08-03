package com.earthtrip.wallet.adapter.out.persistence.template;

import java.io.Serializable;

record PreparationSuggestionDismissalId(String suggestionId, String userId)
    implements Serializable { }
