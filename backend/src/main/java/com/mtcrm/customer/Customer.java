package com.mtcrm.customer;

import com.mtcrm.common.domain.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer extends TenantOwnedEntity {
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 254)
    private String email;
    @Column(length = 40)
    private String phone;
    @Column(length = 160)
    private String company;
    @Column(length = 300)
    private String website;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.PROSPECT;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "owner_id")
    private java.util.UUID ownerId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public java.util.UUID getOwnerId() { return ownerId; }
    public void setOwnerId(java.util.UUID ownerId) { this.ownerId = ownerId; }
}
