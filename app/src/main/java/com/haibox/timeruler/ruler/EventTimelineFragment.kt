package com.haibox.timeruler.ruler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.haibox.timeruler.databinding.FragmentEventTimelineBinding
import com.haibox.timeruler.databinding.FragmentTimelineBinding

class EventTimelineFragment : Fragment() {
    private lateinit var binding: FragmentEventTimelineBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}