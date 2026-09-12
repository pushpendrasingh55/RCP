package com.rcp.service;

import com.rcp.config.RcpProperties;
import com.rcp.util.FinancialYearUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Hands out application numbers of the form DDN-RCP-000123-2026-27.
//
// Why this is concurrency-safe: the increment below is a single SQL statement
//   INSERT ... ON CONFLICT (tenant_id, financial_year) DO UPDATE SET last_value = last_value + 1 RETURNING last_value
// PostgreSQL executes an UPSERT as one atomic operation - it takes a row-level lock on the
// (tenant_id, financial_year) row for the duration of that statement, so two concurrent transactions
// calling this at the same moment are serialised by Postgres itself, and each gets a distinct,
// strictly-increasing value. This is why we never do "SELECT max(...) then +1 then INSERT": that
// read-then-write pattern has a race window between the SELECT and the INSERT where two transactions
// can both read the same max and both try to use the same next number.
//
// Runs in its own REQUIRES_NEW transaction, deliberately separate from the enclosing "create
// application" transaction: we want the sequence row's lock held for the shortest possible time
// (a few milliseconds), not for the whole duration of validating and inserting the application row.
@Component
public class ApplicationNumberGenerator {

    private final JdbcTemplate jdbcTemplate;
    private final RcpProperties rcpProperties;

    public ApplicationNumberGenerator(JdbcTemplate jdbcTemplate, RcpProperties rcpProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.rcpProperties = rcpProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(String tenantId, LocalDate applicationDate) {
        String prefix = rcpProperties.getTenantPrefixes().get(tenantId);
        if (prefix == null) {
            // Defence in depth: TenantContext should already have rejected an unknown tenantId earlier
            // in the request, but a generator that silently produced "null-RCP-..." would be worse.
            throw new IllegalStateException("No application-number prefix configured for tenant " + tenantId);
        }

        String financialYear = FinancialYearUtil.financialYearOf(applicationDate);

        Long nextValue = jdbcTemplate.queryForObject(
                """
                INSERT INTO application_sequence (tenant_id, financial_year, last_value)
                VALUES (?, ?, 1)
                ON CONFLICT (tenant_id, financial_year)
                DO UPDATE SET last_value = application_sequence.last_value + 1
                RETURNING last_value
                """,
                Long.class, tenantId, financialYear
        );

        String paddedSequence = String.format("%06d", nextValue);
        return String.join("-", prefix, "RCP", paddedSequence, financialYear);
    }
}
