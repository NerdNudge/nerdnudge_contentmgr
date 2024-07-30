package com.neurospark.nerdnudge.contentmgr.service;

import java.util.Map;

public interface QuotesService {
    public Map<String, String> getQuoteOfTheDay();

    public Map<String, String> getQuoteById(String id);
}
