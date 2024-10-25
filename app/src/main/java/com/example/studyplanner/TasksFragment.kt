package com.example.studyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class TasksFragment : Fragment() {

    private lateinit var viewDetailsButton1: Button
    private lateinit var viewDetailsButton2: Button
    private lateinit var viewDetailsButton3: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        val viewDetailsButton1 = view.findViewById<Button>(R.id.btnViewDetails1)
        val viewDetailsButton2 = view.findViewById<Button>(R.id.btnViewDetails2)
        val viewDetailsButton3 = view.findViewById<Button>(R.id.btnViewDetails3)

        viewDetailsButton1.setOnClickListener {
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, TaskDetailsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        viewDetailsButton2.setOnClickListener {
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, TaskDetailsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        viewDetailsButton3.setOnClickListener {
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, TaskDetailsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }



    return view

    }

}