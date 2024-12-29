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
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.adapter.TimeslotAdapter
import com.example.studyplanner.adapter.TaskAdapter
import com.example.studyplanner.databinding.FragmentHomeBinding
import com.example.studyplanner.model.Task
import com.example.studyplanner.model.TimeSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var firestore: FirebaseFirestore

    // For caching the username
    private var cachedUserName: String? = null

    // RecyclerViews for time slots

    private lateinit var calendarAdapter: TimeslotAdapter

    private lateinit var taskAdapter: TaskAdapter
    private val tasksList = mutableListOf<Task>()

    // Clock Needle
    private lateinit var clockNeedle: View
    private lateinit var clockContainer: RelativeLayout
    private lateinit var handler: Handler
    private val updateInterval: Long = 1000 // 1 second

    // Firestore task count listener
    private var taskCountListener: ListenerRegistration? = null

    private val updateTimeRunnable: Runnable = object : Runnable {
        override fun run() {
            animateClockNeedle()
            val dateTextView = binding.root.findViewById<TextView>(R.id.dateTextView)
            val timeTextView = binding.root.findViewById<TextView>(R.id.timeTextView)
            updateDateTime(dateTextView, timeTextView)
            handler.postDelayed(this, updateInterval)
        }
    }

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
        fetchTasks()

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

        updateDateTime(dateTextView, timeTextView)
    }

    private fun updateDateTime(dateTextView: TextView, timeTextView: TextView) {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Use SimpleDateFormat to get the month as text
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = dateFormat.format(calendar.time)

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

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

    private fun fetchTasks() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val selectedDate = getCurrentDate()  // Get the current date

        currentUserId?.let { userId ->
            firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error fetching tasks", e)
                        return@addSnapshotListener
                    }

                    // Safeguard: Ensure the fragment's view is still valid
                    if (view == null) return@addSnapshotListener

                    // Initialize a map to hold tasks grouped by their starting hour
                    val taskMap = mutableMapOf<String, MutableList<Task>>()

                    snapshot?.documents?.forEach { document ->
                        val task = document.toObject(Task::class.java)
                        task?.let {
                            // Filter tasks by the selected date
                            if (it.date == selectedDate) {
                                val hour = it.timeRange.split(":")[0]
                                taskMap.getOrPut(hour) { mutableListOf() }.add(it)
                            }
                        }
                    }

                    // Generate time slots and map the filtered tasks to the correct time slots
                    val timeSlots = generateTimeSlots().map { timeSlot ->
                        timeSlot.copy(tasks = taskMap[timeSlot.hour.split(":")[0]] ?: emptyList())
                    }

                    // Safeguard: Check if the fragment's view is still valid before updating UI
                    if (_binding != null) {
                        calendarAdapter = TimeslotAdapter(timeSlots)
                        binding.recyclerViewTimeSlots.adapter = calendarAdapter
                    }
                }
        }
    }


    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
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
