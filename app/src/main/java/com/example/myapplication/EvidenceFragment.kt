package com.example.myapplication
import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture

data class EvidenceItem(val uri: Uri, val category: String)

class EvidenceFragment : Fragment() {

    private lateinit var tagAll: TextView
    private lateinit var tagBedroom: TextView
    private lateinit var tagBathroom: TextView
    private lateinit var rvEvidence: RecyclerView
    private lateinit var tvEmptyGallery: TextView
    private lateinit var fabTakePhoto: FloatingActionButton
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null
    private var isProcessing = false
    private var placeholderContainer: FrameLayout? = null
    private val allEvidenceList = mutableListOf<EvidenceItem>()
    private lateinit var evidenceAdapter: EvidenceAdapter
    private var currentCategory = "全部"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_evidence, container, false)

        tagAll = view.findViewById(R.id.tag_all)
        tagBedroom = view.findViewById(R.id.tag_bedroom)
        tagBathroom = view.findViewById(R.id.tag_bathroom)
        rvEvidence = view.findViewById(R.id.rv_evidence)
        tvEmptyGallery = view.findViewById(R.id.tv_empty_gallery)
        fabTakePhoto = view.findViewById(R.id.fab_take_photo)

        placeholderContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        (view as ViewGroup).addView(placeholderContainer)

        evidenceAdapter = EvidenceAdapter(allEvidenceList)
        rvEvidence.layoutManager = GridLayoutManager(requireContext(), 2)
        rvEvidence.adapter = evidenceAdapter

        tagAll.setOnClickListener { switchTag("全部") }
        tagBedroom.setOnClickListener { switchTag("臥室") }
        tagBathroom.setOnClickListener { switchTag("浴室") }

        fabTakePhoto.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            isProcessing = true

            val targetCategory = if (currentCategory == "全部") "臥室" else currentCategory
            startCameraWorkflow(targetCategory)
        }

        refreshGallery()

        return view
    }


    private fun switchTag(category: String) {
        currentCategory = category

        val activeColor = Color.parseColor("#2C3E50")
        val normalColor = Color.parseColor("#7F8C8D")

        tagAll.setTextColor(normalColor)
        tagAll.setTypeface(null, Typeface.NORMAL)
        tagBedroom.setTextColor(normalColor)
        tagBedroom.setTypeface(null, Typeface.NORMAL)
        tagBathroom.setTextColor(normalColor)
        tagBathroom.setTypeface(null, Typeface.NORMAL)

        when (category) {
            "全部" -> {
                tagAll.setTextColor(activeColor)
                tagAll.setTypeface(null, Typeface.BOLD)
            }
            "臥室" -> {
                tagBedroom.setTextColor(activeColor)
                tagBedroom.setTypeface(null, Typeface.BOLD)
            }
            "浴室" -> {
                tagBathroom.setTextColor(activeColor)
                tagBathroom.setTypeface(null, Typeface.BOLD)
            }
        }

        refreshGallery()
    }

    private fun refreshGallery() {
        val filteredList = if (currentCategory == "全部") {
            allEvidenceList
        } else {
            allEvidenceList.filter { it.category == currentCategory }
        }

        evidenceAdapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            tvEmptyGallery.visibility = View.VISIBLE
            rvEvidence.visibility = View.GONE
        } else {
            tvEmptyGallery.visibility = View.GONE
            rvEvidence.visibility = View.VISIBLE
        }
    }

    private fun startCameraWorkflow(category: String) {
        Toast.makeText(requireContext(), "正在啟動相機預覽...", Toast.LENGTH_SHORT).show()

        if (placeholderContainer != null) {
            placeholderContainer!!.removeAllViews()

            val dynamicPreviewView = PreviewView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            placeholderContainer!!.addView(dynamicPreviewView)

            cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener(Runnable {
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    imageCapture = ImageCapture.Builder().build()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(dynamicPreviewView.surfaceProvider)
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )

                } catch (e: Exception) {
                    Log.d("AAA", "獲取相機驅動失敗")
                    isProcessing = false
                }
            }, ContextCompat.getMainExecutor(requireContext()))

            dynamicPreviewView.foreground = null

            dynamicPreviewView.postDelayed({
                Toast.makeText(requireContext(), "正在自動擷取點交影像...", Toast.LENGTH_SHORT).show()

                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "evidence_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    }
                    val outputFile = ImageCapture.OutputFileOptions.Builder(
                        requireActivity().contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    ).build()

                    val executor = ContextCompat.getMainExecutor(requireContext())

                    imageCapture?.takePicture(outputFile, executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(e: ImageCaptureException) {
                                Log.d("AAA", "拍照失敗:" + e.message.toString())
                                resetCameraEnvironment(placeholderContainer)
                            }

                            override fun onImageSaved(e: ImageCapture.OutputFileResults) {
                                val savedUri = e.savedUri
                                Log.d("AAA", "拍照成功:" + savedUri.toString())

                                resetCameraEnvironment(placeholderContainer)

                                if (savedUri != null) {
                                    allEvidenceList.add(EvidenceItem(savedUri, category))

                                    activity?.runOnUiThread {
                                        refreshGallery()
                                        Toast.makeText(requireContext(), "存證成功！已存入「$category」分類", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                } catch (e: Exception) {
                    Log.d("AAA", e.message.toString())
                    resetCameraEnvironment(placeholderContainer)
                }

            }, 2500)
        }
    }

    private fun resetCameraEnvironment(container: FrameLayout?) {
        try {
            if (::cameraProviderFuture.isInitialized) {
                cameraProviderFuture.get().unbindAll()
            }
        } catch (e: Exception) {
            Log.d("AAA", "釋放相機異常")
        }
        activity?.runOnUiThread {
            container?.removeAllViews()
            isProcessing = false
        }
    }
    private class EvidenceAdapter(private var dataset: List<EvidenceItem>) :
        RecyclerView.Adapter<EvidenceAdapter.EvidenceViewHolder>() {

        class EvidenceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(android.R.id.icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvidenceViewHolder {
            val imageView = ImageView(parent.context).apply {
                id = android.R.id.icon
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    420
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(12, 12, 12, 12)
            }
            return EvidenceViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: EvidenceViewHolder, position: Int) {
            holder.ivPhoto.setImageURI(dataset[position].uri)
        }

        override fun getItemCount(): Int = dataset.size

        fun updateData(newDataset: List<EvidenceItem>) {
            this.dataset = newDataset
            notifyDataSetChanged()
        }
    }
}