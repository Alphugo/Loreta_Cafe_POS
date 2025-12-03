package com.loretacafe.pos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.loretacafe.pos.data.firebase.FirebaseAuthRepository;
import com.loretacafe.pos.data.session.SessionManager;
import com.loretacafe.pos.security.PermissionManager;
import com.loretacafe.pos.ui.transactions.TransactionsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private TextView tvSeeAll;
    private TextView tvGrossDailySales;
    private TextView tvTotalOrders;
    private TextView tvMonthlyRevenue;
    private TextView tvEstimatedProfit;
    private TextView tvStocksCount; // Now used for stocks status text
    private RecyclerView rvTransactions;
    private LinearLayout llEmptyState;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNavigation;
    private androidx.cardview.widget.CardView cardGrossDailySales;
    
    private TransactionAdapter transactionAdapter;
    private List<Transaction> recentTransactions = new ArrayList<>();
    private TransactionsViewModel transactionsViewModel;
    private FirebaseAuthRepository firebaseAuthRepository;
    
    // Role-based access control
    private PermissionManager permissionManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize role-based access control
        permissionManager = new PermissionManager(this);
        sessionManager = new SessionManager(this);

        // Initialize Firebase Auth Repository
        try {
            firebaseAuthRepository = new FirebaseAuthRepository();
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Firebase not available", e);
            firebaseAuthRepository = null;
        }

        // Initialize views
        initializeViews();

        // Apply role-based UI restrictions
        applyRoleBasedRestrictions();

        // Setup navigation drawer
        setupNavigationDrawer();

        // Setup listeners
        setupClickListeners();

        // Setup bottom navigation
        setupBottomNavigation();
        setupViewModel();
        
        // Setup network change listener for automatic backend discovery
        setupNetworkListener();
    }
    
    /**
     * Setup network change listener to automatically discover backend and sync pending data when network changes
     */
    private void setupNetworkListener() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(android.net.Network network) {
                        runOnUiThread(() -> {
                            android.util.Log.d("DashboardActivity", "Network available - starting backend discovery and sync");
                            // Discover backend server
                            com.loretacafe.pos.data.remote.ApiConfig.startDiscovery(DashboardActivity.this);
                            
                            // Sync pending transactions (unified account - sync offline data when online)
                            syncPendingTransactions();
                        });
                    }
                    
                    @Override
                    public void onLost(android.net.Network network) {
                        runOnUiThread(() -> {
                            android.util.Log.d("DashboardActivity", "Network lost - will retry on reconnect");
                        });
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Error setting up network listener", e);
        }
    }
    
    /**
     * Sync pending transactions to backend (unified account - ensures offline data syncs when online)
     */
    private void syncPendingTransactions() {
        new Thread(() -> {
            try {
                com.loretacafe.pos.di.RepositoryProvider provider = 
                    ((com.loretacafe.pos.PosApp) getApplication()).getRepositoryProvider();
                com.loretacafe.pos.data.repository.SyncRepository syncRepository = 
                    provider.getSyncRepository();
                if (syncRepository != null) {
                    android.util.Log.d("DashboardActivity", "Syncing pending transactions...");
                    syncRepository.syncPending();
                    android.util.Log.d("DashboardActivity", "Pending transactions sync completed");
                }
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Error syncing pending transactions", e);
            }
        }).start();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        tvSeeAll = findViewById(R.id.tvSeeAll);
        tvGrossDailySales = findViewById(R.id.tvGrossDailySales);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvMonthlyRevenue = findViewById(R.id.tvMonthlyRevenue);
        tvEstimatedProfit = findViewById(R.id.tvEstimatedProfit);
        tvStocksCount = findViewById(R.id.tvStocksStatus);
        rvTransactions = findViewById(R.id.rvTransactions);
        llEmptyState = findViewById(R.id.llEmptyState);
        fabAdd = findViewById(R.id.fabAdd);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        cardGrossDailySales = findViewById(R.id.cardGrossDailySales);
        
        // Verify critical views are not null
        if (tvGrossDailySales == null || tvTotalOrders == null || tvMonthlyRevenue == null) {
            android.util.Log.e("DashboardActivity", "Critical views are null!");
            Toast.makeText(this, "Layout error: Missing views", Toast.LENGTH_LONG).show();
        }
        
        // Setup Stocks card click listener
        findViewById(R.id.cardStocks).setOnClickListener(v -> {
            Intent intent = new Intent(this, InventoryActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Setup RecyclerView
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setNestedScrollingEnabled(false);
        
        transactionAdapter = new TransactionAdapter(transaction -> {
            // Navigate to transaction detail
            Intent intent = new Intent(DashboardActivity.this, TransactionDetailActivity.class);
            intent.putExtra("transaction", transaction);
            startActivity(intent);
        });
        
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                // Already on dashboard
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_recent_transactions) {
                Intent intent = new Intent(this, RecentTransactionsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_create_order) {
                Intent intent = new Intent(this, CreateOrderActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_shift_management) {
                Intent intent = new Intent(this, ShiftManagementActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_add_item) {
                Intent intent = new Intent(this, EditItemActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_add_category) {
                Intent intent = new Intent(this, CategoriesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(this, InventoryActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_sales_report) {
                Intent intent = new Intent(this, SalesReportActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                // Close right-side drawer
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_user_management) {
                Intent intent = new Intent(this, UserManagementActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                // Close right-side drawer
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_printer_settings) {
                Intent intent = new Intent(this, PrinterSettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                // Close right-side drawer
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            } else if (itemId == R.id.nav_sign_out) {
                handleSignOut();
                // Close right-side drawer
                drawerLayout.closeDrawer(GravityCompat.END);
                return true;
            }
            
            return false;
        });
    }

    private void setupClickListeners() {
        // Menu button click - Open right-side navigation drawer
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // See all transactions click
        tvSeeAll.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, RecentTransactionsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Floating action button click
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CreateOrderActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Gross Daily Sales card click - navigate to Sales Report with today's date
        if (cardGrossDailySales != null) {
            cardGrossDailySales.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, SalesReportActivity.class);
                // SalesReportActivity defaults to today's date (LocalDate.now())
                // so no need to pass extra, but we can pass it for clarity
                intent.putExtra("selectedDate", java.time.LocalDate.now().toString());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    private void setupBottomNavigation() {
        // Set Home/Dashboard as selected
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on dashboard/home
                return true;
            } else if (itemId == R.id.nav_history) {
                // Navigate to Recent Transactions
                Intent intent = new Intent(DashboardActivity.this, RecentTransactionsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.nav_add) {
                // Navigate to Create Order
                Intent intent = new Intent(DashboardActivity.this, CreateOrderActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.nav_menu) {
                // Navigate to Menu Activity
                Intent intent = new Intent(DashboardActivity.this, MenuActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.nav_inventory) {
                // Navigate to Inventory screen
                Intent intent = new Intent(DashboardActivity.this, InventoryActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }

            return false;
        });
    }

    private void setupViewModel() {
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        transactionsViewModel.getTransactions().observe(this, transactions -> {
            recentTransactions = transactions != null ? transactions : new ArrayList<>();
            renderTransactions();
        });
        
        // Gross Daily Sales
        transactionsViewModel.getGrossDailySales().observe(this, sales -> {
            if (tvGrossDailySales != null) {
                double value = sales != null ? sales : 0;
                tvGrossDailySales.setText(String.format(Locale.getDefault(), "₱ %,.2f", value));
            }
        });
        
        // Total Orders
        transactionsViewModel.getTotalOrders().observe(this, orders -> {
            if (tvTotalOrders != null) {
                int count = orders != null ? orders : 0;
                tvTotalOrders.setText(String.valueOf(count));
            }
        });
        
        // Monthly Revenue
        transactionsViewModel.getMonthlyRevenue().observe(this, revenue -> {
            if (tvMonthlyRevenue != null) {
                double value = revenue != null ? revenue : 0;
                tvMonthlyRevenue.setText(String.format(Locale.getDefault(), "₱ %,.2f", value));
            }
        });
        
        // Estimated Profit
        transactionsViewModel.getTodayProfit().observe(this, profit -> {
            if (tvEstimatedProfit != null) {
                double value = profit != null ? profit : 0;
                tvEstimatedProfit.setText(String.format(Locale.getDefault(), "₱ %,.2f", value));
            }
        });
        
        // Stock Status
        transactionsViewModel.getStockStatus().observe(this, status -> {
            if (tvStocksCount != null) {
                if (status != null) {
                    tvStocksCount.setText(status);
                } else {
                    tvStocksCount.setText(R.string.dashboard_stock_default);
                }
            }
        });
    }

    private void renderTransactions() {
        if (recentTransactions.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            tvSeeAll.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            tvSeeAll.setVisibility(View.VISIBLE);

            List<Transaction> limitedTransactions = recentTransactions.size() > 3
                    ? recentTransactions.subList(0, 3)
                    : recentTransactions;

            transactionAdapter.setTransactions(limitedTransactions);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // LiveData automatically updates when database changes
        // This method ensures UI is refreshed when returning to dashboard
        // The observer in setupViewModel() will automatically receive updates
        android.util.Log.d("DashboardActivity", "onResume: Dashboard refreshed");
    }

    /**
     * Apply role-based UI restrictions
     * Hides admin-only features from cashiers to protect business data
     */
    private void applyRoleBasedRestrictions() {
        boolean isAdmin = permissionManager.isAdmin();
        
        android.util.Log.d("DashboardActivity", "Applying role restrictions. Is Admin: " + isAdmin);
        
        // Hide admin-only menu items for cashiers
        if (!isAdmin) {
            android.view.Menu menu = navigationView.getMenu();
            
            // Hide admin-only features
            menu.findItem(R.id.nav_add_item).setVisible(false);
            menu.findItem(R.id.nav_add_category).setVisible(false);
            menu.findItem(R.id.nav_inventory).setVisible(false);
            menu.findItem(R.id.nav_user_management).setVisible(false);
            menu.findItem(R.id.nav_printer_settings).setVisible(false);
            
            // Hide profit-sensitive cards
            View cardEstimatedProfit = findViewById(R.id.cardEstimatedProfit);
            View cardMonthlyRevenue = findViewById(R.id.cardMonthlyRevenue);
            
            if (cardEstimatedProfit != null) {
                cardEstimatedProfit.setVisibility(View.GONE);
            }
            if (cardMonthlyRevenue != null) {
                cardMonthlyRevenue.setVisibility(View.GONE);
            }
            
            android.util.Log.d("DashboardActivity", "Cashier UI restrictions applied");
        } else {
            android.util.Log.d("DashboardActivity", "Admin has full access");
        }
    }

    private void handleSignOut() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    performSignOut();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void performSignOut() {
        // Sign out from Firebase
        if (firebaseAuthRepository != null) {
            try {
                firebaseAuthRepository.logout();
                android.util.Log.d("DashboardActivity", "Signed out from Firebase");
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Error signing out from Firebase", e);
            }
        }

        // Navigate to login screen
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
        
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
