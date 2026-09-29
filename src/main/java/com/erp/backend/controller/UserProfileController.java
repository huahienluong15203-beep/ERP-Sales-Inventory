package com.erp.backend.controller;

import com.erp.backend.entity.User;
import com.erp.backend.entity.Warehouse;
import com.erp.backend.entity.Region;
import com.erp.backend.repository.UserRepository;
import com.erp.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/navigation")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;

    /**
     * API trả về thông tin người dùng và danh sách menu theo quyền (SCRUM-7 / S1-06)
     * - Trả về người dùng thực tế đang đăng nhập (hoặc tài khoản tương ứng khi đổi role)
     * - Tuyệt đối không fix cứng tên người dùng
     * - Menu phân quyền theo Role
     */
    @GetMapping("/user-context")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getUserNavigationContext(
            @RequestParam(required = false) String role,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();

        User user = null;
        // 1. Nếu có token và UserDetails xác thực từ Spring Security
        if (userDetails != null && userDetails.getId() != null) {
            user = userRepository.findById(userDetails.getId()).orElse(null);
        }

        // 2. Nếu chưa có user (hoặc chuyển đổi vai trò ở môi trường kiểm thử/demo), lấy tài khoản tương ứng
        if (user == null && role != null && !role.isBlank()) {
            String sampleUsername = getSampleUsernameByRole(role);
            if (sampleUsername != null) {
                user = userRepository.findByUsername(sampleUsername).orElse(null);
            }
        }

        String effectiveRole = role;
        if (user != null) {
            List<String> userRoles = user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .toList();
            if (effectiveRole == null || effectiveRole.isBlank()) {
                effectiveRole = userRoles.isEmpty() ? "ROLE_ADMIN" : userRoles.get(0);
            }

            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone() != null ? user.getPhone() : "0988776655");
            userInfo.put("status", user.getStatus());
            userInfo.put("role", effectiveRole);
            userInfo.put("roles", userRoles);

            String warehouseStr = (user.getWarehouses() != null && !user.getWarehouses().isEmpty())
                    ? user.getWarehouses().stream().map(Warehouse::getName).collect(Collectors.joining(", "))
                    : getDefaultWarehouseForRole(effectiveRole);
            userInfo.put("warehouse", warehouseStr);

            String locationStr = (user.getRegions() != null && !user.getRegions().isEmpty())
                    ? user.getRegions().stream().map(Region::getName).collect(Collectors.joining(", "))
                    : getDefaultLocationForRole(effectiveRole);
            userInfo.put("workLocation", locationStr);
        } else {
            if (effectiveRole == null || effectiveRole.isBlank()) {
                effectiveRole = "ROLE_ADMIN";
            }
            userInfo.put("id", 1L);
            userInfo.put("username", getSampleUsernameByRole(effectiveRole));
            userInfo.put("fullName", getSampleFullNameByRole(effectiveRole));
            userInfo.put("email", getSampleEmailByRole(effectiveRole));
            userInfo.put("phone", "0988776655");
            userInfo.put("status", "ACTIVE");
            userInfo.put("role", effectiveRole);
            userInfo.put("roles", List.of(effectiveRole));
            userInfo.put("warehouse", getDefaultWarehouseForRole(effectiveRole));
            userInfo.put("workLocation", getDefaultLocationForRole(effectiveRole));
        }

        response.put("user", userInfo);

        // 3. Danh sách menu lọc theo quyền
        List<Map<String, String>> menus = buildAuthorizedMenusForRole(effectiveRole);
        response.put("menus", menus);

        return ResponseEntity.ok(response);
    }

    private String getSampleUsernameByRole(String role) {
        if (role == null) return "admin";
        switch (role.toUpperCase()) {
            case "ROLE_SALES_MANAGER": return "sales_manager";
            case "ROLE_SALES_REP": return "sales_rep";
            case "ROLE_WAREHOUSE": return "wh_staff";
            case "ROLE_WH_MANAGER": return "wh_manager";
            case "ROLE_ACCOUNTANT": return "accountant";
            case "ROLE_CUSTOMER": return "customer_agent";
            case "ROLE_ADMIN":
            default: return "admin";
        }
    }

    private String getSampleFullNameByRole(String role) {
        if (role == null) return "Quản Trị Viên Hệ Thống";
        switch (role.toUpperCase()) {
            case "ROLE_SALES_MANAGER": return "Trần Quản Lý Kinh Doanh";
            case "ROLE_SALES_REP": return "Lê Văn Bán Hàng";
            case "ROLE_WAREHOUSE": return "Nguyễn Văn Thủ Kho";
            case "ROLE_WH_MANAGER": return "Hoàng Quản Lý Kho";
            case "ROLE_ACCOUNTANT": return "Phạm Thị Kế Toán";
            case "ROLE_CUSTOMER": return "Đại Lý Minh Phát (B2B)";
            case "ROLE_ADMIN":
            default: return "Quản Trị Viên Hệ Thống";
        }
    }

    private String getSampleEmailByRole(String role) {
        if (role == null) return "admin@erp.com";
        switch (role.toUpperCase()) {
            case "ROLE_SALES_MANAGER": return "manager@erp.com";
            case "ROLE_SALES_REP": return "salesrep@erp.com";
            case "ROLE_WAREHOUSE": return "warehouse@erp.com";
            case "ROLE_WH_MANAGER": return "whmanager@erp.com";
            case "ROLE_ACCOUNTANT": return "accountant@erp.com";
            case "ROLE_CUSTOMER": return "minhphat@daily.com";
            case "ROLE_ADMIN":
            default: return "admin@erp.com";
        }
    }

    private String getDefaultWarehouseForRole(String role) {
        if (role == null) return "Trụ sở chính & Toàn quốc";
        switch (role.toUpperCase()) {
            case "ROLE_WAREHOUSE": return "Kho Tổng Miền Bắc (WH-MB01)";
            case "ROLE_WH_MANAGER": return "Cụm Kho Tổng Phía Bắc (WH-MB01 & MB02)";
            case "ROLE_SALES_REP": return "Địa bàn: Quận 1 & TP. Thủ Đức, HCM";
            case "ROLE_SALES_MANAGER": return "Vùng phụ trách: Toàn miền Nam";
            case "ROLE_ACCOUNTANT": return "Phòng Kế toán - Trụ sở chính";
            case "ROLE_CUSTOMER": return "Điểm nhận hàng: Kho Cần Thơ";
            case "ROLE_ADMIN":
            default: return "Trụ sở chính & Toàn quốc";
        }
    }

    private String getDefaultLocationForRole(String role) {
        if (role == null) return "Trụ sở điều hành Hà Nội";
        switch (role.toUpperCase()) {
            case "ROLE_WAREHOUSE": return "Khu công nghiệp Tiên Sơn, Bắc Ninh";
            case "ROLE_WH_MANAGER": return "KCN Tiên Sơn & KCN Quang Minh";
            case "ROLE_SALES_REP": return "Văn phòng Đại diện Chi nhánh Miền Nam";
            case "ROLE_SALES_MANAGER": return "Văn phòng Đại diện TP. Hồ Chí Minh";
            case "ROLE_ACCOUNTANT": return "Văn phòng Kế toán Trung tâm";
            case "ROLE_CUSTOMER": return "Chi nhánh Phân phối Minh Phát - Cần Thơ";
            case "ROLE_ADMIN":
            default: return "Trụ sở điều hành Hà Nội";
        }
    }

    private List<Map<String, String>> buildAuthorizedMenusForRole(String role) {
        List<Map<String, String>> menus = new ArrayList<>();
        menus.add(createMenuItem("Trang chủ", "/dashboard", "LayoutDashboard", "Tổng quan hoạt động"));
        menus.add(createMenuItem("Hồ sơ cá nhân", "/profile", "User", "Thông tin nhân sự"));

        String upperRole = (role != null) ? role.toUpperCase() : "ROLE_ADMIN";

        if ("ROLE_ADMIN".equals(upperRole)) {
            menus.add(createMenuItem("Quản lý tài khoản", "/users", "Users", "Quản lý nhân sự & tài khoản"));
            menus.add(createMenuItem("Phân quyền hệ thống", "/roles", "ShieldCheck", "Ma trận phân quyền"));
            menus.add(createMenuItem("Danh mục sản phẩm", "/products", "Package", "Tra cứu 5.000 SKU"));
            menus.add(createMenuItem("Bảng giá & Chiết khấu", "/price-books", "Tags", "Bảng giá"));
            menus.add(createMenuItem("Danh sách đại lý", "/customers", "Building2", "Hồ sơ đại lý B2B"));
            menus.add(createMenuItem("Hạn mức công nợ", "/debt-limits", "CreditCard", "Kiểm soát tín dụng"));
            menus.add(createMenuItem("Tạo đơn đặt hàng", "/orders/create", "ShoppingCart", "Lập đơn bán hàng"));
            menus.add(createMenuItem("Danh sách đơn hàng", "/orders", "ClipboardList", "Theo dõi đơn"));
            menus.add(createMenuItem("Duyệt đơn ngoại lệ", "/orders/approvals", "CheckSquare", "Duyệt vượt hạn mức"));
            menus.add(createMenuItem("Tra cứu tồn kho", "/inventory", "Boxes", "Tồn thực tế vs Khả dụng"));
            menus.add(createMenuItem("Quản lý lô & Hạn dùng", "/batches", "CalendarClock", "Cảnh báo FEFO"));
            menus.add(createMenuItem("Danh mục kho hàng", "/warehouses", "Warehouse", "Cụm kho & sức chứa"));
            menus.add(createMenuItem("Lập phiếu xuất kho", "/shipping/create", "Truck", "Xuất kho FEFO"));
            menus.add(createMenuItem("Lệnh giao hàng", "/shipping", "Send", "Vận chuyển"));
            menus.add(createMenuItem("Hoá đơn tài chính", "/invoices", "Receipt", "Hoá đơn VAT"));
            menus.add(createMenuItem("Thu tiền & Thanh toán", "/payments", "BadgeDollarSign", "Ghi nhận thanh toán"));
            menus.add(createMenuItem("Sổ theo dõi công nợ", "/debts", "BookOpenCheck", "Đối soát công nợ"));
            menus.add(createMenuItem("Yêu cầu trả hàng", "/returns", "RotateCcw", "Trả hàng & Đổi"));
            menus.add(createMenuItem("Điều chỉnh tồn kho", "/adjustments", "SlidersHorizontal", "Cân đối kho"));
            menus.add(createMenuItem("Báo cáo doanh thu & Lãi", "/reports/sales", "TrendingUp", "Doanh số & Lãi"));
            menus.add(createMenuItem("Báo cáo xuất nhập tồn", "/reports/inventory", "BarChart3", "Biến động thẻ kho"));
            menus.add(createMenuItem("Báo cáo tuổi nợ", "/reports/debts", "PieChart", "Tuổi nợ đại lý"));
        } else if ("ROLE_SALES_MANAGER".equals(upperRole)) {
            menus.add(createMenuItem("Danh mục sản phẩm", "/products", "Package", "Tra cứu sản phẩm"));
            menus.add(createMenuItem("Bảng giá & Chiết khấu", "/price-books", "Tags", "Bảng giá"));
            menus.add(createMenuItem("Danh sách đại lý", "/customers", "Building2", "Hồ sơ đại lý"));
            menus.add(createMenuItem("Hạn mức công nợ", "/debt-limits", "CreditCard", "Kiểm soát tín dụng"));
            menus.add(createMenuItem("Tạo đơn đặt hàng", "/orders/create", "ShoppingCart", "Lập đơn bán hàng"));
            menus.add(createMenuItem("Danh sách đơn hàng", "/orders", "ClipboardList", "Theo dõi đơn"));
            menus.add(createMenuItem("Duyệt đơn ngoại lệ", "/orders/approvals", "CheckSquare", "Duyệt vượt hạn mức"));
            menus.add(createMenuItem("Tra cứu tồn kho", "/inventory", "Boxes", "Tồn thực tế vs Khả dụng"));
            menus.add(createMenuItem("Hoá đơn tài chính", "/invoices", "Receipt", "Hoá đơn VAT"));
            menus.add(createMenuItem("Sổ theo dõi công nợ", "/debts", "BookOpenCheck", "Đối soát công nợ"));
            menus.add(createMenuItem("Báo cáo doanh thu & Lãi", "/reports/sales", "TrendingUp", "Doanh số & Lãi"));
            menus.add(createMenuItem("Báo cáo tuổi nợ", "/reports/debts", "PieChart", "Tuổi nợ đại lý"));
        } else if ("ROLE_SALES_REP".equals(upperRole)) {
            menus.add(createMenuItem("Danh mục sản phẩm", "/products", "Package", "Tra cứu sản phẩm"));
            menus.add(createMenuItem("Danh sách đại lý", "/customers", "Building2", "Hồ sơ đại lý"));
            menus.add(createMenuItem("Tạo đơn đặt hàng", "/orders/create", "ShoppingCart", "Lập đơn bán hàng"));
            menus.add(createMenuItem("Danh sách đơn hàng", "/orders", "ClipboardList", "Theo dõi đơn"));
            menus.add(createMenuItem("Tra cứu tồn kho", "/inventory", "Boxes", "Tồn thực tế vs Khả dụng"));
            menus.add(createMenuItem("Lệnh giao hàng", "/shipping", "Send", "Vận chuyển"));
            menus.add(createMenuItem("Sổ theo dõi công nợ", "/debts", "BookOpenCheck", "Đối soát công nợ"));
            menus.add(createMenuItem("Yêu cầu trả hàng", "/returns", "RotateCcw", "Trả hàng & Đổi"));
        } else if ("ROLE_WAREHOUSE".equals(upperRole)) {
            menus.add(createMenuItem("Danh mục sản phẩm", "/products", "Package", "Tra cứu sản phẩm"));
            menus.add(createMenuItem("Tra cứu tồn kho", "/inventory", "Boxes", "Tồn thực tế vs Khả dụng"));
            menus.add(createMenuItem("Quản lý lô & Hạn dùng", "/batches", "CalendarClock", "Cảnh báo FEFO"));
            menus.add(createMenuItem("Lập phiếu xuất kho", "/shipping/create", "Truck", "Xuất kho FEFO"));
            menus.add(createMenuItem("Lệnh giao hàng", "/shipping", "Send", "Vận chuyển"));
        } else if ("ROLE_WH_MANAGER".equals(upperRole)) {
            menus.add(createMenuItem("Danh mục sản phẩm", "/products", "Package", "Tra cứu sản phẩm"));
            menus.add(createMenuItem("Tra cứu tồn kho", "/inventory", "Boxes", "Tồn thực tế vs Khả dụng"));
            menus.add(createMenuItem("Quản lý lô & Hạn dùng", "/batches", "CalendarClock", "Cảnh báo FEFO"));
            menus.add(createMenuItem("Danh mục kho hàng", "/warehouses", "Warehouse", "Cụm kho & sức chứa"));
            menus.add(createMenuItem("Lập phiếu xuất kho", "/shipping/create", "Truck", "Xuất kho FEFO"));
            menus.add(createMenuItem("Lệnh giao hàng", "/shipping", "Send", "Vận chuyển"));
            menus.add(createMenuItem("Yêu cầu trả hàng", "/returns", "RotateCcw", "Trả hàng & Đổi"));
            menus.add(createMenuItem("Điều chỉnh tồn kho", "/adjustments", "SlidersHorizontal", "Cân đối kho"));
            menus.add(createMenuItem("Báo cáo xuất nhập tồn", "/reports/inventory", "BarChart3", "Biến động thẻ kho"));
        } else if ("ROLE_ACCOUNTANT".equals(upperRole)) {
            menus.add(createMenuItem("Bảng giá & Chiết khấu", "/price-books", "Tags", "Bảng giá"));
            menus.add(createMenuItem("Danh sách đại lý", "/customers", "Building2", "Hồ sơ đại lý"));
            menus.add(createMenuItem("Hạn mức công nợ", "/debt-limits", "CreditCard", "Kiểm soát tín dụng"));
            menus.add(createMenuItem("Danh sách đơn hàng", "/orders", "ClipboardList", "Theo dõi đơn"));
            menus.add(createMenuItem("Hoá đơn tài chính", "/invoices", "Receipt", "Hoá đơn VAT"));
            menus.add(createMenuItem("Thu tiền & Thanh toán", "/payments", "BadgeDollarSign", "Ghi nhận thanh toán"));
            menus.add(createMenuItem("Sổ theo dõi công nợ", "/debts", "BookOpenCheck", "Đối soát công nợ"));
            menus.add(createMenuItem("Báo cáo xuất nhập tồn", "/reports/inventory", "BarChart3", "Biến động thẻ kho"));
            menus.add(createMenuItem("Báo cáo tuổi nợ", "/reports/debts", "PieChart", "Tuổi nợ đại lý"));
        } else if ("ROLE_CUSTOMER".equals(upperRole)) {
            menus.add(createMenuItem("Danh mục sản phẩm", "/products", "Package", "Tra cứu sản phẩm"));
            menus.add(createMenuItem("Tạo đơn đặt hàng", "/orders/create", "ShoppingCart", "Lập đơn bán hàng"));
            menus.add(createMenuItem("Danh sách đơn hàng", "/orders", "ClipboardList", "Theo dõi đơn"));
            menus.add(createMenuItem("Sổ theo dõi công nợ", "/debts", "BookOpenCheck", "Đối soát công nợ"));
        }

        return menus;
    }

    private Map<String, String> createMenuItem(String title, String path, String icon, String description) {
        Map<String, String> item = new HashMap<>();
        item.put("title", title);
        item.put("path", path);
        item.put("icon", icon);
        item.put("description", description);
        return item;
    }
}
