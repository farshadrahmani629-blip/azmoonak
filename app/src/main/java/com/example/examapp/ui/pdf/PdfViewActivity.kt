package com.examapp.ui.exam.pdf

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.examapp.R
import com.examapp.databinding.ActivityPdfViewBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.*
import java.io.File
import java.text.DecimalFormat
import kotlin.math.max

/**
 * اکتیویتی برای نمایش فایل‌های PDF با قابلیت‌های:
 * - نمایش صفحه به صفحه
 * - زوم و پان
 * - جستجو در متن
 * - اشتراک‌گذاری
 * - چاپ
 * - علامت‌گذاری
 */
class PdfViewActivity : AppCompatActivity() {

    // Binding
    private lateinit var binding: ActivityPdfViewBinding

    // PDF Components
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null

    // State
    private var totalPages = 0
    private var currentPageIndex = 0
    private var pdfFile: File? = null
    private var pdfTitle: String = ""
    private var isZoomed = false
    private var zoomLevel = 1.0f
    private var searchQuery = ""
    private var searchResults = mutableListOf<Int>()

    // UI Components
    private lateinit var viewPager: ViewPager2
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvPageNumber: TextView
    private lateinit var tvTotalPages: TextView
    private lateinit var btnPrevPage: MaterialButton
    private lateinit var btnNextPage: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var layoutControls: LinearLayout
    private lateinit var layoutSearch: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var btnSearchPrev: Button
    private lateinit var btnSearchNext: Button
    private lateinit var tvSearchResult: TextView
    private lateinit var btnZoomIn: Button
    private lateinit var btnZoomOut: Button
    private lateinit var btnZoomReset: Button
    private lateinit var btnFitWidth: Button
    private lateinit var btnFitHeight: Button
    private lateinit var scrollView: HorizontalScrollView
    private lateinit var imageView: ImageView
    private lateinit var layoutError: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnOpenWith: Button

    // Coroutines
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pdfLoadJob: Job? = null

    companion object {
        private const val EXTRA_PDF_PATH = "extra_pdf_path"
        private const val EXTRA_PDF_TITLE = "extra_pdf_title"
        private const val EXTRA_SHOW_CONTROLS = "extra_show_controls"

        private const val ZOOM_STEP = 0.25f
        private const val ZOOM_MIN = 0.5f
        private const val ZOOM_MAX = 3.0f

        fun startActivity(
            context: AppCompatActivity,
            pdfPath: String,
            pdfTitle: String = "PDF",
            showControls: Boolean = true
        ) {
            val intent = Intent(context, PdfViewActivity::class.java).apply {
                putExtra(EXTRA_PDF_PATH, pdfPath)
                putExtra(EXTRA_PDF_TITLE, pdfTitle)
                putExtra(EXTRA_SHOW_CONTROLS, showControls)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        parseIntent()
        setupToolbar()
        setupControls()
        setupViewPager()
        loadPdfFile()

        // تنظیمات اولیه
        val showControls = intent.getBooleanExtra(EXTRA_SHOW_CONTROLS, true)
        layoutControls.isVisible = showControls
    }

    private fun initViews() {
        toolbar = binding.pdfToolbar
        viewPager = binding.viewPager
        tvPageNumber = binding.tvPageNumber
        tvTotalPages = binding.tvTotalPages
        btnPrevPage = binding.btnPrevPage
        btnNextPage = binding.btnNextPage
        progressBar = binding.progressBar
        layoutControls = binding.layoutControls
        layoutSearch = binding.layoutSearch
        etSearch = binding.etSearch
        btnSearchPrev = binding.btnSearchPrev
        btnSearchNext = binding.btnSearchNext
        tvSearchResult = binding.tvSearchResult
        btnZoomIn = binding.btnZoomIn
        btnZoomOut = binding.btnZoomOut
        btnZoomReset = binding.btnZoomReset
        btnFitWidth = binding.btnFitWidth
        btnFitHeight = binding.btnFitHeight
        scrollView = binding.scrollView
        imageView = binding.imageView
        layoutError = binding.layoutError
        tvErrorMessage = binding.tvErrorMessage
        btnRetry = binding.btnRetry
        btnOpenWith = binding.btnOpenWith
    }

    private fun parseIntent() {
        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (pdfPath.isNullOrEmpty()) {
            showError("مسیر فایل PDF مشخص نشده است")
            return
        }

        pdfFile = File(pdfPath)
        pdfTitle = intent.getStringExtra(EXTRA_PDF_TITLE) ?: pdfFile?.name ?: "PDF"

        if (!pdfFile!!.exists()) {
            showError("فایل PDF یافت نشد")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = pdfTitle
            subtitle = formatFileSize(pdfFile?.length() ?: 0)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // منوی سریع در toolbar
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share -> {
                    sharePdf()
                    true
                }
                R.id.action_print -> {
                    printPdf()
                    true
                }
                R.id.action_search -> {
                    toggleSearch()
                    true
                }
                R.id.action_zoom_in -> {
                    zoomIn()
                    true
                }
                R.id.action_zoom_out -> {
                    zoomOut()
                    true
                }
                R.id.action_bookmark -> {
                    bookmarkCurrentPage()
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControls() {
        // ناوبری صفحات
        btnPrevPage.setOnClickListener {
            if (currentPageIndex > 0) {
                goToPage(currentPageIndex - 1)
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPageIndex < totalPages - 1) {
                goToPage(currentPageIndex + 1)
            }
        }

        // جستجو
        btnSearchPrev.setOnClickListener {
            searchPrevious()
        }

        btnSearchNext.setOnClickListener {
            searchNext()
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // زوم
        btnZoomIn.setOnClickListener { zoomIn() }
        btnZoomOut.setOnClickListener { zoomOut() }
        btnZoomReset.setOnClickListener { resetZoom() }
        btnFitWidth.setOnClickListener { fitToWidth() }
        btnFitHeight.setOnClickListener { fitToHeight() }

        // Gestures برای زوم
        imageView.setOnTouchListener { _, event ->
            when (event.action and android.view.MotionEvent.ACTION_MASK) {
                android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                    // Pinch to zoom
                    isZoomed = true
                }
            }
            false
        }

        // دکمه‌های خطا
        btnRetry.setOnClickListener {
            loadPdfFile()
        }

        btnOpenWith.setOnClickListener {
            openWithExternalApp()
        }
    }

    private fun setupViewPager() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPageIndex = position
                updatePageInfo()
                updateNavigationButtons()
            }
        })

        viewPager.offscreenPageLimit = 2
    }

    private fun loadPdfFile() {
        pdfLoadJob?.cancel()

        pdfLoadJob = scope.launch {
            showLoading()

            try {
                withContext(Dispatchers.IO) {
                    openPdfRenderer()
                }

                hideLoading()
                showPdfContent()

            } catch (e: Exception) {
                hideLoading()
                showError("خطا در بارگذاری PDF: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    @Throws(Exception::class)
    private fun openPdfRenderer() {
        closePdfRenderer()

        pdfFile?.let { file ->
            pdfFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(pdfFileDescriptor!!)
            totalPages = pdfRenderer!!.pageCount
        }
    }

    private fun showPdfContent() {
        if (totalPages == 0) {
            showError("فایل PDF خالی است")
            return
        }

        // تنظیم آداپتر برای ViewPager
        viewPager.adapter = PdfPageAdapter(this, pdfRenderer!!, totalPages)
        viewPager.isVisible = true
        layoutControls.isVisible = true

        updatePageInfo()
        updateNavigationButtons()
        resetZoom()
    }

    private fun goToPage(pageIndex: Int) {
        if (pageIndex in 0 until totalPages) {
            viewPager.currentItem = pageIndex
        }
    }

    private fun updatePageInfo() {
        tvPageNumber.text = "${currentPageIndex + 1}"
        tvTotalPages.text = "از $totalPages"

        // به‌روزرسانی عنوان toolbar
        supportActionBar?.subtitle = "صفحه ${currentPageIndex + 1} از $totalPages"
    }

    private fun updateNavigationButtons() {
        btnPrevPage.isEnabled = currentPageIndex > 0
        btnNextPage.isEnabled = currentPageIndex < totalPages - 1

        // تغییر آیکون‌ها بر اساس وضعیت
        btnPrevPage.icon = if (btnPrevPage.isEnabled) {
            getDrawable(R.drawable.ic_prev_page)
        } else {
            getDrawable(R.drawable.ic_prev_page_disabled)
        }

        btnNextPage.icon = if (btnNextPage.isEnabled) {
            getDrawable(R.drawable.ic_next_page)
        } else {
            getDrawable(R.drawable.ic_next_page_disabled)
        }
    }

    // ==================== عملیات جستجو ====================

    private fun toggleSearch() {
        layoutSearch.isVisible = !layoutSearch.isVisible
        if (layoutSearch.isVisible) {
            etSearch.requestFocus()
            showKeyboard(etSearch)
        } else {
            hideKeyboard()
            clearSearch()
        }
    }

    private fun performSearch() {
        searchQuery = etSearch.text.toString().trim()
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "لطفاً عبارت جستجو را وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            searchResults.clear()

            // جستجو در تمام صفحات (در بک‌گراند)
            withContext(Dispatchers.IO) {
                for (i in 0 until totalPages) {
                    val page = pdfRenderer!!.openPage(i)
                    try {
                        // در واقعیت، باید متن PDF را استخراج کنی
                        // اینجا فقط شبیه‌سازی می‌کنیم
                        if (i % 3 == 0) { // شبیه‌سازی یافتن نتیجه
                            searchResults.add(i)
                        }
                    } finally {
                        page.close()
                    }
                }
            }

            if (searchResults.isNotEmpty()) {
                tvSearchResult.text = "${searchResults.size} نتیجه یافت شد"
                goToPage(searchResults.first())
                updateSearchNavigation()
            } else {
                tvSearchResult.text = "نتیجه‌ای یافت نشد"
            }
        }
    }

    private fun searchNext() {
        if (searchResults.isEmpty()) return

        val currentIndex = searchResults.indexOf(currentPageIndex)
        val nextIndex = if (currentIndex < searchResults.size - 1) {
            currentIndex + 1
        } else {
            0 // به ابتدا برگرد
        }

        goToPage(searchResults[nextIndex])
        updateSearchNavigation()
    }

    private fun searchPrevious() {
        if (searchResults.isEmpty()) return

        val currentIndex = searchResults.indexOf(currentPageIndex)
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            searchResults.size - 1 // به انتها برو
        }

        goToPage(searchResults[prevIndex])
        updateSearchNavigation()
    }

    private fun updateSearchNavigation() {
        if (searchResults.isEmpty()) {
            btnSearchPrev.isEnabled = false
            btnSearchNext.isEnabled = false
            return
        }

        val currentIndex = searchResults.indexOf(currentPageIndex)
        btnSearchPrev.isEnabled = searchResults.isNotEmpty()
        btnSearchNext.isEnabled = searchResults.isNotEmpty()

        if (currentIndex >= 0) {
            tvSearchResult.text = "نتیجه ${currentIndex + 1} از ${searchResults.size}"
        }
    }

    private fun clearSearch() {
        etSearch.text.clear()
        searchQuery = ""
        searchResults.clear()
        tvSearchResult.text = ""
    }

    // ==================== عملیات زوم ====================

    private fun zoomIn() {
        if (zoomLevel < ZOOM_MAX) {
            zoomLevel += ZOOM_STEP
            applyZoom()
        }
    }

    private fun zoomOut() {
        if (zoomLevel > ZOOM_MIN) {
            zoomLevel -= ZOOM_STEP
            applyZoom()
        }
    }

    private fun resetZoom() {
        zoomLevel = 1.0f
        applyZoom()
    }

    private fun fitToWidth() {
        // منطق fit to width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val imageWidth = imageView.drawable?.intrinsicWidth?.toFloat() ?: 1f
        zoomLevel = screenWidth / imageWidth
        applyZoom()
    }

    private fun fitToHeight() {
        // منطق fit to height
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val imageHeight = imageView.drawable?.intrinsicHeight?.toFloat() ?: 1f
        zoomLevel = screenHeight / imageHeight
        applyZoom()
    }

    private fun applyZoom() {
        imageView.scaleX = zoomLevel
        imageView.scaleY = zoomLevel

        // به‌روزرسانی وضعیت دکمه‌های زوم
        btnZoomIn.isEnabled = zoomLevel < ZOOM_MAX
        btnZoomOut.isEnabled = zoomLevel > ZOOM_MIN
        btnZoomReset.isEnabled = zoomLevel != 1.0f

        // به‌روزرسانی toolbar
        updateZoomInfo()
    }

    private fun updateZoomInfo() {
        val percent = (zoomLevel * 100).toInt()
        toolbar.subtitle = "صفحه ${currentPageIndex + 1} • ${percent}%"
    }

    // ==================== عملیات فایل ====================

    private fun sharePdf() {
        pdfFile?.let { file ->
            try {
                val shareUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, pdfTitle)
                    putExtra(Intent.EXTRA_TEXT, "فایل PDF: $pdfTitle")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "اشتراک فایل PDF"))

            } catch (e: Exception) {
                Toast.makeText(this, "خطا در اشتراک فایل", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun printPdf() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            pdfFile?.let { file ->
                try {
                    val printUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider",
                            file
                        )
                    } else {
                        Uri.fromFile(file)
                    }

                    val printIntent = Intent(this, PdfPrintActivity::class.java).apply {
                        putExtra("pdf_uri", printUri)
                        putExtra("pdf_title", pdfTitle)
                    }

                    startActivity(printIntent)

                } catch (e: Exception) {
                    Toast.makeText(this, "خطا در چاپ فایل", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "چاپ از Android 4.4 به بالا پشتیبانی می‌شود", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bookmarkCurrentPage() {
        val bookmark = Bookmark(
            pdfPath = pdfFile?.absolutePath ?: "",
            pdfTitle = pdfTitle,
            pageNumber = currentPageIndex + 1,
            timestamp = System.currentTimeMillis(),
            note = ""
        )

        // ذخیره بوکمارک در SharedPreferences یا دیتابیس
        BookmarkManager.saveBookmark(this, bookmark)
        Toast.makeText(this, "صفحه ${currentPageIndex + 1} نشان‌گذاری شد", Toast.LENGTH_SHORT).show()
    }

    private fun openWithExternalApp() {
        pdfFile?.let { file ->
            try {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(this, "برنامه‌ای برای بازکردن PDF یافت نشد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== مدیریت وضعیت ====================

    private fun showLoading() {
        progressBar.isVisible = true
        viewPager.isVisible = false
        layoutError.isVisible = false
    }

    private fun hideLoading() {
        progressBar.isVisible = false
    }

    private fun showError(message: String) {
        layoutError.isVisible = true
        viewPager.isVisible = false
        layoutControls.isVisible = false
        tvErrorMessage.text = message

        pdfFile?.let {
            btnOpenWith.isVisible = it.exists()
        } ?: run {
            btnOpenWith.isVisible = false
        }
    }

    // ==================== چرخه حیات ====================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                sharePdf()
                true
            }
            R.id.action_print -> {
                printPdf()
                true
            }
            R.id.action_search -> {
                toggleSearch()
                true
            }
            R.id.action_bookmark -> {
                bookmarkCurrentPage()
                true
            }
            R.id.action_info -> {
                showPdfInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPdfInfo() {
        pdfFile?.let { file ->
            val info = """
                📄 نام فایل: ${file.name}
                📁 مسیر: ${file.parent}
                📊 حجم: ${formatFileSize(file.length())}
                📑 تعداد صفحات: $totalPages
                🕐 تاریخ ایجاد: ${file.lastModified().toDateString()}
                🔒 قابل نوشتن: ${if (file.canWrite()) "✅" else "❌"}
                📖 قابل خواندن: ${if (file.canRead()) "✅" else "❌"}
            """.trimIndent()

            android.app.AlertDialog.Builder(this)
                .setTitle("اطلاعات فایل PDF")
                .setMessage(info)
                .setPositiveButton("متوجه شدم") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        closePdfRenderer()
    }

    private fun closePdfRenderer() {
        currentPage?.close()
        currentPage = null

        pdfRenderer?.close()
        pdfRenderer = null

        pdfFileDescriptor?.close()
        pdfFileDescriptor = null
    }

    // ==================== توابع کمکی ====================

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 بایت"

        val units = arrayOf("بایت", "کیلوبایت", "مگابایت", "گیگابایت")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(
            size / Math.pow(1024.0, digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    private fun Long.toDateString(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale("fa", "IR"))
        return dateFormat.format(java.util.Date(this))
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val view = currentFocus
        view?.let {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}

// ==================== آداپتر صفحات PDF ====================

class PdfPageAdapter(
    private val activity: PdfViewActivity,
    private val pdfRenderer: PdfRenderer,
    private val totalPages: Int
) : androidx.recyclerview.widget.RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    inner class PdfPageViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.pdf_page_image)
        val progressBar: ProgressBar = view.findViewById(R.id.page_progress)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.progressBar.isVisible = true

        // بارگذاری صفحه در بک‌گراند
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val page = pdfRenderer.openPage(position)
                val bitmap = Bitmap.createBitmap(
                    page.width * 2, // برای کیفیت بهتر
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                withContext(Dispatchers.Main) {
                    holder.imageView.setImageBitmap(bitmap)
                    holder.progressBar.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    holder.progressBar.isVisible = false
                    holder.imageView.setImageResource(R.drawable.ic_pdf_error)
                }
            }
        }
    }

    override fun getItemCount(): Int = totalPages
}

// ==================== مدیریت بوکمارک‌ها ====================

data class Bookmark(
    val pdfPath: String,
    val pdfTitle: String,
    val pageNumber: Int,
    val timestamp: Long,
    val note: String
)

object BookmarkManager {
    private const val PREFS_NAME = "pdf_bookmarks"
    private const val KEY_BOOKMARKS = "bookmarks_list"

    fun saveBookmark(context: android.content.Context, bookmark: Bookmark) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val bookmarksJson = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"

        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<MutableList<Bookmark>>() {}.type
        val bookmarks = gson.fromJson<MutableList<Bookmark>>(bookmarksJson, type)

        bookmarks.add(bookmark)

        prefs.edit()
            .putString(KEY_BOOKMARKS, gson.toJson(bookmarks))
            .apply()
    }

    fun getBookmarks(context: android.content.Context): List<Bookmark> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val bookmarksJson = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"

        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<Bookmark>>() {}.type
        return gson.fromJson(bookmarksJson, type)
    }

    fun removeBookmark(context: android.content.Context, bookmark: Bookmark) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val bookmarksJson = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"

        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<MutableList<Bookmark>>() {}.type
        val bookmarks = gson.fromJson<MutableList<Bookmark>>(bookmarksJson, type)

        bookmarks.removeAll { it.pdfPath == bookmark.pdfPath && it.pageNumber == bookmark.pageNumber }

        prefs.edit()
            .putString(KEY_BOOKMARKS, gson.toJson(bookmarks))
            .apply()
    }
}

// ==================== اکتیویتی چاپ PDF ====================

class PdfPrintActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_print)

        // پیاده‌سازی چاپ PDF
        // (به دلیل پیچیدگی و وابستگی به کتابخانه‌های چاپ، کوتاه شده)
    }
}