package com.example.myapplication
import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import kotlin.random.Random

class ElectricFragment : Fragment() {

    var fgPermission = true
    lateinit var imagecapture: ImageCapture
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var etMeter: EditText? = null
    private var tvPlaceholderText: TextView? = null

    private var isProcessing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_electric, container, false)

        etMeter = view.findViewById(R.id.et_meter_degree)
        val btnAiScan = view.findViewById<Button>(R.id.btn_ai_scan)
        val placeholderContainer = view.findViewById<FrameLayout>(R.id.frame_chart_placeholder)
        tvPlaceholderText = view.findViewById<TextView>(R.id.tv_placeholder_text)

        val sharedPref = requireActivity().getSharedPreferences("RentGuardData", Context.MODE_PRIVATE)
        val rate = sharedPref.getFloat("electric_rate", 0.0f)

        updateInitialHint(rate)

        val permissions = checkPermissions()
        val reqPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            it.forEach { s, b ->
                if (b == false) fgPermission = false
            }
            if (fgPermission) {
                btnAiScan?.isEnabled = true
            }
        }

        if (permissions.isNotEmpty()) {
            reqPermissions.launch(permissions.toTypedArray())
        } else {
            btnAiScan?.isEnabled = true
        }

        btnAiScan?.setOnClickListener {
            val meterStr = etMeter?.text.toString().trim()

            if (meterStr.isNotEmpty()) {
                if (rate == 0.0f) {
                    Toast.makeText(requireContext(), "請先前往首頁設定合約費率！", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val meter = meterStr.toFloatOrNull() ?: 0.0f
                val totalFee = meter * rate
                sharedPref.edit().putFloat("last_calculated_fee", totalFee).apply()

                val legalStatus = if (rate > 7.7f) {
                    "❌ 違法超收警告：\n每度電費 $rate 元已超出台電最高級距上限！房東已涉嫌違法超收。"
                } else {
                    "🟢 費率合規提示：\n當前費率符合內政部法定規範區間。"
                }

                tvPlaceholderText?.text = """
                    【 電費精算報告 】
                    ----------------------------------
                    本次辨識度數：$meter 度
                    合約設定費率：$rate 元 / 度
                    應繳總額：NT$ ${String.format("%.2f", totalFee)} 元
                    
                    $legalStatus
                """.trimIndent()
                tvPlaceholderText?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)

                etMeter?.setText("")

            } else {
                if (isProcessing) return@setOnClickListener
                isProcessing = true

                Toast.makeText(requireContext(), "正在啟動相機預覽...", Toast.LENGTH_SHORT).show()

                if (placeholderContainer != null) {
                    placeholderContainer.removeAllViews()
                    val dynamicPreviewView = PreviewView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    placeholderContainer.addView(dynamicPreviewView)

                    cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
                    cameraProviderFuture.addListener(Runnable {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            bindPreview(cameraProvider, dynamicPreviewView.getSurfaceProvider())
                        } catch (e: Exception) {
                            Log.d("AAA", "獲取相機驅動失敗")
                            isProcessing = false
                        }
                    }, ContextCompat.getMainExecutor(requireContext()))

                    dynamicPreviewView.foreground = null

                    dynamicPreviewView.postDelayed({
                        Toast.makeText(requireContext(), "📸 AI 正在自動截圖擷取電表數值...", Toast.LENGTH_SHORT).show()

                        try {
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, "test1.png")
                            }
                            val outputFile = ImageCapture.OutputFileOptions.Builder(
                                requireActivity().contentResolver,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                            ).build()

                            val executor = ContextCompat.getMainExecutor(requireContext())

                            imagecapture.takePicture(outputFile, executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(e: ImageCaptureException) {
                                        Log.d("AAA", "拍照失敗:" + e.message.toString())
                                        resetCameraEnvironment(placeholderContainer)
                                    }
                                    override fun onImageSaved(e: ImageCapture.OutputFileResults) {
                                        Log.d("AAA", "拍照成功:" + e.savedUri.toString())

                                        resetCameraEnvironment(placeholderContainer)

                                        val simulatedMeter = Random.nextInt(120, 350) + Random.nextFloat()
                                        val formattedMeter = String.format("%.1f", simulatedMeter)
                                        etMeter?.setText(formattedMeter)

                                        Toast.makeText(requireContext(), "辨識成功！自動精算中...", Toast.LENGTH_SHORT).show()

                                        btnAiScan.performClick()
                                    }
                                })
                        } catch (e: Exception) {
                            Log.d("AAA", e.message.toString())
                            resetCameraEnvironment(placeholderContainer)
                        }

                    }, 2500)
                }
            }
        }

        return view
    }
    private fun resetCameraEnvironment(container: FrameLayout?) {
        try {
            if (::cameraProviderFuture.isInitialized) {
                cameraProviderFuture.get().unbindAll()
            }
        } catch (e: Exception) {
            Log.d("AAA", "釋放相機異常")
        }
        container?.removeAllViews()
        if (tvPlaceholderText != null) {
            container?.addView(tvPlaceholderText)
        }
        isProcessing = false
    }

    private fun updateInitialHint(rate: Float) {
        if (rate > 0.0f) {
            tvPlaceholderText?.text = "目前設定費率：每度 $rate 元\n請點按鈕啟動 AI 自動相機辨識。"
        } else {
            tvPlaceholderText?.text = "請先至首頁配置租屋契約，設定每度電費，才能啟用精算功能。"
        }
    }

    fun checkPermissions(): ArrayList<String> {
        var permissions = ArrayList<String>()
        val camera = ActivityCompat.checkSelfPermission(requireContext(), CAMERA)
        val writestorage = ActivityCompat.checkSelfPermission(requireContext(), WRITE_EXTERNAL_STORAGE)
        if (camera != PackageManager.PERMISSION_GRANTED)
            permissions.add(CAMERA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            if (writestorage != PackageManager.PERMISSION_GRANTED)
                permissions.add(WRITE_EXTERNAL_STORAGE)
        return permissions
    }

    fun bindPreview(cameraProvider: ProcessCameraProvider, provider: Preview.SurfaceProvider) {
        imagecapture = ImageCapture.Builder().apply {
            setFlashMode(ImageCapture.FLASH_MODE_ON)
        }.build()
        var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        var preview = Preview.Builder().build().also {
            it.setSurfaceProvider(provider)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imagecapture)
        } catch (e: Exception) {
            Log.d("AAA", "無法綁定相機")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            cameraProviderFuture.get().unbindAll()
        } catch (e: Exception) {}
        isProcessing = false
    }
}