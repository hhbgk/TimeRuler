package com.haibox.timeruler

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.haibox.timeruler.databinding.ActivityMainBinding
import com.haibox.timeruler.ruler.EventTimelineFragment
import com.haibox.timeruler.ruler.TimelineFragment

class MainActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName
    private lateinit var binding: ActivityMainBinding
    private val adapter = RulerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val list: MutableList<String> = ArrayList()
        list.add(TimelineFragment::class.java.canonicalName as String)
        list.add(EventTimelineFragment::class.java.canonicalName as String)
        list.add("Test3")

        adapter.addData(list)
        val layoutManager = LinearLayoutManager(this)
        val divider = DividerItemDecoration(this, layoutManager.orientation)
        binding.rvRuler.addItemDecoration(divider)
        binding.rvRuler.layoutManager = layoutManager
        binding.rvRuler.adapter = adapter
        adapter.setOnItemClickListener { _, fragment, _ ->
            Log.i(TAG, "item click=$fragment")
            fragment?.let {
                ContainerActivity.startActivity(this@MainActivity, it)
            }
        }
    }
}