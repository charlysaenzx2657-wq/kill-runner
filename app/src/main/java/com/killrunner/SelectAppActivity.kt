package com.killrunner

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.killrunner.databinding.ActivitySelectAppBinding
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

class SelectAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectAppBinding
    private lateinit var adapter: AppAdapter
    private var allApps: List<AppInfo> = emptyList()

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_APP_NAME = "extra_app_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecycler()
        setupSearch()
        loadApps()
    }

    private fun setupRecycler() {
        adapter = AppAdapter(emptyList()) { app ->
            val result = Intent().apply {
                putExtra(EXTRA_PACKAGE, app.packageName)
                putExtra(EXTRA_APP_NAME, app.name)
            }
            setResult(RESULT_OK, result)
            finish()
        }
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateList(filtered)
    }

    private fun loadApps() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.recyclerApps.visibility = View.GONE

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val activities = pm.queryIntentActivities(intent, 0)

            val apps = activities
                .map { resolveInfo ->
                    AppInfo(
                        name = resolveInfo.loadLabel(pm).toString(),
                        packageName = resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(pm)
                    )
                }
                .filter { it.packageName != packageName } // excluir nuestra propia app
                .sortedBy { it.name.lowercase() }
                .distinctBy { it.packageName }

            handler.post {
                allApps = apps
                adapter.updateList(apps)
                binding.layoutLoading.visibility = View.GONE
                binding.recyclerApps.visibility = View.VISIBLE
            }
        }
    }
}
