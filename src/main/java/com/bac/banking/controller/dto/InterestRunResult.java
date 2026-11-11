package com.bac.banking.controller.dto;

import java.util.List;

/** Outcome of the administrator's monthly interest run. */
public record InterestRunResult(int paid, int skipped, int failed, String totalPaid, List<String> details) {
}
