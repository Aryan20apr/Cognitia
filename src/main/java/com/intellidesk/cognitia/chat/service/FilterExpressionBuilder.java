package com.intellidesk.cognitia.chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.intellidesk.cognitia.chat.models.dtos.AccessPolicy;

public final class FilterExpressionBuilder {

    private FilterExpressionBuilder() {}

    public static String build(AccessPolicy policy, String sourceFormat) {
        List<String> clauses = new ArrayList<>();

        clauses.add("tenantId == '" + policy.tenantId().toString() + "'");

        if (!policy.unrestricted()) {
            clauses.add(buildDepartmentClause(policy.departmentNames()));
            clauses.add("classificationRank <= " + policy.clearanceRank());
        }

        if (sourceFormat != null && !sourceFormat.isBlank()) {
            clauses.add("sourceFormat == '" + sourceFormat.strip() + "'");
        }

        return String.join(" && ", clauses);
    }

    private static String buildDepartmentClause(Set<String> departmentNames) {
        if (departmentNames.size() == 1) {
            return "department == '" + departmentNames.iterator().next() + "'";
        }
        List<String> parts = departmentNames.stream()
            .map(name -> "department == '" + name + "'")
            .toList();
        return "(" + String.join(" || ", parts) + ")";
    }
}
