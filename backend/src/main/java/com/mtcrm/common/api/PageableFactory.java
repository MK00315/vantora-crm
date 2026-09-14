package com.mtcrm.common.api;

import com.mtcrm.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableFactory {
    private PageableFactory() {}

    public static Pageable create(int page, int size, String sort, String direction, Set<String> allowed) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",", 2);
            sort = parts[0];
            direction = parts[1];
        }
        if (!allowed.contains(sort)) throw new BadRequestException("Unsupported sort field: " + sort);
        Sort.Direction dir;
        try { dir = Sort.Direction.fromString(direction); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("direction must be asc or desc"); }
        return PageRequest.of(page, size, Sort.by(dir, sort));
    }
}
