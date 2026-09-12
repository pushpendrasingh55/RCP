package com.rcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

// Binds the "rcp.*" block from application.yml. Keeping these as configuration (not constants
// scattered through the code) is what lets a new tenant be onboarded, or the search page-size limit
// changed, without touching Java source.
@Configuration
@ConfigurationProperties(prefix = "rcp")
public class RcpProperties {

    // city/tenantId -> application-number prefix, e.g. "dehradun" -> "DDN".
    private Map<String, String> tenantPrefixes;

    private Search search = new Search();

    public Map<String, String> getTenantPrefixes() {
        return tenantPrefixes;
    }

    public void setTenantPrefixes(Map<String, String> tenantPrefixes) {
        this.tenantPrefixes = tenantPrefixes;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public static class Search {
        private int defaultLimit = 20;
        private int maxLimit = 100;

        public int getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }

        public int getMaxLimit() {
            return maxLimit;
        }

        public void setMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
        }
    }
}
