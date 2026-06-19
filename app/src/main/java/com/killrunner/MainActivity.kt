package com.killrunner

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.killrunner.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "kill_runner_prefs"
        const val KEY_PACKAGE = "selected_package"
        const val KEY_APP_NAME = "selected_app_name"
        const val REQUEST_SELECT_APP = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setSupportActionBar(binding.toolbar)
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            restoreSelectedApp()
            setupListeners()
        } catch (e: Exception) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Error al iniciar")
                .setMessage(e.toString())
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun restoreSelectedApp() {
        val pkg = prefs.getString(KEY_PACKAGE, null)
        val name = prefs.getString(KEY_APP_NAME, null)
        if (pkg != null && name != null) {
            updateSelectedAppUI(pkg, name)
        }
    }

    private fun setupListeners() {
        binding.cardSelectApp.setOnClickListener {
            val intent = Intent(this, SelectAppActivity::class.java)
            startActivityForResult(intent, REQUEST_SELECT_APP)
        }
        binding.btnLaunch.setOnClickListener {
            val pkg = prefs.getString(KEY_PACKAGE, null) ?: return@setOnClickListener
            launchAndClean(pkg)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_APP && resultCode == RESULT_OK) {
            val pkg = data?.getStringExtra(SelectAppActivity.EXTRA_PACKAGE) ?: return
            val name = data.getStringExtra(SelectAppActivity.EXTRA_APP_NAME) ?: return
            prefs.edit().putString(KEY_PACKAGE, pkg).putString(KEY_APP_NAME, name).apply()
            updateSelectedAppUI(pkg, name)
        }
    }

    private fun updateSelectedAppUI(pkg: String, name: String) {
        binding.tvAppName.text = name
        binding.tvAppPackage.text = pkg
        binding.btnLaunch.isEnabled = true
        try {
            val icon: Drawable = packageManager.getApplicationIcon(pkg)
            binding.imgSelectedApp.setImageDrawable(icon)
            binding.imgSelectedApp.background = null
            binding.imgSelectedApp.setPadding(0, 0, 0, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            binding.imgSelectedApp.setImageResource(R.drawable.ic_placeholder_app)
        }
    }

    private fun launchAndClean(protectedPackage: String) {
        binding.cardStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.cleaning)
        binding.btnLaunch.isEnabled = false
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            handler.post { launchApp(protectedPackage) }
            Thread.sleep(800)
            val result = killBackgroundProcesses(protectedPackage)
            handler.post {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = getString(R.string.done)
                binding.tvKilledCount.text = result.first.toString()
                binding.tvRamFreed.text = formatRam(result.second)
                binding.btnLaunch.isEnabled = true
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.cardStatus.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                }, 3000)
            }
        }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(this)
        }
    }

    private fun killBackgroundProcesses(protectedPackage: String): Pair<Int, Long> {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memBefore = getAvailableRam()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var killedCount = 0
        for (app in installedApps) {
            val pkg = app.packageName
            if (pkg == protectedPackage || pkg == packageName || isSystemCritical(pkg)) continue
            try {
                activityManager.killBackgroundProcesses(pkg)
                killedCount++
            } catch (e: Exception) { }
        }
        Thread.sleep(300)
        val freed = maxOf(0L, getAvailableRam() - memBefore)
        return Pair(killedCount, freed)
    }

    private fun isSystemCritical(pkg: String): Boolean {
        return listOf("android", "com.android.systemui", "com.android.phone",
            "com.android.launcher", "com.android.settings", "com.android.inputmethod")
            .any { pkg.startsWith(it) }
    }

    private fun getAvailableRam(): Long {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return mi.availMem
    }

    private fun formatRam(bytes: Long): String {
        return if (bytes < 1024 * 1024) "${bytes / 1024} KB"
        else String.format("%.0f MB", bytes / (1024.0 * 1024.0))
    }
}
