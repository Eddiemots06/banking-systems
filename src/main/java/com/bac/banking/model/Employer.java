package com.bac.banking.model;

import java.util.Objects;

/**
 * Employment details for a customer. Required before a ChequeAccount can be opened.
 */
public class Employer {
    private String companyName;
    private String companyAddress;
    private String jobTitle;

    public Employer(String companyName, String companyAddress, String jobTitle) {
        setCompanyName(companyName);
        setCompanyAddress(companyAddress);
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }
        this.companyName = companyName.trim();
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        if (companyAddress == null || companyAddress.isBlank()) {
            throw new IllegalArgumentException("Company address is required");
        }
        this.companyAddress = companyAddress.trim();
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employer other)) return false;
        return companyName.equals(other.companyName) && companyAddress.equals(other.companyAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyName, companyAddress);
    }

    @Override
    public String toString() {
        return companyName + ", " + companyAddress;
    }
}
