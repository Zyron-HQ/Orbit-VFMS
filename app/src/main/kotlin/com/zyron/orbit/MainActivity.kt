package com.zyron.orbit

import android.Manifest
import android.content.*
import android.content.pm.*
import android.net.*
import android.os.*
import android.provider.*
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.*
import androidx.activity.ComponentActivity
import androidx.core.app.*
import androidx.core.view.*
import androidx.core.content.*
import androidx.drawerlayout.widget.*
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.zyron.orbit.widget.FileTreeView
import com.zyron.orbit.widget.FileListView
import com.zyron.orbit.widget.InterceptableDrawerLayout
import java.io.File

class MainActivity : AppCompatActivity() {

companion object {
    var instance: AppCompatActivity? = null
    private const val REQUEST_EXTERNAL_STORAGE = 1
    private const val REQUEST_DIRECTORY_SELECTION = 2
    private const val TREE_PRIMARY = "/tree/primary:"
    private const val ROOT_DIRECTORY = "/storage/emulated/0/"
}

    private lateinit var fileTreeView: FileTreeView
    private lateinit var fileListView: FileListView
    private lateinit var fileOperationExecutor: FileOperationExecutor
    private lateinit var drawerLayout: InterceptableDrawerLayout 
    private lateinit var navigationView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        instance = this
        fileTreeView = findViewById(R.id.file_tree_view)
        fileListView = findViewById(R.id.file_list_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name)

        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        toolbar.setTitle("FileTree")
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        val fileListClickListener = FileListClickListener(this)
        fileListView.initializeFileList(ROOT_DIRECTORY, null, fileListClickListener)
        setupOnClickListeners()
        checkPermission()

        fileTreeView.setRecursiveExpansionEnabled(Preferences.isRecursiveExpansionEnabled())
        fileTreeView.setRecursiveContractionEnabled(Preferences.isRecursiveContractionEnabled())
        fileTreeView.isFileMapEnabled(Preferences.isFileMapEnabled())
        fileTreeView.isLayoutAnimationEnabled(Preferences.isLayoutAnimationEnabled())
        fileTreeView.isItemAnimatorEnabled(Preferences.isItemAnimatorEnabled())
        fileTreeView.isItemViewCachingEnabled(Preferences.isItemViewCachingEnabled())
        fileTreeView.isRecyclerItemViewEnabled(Preferences.isRecyclerItemViewEnabled())
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.application_settings -> {
                val intent = Intent(this, PreferencesActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }    

    fun setupOnClickListeners() {  
        val selectDirectory: MaterialButton = findViewById(R.id.select_directory)
        selectDirectory.setOnClickListener {
            selectDirectory()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission()
            }
        } else {
            if (!Environment.isExternalStorageManager()) {
                requestAllFilesAccess()
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE)
    }

    private fun requestAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Storage access is required to browse files. Please grant permission.", Toast.LENGTH_SHORT).show()
                    requestStoragePermission()
                } else {
                    Toast.makeText(this, "Permission denied. Please allow storage access in App Settings.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
        }
    }

    private fun selectDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_DIRECTORY_SELECTION)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_DIRECTORY_SELECTION -> {
                if (resultCode == RESULT_OK && data != null) {
                    val treeUri = data.data
                    val path = treeUri?.path?.replace(TREE_PRIMARY, ROOT_DIRECTORY)
                    val fileOperationExecutor = FileOperationExecutor(this)
                    if (path != null) {
                        fileTreeView.initializeFileTree(path, fileOperationExecutor)
                    } else {
                        Toast.makeText(this,"File Path is null", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}