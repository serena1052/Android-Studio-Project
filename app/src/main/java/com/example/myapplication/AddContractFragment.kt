package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.util.Calendar
import java.util.Locale

class AddContractFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_contract, container, false)

        val etTitle = view.findViewById<EditText>(R.id.et_contract_title)
        val etStartDate = view.findViewById<EditText>(R.id.et_contract_start_date)
        val etEndDate = view.findViewById<EditText>(R.id.et_contract_end_date)
        val etRate = view.findViewById<EditText>(R.id.et_electric_rate)
        val btnSave = view.findViewById<Button>(R.id.btn_save_contract)

        val sharedPref = requireActivity().getSharedPreferences("RentGuardData", Context.MODE_PRIVATE)

        etTitle?.setText(sharedPref.getString("contract_title", ""))
        etStartDate?.setText(sharedPref.getString("contract_start_date", ""))
        etEndDate?.setText(sharedPref.getString("contract_end_date", ""))

        val existingRate = sharedPref.getFloat("electric_rate", -1.0f)
        if (existingRate >= 0.0f) {
            etRate?.setText(existingRate.toString())
        }

        fun setupDatePicker(editText: EditText?) {
            editText?.setOnClickListener {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                    val dateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    editText.setText(dateString)
                }, year, month, day).show()
            }
        }

        setupDatePicker(etStartDate)
        setupDatePicker(etEndDate)

        btnSave?.setOnClickListener {
            val title = etTitle?.text.toString().trim()
            val startDate = etStartDate?.text.toString().trim()
            val endDate = etEndDate?.text.toString().trim()
            val rateStr = etRate?.text.toString().trim()

            if (title.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || rateStr.isEmpty()) {
                Toast.makeText(requireContext(), "請填入完整合約資訊", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rate = rateStr.toFloatOrNull() ?: 0.0f

            sharedPref.edit().apply {
                putString("contract_title", title)
                putString("contract_start_date", startDate)
                putString("contract_end_date", endDate)
                putFloat("electric_rate", rate)
                apply()
            }

            Toast.makeText(requireContext(), "契約資料已成功永久儲存", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.switchFragment(HomeFragment())
        }

        return view
    }
}