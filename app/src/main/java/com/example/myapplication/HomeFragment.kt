package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val tvHouseName = view.findViewById<TextView>(R.id.tv_house_name)
        val tvElectricFee = view.findViewById<TextView>(R.id.tv_electric_fee)
        val tvDaysRemaining = view.findViewById<TextView>(R.id.tv_days_remaining)
        val progressContract = view.findViewById<ProgressBar>(R.id.progress_contract)
        val btnConfigure = view.findViewById<Button>(R.id.btn_configure_contract)

        val sharedPref = requireActivity().getSharedPreferences("RentGuardData", Context.MODE_PRIVATE)
        val savedTitle = sharedPref.getString("contract_title", "尚未配置契約")
        val savedRate = sharedPref.getFloat("electric_rate", 0.0f)
        val lastCalculatedFee = sharedPref.getFloat("last_calculated_fee", 0.0f)

        val startDateStr = sharedPref.getString("contract_start_date", "")
        val endDateStr = sharedPref.getString("contract_end_date", "")

        tvHouseName?.text = savedTitle

        if (lastCalculatedFee > 0.0f) {
            tvElectricFee?.text = "NT$ ${String.format("%.0f", lastCalculatedFee)}"
        } else if (savedRate > 0.0f) {
            tvElectricFee?.text = "費率: NT$ $savedRate/度"
        } else {
            tvElectricFee?.text = "NT$ --"
        }

        if (!startDateStr.isNullOrEmpty() && !endDateStr.isNullOrEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDate = sdf.parse(startDateStr)
                val endDate = sdf.parse(endDateStr)
                val today = Date()

                if (startDate != null && endDate != null) {
                    val totalDurationMs = endDate.time - startDate.time
                    val totalDays = TimeUnit.MILLISECONDS.toDays(totalDurationMs)

                    val diffInMs = endDate.time - today.time
                    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffInMs)

                    if (daysRemaining > 0) {
                        tvDaysRemaining?.text = "合約剩餘 $daysRemaining 天"

                        if (totalDays > 0) {
                            val progress = ((daysRemaining.toFloat() / totalDays.toFloat()) * 100).toInt()
                            progressContract?.progress = progress.coerceIn(0, 100)
                        } else {
                            progressContract?.progress = 100
                        }
                    } else {
                        tvDaysRemaining?.text = "合約已到期"
                        progressContract?.progress = 0
                    }
                }
            } catch (e: Exception) {
                tvDaysRemaining?.text = "日期解析錯誤"
                progressContract?.progress = 0
            }
        } else {
            tvDaysRemaining?.text = "尚未設定合約期限"
            progressContract?.progress = 0
        }

        btnConfigure?.setOnClickListener {
            (activity as? MainActivity)?.switchFragment(AddContractFragment())
        }

        return view
    }
}