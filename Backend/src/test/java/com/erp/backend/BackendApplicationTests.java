package com.erp.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	// TEST 1: ADMIN được phép xem giá vốn -> Kỳ vọng HTTP 200 OK
	@Test
	@DisplayName("S1-05: ADMIN có quyền xem giá vốn và biên lợi nhuận (200 OK)")
	@WithMockUser(roles = "ADMIN")
	void testAdminCanAccessCostPrice() throws Exception {
		mockMvc.perform(get("/api/test/cost-price"))
				.andExpect(status().isOk());
	}

	// TEST 2: SALES_MANAGER được phép xem giá vốn -> Kỳ vọng HTTP 200 OK
	@Test
	@DisplayName("S1-05: SALES_MANAGER có quyền xem giá vốn và biên lợi nhuận (200 OK)")
	@WithMockUser(roles = "SALES_MANAGER")
	void testSalesManagerCanAccessCostPrice() throws Exception {
		mockMvc.perform(get("/api/test/cost-price"))
				.andExpect(status().isOk());
	}

	// TEST 3: WAREHOUSE (Kho) xem giá vốn -> Bị chặn đúng thiết kế -> Kỳ vọng HTTP
	// 403 Forbidden
	@Test
	@DisplayName("S1-05: WAREHOUSE không có quyền xem giá vốn -> Bị chặn 403 Forbidden")
	@WithMockUser(roles = "WAREHOUSE")
	void testWarehouseCannotAccessCostPrice() throws Exception {
		mockMvc.perform(get("/api/test/cost-price"))
				.andExpect(status().isForbidden());
	}
}
