package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.entityadmin.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private lateinit var btnWeekly: Button
    private lateinit var btnMonthly: Button
    private lateinit var btnYearly: Button
    private lateinit var btnExportPDF: Button
    private lateinit var btnExportExcel: Button

    private lateinit var tvTotalAttendance: TextView
    private lateinit var tvAverageAttendance: TextView
    private lateinit var tvPeakHours: TextView
    private lateinit var tvMostActiveDay: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadReportsData()
    }

    private fun initViews(view: View) {
        btnWeekly = view.findViewById(R.id.btnWeekly)
        btnMonthly = view.findViewById(R.id.btnMonthly)
        btnYearly = view.findViewById(R.id.btnYearly)
        btnExportPDF = view.findViewById(R.id.btnExportPDF)
        btnExportExcel = view.findViewById(R.id.btnExportExcel)

        tvTotalAttendance = view.findViewById(R.id.tvTotalAttendance)
        tvAverageAttendance = view.findViewById(R.id.tvAverageAttendance)
        tvPeakHours = view.findViewById(R.id.tvPeakHours)
        tvMostActiveDay = view.findViewById(R.id.tvMostActiveDay)
    }

    private fun setupClickListeners() {
        btnWeekly.setOnClickListener { selectTimePeriod("weekly") }
        btnMonthly.setOnClickListener { selectTimePeriod("monthly") }
        btnYearly.setOnClickListener { selectTimePeriod("yearly") }

        btnExportPDF.setOnClickListener {
            Toast.makeText(requireContext(), "Exporting PDF report...", Toast.LENGTH_SHORT).show()
            // TODO: Implement PDF export
        }

        btnExportExcel.setOnClickListener {
            Toast.makeText(requireContext(), "Exporting Excel report...", Toast.LENGTH_SHORT).show()
            // TODO: Implement Excel export
        }
    }

    private fun selectTimePeriod(period: String) {
        // Reset all button backgrounds and text colors
        btnWeekly.setBackgroundResource(R.drawable.button_outline_background)
        btnWeekly.setTextColor(resources.getColor(R.color.primary_blue, null))

        btnMonthly.setBackgroundResource(R.drawable.button_outline_background)
        btnMonthly.setTextColor(resources.getColor(R.color.primary_blue, null))

        btnYearly.setBackgroundResource(R.drawable.button_outline_background)
        btnYearly.setTextColor(resources.getColor(R.color.primary_blue, null))

        // Set selected button background and text color
        when (period) {
            "weekly" -> {
                btnWeekly.setBackgroundResource(R.drawable.button_primary_background)
                btnWeekly.setTextColor(resources.getColor(R.color.text_on_primary, null))
            }
            "monthly" -> {
                btnMonthly.setBackgroundResource(R.drawable.button_primary_background)
                btnMonthly.setTextColor(resources.getColor(R.color.text_on_primary, null))
            }
            "yearly" -> {
                btnYearly.setBackgroundResource(R.drawable.button_primary_background)
                btnYearly.setTextColor(resources.getColor(R.color.text_on_primary, null))
            }
        }

        // Load data for selected period
        loadReportsData(period)
        Toast.makeText(requireContext(), "Loading ${period} reports...", Toast.LENGTH_SHORT).show()
    }

    private fun loadReportsData(period: String = "weekly") {
        // TODO: Load actual data from repository
        // For now, show demo data
        when (period) {
            "weekly" -> {
                tvTotalAttendance.text = "156"
                tvAverageAttendance.text = "78%"
                tvPeakHours.text = "10-11 AM"
                tvMostActiveDay.text = "Monday"
            }
            "monthly" -> {
                tvTotalAttendance.text = "642"
                tvAverageAttendance.text = "82%"
                tvPeakHours.text = "9-10 AM"
                tvMostActiveDay.text = "Tuesday"
            }
            "yearly" -> {
                tvTotalAttendance.text = "7,824"
                tvAverageAttendance.text = "75%"
                tvPeakHours.text = "10-11 AM"
                tvMostActiveDay.text = "Wednesday"
            }
        }
    }
}
