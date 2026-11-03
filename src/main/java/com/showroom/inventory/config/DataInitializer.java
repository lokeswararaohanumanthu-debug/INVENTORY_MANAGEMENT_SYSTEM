package com.showroom.inventory.config;

import com.showroom.inventory.entity.*;
import com.showroom.inventory.enums.*;
import com.showroom.inventory.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ProductRepository productRepository,
                           SupplierRepository supplierRepository,
                           PurchaseRepository purchaseRepository,
                           SaleRepository saleRepository,
                           ExpenseRepository expenseRepository,
                           StockMovementRepository stockMovementRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleRepository = saleRepository;
        this.expenseRepository = expenseRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Initialize Admin User
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Showroom Manager");
            admin.setEmail("admin@mobilezonepro.com");
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
            logger.info("Default admin user created: admin / admin123");
        }

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
            po1.setPurchaseDate(LocalDate.now().minusDays(3));
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
            sale1.setSaleDate(LocalDate.now());
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
            sale2.setSaleDate(LocalDate.now());
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
            createExpense(ExpenseCategory.RENT, new BigDecimal("45000.00"), LocalDate.now().minusDays(2), "Showroom Monthly Lease Rental");
            createExpense(ExpenseCategory.ELECTRICITY, new BigDecimal("7800.00"), LocalDate.now().minusDays(1), "Commercial Electricity & AC Bill");
            createExpense(ExpenseCategory.SALARY, new BigDecimal("55000.00"), LocalDate.now().minusDays(3), "Showroom Sales Staff & Cashier Salaries");
            createExpense(ExpenseCategory.MARKETING, new BigDecimal("12000.00"), LocalDate.now().minusDays(2), "Diwali Festive Promotion & Social Ads");

            logger.info("Demo showroom catalog, transactions, and expenses seeded successfully!");
        }
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
