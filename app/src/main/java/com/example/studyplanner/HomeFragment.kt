package com.example.studyplanner

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.example.studyplanner.databinding.FragmentHomeBinding
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var clockNeedle: View
    private lateinit var clockContainer: RelativeLayout
    private lateinit var handler: Handler
    private val updateInterval: Long = 1000 // 1 second

    // Define the updateTimeRunnable explicitly as a Runnable type
    private val updateTimeRunnable: Runnable = object : Runnable {
        override fun run() {
            animateClockNeedle()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        clockNeedle = binding.root.findViewById(R.id.clockNeedle)
        clockContainer = binding.root.findViewById(R.id.clockContainer)

        // Initialize the handler
        handler = Handler(Looper.getMainLooper())

        // Start updating the needle
        handler.post(updateTimeRunnable)

        return view
    }

    private fun animateClockNeedle() {
        // Get the current time
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis

        // Extract hour and minute from the current time
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Calculate the total time passed in the day (from 0:00 to 23:59)
        val totalMinutesInDay = 24 * 60
        val elapsedMinutes = (hour * 60 + minute).toFloat()

        // Calculate the vertical position (where 0% is the top and 100% is the bottom)
        val verticalPosition = (elapsedMinutes / totalMinutesInDay) * clockContainer.height

        // Create a ValueAnimator to animate the vertical position of the needle
        val animator = ValueAnimator.ofFloat(clockNeedle.translationY, verticalPosition)
        animator.duration = updateInterval // Duration of animation (1 second)
        animator.addUpdateListener { animation ->
            // Update the needle's translationY based on the animation progress
            clockNeedle.translationY = animation.animatedValue as Float
        }

        animator.start()
    }

    override fun onResume() {
        super.onResume()
        // Ensure that the needle keeps updating even when the fragment is in the foreground
        handler.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Stop the updates when the fragment goes into the background
        handler.removeCallbacks(updateTimeRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null) // Clean up handler
    }
}
