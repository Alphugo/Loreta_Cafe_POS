package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loretacafe.pos.ui.transactions.TransactionsViewModel;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display list of recent transactions
 */
public class RecentTransactionsActivity extends AppCompatActivity
        implements TransactionAdapter.OnTransactionClickListener {

    private RecyclerView rvTransactions;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TransactionAdapter adapter;
    private ImageView btnBack;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAdd;
    private TransactionsViewModel transactionsViewModel;
    private Chip chipToday, chipYesterday, chipLast7Days, chipThisMonth, chipCustom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_transactions);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        setupViewModel();
        setupFilterChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void initializeViews() {
        rvTransactions = findViewById(R.id.rvTransactions);
        emptyState = findViewById(R.id.emptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnBack = findViewById(R.id.btnBack);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAdd = findViewById(R.id.fabAdd);
        chipToday = findViewById(R.id.chipToday);
        chipYesterday = findViewById(R.id.chipYesterday);
        chipLast7Days = findViewById(R.id.chipLast7Days);
        chipThisMonth = findViewById(R.id.chipThisMonth);
        chipCustom = findViewById(R.id.chipCustom);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // FAB is hidden - plus sign is handled by bottom navigation (nav_add)

        // Pull to refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Transactions refreshed", Toast.LENGTH_SHORT).show();
            });

            swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_history);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Navigate to Dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                // Already here
                return true;
            } else if (itemId == R.id.nav_add) {
                // Navigate to Create Order
                Intent intent = new Intent(this, CreateOrderActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                Intent intent = new Intent(this, MenuActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(this, InventoryActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    /**
     * Setup filter chips
     */
    private void setupFilterChips() {
        chipToday.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                transactionsViewModel.setFilter(TransactionsViewModel.FilterType.TODAY);
                uncheckOtherChips(chipToday);
            }
        });

        chipYesterday.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                transactionsViewModel.setFilter(TransactionsViewModel.FilterType.YESTERDAY);
                uncheckOtherChips(chipYesterday);
            }
        });

        chipLast7Days.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                transactionsViewModel.setFilter(TransactionsViewModel.FilterType.LAST_7_DAYS);
                uncheckOtherChips(chipLast7Days);
            }
        });

        chipThisMonth.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                transactionsViewModel.setFilter(TransactionsViewModel.FilterType.THIS_MONTH);
                uncheckOtherChips(chipThisMonth);
            }
        });

        chipCustom.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                showCustomDateRangeDialog();
                uncheckOtherChips(chipCustom);
            }
        });
    }

    private void uncheckOtherChips(Chip selectedChip) {
        if (chipToday != selectedChip) chipToday.setChecked(false);
        if (chipYesterday != selectedChip) chipYesterday.setChecked(false);
        if (chipLast7Days != selectedChip) chipLast7Days.setChecked(false);
        if (chipThisMonth != selectedChip) chipThisMonth.setChecked(false);
        if (chipCustom != selectedChip) chipCustom.setChecked(false);
    }

    private void showCustomDateRangeDialog() {
        // Show date picker dialog for custom range
        // For now, just show a toast - can be enhanced with actual date picker
        Toast.makeText(this, "Custom date range picker coming soon", Toast.LENGTH_SHORT).show();
        chipCustom.setChecked(false);
        chipToday.setChecked(true); // Default back to Today
    }

    /**
     * Load transactions
     */
    private void setupViewModel() {
        transactionsViewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);
        transactionsViewModel.getTransactions().observe(this, transactions -> {
            List<Transaction> list = transactions != null ? transactions : new ArrayList<>();
            if (list.isEmpty()) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setVisibility(View.GONE);
                }
                rvTransactions.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setVisibility(View.VISIBLE);
                }
                rvTransactions.setVisibility(View.VISIBLE);
                adapter.setTransactions(list);
            }
        });
    }

    /**
     * Handle transaction item click - Navigate to detail screen
     */
    @Override
    public void onTransactionClick(Transaction transaction) {
        Intent intent = new Intent(this, TransactionDetailActivity.class);
        intent.putExtra("transaction", transaction);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
