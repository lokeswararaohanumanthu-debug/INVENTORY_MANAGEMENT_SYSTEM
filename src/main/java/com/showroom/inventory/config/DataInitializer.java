package com.showroom.inventory.config;

import com.showroom.inventory.entity.*;
import com.showroom.inventory.enums.*;
import com.showroom.inventory.repository.*;
import com.showroom.inventory.service.SaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Initializer running on startup.
 * Seeds initial Administrator account, realistic product catalog, suppliers,
 * stock movements, and initial sales and expenses for showroom demonstration.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository,
                           ProductRepository productRepository,
                           SupplierRepository supplierRepository,
                           PurchaseRepository purchaseRepository,
                           SaleRepository saleRepository,
                           ExpenseRepository expenseRepository,
                           StockMovementRepository stockMovementRepository,
                           ActivityLogRepository activityLogRepository,
                           ApprovalRequestRepository approvalRequestRepository,
                           SystemSettingRepository systemSettingRepository,
                           PasswordEncoder passwordEncoder,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleRepository = saleRepository;
        this.expenseRepository = expenseRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.activityLogRepository = activityLogRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Migrate H2 users table constraints & seed the 7 predefined RBAC demo accounts idempotently
        migrateUserTableConstraintAndSeedSevenRoles();

        // 2. Initialize Showroom Catalog Data if empty
        if (productRepository.count() == 0) {
            logger.info("Seeding showroom demo catalog, suppliers, and initial transactions...");

            // Create Suppliers
            Supplier s1 = new Supplier();
            s1.setName("Apple Distribution India Ltd");
            s1.setContactPerson("Rajesh Nair");
            s1.setPhone("+91 98200 11223");
            s1.setEmail("orders@appledist.in");
            s1.setAddress("Unit 402, Trade World, BKC, Mumbai");
            s1.setActive(true);
            supplierRepository.save(s1);

            Supplier s2 = new Supplier();
            s2.setName("Samsung Wholesale Electronics");
            s2.setContactPerson("Kavita Rao");
            s2.setPhone("+91 98450 33445");
            s2.setEmail("supply@samsungdist.co.in");
            s2.setAddress("Industrial Area Phase 2, Bengaluru");
            s2.setActive(true);
            supplierRepository.save(s2);

            Supplier s3 = new Supplier();
            s3.setName("Ace Mobile Accessories Wholesale");
            s3.setContactPerson("Vikram Patel");
            s3.setPhone("+91 99001 55667");
            s3.setEmail("vikram@aceaccessories.com");
            s3.setAddress("Electronics Street, SP Road, Bengaluru");
            s3.setActive(true);
            supplierRepository.save(s3);

            // Create Products
            Product p1 = createProduct("MOB-IP15P", "Apple iPhone 15 Pro", "Apple", "256GB Natural Titanium",
                    Category.MOBILE_PHONE, new BigDecimal("112000.00"), new BigDecimal("127999.00"), 12, 5,
                    "A17 Pro chip, 48MP Pro camera system, USB-C 3");

            Product p2 = createProduct("MOB-IP15", "Apple iPhone 15", "Apple", "128GB Black",
                    Category.MOBILE_PHONE, new BigDecimal("62500.00"), new BigDecimal("71999.00"), 8, 4,
                    "Dynamic Island, 48MP main camera, USB-C");

            Product p3 = createProduct("MOB-S24U", "Samsung Galaxy S24 Ultra", "Samsung", "512GB Titanium Black",
                    Category.MOBILE_PHONE, new BigDecimal("108000.00"), new BigDecimal("124999.00"), 10, 5,
                    "Galaxy AI, Snapdragon 8 Gen 3, S-Pen included");

            Product p4 = createProduct("MOB-A55", "Samsung Galaxy A55 5G", "Samsung", "128GB Awesome Navy",
                    Category.MOBILE_PHONE, new BigDecimal("31000.00"), new BigDecimal("36999.00"), 15, 5,
                    "Exynos 1480, Super AMOLED 120Hz display");

            Product p5 = createProduct("MOB-OP12", "OnePlus 12 5G", "OnePlus", "256GB Silky Black",
                    Category.MOBILE_PHONE, new BigDecimal("54000.00"), new BigDecimal("64999.00"), 6, 4,
                    "Snapdragon 8 Gen 3, 5400mAh, 100W SUPERVOOC");

            Product p6 = createProduct("MOB-RN13P", "Redmi Note 13 Pro+ 5G", "Xiaomi", "256GB Fusion Purple",
                    Category.MOBILE_PHONE, new BigDecimal("25000.00"), new BigDecimal("29999.00"), 2, 5,
                    "200MP camera, 120W HyperCharge, IP68"); // LOW STOCK (2 <= 5)

            Product p7 = createProduct("ACC-APP2", "Apple AirPods Pro (2nd Gen)", "Apple", "USB-C Case",
                    Category.EARPHONE, new BigDecimal("18500.00"), new BigDecimal("23900.00"), 14, 5,
                    "Active Noise Cancellation, Adaptive Audio");

            Product p8 = createProduct("ACC-SAM45W", "Samsung 45W Power Adapter", "Samsung", "Type-C Fast Charger",
                    Category.CHARGER, new BigDecimal("1800.00"), new BigDecimal("2999.00"), 25, 8,
                    "Super Fast Charging 2.0 with USB-C to USB-C Cable");

            Product p9 = createProduct("ACC-SPG15P", "Spigen Ultra Hybrid Case", "Spigen", "iPhone 15 Pro Clear",
                    Category.CASE_COVER, new BigDecimal("650.00"), new BigDecimal("1499.00"), 30, 10,
                    "Air Cushion Technology, Yellowing Resistant");

            Product p10 = createProduct("ACC-ANK20K", "Anker 20000mAh Power Bank", "Anker", "30W Power Delivery",
                    Category.POWER_BANK, new BigDecimal("2100.00"), new BigDecimal("3499.00"), 1, 4,
                    "Fast charge iPhones and Android phones on the go"); // LOW STOCK (1 <= 4)

            // Seed an initial Purchase Order to record Restocking
            Purchase po1 = new Purchase();
            po1.setPurchaseNumber("PO-20261101-1001");
            po1.setSupplier(s1);
            po1.setPurchaseDate(LocalDate.now(SaleService.IST_ZONE).minusDays(3));
            po1.setNotes("Opening showroom stock batch #1");
            po1.setTotalAmount(new BigDecimal("1745000.00"));

            PurchaseItem pi1 = new PurchaseItem();
            pi1.setPurchase(po1);
            pi1.setProduct(p1);
            pi1.setQuantity(10);
            pi1.setUnitPrice(p1.getPurchasePrice());
            pi1.setTotalPrice(p1.getPurchasePrice().multiply(BigDecimal.valueOf(10)));
            po1.addItem(pi1);

            PurchaseItem pi2 = new PurchaseItem();
            pi2.setPurchase(po1);
            pi2.setProduct(p2);
            pi2.setQuantity(10);
            pi2.setUnitPrice(p2.getPurchasePrice());
            pi2.setTotalPrice(p2.getPurchasePrice().multiply(BigDecimal.valueOf(10)));
            po1.addItem(pi2);
            purchaseRepository.save(po1);

            // Record initial Sales
            Sale sale1 = new Sale();
            sale1.setBillNumber("BILL-20261104-1011");
            sale1.setCustomerName("Rohan Verma");
            sale1.setCustomerPhone("+91 98765 12345");
            sale1.setSaleDate(LocalDate.now(SaleService.IST_ZONE));
            sale1.setPaymentMethod("UPI");
            sale1.setDiscount(new BigDecimal("1000.00"));

            SaleItem si1 = new SaleItem();
            si1.setSale(sale1);
            si1.setProduct(p1);
            si1.setQuantity(1);
            si1.setUnitPrice(p1.getSellingPrice());
            si1.setCostPrice(p1.getPurchasePrice());
            si1.setTotalPrice(p1.getSellingPrice());
            sale1.addItem(si1);

            SaleItem si2 = new SaleItem();
            si2.setSale(sale1);
            si2.setProduct(p9);
            si2.setQuantity(1);
            si2.setUnitPrice(p9.getSellingPrice());
            si2.setCostPrice(p9.getPurchasePrice());
            si2.setTotalPrice(p9.getSellingPrice());
            sale1.addItem(si2);

            BigDecimal subtotal1 = p1.getSellingPrice().add(p9.getSellingPrice());
            sale1.setTotalAmount(subtotal1);
            sale1.setNetAmount(subtotal1.subtract(sale1.getDiscount()));
            saleRepository.save(sale1);

            Sale sale2 = new Sale();
            sale2.setBillNumber("BILL-20261104-1012");
            sale2.setCustomerName("Pooja Sundaram");
            sale2.setCustomerPhone("+91 97410 99887");
            sale2.setSaleDate(LocalDate.now(SaleService.IST_ZONE));
            sale2.setPaymentMethod("CARD");
            sale2.setDiscount(BigDecimal.ZERO);

            SaleItem si3 = new SaleItem();
            si3.setSale(sale2);
            si3.setProduct(p3);
            si3.setQuantity(1);
            si3.setUnitPrice(p3.getSellingPrice());
            si3.setCostPrice(p3.getPurchasePrice());
            si3.setTotalPrice(p3.getSellingPrice());
            sale2.addItem(si3);

            sale2.setTotalAmount(p3.getSellingPrice());
            sale2.setNetAmount(p3.getSellingPrice());
            saleRepository.save(sale2);

            // Record initial Showroom Operational Expenses
            createExpense(ExpenseCategory.RENT, new BigDecimal("45000.00"), LocalDate.now(SaleService.IST_ZONE).minusDays(2), "Showroom Monthly Lease Rental");
            createExpense(ExpenseCategory.ELECTRICITY, new BigDecimal("7800.00"), LocalDate.now(SaleService.IST_ZONE).minusDays(1), "Commercial Electricity & AC Bill");
            createExpense(ExpenseCategory.SALARY, new BigDecimal("55000.00"), LocalDate.now(SaleService.IST_ZONE).minusDays(3), "Showroom Sales Staff & Cashier Salaries");
            createExpense(ExpenseCategory.MARKETING, new BigDecimal("12000.00"), LocalDate.now(SaleService.IST_ZONE).minusDays(2), "Diwali Festive Promotion & Social Ads");

            logger.info("Demo showroom catalog, transactions, and expenses seeded successfully!");
        }

        // Ensure existing products have barcodes populated for POS barcode scanning
        java.util.Map<String, String> defaultBarcodes = java.util.Map.of(
                "MOB-IP15P", "8901234560011",
                "MOB-IP15", "8901234560028",
                "MOB-S24U", "8901234560035",
                "MOB-A55", "8901234560042",
                "MOB-OP12", "8901234560059",
                "MOB-RN13P", "8901234560066",
                "ACC-APP2", "8901234560073",
                "ACC-SAM45W", "8901234560080",
                "ACC-SPG15P", "8901234560097",
                "ACC-ANK20K", "8901234560103"
        );
        for (Product product : productRepository.findAll()) {
            if (product.getBarcode() == null || product.getBarcode().isBlank()) {
                String assigned = defaultBarcodes.getOrDefault(product.getProductCode(), product.getProductCode());
                product.setBarcode(assigned);
                productRepository.save(product);
            }
        }

        // Backfill multi-vendor purchase orders and multi-period transactions idempotently
        seedMultiVendorPurchasesAndPeriodData();

        // Backfill RBAC ownership metadata, system settings, approval requests, and audit logs idempotently
        seedRbacDemoMetadataAndHistory();
    }

    private void migrateUserTableConstraintAndSeedSevenRoles() {
        try {
            // Drop any legacy H2 CHECK constraints on the users table so all 7 UserRole values are permitted
            var constraintNames = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                    "WHERE TABLE_NAME = 'USERS' AND CONSTRAINT_TYPE = 'CHECK'",
                    String.class
            );
            for (String cName : constraintNames) {
                jdbcTemplate.execute("ALTER TABLE USERS DROP CONSTRAINT IF EXISTS \"" + cName + "\"");
            }
            // Migrate any legacy 'MANAGER' role string to 'SHOWROOM_MANAGER'
            jdbcTemplate.update("UPDATE USERS SET ROLE = 'SHOWROOM_MANAGER' WHERE ROLE = 'MANAGER'");
        } catch (Exception ex) {
            logger.debug("H2 constraint check note: {}", ex.getMessage());
        }

        // Remove legacy 'admin' placeholder account if present so the system maintains strictly the 7 predefined demo accounts
        userRepository.findByUsername("admin").ifPresent(legacyAdmin -> {
            if (userRepository.findByUsername("admin01").isEmpty()) {
                legacyAdmin.setUsername("admin01");
                legacyAdmin.setPassword(passwordEncoder.encode("Admin@2026#01"));
                legacyAdmin.setFullName("Rajeshwari Krishnan");
                legacyAdmin.setEmail("admin01@mobilezonepro.com");
                legacyAdmin.setPhone("+91 98450 10002");
                legacyAdmin.setRole(UserRole.ADMIN);
                legacyAdmin.setActive(true);
                legacyAdmin.setProtectedAccount(false);
                userRepository.save(legacyAdmin);
            } else {
                userRepository.delete(legacyAdmin);
            }
        });

        // Create or verify the 7 Predefined Demo Accounts idempotently
        ensureDemoAccount("superadmin", "Super@2026#SA", "Vikramaditya Rao",
                "superadmin@mobilezonepro.com", "+91 98450 10001", UserRole.SUPER_ADMIN, true);

        ensureDemoAccount("admin01", "Admin@2026#01", "Rajeshwari Krishnan",
                "admin01@mobilezonepro.com", "+91 98450 10002", UserRole.ADMIN, false);

        ensureDemoAccount("manager01", "Manager@2026#01", "Aravind Swamy",
                "manager01@mobilezonepro.com", "+91 98450 10003", UserRole.SHOWROOM_MANAGER, false);

        ensureDemoAccount("salesman01", "Sales@2026#01", "Kiran Kumar",
                "salesman01@mobilezonepro.com", "+91 98450 10004", UserRole.SALESMAN, false);

        ensureDemoAccount("inventory01", "Stock@2026#01", "Deepak Verma",
                "inventory01@mobilezonepro.com", "+91 98450 10005", UserRole.INVENTORY_MANAGER, false);

        ensureDemoAccount("purchase01", "Purchase@2026#01", "Suresh Menon",
                "purchase01@mobilezonepro.com", "+91 98450 10006", UserRole.PURCHASE_OFFICER, false);

        ensureDemoAccount("accountant01", "Finance@2026#01", "Priya Deshmukh",
                "accountant01@mobilezonepro.com", "+91 98450 10007", UserRole.ACCOUNTANT, false);
    }

    private void ensureDemoAccount(String username,
                                   String rawDemoPassword,
                                   String fullName,
                                   String email,
                                   String phone,
                                   UserRole expectedRole,
                                   boolean isProtected) {
        userRepository.findByUsername(username).ifPresentOrElse(existing -> {
            boolean changed = false;
            if (existing.getRole() != expectedRole) {
                existing.setRole(expectedRole);
                changed = true;
            }
            if (isProtected && (!existing.isActive() || !existing.isProtectedAccount())) {
                existing.setActive(true);
                existing.setProtectedAccount(true);
                changed = true;
            }
            if (existing.getPhone() == null || existing.getPhone().isBlank()) {
                existing.setPhone(phone);
                changed = true;
            }
            if (existing.getEmail() == null || existing.getEmail().isBlank()) {
                existing.setEmail(email);
                changed = true;
            }
            if (changed) {
                userRepository.save(existing);
            }
        }, () -> {
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(rawDemoPassword));
            u.setFullName(fullName);
            u.setEmail(email);
            u.setPhone(phone);
            u.setRole(expectedRole);
            u.setActive(true);
            u.setProtectedAccount(isProtected);
            u.setPasswordChangeRequired(false);
            userRepository.save(u);
            logger.info("Seeded RBAC demo account '{}' with role {}", username, expectedRole.getDisplayName());
        });
    }

    private void seedRbacDemoMetadataAndHistory() {
        // 1. Backfill createdBy on Sales so Salesman 'salesman01' has own sales and Manager 'manager01' has distinct sales
        for (Sale s : saleRepository.findAll()) {
            if (s.getCreatedBy() == null || s.getCreatedBy().isBlank()) {
                if ("BILL-20261102-1022".equals(s.getBillNumber()) || "BILL-20261103-1012".equals(s.getBillNumber())) {
                    s.setCreatedBy("manager01");
                    s.setCreatedByName("Aravind Swamy");
                } else {
                    s.setCreatedBy("salesman01");
                    s.setCreatedByName("Kiran Kumar");
                }
                saleRepository.save(s);
            }
        }

        // 2. Backfill createdBy on Purchases
        for (Purchase p : purchaseRepository.findAll()) {
            if (p.getCreatedBy() == null || p.getCreatedBy().isBlank()) {
                if (p.getPurchaseNumber() != null && p.getPurchaseNumber().endsWith("1005")) {
                    p.setCreatedBy("inventory01");
                    p.setCreatedByName("Deepak Verma");
                } else {
                    p.setCreatedBy("purchase01");
                    p.setCreatedByName("Suresh Menon");
                }
                purchaseRepository.save(p);
            }
        }

        // 3. Seed System Settings idempotently
        ensureSystemSetting("SHOWROOM_BRANCH_NAME", "MobileZone Pro — Flagship Showroom",
                "Official showroom branch name displayed on tax invoices and reports", false);
        ensureSystemSetting("LARGE_STOCK_ADJUSTMENT_THRESHOLD", "15",
                "Minimum unit delta that triggers mandatory manager/admin approval for stock adjustments", true);
        ensureSystemSetting("PURCHASE_APPROVAL_THRESHOLD_INR", "500000",
                "Purchase order value threshold (INR) requiring managerial approval", true);
        ensureSystemSetting("REQUIRE_PRODUCT_DELETE_APPROVAL", "true",
                "Mandate formal approval and historical preservation before archiving/deleting products", true);
        ensureSystemSetting("ENFORCE_RBAC_STRICT_MODE", "true",
                "Enforce strict backend and frontend Role-Based Access Control across all endpoints", true);
        ensureSystemSetting("SESSION_IDLE_TIMEOUT_MINUTES", "30",
                "Maximum session inactivity window before re-authentication is required", true);

        // 4. Seed sample Approval Requests if empty
        if (approvalRequestRepository.count() == 0) {
            Product rn13p = productRepository.findByProductCode("MOB-RN13P").orElse(null);
            ApprovalRequest req1 = new ApprovalRequest();
            req1.setRequestCode("APR-20261104-1001");
            req1.setOperationType("LARGE_STOCK_ADJUSTMENT");
            req1.setModuleAffected("INVENTORY");
            req1.setTargetEntityId(rn13p != null ? rn13p.getId() : 6L);
            req1.setTargetIdentifier("MOB-RN13P - Redmi Note 13 Pro+ 5G (Current: 2 -> Requested: 20)");
            req1.setPayloadData("NEW_QTY:20");
            req1.setReason("Physical warehouse recount after festive restock verification");
            req1.setStatus("PENDING");
            req1.setRequestedByUsername("inventory01");
            req1.setRequestedByName("Deepak Verma");
            req1.setRequestedByRole(UserRole.INVENTORY_MANAGER.getDisplayName());
            req1.setRequestedAt(LocalDateTime.of(2026, 11, 4, 10, 15));
            approvalRequestRepository.save(req1);

            ApprovalRequest req2 = new ApprovalRequest();
            req2.setRequestCode("APR-20261103-1002");
            req2.setOperationType("PURCHASE_APPROVAL");
            req2.setModuleAffected("PURCHASES");
            req2.setTargetIdentifier("PO-20261102-1002 - Samsung Wholesale Electronics (₹15,90,000)");
            req2.setPayloadData("PO_AMOUNT:1590000");
            req2.setReason("High-value flagship Galaxy S24 Ultra procurement batch");
            req2.setStatus("APPROVED");
            req2.setRequestedByUsername("purchase01");
            req2.setRequestedByName("Suresh Menon");
            req2.setRequestedByRole(UserRole.PURCHASE_OFFICER.getDisplayName());
            req2.setRequestedAt(LocalDateTime.of(2026, 11, 2, 11, 0));
            req2.setReviewedByUsername("manager01");
            req2.setReviewedByName("Aravind Swamy");
            req2.setReviewedByRole(UserRole.SHOWROOM_MANAGER.getDisplayName());
            req2.setReviewedAt(LocalDateTime.of(2026, 11, 2, 11, 45));
            req2.setReviewRemarks("Verified vendor invoice #SAM-8841 and approved stock-in.");
            approvalRequestRepository.save(req2);
        }

        // 5. Seed initial Activity Logs if empty
        if (activityLogRepository.count() == 0) {
            activityLogRepository.save(new ActivityLog(
                    "superadmin", "Vikramaditya Rao", UserRole.SUPER_ADMIN.getDisplayName(),
                    "RBAC_INITIALIZED", "SECURITY",
                    "Initialized 7 predefined RBAC roles and employee accounts with BCrypt password hashing",
                    "Legacy Auth", "7 Roles Active (Super Admin, Admin, Manager, Salesman, Inventory, Purchase, Accountant)"
            ));
            activityLogRepository.save(new ActivityLog(
                    "salesman01", "Kiran Kumar", UserRole.SALESMAN.getDisplayName(),
                    "CREATE_SALE", "SALES_POS",
                    "Generated customer tax invoice BILL-20261104-1011 via POS Billing Counter",
                    "None", "Bill: BILL-20261104-1011, Customer: Rohan Verma, Payment: UPI"
            ));
            activityLogRepository.save(new ActivityLog(
                    "purchase01", "Suresh Menon", UserRole.PURCHASE_OFFICER.getDisplayName(),
                    "CREATE_PURCHASE", "PURCHASES",
                    "Recorded vendor purchase order PO-20261104-1005 from Ace Mobile Accessories Wholesale",
                    "None", "PO: PO-20261104-1005, Units Received: 21"
            ));
            activityLogRepository.save(new ActivityLog(
                    "inventory01", "Deepak Verma", UserRole.INVENTORY_MANAGER.getDisplayName(),
                    "APPROVAL_REQUESTED", "INVENTORY",
                    "Submitted large stock adjustment approval request APR-20261104-1001 for MOB-RN13P",
                    "Qty: 2", "Requested Qty: 20 (Pending Approval)"
            ));
            activityLogRepository.save(new ActivityLog(
                    "accountant01", "Priya Deshmukh", UserRole.ACCOUNTANT.getDisplayName(),
                    "CREATE_EXPENSE", "EXPENSES",
                    "Verified monthly showroom lease and staff salary expense ledger",
                    "None", "Monthly Expenses Verified: ₹1,19,800"
            ));
        }
    }

    private void ensureSystemSetting(String key, String defaultVal, String desc, boolean securityCritical) {
        if (systemSettingRepository.findBySettingKey(key).isEmpty()) {
            SystemSetting s = new SystemSetting(key, defaultVal, desc, securityCritical);
            s.setUpdatedBy("superadmin");
            systemSettingRepository.save(s);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    protected void seedMultiVendorPurchasesAndPeriodData() {
        Supplier samsungSupplier = supplierRepository.findByNameContainingIgnoreCase("Samsung")
                .stream().findFirst().orElse(null);
        Supplier aceSupplier = supplierRepository.findByNameContainingIgnoreCase("Ace Mobile")
                .stream().findFirst().orElse(null);
        Supplier appleSupplier = supplierRepository.findByNameContainingIgnoreCase("Apple")
                .stream().findFirst().orElse(null);

        Product pIp15p = productRepository.findByProductCode("MOB-IP15P").orElse(null);
        Product pIp15 = productRepository.findByProductCode("MOB-IP15").orElse(null);
        Product pS24u = productRepository.findByProductCode("MOB-S24U").orElse(null);
        Product pA55 = productRepository.findByProductCode("MOB-A55").orElse(null);
        Product pOp12 = productRepository.findByProductCode("MOB-OP12").orElse(null);
        Product pRn13p = productRepository.findByProductCode("MOB-RN13P").orElse(null);
        Product pAirpods = productRepository.findByProductCode("ACC-APP2").orElse(null);
        Product pSam45w = productRepository.findByProductCode("ACC-SAM45W").orElse(null);
        Product pSpigen = productRepository.findByProductCode("ACC-SPG15P").orElse(null);
        Product pAnker = productRepository.findByProductCode("ACC-ANK20K").orElse(null);

        // 1. Seed Purchase Orders for all registered vendors if not already present
        if (samsungSupplier != null && pS24u != null && pA55 != null && pSam45w != null
                && purchaseRepository.findByPurchaseNumber("PO-20261102-1002").isEmpty()) {
            Purchase po2 = new Purchase();
            po2.setPurchaseNumber("PO-20261102-1002");
            po2.setSupplier(samsungSupplier);
            po2.setPurchaseDate(LocalDate.of(2026, 11, 2));
            po2.setNotes("Samsung flagship & A-series handset stock batch #2 (Invoice #SAM-8841)");

            addPurchaseLine(po2, pS24u, 10);
            addPurchaseLine(po2, pA55, 15);
            addPurchaseLine(po2, pSam45w, 25);
            purchaseRepository.save(po2);
        }

        if (aceSupplier != null && pSpigen != null && pAnker != null
                && purchaseRepository.findByPurchaseNumber("PO-20261103-1003").isEmpty()) {
            Purchase po3 = new Purchase();
            po3.setPurchaseNumber("PO-20261103-1003");
            po3.setSupplier(aceSupplier);
            po3.setPurchaseDate(LocalDate.of(2026, 11, 3));
            po3.setNotes("Protective cases & fast-charge power banks batch #3 (Gate Pass #ACE-309)");

            addPurchaseLine(po3, pSpigen, 30);
            addPurchaseLine(po3, pAnker, 10);
            purchaseRepository.save(po3);
        }

        if (samsungSupplier != null && pOp12 != null && pRn13p != null
                && purchaseRepository.findByPurchaseNumber("PO-20261101-1004").isEmpty()
                && purchaseRepository.findByPurchaseNumber("PO-20261110-1004").isEmpty()) {
            Purchase po4 = new Purchase();
            po4.setPurchaseNumber("PO-20261101-1004");
            po4.setSupplier(samsungSupplier);
            po4.setPurchaseDate(LocalDate.of(2026, 11, 1));
            po4.setNotes("5G smartphone restocking batch #4 (Invoice #SAM-9104)");

            addPurchaseLine(po4, pOp12, 6);
            addPurchaseLine(po4, pRn13p, 8);
            purchaseRepository.save(po4);
        }

        if (aceSupplier != null && pAirpods != null && pSam45w != null
                && purchaseRepository.findByPurchaseNumber("PO-20261104-1005").isEmpty()
                && purchaseRepository.findByPurchaseNumber("PO-20261119-1005").isEmpty()) {
            Purchase po5 = new Purchase();
            po5.setPurchaseNumber("PO-20261104-1005");
            po5.setSupplier(aceSupplier);
            po5.setPurchaseDate(LocalDate.of(2026, 11, 4));
            po5.setNotes("Audio & charging accessories replenishment batch #5 (Gate Pass #ACE-412)");

            addPurchaseLine(po5, pAirpods, 6);
            addPurchaseLine(po5, pSam45w, 15);
            purchaseRepository.save(po5);
        }

        if (appleSupplier != null && pIp15 != null && pAirpods != null
                && purchaseRepository.findByPurchaseNumber("PO-20261018-1000").isEmpty()) {
            Purchase poOct = new Purchase();
            poOct.setPurchaseNumber("PO-20261018-1000");
            poOct.setSupplier(appleSupplier);
            poOct.setPurchaseDate(LocalDate.of(2026, 10, 18));
            poOct.setNotes("October festive pre-stocking batch (Invoice #APL-7720)");

            addPurchaseLine(poOct, pIp15, 5);
            addPurchaseLine(poOct, pAirpods, 10);
            purchaseRepository.save(poOct);
        }

        // Normalize any previously seeded future-dated Purchase Orders to valid past dates
        purchaseRepository.findByPurchaseNumber("PO-20261110-1004").ifPresent(po -> {
            po.setPurchaseNumber("PO-20261101-1004");
            po.setPurchaseDate(LocalDate.of(2026, 11, 1));
            po.setCreatedAt(LocalDateTime.of(2026, 11, 1, 11, 15, 0));
            purchaseRepository.save(po);
        });
        purchaseRepository.findByPurchaseNumber("PO-20261119-1005").ifPresent(po -> {
            po.setPurchaseNumber("PO-20261104-1005");
            po.setPurchaseDate(LocalDate.of(2026, 11, 4));
            po.setCreatedAt(LocalDateTime.of(2026, 11, 4, 10, 30, 0));
            purchaseRepository.save(po);
        });

        // 2. Seed historical Sales across Oct 21, Nov 1, Nov 2 (Day Before Yesterday), Nov 3 (Yesterday)
        if (saleRepository.findByBillNumber("BILL-20261102-1021").isEmpty()
                && saleRepository.findByBillNumber("BILL-20261110-1021").isEmpty()
                && pOp12 != null && pA55 != null && pSam45w != null) {
            Sale sMid1 = new Sale();
            sMid1.setBillNumber("BILL-20261102-1021");
            sMid1.setCustomerName("Arjun Mehta");
            sMid1.setCustomerPhone("+91 98111 22334");
            sMid1.setSaleDate(LocalDate.of(2026, 11, 2));
            sMid1.setCreatedAt(LocalDateTime.of(2026, 11, 2, 14, 20, 0));
            sMid1.setPaymentMethod("UPI");
            sMid1.setDiscount(new BigDecimal("500.00"));
            addSaleLine(sMid1, pOp12, 2);
            addSaleLine(sMid1, pSam45w, 2);
            finalizeSaleTotals(sMid1);
            saleRepository.save(sMid1);

            Sale sMid2 = new Sale();
            sMid2.setBillNumber("BILL-20261102-1022");
            sMid2.setCustomerName("Divya Krishnan");
            sMid2.setCustomerPhone("+91 98440 55661");
            sMid2.setSaleDate(LocalDate.of(2026, 11, 2));
            sMid2.setCreatedAt(LocalDateTime.of(2026, 11, 2, 17, 45, 0));
            sMid2.setPaymentMethod("CARD");
            sMid2.setDiscount(BigDecimal.ZERO);
            addSaleLine(sMid2, pA55, 3);
            finalizeSaleTotals(sMid2);
            saleRepository.save(sMid2);
        }

        if (saleRepository.findByBillNumber("BILL-20261103-1031").isEmpty()
                && saleRepository.findByBillNumber("BILL-20261120-1031").isEmpty()
                && pIp15 != null && pAirpods != null && pRn13p != null) {
            Sale sLate1 = new Sale();
            sLate1.setBillNumber("BILL-20261103-1031");
            sLate1.setCustomerName("Karthik Subramanian");
            sLate1.setCustomerPhone("+91 99002 77889");
            sLate1.setSaleDate(LocalDate.of(2026, 11, 3));
            sLate1.setCreatedAt(LocalDateTime.of(2026, 11, 3, 11, 30, 0));
            sLate1.setPaymentMethod("BANK_TRANSFER");
            sLate1.setDiscount(new BigDecimal("1500.00"));
            addSaleLine(sLate1, pIp15, 2);
            addSaleLine(sLate1, pAirpods, 2);
            finalizeSaleTotals(sLate1);
            saleRepository.save(sLate1);

            Sale sLate2 = new Sale();
            sLate2.setBillNumber("BILL-20261101-1032");
            sLate2.setCustomerName("Neha Sharma");
            sLate2.setCustomerPhone("+91 97312 44556");
            sLate2.setSaleDate(LocalDate.of(2026, 11, 1));
            sLate2.setCreatedAt(LocalDateTime.of(2026, 11, 1, 15, 10, 0));
            sLate2.setPaymentMethod("CASH");
            sLate2.setDiscount(BigDecimal.ZERO);
            addSaleLine(sLate2, pRn13p, 2);
            finalizeSaleTotals(sLate2);
            saleRepository.save(sLate2);
        }

        if (saleRepository.findByBillNumber("BILL-20261021-1001").isEmpty()
                && pIp15 != null && pS24u != null && pAirpods != null) {
            Sale sOct1 = new Sale();
            sOct1.setBillNumber("BILL-20261021-1001");
            sOct1.setCustomerName("Suresh Babu");
            sOct1.setCustomerPhone("+91 98801 33221");
            sOct1.setSaleDate(LocalDate.of(2026, 10, 21));
            sOct1.setCreatedAt(LocalDateTime.of(2026, 10, 21, 16, 10, 0));
            sOct1.setPaymentMethod("CARD");
            sOct1.setDiscount(new BigDecimal("2000.00"));
            addSaleLine(sOct1, pIp15, 2);
            addSaleLine(sOct1, pS24u, 1);
            addSaleLine(sOct1, pAirpods, 2);
            finalizeSaleTotals(sOct1);
            saleRepository.save(sOct1);

            createExpense(ExpenseCategory.RENT, new BigDecimal("45000.00"),
                    LocalDate.of(2026, 10, 5), "October Showroom Lease Rental");
            createExpense(ExpenseCategory.SALARY, new BigDecimal("55000.00"),
                    LocalDate.of(2026, 10, 28), "October Staff Salaries");
            createExpense(ExpenseCategory.ELECTRICITY, new BigDecimal("7200.00"),
                    LocalDate.of(2026, 10, 19), "October Showroom Electricity Bill");
        }

        // 3. Normalize seeded demo sales and expenses across distinct November dates so that:
        //    Today (Nov 6), Yesterday (Nov 5), Day Before Yesterday (Nov 4), Last 7 Days,
        //    This Week (Nov 2 - Nov 6), This Month (Nov 1 - Nov 6), Nov 1-10, Nov 1-15, Nov 1-30,
        //    and Last Month (Oct 2026) each produce distinct, realistic, verifiable figures.

        // Normalize historical sales to their intended calendar dates
        saleRepository.findByBillNumber("BILL-20261102-1021").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 2));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 2, 14, 20, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261110-1021").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 10));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 10, 14, 20, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261102-1022").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 2));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 2, 17, 45, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261112-1022").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 12));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 12, 17, 45, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261103-1031").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 3));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 3, 11, 30, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261120-1031").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 20));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 20, 11, 30, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261101-1032").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 1));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 1, 15, 10, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261125-1032").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 11, 25));
            s.setCreatedAt(LocalDateTime.of(2026, 11, 25, 15, 10, 0));
            saleRepository.save(s);
        });
        saleRepository.findByBillNumber("BILL-20261021-1001").ifPresent(s -> {
            s.setSaleDate(LocalDate.of(2026, 10, 21));
            s.setCreatedAt(LocalDateTime.of(2026, 10, 21, 16, 10, 0));
            saleRepository.save(s);
        });

        // Normalize BILL-20261103-6534 / BILL-20261104-6534 (11 iPhones spike) to 1 realistic iPhone 15 Pro unit
        jdbcTemplate.update("UPDATE sale_items SET quantity = 1, total_price = unit_price WHERE sale_id IN (SELECT id FROM sales WHERE bill_number IN ('BILL-20261103-6534', 'BILL-20261104-6534')) AND quantity > 1");
        jdbcTemplate.update("UPDATE sales SET total_amount = (SELECT COALESCE(SUM(total_price), 0) FROM sale_items WHERE sale_id = sales.id), net_amount = (SELECT COALESCE(SUM(total_price), 0) FROM sale_items WHERE sale_id = sales.id) - COALESCE(discount, 0) WHERE bill_number IN ('BILL-20261103-6534', 'BILL-20261104-6534')");

        // Ensure realistic, date-consistent showroom transactions exist for Today, Yesterday, and Day Before Yesterday
        LocalDate istToday = LocalDate.now(SaleService.IST_ZONE);
        ensureDailyShowroomTransactions(istToday);

        // Accurately assign expense dates based on their descriptions
        for (Expense exp : expenseRepository.findAll()) {
            String desc = exp.getDescription() != null ? exp.getDescription().toLowerCase() : "";
            if (desc.contains("october showroom lease rental")) {
                exp.setExpenseDate(LocalDate.of(2026, 10, 5));
                expenseRepository.save(exp);
            } else if (desc.contains("october staff salaries")) {
                exp.setExpenseDate(LocalDate.of(2026, 10, 28));
                expenseRepository.save(exp);
            } else if (desc.contains("october showroom electricity bill")) {
                exp.setExpenseDate(LocalDate.of(2026, 10, 19));
                expenseRepository.save(exp);
            } else if (desc.contains("sales staff & cashier salaries")) {
                exp.setExpenseDate(LocalDate.of(2026, 11, 1));
                expenseRepository.save(exp);
            } else if (desc.contains("monthly lease rental")) {
                exp.setExpenseDate(LocalDate.of(2026, 11, 2));
                expenseRepository.save(exp);
            } else if (desc.contains("commercial electricity & ac bill") && !desc.contains("maintenance")) {
                exp.setExpenseDate(LocalDate.of(2026, 11, 3));
                expenseRepository.save(exp);
            } else if (desc.contains("diwali festive promotion")) {
                exp.setExpenseDate(LocalDate.of(2026, 11, 4));
                expenseRepository.save(exp);
            } else if (desc.contains("display counter & led servicing")) {
                exp.setExpenseDate(LocalDate.of(2026, 11, 12));
                expenseRepository.save(exp);
            } else if (desc.contains("late-november local newspaper")) {
                exp.setExpenseDate(LocalDate.of(2026, 11, 18));
                expenseRepository.save(exp);
            }
        }
    }

    /**
     * Ensures that realistic, verifiable completed sales and operating expense vouchers exist
     * for Today, Yesterday, and Day Before Yesterday, dynamically aligned with the local IST calendar date.
     */
    @org.springframework.transaction.annotation.Transactional
    public void ensureDailyShowroomTransactions(LocalDate today) {
        if (today == null) {
            today = LocalDate.now(SaleService.IST_ZONE);
        }
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        Product pIp15 = productRepository.findByProductCode("MOB-IP15").orElse(null);
        Product pS24u = productRepository.findByProductCode("MOB-S24U").orElse(null);
        Product pOp12 = productRepository.findByProductCode("MOB-OP12").orElse(null);
        Product pAirpods = productRepository.findByProductCode("ACC-APP2").orElse(null);
        Product pSam45w = productRepository.findByProductCode("ACC-SAM45W").orElse(null);
        Product pSpigen = productRepository.findByProductCode("ACC-SPG15P").orElse(null);

        // 1. TODAY: Ensure completed bill & valid operating expense exist
        if (saleRepository.findBySaleDateBetween(today, today).isEmpty() && pIp15 != null && pAirpods != null) {
            String dateCode = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            Sale sToday = new Sale();
            sToday.setBillNumber("BILL-" + dateCode + "-8001");
            sToday.setCustomerName("Rohan Verma");
            sToday.setCustomerPhone("+91 98450 11223");
            sToday.setSaleDate(today);
            sToday.setCreatedAt(LocalDateTime.of(today.getYear(), today.getMonthValue(), today.getDayOfMonth(), 11, 15, 0));
            sToday.setPaymentMethod("UPI");
            sToday.setDiscount(new BigDecimal("1000.00"));
            sToday.setCreatedBy("salesman01");
            sToday.setCreatedByName("Kiran Kumar");
            addSaleLine(sToday, pIp15, 1);
            addSaleLine(sToday, pAirpods, 1);
            finalizeSaleTotals(sToday);
            saleRepository.save(sToday);
        }

        if (expenseRepository.findByExpenseDateBetween(today, today).isEmpty()) {
            createExpense(ExpenseCategory.MAINTENANCE, new BigDecimal("3500.00"), today, "Daily Showroom AC & Counter Maintenance");
        }

        // 2. YESTERDAY: Ensure completed bill & valid operating expense exist
        if (saleRepository.findBySaleDateBetween(yesterday, yesterday).isEmpty() && pS24u != null && pSpigen != null) {
            String dateCode = yesterday.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            Sale sYest = new Sale();
            sYest.setBillNumber("BILL-" + dateCode + "-8002");
            sYest.setCustomerName("Pooja Sundaram");
            sYest.setCustomerPhone("+91 97410 99887");
            sYest.setSaleDate(yesterday);
            sYest.setCreatedAt(LocalDateTime.of(yesterday.getYear(), yesterday.getMonthValue(), yesterday.getDayOfMonth(), 15, 30, 0));
            sYest.setPaymentMethod("CARD");
            sYest.setDiscount(new BigDecimal("1500.00"));
            sYest.setCreatedBy("salesman01");
            sYest.setCreatedByName("Kiran Kumar");
            addSaleLine(sYest, pS24u, 1);
            addSaleLine(sYest, pSpigen, 1);
            finalizeSaleTotals(sYest);
            saleRepository.save(sYest);
        }

        if (expenseRepository.findByExpenseDateBetween(yesterday, yesterday).isEmpty()) {
            createExpense(ExpenseCategory.OFFICE_SUPPLIES, new BigDecimal("2800.00"), yesterday, "Store Packaging Boxes, Tamper Seals & Bubble Wraps");
        }

        // 3. DAY BEFORE YESTERDAY: Ensure completed bill & valid operating expense exist
        if (saleRepository.findBySaleDateBetween(dayBefore, dayBefore).isEmpty() && pOp12 != null && pSam45w != null) {
            String dateCode = dayBefore.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            Sale sDayBefore = new Sale();
            sDayBefore.setBillNumber("BILL-" + dateCode + "-8003");
            sDayBefore.setCustomerName("Arjun Mehta");
            sDayBefore.setCustomerPhone("+91 98111 22334");
            sDayBefore.setSaleDate(dayBefore);
            sDayBefore.setCreatedAt(LocalDateTime.of(dayBefore.getYear(), dayBefore.getMonthValue(), dayBefore.getDayOfMonth(), 14, 20, 0));
            sDayBefore.setPaymentMethod("UPI");
            sDayBefore.setDiscount(new BigDecimal("500.00"));
            sDayBefore.setCreatedBy("salesman01");
            sDayBefore.setCreatedByName("Kiran Kumar");
            addSaleLine(sDayBefore, pOp12, 1);
            addSaleLine(sDayBefore, pSam45w, 1);
            finalizeSaleTotals(sDayBefore);
            saleRepository.save(sDayBefore);
        }

        if (expenseRepository.findByExpenseDateBetween(dayBefore, dayBefore).isEmpty()) {
            createExpense(ExpenseCategory.OTHER, new BigDecimal("1500.00"), dayBefore, "Staff Daily Tea & Refreshment Allowance");
        }
    }

    private void addPurchaseLine(Purchase purchase, Product product, int qty) {
        PurchaseItem item = new PurchaseItem();
        item.setPurchase(purchase);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setUnitPrice(product.getPurchasePrice());
        item.setTotalPrice(product.getPurchasePrice().multiply(BigDecimal.valueOf(qty)));
        purchase.addItem(item);

        BigDecimal currentTotal = purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO;
        purchase.setTotalAmount(currentTotal.add(item.getTotalPrice()));
    }

    private void addSaleLine(Sale sale, Product product, int qty) {
        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setUnitPrice(product.getSellingPrice());
        item.setCostPrice(product.getPurchasePrice());
        item.setTotalPrice(product.getSellingPrice().multiply(BigDecimal.valueOf(qty)));
        sale.addItem(item);
    }

    private void finalizeSaleTotals(Sale sale) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleItem item : sale.getItems()) {
            subtotal = subtotal.add(item.getTotalPrice());
        }
        sale.setTotalAmount(subtotal);
        BigDecimal disc = sale.getDiscount() != null ? sale.getDiscount() : BigDecimal.ZERO;
        sale.setNetAmount(subtotal.subtract(disc));
    }

    private Product createProduct(String code, String name, String brand, String model,
                                  Category category, BigDecimal purchasePrice, BigDecimal sellingPrice,
                                  int quantity, int minStock, String desc) {
        Product p = new Product();
        p.setProductCode(code);
        p.setName(name);
        p.setBrand(brand);
        p.setModel(model);
        p.setCategory(category);
        p.setPurchasePrice(purchasePrice);
        p.setSellingPrice(sellingPrice);
        p.setQuantity(quantity);
        p.setMinStockLevel(minStock);
        p.setDescription(desc);
        p.setActive(true);
        Product saved = productRepository.save(p);

        StockMovement sm = new StockMovement();
        sm.setProduct(saved);
        sm.setMovementType(StockMovementType.PURCHASE_IN);
        sm.setQuantity(quantity);
        sm.setStockBefore(0);
        sm.setStockAfter(quantity);
        sm.setReferenceNumber("INIT-STOCK");
        sm.setNotes("Opening inventory balance");
        stockMovementRepository.save(sm);

        return saved;
    }

    private void createExpense(ExpenseCategory cat, BigDecimal amount, LocalDate date, String desc) {
        Expense e = new Expense();
        e.setCategory(cat);
        e.setAmount(amount);
        e.setExpenseDate(date);
        e.setDescription(desc);
        expenseRepository.save(e);
    }
}
