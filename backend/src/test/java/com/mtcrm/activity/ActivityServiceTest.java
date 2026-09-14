package com.mtcrm.activity;

import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.tenant.TenantQuotaService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ActivityServiceTest {
    private final ActivityService service = new ActivityService(mock(ActivityRepository.class), mock(TenantQuotaService.class));

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> service.list(-1, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("page");
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> service.list(0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size");
        assertThatThrownBy(() -> service.list(0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size");
    }
}
