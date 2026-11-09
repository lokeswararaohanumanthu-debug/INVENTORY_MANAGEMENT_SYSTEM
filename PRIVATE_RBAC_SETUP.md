# MobileZone Pro — Private RBAC Development & Demo Accounts Reference

> **SECURITY NOTICE**: This document is for local development, QA verification, and academic evaluation only. Demo credentials have been completely removed from the public `/login` page to prevent credential exposure.

## 1. Seven Predefined User Roles & Demo Credentials

All employees authenticate through the single common login endpoint: `http://localhost:8080/login`.

| # | Predefined Role (`UserRole`) | Employee Name | Demo User ID | Temporary Password | Hierarchy Level |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 1 | `SUPER_ADMIN` | Vikramaditya Rao | `superadmin` | `Super@2026#SA` | 100 |
| 2 | `ADMIN` | Ananya Sharma | `admin01` | `Admin@2026#01` | 80 |
| 3 | `SHOWROOM_MANAGER` | Rajeshwari Nair | `manager01` | `Manager@2026#01` | 60 |
| 4 | `SALESMAN` (Salesman / Cashier) | Karthikeya Verma | `salesman01` | `Sales@2026#01` | 30 |
| 5 | `INVENTORY_MANAGER` | Suresh Kumar | `inventory01` | `Stock@2026#01` | 40 |
| 6 | `PURCHASE_OFFICER` | Meenakshi Iyer | `purchase01` | `Purchase@2026#01` | 40 |
| 7 | `ACCOUNTANT` (Accountant / Finance) | Prashanth Deshmukh | `accountant01` | `Finance@2026#01` | 50 |

## 2. Password Security & First-Login Policy

- All passwords are stored as salted **BCrypt** hashes (`BCryptPasswordEncoder`) in the `users` table.
- New employee accounts created via `/users` or seeded for demonstration are flagged with `passwordChangeRequired = true`.
- Employees can update their temporary password at any time via `/profile/change-password`, which clears the temporary password flag and logs `PASSWORD_CHANGED` in the immutable audit trail (`/activity-logs`).

