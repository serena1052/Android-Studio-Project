package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RemindFragment : Fragment() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_remind, container, false)

        val tvNextRentDate = view.findViewById<TextView>(R.id.tv_next_rent_date)
        val tvRentStatus = view.findViewById<TextView>(R.id.tv_rent_status)
        val tvNextElectricDate = view.findViewById<TextView>(R.id.tv_next_electric_date)
        val tvElectricStatus = view.findViewById<TextView>(R.id.tv_electric_status)
        val btnRentPaid = view.findViewById<Button>(R.id.btn_rent_paid)
        val btnElectricPaid = view.findViewById<Button>(R.id.btn_electric_paid)

        val sharedPref = requireActivity().getSharedPreferences("RentGuardData", Context.MODE_PRIVATE)

        sharedPref.edit().apply {
            putInt("rent_paid_count", 0)
            putInt("electric_paid_count", 0)
            apply()
        }

        fun refreshUI() {
            val startDateStr = sharedPref.getString("contract_start_date", "")

            if (startDateStr.isNullOrEmpty()) {
                tvNextRentDate?.text = "請先至首頁配置契約日期"
                tvNextElectricDate?.text = "請先至首頁配置契約日期"
                tvRentStatus?.text = "尚未設定合約"
                tvElectricStatus?.text = "尚未設定合約"
                return
            }

            try {
                val startDate = sdf.parse(startDateStr)
                if (startDate != null) {
                    val today = Date()

                    val rentCount = sharedPref.getInt("rent_paid_count", 0)
                    val rentCal = Calendar.getInstance().apply { time = startDate }
                    rentCal.add(Calendar.MONTH, rentCount)
                    val currentRentStr = sdf.format(rentCal.time)
                    tvNextRentDate?.text = "下次繳房租日: $currentRentStr"

                    val rentDiff = rentCal.time.time - today.time
                    val rentDays = TimeUnit.MILLISECONDS.toDays(rentDiff)
                    tvRentStatus?.text = if (rentDays >= 0) "提示: 還有 $rentDays 天該付房租" else "警告: 已逾期 ${Math.abs(rentDays)} 天！"

                    val electricCount = sharedPref.getInt("electric_paid_count", 0)
                    val electricCal = Calendar.getInstance().apply { time = startDate }
                    electricCal.add(Calendar.MONTH, electricCount * 2)
                    val currentElectricStr = sdf.format(electricCal.time)
                    tvNextElectricDate?.text = "下次電費結算日: $currentElectricStr"

                    val elecDiff = electricCal.time.time - today.time
                    val elecDays = TimeUnit.MILLISECONDS.toDays(elecDiff)
                    tvElectricStatus?.text = if (elecDays >= 0) "提示: 還有 $elecDays 天結算電費" else "警告: 已逾期 ${Math.abs(elecDays)} 天！"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        refreshUI()

        btnRentPaid?.setOnClickListener {
            val startDateStr = sharedPref.getString("contract_start_date", "")
            if (startDateStr.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "請先至首頁設定合約日期！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rentCount = sharedPref.getInt("rent_paid_count", 0)
            sharedPref.edit().putInt("rent_paid_count", rentCount + 1).apply()

            refreshUI()
            Toast.makeText(requireContext(), "已更新房租繳納紀錄！自動順延 1 個月", Toast.LENGTH_SHORT).show()
        }

        btnElectricPaid?.setOnClickListener {
            val startDateStr = sharedPref.getString("contract_start_date", "")
            if (startDateStr.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "請先至首頁設定合約日期！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val electricCount = sharedPref.getInt("electric_paid_count", 0)
            sharedPref.edit().putInt("electric_paid_count", electricCount + 1).apply()

            refreshUI()
            Toast.makeText(requireContext(), "已更新電費繳納紀錄！自動順延 2 個月", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}