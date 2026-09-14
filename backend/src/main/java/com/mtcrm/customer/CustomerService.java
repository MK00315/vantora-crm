package com.mtcrm.customer;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.common.api.PageResponse;
import com.mtcrm.common.api.PageableFactory;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomerService {
    private static final Set<String> SORTS = Set.of("name", "company", "status", "createdAt", "updatedAt");
    private final CustomerRepository customers;
    private final UserRepository users;
    private final ActivityService activities;
    private final TenantQuotaService quotas;

    public CustomerService(CustomerRepository customers, UserRepository users, ActivityService activities,
                           TenantQuotaService quotas) {
        this.customers = customers;
        this.users = users;
        this.activities = activities;
        this.quotas = quotas;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.Response> list(String query, CustomerStatus status, int page, int size,
                                                     String sort, String direction) {
        var result = customers.search(TenantContext.require(), query == null ? "" : query.trim(), status,
                PageableFactory.create(page, size, sort, direction, SORTS));
        return PageResponse.from(result, CustomerDtos.Response::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "customers", key = "T(com.mtcrm.tenant.TenantContext).require().toString() + ':' + #id")
    public CustomerDtos.Response get(UUID id) { return CustomerDtos.Response.from(find(id)); }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public CustomerDtos.Response create(CustomerDtos.Request request) {
        quotas.assertCanCreate(TenantQuotaService.Resource.CUSTOMER);
        validateOwner(request.ownerId());
        Customer customer = new Customer();
        customer.setTenantId(TenantContext.require());
        apply(customer, request);
        customers.save(customer);
        activities.record("CREATED", "CUSTOMER", customer.getId(), "Created customer " + customer.getName());
        return CustomerDtos.Response.from(customer);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()"),
            @CacheEvict(cacheNames = "customers", key = "T(com.mtcrm.tenant.TenantContext).require().toString() + ':' + #id")
    })
    public CustomerDtos.Response update(UUID id, CustomerDtos.Request request) {
        validateOwner(request.ownerId());
        Customer customer = find(id);
        apply(customer, request);
        activities.record("UPDATED", "CUSTOMER", customer.getId(), "Updated customer " + customer.getName());
        return CustomerDtos.Response.from(customer);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()"),
            @CacheEvict(cacheNames = "customers", key = "T(com.mtcrm.tenant.TenantContext).require().toString() + ':' + #id")
    })
    public void delete(UUID id) {
        Customer customer = find(id);
        customers.delete(customer);
        activities.record("DELETED", "CUSTOMER", id, "Deleted customer " + customer.getName());
    }

    public StreamingResponseBody exportCsv() {
        UUID tenantId = TenantContext.require();
        return output -> {
            var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            writer.write("id,name,email,phone,company,website,status,createdAt\r\n");
            int page = 0;
            boolean more;
            do {
                var slice = customers.findAllByTenantId(tenantId, PageRequest.of(page++, 500,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
                for (Customer c : slice) {
                    writer.write(csv(c.getId())); writer.write(','); writer.write(csv(c.getName())); writer.write(',');
                    writer.write(csv(c.getEmail())); writer.write(','); writer.write(csv(c.getPhone())); writer.write(',');
                    writer.write(csv(c.getCompany())); writer.write(','); writer.write(csv(c.getWebsite())); writer.write(',');
                    writer.write(csv(c.getStatus())); writer.write(','); writer.write(csv(c.getCreatedAt())); writer.write("\r\n");
                }
                writer.flush();
                more = slice.hasNext();
            } while (more);
        };
    }

    private Customer find(UUID id) {
        return customers.findByIdAndTenantId(id, TenantContext.require())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private void validateOwner(UUID id) {
        if (id != null && users.findByIdAndTenantId(id, TenantContext.require()).isEmpty())
            throw new com.mtcrm.common.exception.BadRequestException("Owner must be a user in your company");
    }

    private static void apply(Customer c, CustomerDtos.Request r) {
        c.setName(r.name().trim());
        c.setEmail(blankToNull(r.email()));
        c.setPhone(blankToNull(r.phone()));
        c.setCompany(blankToNull(r.company()));
        c.setWebsite(blankToNull(r.website()));
        c.setStatus(r.status());
        c.setNotes(blankToNull(r.notes()));
        c.setOwnerId(r.ownerId());
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String csv(Object value) {
        if (value == null) return "";
        String text = value.toString();
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
