# 👥 ADMIN vs CASHIER - Quick Reference Guide

## 🎯 AT A GLANCE

```
┌─────────────────────────────────────────┐
│         ADMIN (Owner/Manager)           │
├─────────────────────────────────────────┤
│ ✅ Full system access                   │
│ ✅ See profits & revenue                │
│ ✅ Manage staff accounts                │
│ ✅ Change prices & inventory            │
│ ✅ Configure settings                   │
│ ✅ Delete orders & refund stock         │
│ ✅ Send email reports                   │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│        CASHIER (Staff/Employee)         │
├─────────────────────────────────────────┤
│ ✅ Create customer orders               │
│ ✅ Process payments                     │
│ ✅ Print receipts                       │
│ ✅ Clock in/out shifts                  │
│ ❌ CANNOT see profits                   │
│ ❌ CANNOT change prices                 │
│ ❌ CANNOT access settings               │
└─────────────────────────────────────────┘
```

---

## 🔑 LOGIN CREDENTIALS

### **Admin Account** (Auto-created):
```
Email: Loreta_Admin@gmail.com
Password: LoretaAdmin123
Role: ADMIN
```

### **Cashier Accounts** (Create via User Management):
```
Created by: Admin only
Login: Use email/password you set
Role: CASHIER
```

---

## 📱 DASHBOARD DIFFERENCES

### **Admin Dashboard**:
```
✅ Gross Daily Sales       [Visible]
✅ Total Orders            [Visible]
✅ Monthly Revenue         [Visible] ← Admin Only
✅ Estimated Profit        [Visible] ← Admin Only
✅ Stock Status            [Visible]
✅ Recent Transactions     [Visible]
```

### **Cashier Dashboard**:
```
✅ Gross Daily Sales       [Visible]
✅ Total Orders            [Visible]
❌ Monthly Revenue         [HIDDEN]
❌ Estimated Profit        [HIDDEN]
✅ Stock Status            [Visible]
✅ Recent Transactions     [Visible]
```

---

## 🔐 MENU ACCESS

### **Admin Menu** (☰):
```
✅ Dashboard
✅ Recent Transactions
✅ Create Order
✅ My Shifts
✅ Menu List
   ✅ Add Item
   ✅ Add Category
✅ Inventory
   ✅ Sales Report
✅ Settings
   ✅ User Management
   ✅ Printer Settings
✅ Sign Out
```

### **Cashier Menu** (☰):
```
✅ Dashboard
✅ Recent Transactions
✅ Create Order
✅ My Shifts
❌ Add Item            [HIDDEN]
❌ Add Category        [HIDDEN]
❌ Inventory           [HIDDEN]
❌ Sales Report        [HIDDEN]
❌ User Management     [HIDDEN]
❌ Printer Settings    [HIDDEN]
✅ Sign Out
```

---

## ⚡ COMMON TASKS

### **Admin Tasks**:

**Create Cashier Account**:
```
1. Login as admin (Loreta_Admin@gmail.com)
2. ☰ Menu → Settings → User Management
3. Tap + button
4. Enter: Name, Email, Password
5. Tap "Create Account"
6. Give credentials to employee
```

**Delete Cashier Account**:
```
1. User Management
2. Find cashier in list
3. Tap 🗑️ delete icon
4. Confirm deletion
```

**Delete Order** (Mistakes/Refunds):
```
1. Recent Transactions
2. Open order
3. Tap "Delete Order & Refund Stock"
4. Confirm
5. Stock automatically returned
```

**Email Sales Report**:
```
1. ☰ Menu → Inventory → Sales Report
2. Tap 📧 (print icon)
3. Enter email address
4. Tap "Send Report"
```

**View Profits**:
```
1. Dashboard → See "Estimated Profit" card
2. Or: Sales Report → See full breakdown
```

---

### **Cashier Tasks**:

**Clock In (Start Shift)**:
```
1. Login
2. ☰ Menu → My Shifts
3. Tap "🕐 Clock In"
4. Shift starts
```

**Clock Out (End Shift)**:
```
1. ☰ Menu → My Shifts
2. Tap "🕐 Clock Out"
3. See total hours worked
```

**Create Order**:
```
1. Dashboard → + Button
2. Add items to cart
3. Proceed to Order Summary
4. Enter payment details
5. Tap "Charge"
6. Receipt prints automatically
```

**View Shift History**:
```
1. ☰ Menu → My Shifts
2. Scroll down to see past shifts
3. See: Date, Time, Duration
```

---

## 🚫 WHAT CASHIERS CANNOT DO

When cashier tries to access admin features:

**Trying to Access Inventory**:
```
⚠️ Admin access required.
This feature is restricted to administrators only.
[Screen closes immediately]
```

**Trying to Access Sales Report**:
```
⚠️ Admin access required.
This feature is restricted to administrators only.
[Screen closes immediately]
```

**Trying to Access Settings**:
```
Menu items are hidden - cashier never sees them
```

---

## 💡 WHY ROLES MATTER

### **Protects Your Business**:
1. **Staff can't see profits** → Don't know your margins
2. **Staff can't change prices** → Prevents theft via discounts
3. **Staff can't delete data** → Prevents cover-ups
4. **Staff can't add admins** → You control access
5. **Track work hours** → Know who worked when

### **Professional Operations**:
- Clear separation of duties
- Accountability through shifts
- Secure data management
- Industry-standard RBAC

---

## 🧪 TESTING SCENARIOS

### **Scenario 1: Hire New Employee**
```
Admin:
1. Login as admin
2. User Management → Create account
3. Name: "Maria Santos"
4. Email: "maria@loreta.com"
5. Password: "maria123"
6. Give credentials to Maria

Maria (Cashier):
1. Login with maria@loreta.com / maria123
2. See limited dashboard
3. Can create orders ✅
4. Cannot see profits ❌
5. Cannot access inventory ❌
```

### **Scenario 2: Employee Shift**
```
Cashier arrives:
1. Login
2. My Shifts → Clock In
3. Work throughout day
4. Create orders, process payments
5. End of shift → Clock Out
6. See total hours: "8 hours 15 minutes"
```

### **Scenario 3: Owner Reviews Day**
```
Admin (End of Day):
1. View dashboard → See daily sales
2. Check Estimated Profit card
3. Open Sales Report → See breakdown
4. Email report to yourself
5. Review shift history → Who worked today
6. Check inventory → Restock if needed
```

### **Scenario 4: Mistake Order Deletion**
```
Customer: "I want to cancel my order"

Admin:
1. Recent Transactions
2. Find the order
3. Tap "Delete Order"
4. Confirm deletion
5. Stock automatically refunded ✅
6. Tell customer "Order cancelled"
```

---

## 📊 BUSINESS SCENARIOS

### **Small Cafe (1-2 employees)**:
```
You: ADMIN account
Staff: 1-2 CASHIER accounts
Setup: 10 minutes
Benefit: Track who sold what
```

### **Medium Cafe (3-5 employees)**:
```
You: ADMIN account
Manager: ADMIN account (trusted)
Staff: 3-4 CASHIER accounts
Setup: 20 minutes
Benefit: Full accountability, shift tracking
```

### **Growing Business (5+ employees)**:
```
Owner: ADMIN account
Managers: ADMIN accounts (2-3)
Cashiers: CASHIER accounts (5+)
Setup: 30 minutes
Benefit: Complete business protection
```

---

## 🎯 QUICK DECISIONS

**Q: Should I give staff admin access?**
**A:** NO! Only give admin to yourself and highly trusted managers.

**Q: What if cashier needs to edit inventory?**
**A:** They ask you. You login as admin and make changes.

**Q: Can cashiers see how much we make?**
**A:** NO. Profit and revenue are hidden from cashiers.

**Q: What if I accidentally delete an order?**
**A:** Stock is automatically refunded, but order data is gone. Be careful!

**Q: Do cashiers need to clock in/out?**
**A:** Optional, but recommended for tracking work hours.

**Q: Can I have multiple admins?**
**A:** YES! Create ADMIN role users via User Management.

---

## ✅ VERIFICATION CHECKLIST

**Before Opening**:
- [ ] Created all cashier accounts
- [ ] Tested admin login
- [ ] Tested cashier login
- [ ] Verified profit cards hidden for cashiers
- [ ] Verified admin menus hidden for cashiers
- [ ] Tested shift clock in/out
- [ ] Tested delete order (admin)
- [ ] Confirmed cashiers can create orders

**Daily Operations**:
- [ ] Cashiers clock in
- [ ] Orders processed smoothly
- [ ] Receipts print
- [ ] Cashiers clock out
- [ ] Admin reviews reports
- [ ] Check shift hours

---

## 🎉 YOU'RE PROTECTED!

Your business data is now secure with:
✅ Role-based access control
✅ Permission enforcement
✅ Shift accountability
✅ Audit trails
✅ Professional security

**Ready for serious business!** 🏪🔒💙

---

**For detailed implementation**: See `ROLE_BASED_ACCESS_CONTROL_COMPLETE.md`
**For complete scope**: See `COMPLETE_SCOPE_IMPLEMENTATION.md`

