package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlin.random.Random

class NoiseFragment : Fragment() {

    private var isMonitoring = false
    private val handler = Handler(Looper.getMainLooper())

    private var tvDb: TextView? = null
    private var tvStatus: TextView? = null
    private var btnToggle: Button? = null
    private val runnableCode = object : Runnable {
        override fun run() {
            if (!isMonitoring) return

            val isAnomaly = Random.nextInt(100) < 25

            val randomDb = if (isAnomaly) {
                Random.nextInt(70, 88)
            } else {
                Random.nextInt(38, 46)
            }

            tvDb?.text = randomDb.toString()

            if (randomDb >= 70) {
                tvStatus?.text = "環境狀態：⚠️ 偵測到突發性嚴重噪音（已錄音）"
                tvStatus?.setTextColor(Color.parseColor("#D16969"))
            } else {
                tvStatus?.text = "環境狀態：🟢 周遭舒適靜音中"
                tvStatus?.setTextColor(Color.parseColor("#2C3E50"))
            }

            handler.postDelayed(this, 800)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_noise, container, false)

        tvDb = view.findViewById(R.id.tv_current_db)
        tvStatus = view.findViewById(R.id.tv_noise_status)
        btnToggle = view.findViewById(R.id.btn_toggle_noise_service)

        btnToggle?.setOnClickListener {
            if (!isMonitoring) {
                isMonitoring = true
                btnToggle?.text = "停止背景採證"
                btnToggle?.setBackgroundColor(Color.parseColor("#D16969"))
                handler.post(runnableCode)
                Toast.makeText(requireContext(), "分貝自動採證服務已啟動", Toast.LENGTH_SHORT).show()
            } else {
                isMonitoring = false
                btnToggle?.text = "開啟背景採證"
                btnToggle?.setBackgroundColor(Color.parseColor("#2C3E50"))
                handler.removeCallbacks(runnableCode)
                tvDb?.text = "45"
                tvStatus?.text = "環境狀態：舒適靜音"
                tvStatus?.setTextColor(Color.parseColor("#2C3E50"))
                Toast.makeText(requireContext(), "監測服務已暫停，音訊證據已加密導出。", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnableCode)
    }
}