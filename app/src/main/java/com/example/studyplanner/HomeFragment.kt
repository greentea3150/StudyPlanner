package com.example.studyplanner

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.adapter.TimeslotAdapter
import com.example.studyplanner.databinding.FragmentHomeBinding
import com.example.studyplanner.model.Task
import com.example.studyplanner.model.TimeSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var firestore: FirebaseFirestore

    // For caching the username
    private var cachedUserName: String? = null

    // RecyclerViews for time slots
    private lateinit var calendarAdapter: TimeslotAdapter

    // Clock Needle
    private lateinit var clockNeedle: View
    private lateinit var clockContainer: RelativeLayout
    private lateinit var handler: Handler
    private val updateInterval: Long = 1000 // 1 second

    // Firestore task count listener
    private var taskCountListener: ListenerRegistration? = null

    // Add flag for updating date
    private var isUpdatingDate: Boolean = false

    private val updateTimeRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isUpdatingDate) {
                animateClockNeedle()
                val dateTextView = binding.root.findViewById<TextView>(R.id.dateTextView)
                val timeTextView = binding.root.findViewById<TextView>(R.id.timeTextView)
                updateDateTime(dateTextView, timeTextView, selectedDate)
            }
            handler.postDelayed(this, updateInterval)
        }
    }

    // Selected date
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        firestore = FirebaseFirestore.getInstance()

        // Reference to the greeting TextView
        val greetingTextView = binding.root.findViewById<TextView>(R.id.greetingText)

        // Fetch the user name from cache or Firestore
        if (cachedUserName != null) {
            // If cached, use it directly
            greetingTextView.text = "Hi, $cachedUserName"
            greetingTextView.visibility = View.VISIBLE
        } else {
            // Fetch the username from Firestore
            greetingTextView.visibility = View.INVISIBLE // Hide until data is fetched
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                val userId = currentUser.uid
                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            cachedUserName = documentSnapshot.getString("name") ?: "User"
                            greetingTextView.text = "Hi, $cachedUserName"
                        } else {
                            cachedUserName = "User"
                            greetingTextView.text = "Hi, $cachedUserName"
                        }
                        greetingTextView.visibility = View.VISIBLE
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error fetching user data", e)
                        cachedUserName = "User"
                        greetingTextView.text = "Hi, $cachedUserName"
                        greetingTextView.visibility = View.VISIBLE
                    }
            } else {
                cachedUserName = "User"
                greetingTextView.text = "Hi, $cachedUserName"
                greetingTextView.visibility = View.VISIBLE
            }
        }

        // Initialize other views
        setupRecyclerView()
        setupClock()
        fetchTasksForSelectedDate()

        // Set up click listeners for navigation buttons
        binding.previousDayButton.setOnClickListener {
            updateSelectedDate(-1)
        }

        binding.nextDayButton.setOnClickListener {
            updateSelectedDate(1)
        }

        // Call addTaskCounterListener to start listening for task count changes
        addTaskCounterListener()

        return view
    }

    private fun setupRecyclerView() {
        val timeSlots = generateTimeSlots()  // This is the list of time slots
        calendarAdapter = TimeslotAdapter(timeSlots)

        val recyclerView = binding.recyclerViewTimeSlots
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = calendarAdapter
    }

    private fun setupClock() {
        clockNeedle = binding.root.findViewById(R.id.clockNeedle)
        clockContainer = binding.root.findViewById(R.id.clockContainer)

        val dateTextView = binding.root.findViewById<TextView>(R.id.dateTextView)
        val timeTextView = binding.root.findViewById<TextView>(R.id.timeTextView)

        handler = Handler(Looper.getMainLooper())
        handler.post(updateTimeRunnable)

        // Initialize date and time with the selected date
        updateDateTime(dateTextView, timeTextView, selectedDate)
    }

    private fun updateDateTime(dateTextView: TextView, timeTextView: TextView, selectedDate: Calendar) {
        val currentTime = Calendar.getInstance()

        // Use SimpleDateFormat to get the month as text
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = dateFormat.format(selectedDate.time)

        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)
        val second = currentTime.get(Calendar.SECOND)

        val time = String.format("%02d:%02d:%02d", hour, minute, second)

        dateTextView.text = date
        timeTextView.text = time
    }

    private fun generateTimeSlots(): List<TimeSlot> {
        return (0..23).map { hour ->
            val formattedHour = String.format("%02d:00", hour)
            TimeSlot(hour = formattedHour, tasks = emptyList()) // Replace emptyList() with real data
        }
    }

    private fun fetchTasksForSelectedDate() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)

        currentUserId?.let { userId ->
            firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", selectedDateStr)  // Fetch tasks for the selected date
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error fetching tasks", e)
                        return@addSnapshotListener
                    }

                    if (view == null) return@addSnapshotListener

                    val taskMap = mutableMapOf<String, MutableList<Task>>()

                    snapshot?.documents?.forEach { document ->
                        val task = document.toObject(Task::class.java)
                        task?.let {
                            val hour = it.timeRange.split(":")[0]
                            taskMap.getOrPut(hour) { mutableListOf() }.add(it)
                        }
                    }

                    val timeSlots = generateTimeSlots().map { timeSlot ->
                        timeSlot.copy(tasks = taskMap[timeSlot.hour.split(":")[0]] ?: emptyList())
                    }

                    if (_binding != null) {
                        calendarAdapter = TimeslotAdapter(timeSlots)
                        binding.recyclerViewTimeSlots.adapter = calendarAdapter
                    }
                }
        }
    }

    private fun updateSelectedDate(days: Int) {
        isUpdatingDate = true
        selectedDate.add(Calendar.DAY_OF_YEAR, days)
        updateDateDisplay()
        fetchTasksForSelectedDate()
        isUpdatingDate = false
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateTextView = binding.root.findViewById<TextView>(R.id.dateTextView)
        dateTextView.text = dateFormat.format(selectedDate.time)
    }

    private fun animateClockNeedle() {
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val totalMinutesInDay = 24 * 60
        val elapsedMinutes = (hour * 60 + minute).toFloat()

        val verticalPosition = (elapsedMinutes / totalMinutesInDay) * clockContainer.height

        val animator = ValueAnimator.ofFloat(clockNeedle.translationY, verticalPosition)
        animator.duration = updateInterval
        animator.addUpdateListener { animation ->
            clockNeedle.translationY = animation.animatedValue as Float
        }
        animator.start()
    }

    // Add real-time listener for task count
    private fun addTaskCounterListener() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val taskCounterTextView = binding.root.findViewById<TextView>(R.id.taskCounter)

        // Check if user is logged in
        if (currentUserId == null) {
            taskCounterTextView.text = "0 Tasks" // Default to 0 if no user is logged in
            return
        }

        // Add Firestore snapshot listener
        taskCountListener = firestore.collection("tasks")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error listening for task count updates", e)
                    taskCounterTextView.text = "0 Tasks" // Display 0 if there is an error
                    return@addSnapshotListener
                }

                // Update task count display
                val taskCount = snapshot?.size() ?: 0
                taskCounterTextView.text = "$taskCount Tasks"
            }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
        taskCountListener?.remove() // Stop the listener when the fragment is paused
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }
}
