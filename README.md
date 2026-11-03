# 📱 Business Inventory Management System – Mobile Showroom

A full-stack, enterprise-grade web application designed for mobile phone showrooms and electronics retail businesses. Built with **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **Hibernate**, **Thymeleaf**, **Bootstrap 5**, and **MySQL / H2**.

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- **Java Development Kit (JDK 21 or higher)**
- Modern Web Browser (Chrome, Firefox, Edge)

### 2. Run the Application
The project includes the Maven Wrapper (`mvnw.cmd`), so no manual Maven installation is required.

Run the pre-built executable JAR:
```bash
java -jar target/inventory-0.0.1-SNAPSHOT.jar
```
*Or run via Maven wrapper:*
```bash
.\mvnw.cmd spring-boot:run
```

### 3. Open in Browser
- **Showroom Dashboard**: [http://localhost:8080](http://localhost:8080)
- **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:file:./data/mobile_showroom_db`
  - User: `sa`
  - Password: *(leave blank)*

### 4. Default Login Credentials
| Role | Username | Password |
|---|---|---|
| **Administrator** | `admin` | `admin123` |

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3.2.5, Spring MVC, Spring Security 6 |
| **Persistence / ORM** | Spring Data JPA, Hibernate 6, HikariCP |
| **Frontend** | HTML5, CSS3, JavaScript, Thymeleaf, Bootstrap 5.3, Bootstrap Icons |
| **Database** | MySQL 8.0+ ready / H2 embedded file-based persistence for immediate evaluation |
| **Build Tool** | Apache Maven 3.9+ (Maven Wrapper included) |

---

## 🏛️ Layered Architecture

```
com.showroom.inventory
│
├── config/             # Spring Security, Data Initialization & Seeder
│   ├── SecurityConfig.java
│   └── DataInitializer.java
│
├── controller/         # Spring MVC HTTP Handlers
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── ProductController.java
│   ├── InventoryController.java
│   ├── SupplierController.java
│   ├── PurchaseController.java
│   ├── SaleController.java
│   ├── ExpenseController.java
│   └── ReportController.java
│
├── service/            # Core Business Logic & Financial Calculations
│   ├── UserService.java
│   ├── ProductService.java
│   ├── InventoryService.java
│   ├── SupplierService.java
│   ├── PurchaseService.java
│   ├── SaleService.java
│   ├── ExpenseService.java
│   └── ReportService.java
│
├── repository/         # Spring Data JPA Data Access Interfaces
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── SupplierRepository.java
│   ├── PurchaseRepository.java
│   ├── PurchaseItemRepository.java
│   ├── SaleRepository.java
│   ├── SaleItemRepository.java
│   ├── ExpenseRepository.java
│   └── StockMovementRepository.java
│
├── entity/             # JPA Database Entities
│   ├── User.java
│   ├── Product.java
│   ├── Supplier.java
│   ├── Purchase.java
│   ├── PurchaseItem.java
│   ├── Sale.java
│   ├── SaleItem.java
│   ├── Expense.java
│   └── StockMovement.java
│
├── dto/                # Data Transfer Objects & Form Validation
├── enums/              # Business Enums (Categories, Roles, Movement Types)
└── exception/          # Custom Domain Exceptions & @ControllerAdvice
```

---

## 📦 Seven Core Project Modules

### 1. Product Management
- Add, update, view, and deactivate smartphones and accessories.
- Categorization: Mobile Phones, Tablets, Smart Watches, Earphones, Chargers, Cases, Power Banks.
- Validates duplicate product codes and prevents negative pricing.
- Search by product name, brand, model, or product SKU.

### 2. Stock and Inventory Management
- Real-time stock level tracking.
- Configurable minimum stock alert thresholds.
- Dedicated Low-Stock & Out-of-Stock alert dashboard.
- Manual physical stock count adjustments with audit notes.
- Complete Stock Movement audit log (tracks before/after counts, timestamps, and reference PO/Invoice numbers).

### 3. Purchase & Restocking Management
- Manage suppliers and vendor directory.
- Dynamic multi-product restocking orders.
- Automatically increases inventory stock levels upon purchase completion.
- Database transactions (`@Transactional`) ensure stock and ledger consistency.

### 4. Sales & Billing Counter (POS)
- Real-time interactive point-of-sale checkout counter.
- Dynamic stock check prevents overselling or negative inventory.
- Calculates subtotal, item totals, customizable discounts, and net bill amounts.
- Generates unique invoice numbers (e.g., `BILL-20261104-XXXX`).
- Printable, tax-compliant customer invoice with showroom terms and signature fields.

### 5. Expense Tracking
- Record recurring showroom overhead: Showroom Rent, Electricity, Staff Salaries, Marketing, Maintenance, and Office Supplies.
- Filter expenses by month and year.
- Calculates periodic operating expense totals.

### 6. Reports & Business Intelligence
- Comprehensive Profit & Loss (P&L) statement.
- **Accrual Accounting Formula**:
  $$\text{Revenue} = \sum \text{Sale Net Amount}$$
  $$\text{COGS (Cost of Goods Sold)} = \sum (\text{Sold Item Quantity} \times \text{Item Cost Price})$$
  $$\text{Gross Profit} = \text{Revenue} - \text{COGS}$$
  $$\text{Net Profit} = \text{Gross Profit} - \text{Operating Expenses}$$
- *Note:* Unsold inventory purchases are correctly preserved as assets and are not miscategorized as expenses.
- Identifies Top 5 Best-Selling and Slow-Moving SKUs.
- Transparent rule-based business recommendations engine.

### 7. Persistent Data Storage
- Permanent relational database persistence.
- Works with MySQL Server and includes reference `src/main/resources/db/schema.sql`.
- Pre-configured with H2 file-based persistence for immediate plug-and-play evaluation.

---

## 🗄️ How to Switch to MySQL Server

When you are ready to connect to your permanent local MySQL instance:

1. Open MySQL Workbench or MySQL CLI:
   ```sql
   CREATE DATABASE mobile_showroom_db;
   ```
2. Open `src/main/resources/application.properties` and uncomment the MySQL section:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mobile_showroom_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
   ```
3. Comment out the H2 configuration lines in the same file.
4. Restart the application. Hibernate will automatically generate all 9 tables in MySQL.

---

## 🎓 Viva & Demonstration Talking Points

1. **Why Layered Architecture?**
   - Keeps business calculations inside `@Service` classes instead of controllers or templates.
   - Enforces Separation of Concerns and clean testability.

2. **How is Transaction Safety Guaranteed?**
   - In `SaleService` and `PurchaseService`, methods are annotated with `@Transactional`. If an exception occurs (e.g., insufficient stock), database changes rollback completely.

3. **How is Net Profit Computed Accurately?**
   - Net profit does **not** subtract total purchases from total sales. It accurately computes **Cost of Goods Sold (COGS)** on items actually sold, subtracting showroom operating overhead from the resulting Gross Profit.
