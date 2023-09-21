package com.haibox.timeruler

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.haibox.timeruler.databinding.ActivityContainerBinding

class ContainerActivity : AppCompatActivity() {
    companion object {
        const val FRAGMENT_NAME = "fragment_name"
        const val FRAGMENT_DATA = "fragment_data"

        @JvmStatic
        fun startActivity(context: Context, fragmentName: String, data: Bundle? = null) {
            val intent = Intent(context, ContainerActivity::class.java).apply {
                putExtra(FRAGMENT_NAME, fragmentName)
                if (data != null) putExtra(FRAGMENT_DATA, data)
            }
            context.startActivity(intent)
        }
    }
    private lateinit var binding: ActivityContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragmentName: String? = intent?.getStringExtra(FRAGMENT_NAME)
        if (null == fragmentName) {
            finish()
            return
        }
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(FRAGMENT_DATA, Bundle::class.java)
        } else {
            intent?.getParcelableExtra(FRAGMENT_DATA)
        }
        replaceFragment(R.id.clContainer, fragmentName, data)
    }

    override fun onBackPressed() {
        finish()
    }
    private fun replaceFragment(containerId: Int, fragmentName: String, bundle: Bundle? = null, replace: Boolean = false) {
        val fragment = supportFragmentManager.findFragmentByTag(fragmentName) ?: try {
            Class.forName(fragmentName).newInstance() as Fragment
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        fragment?.let {
            val transaction = supportFragmentManager.beginTransaction()
            if (null != bundle) {
                it.arguments = bundle
            }
            if (replace) {
                if (Build.VERSION.SDK_INT < 24) {// 兼容Android 7.0以下，清除Fragment不完全的问题
                    for (f in supportFragmentManager.fragments) {
                        transaction.remove(f)
                    }
                }
                transaction.replace(containerId, it)
            } else {
                for (f in supportFragmentManager.fragments) {
                    transaction.hide(f)
                }
                if (!it.isAdded) {
                    transaction.add(containerId, it, fragmentName)
                    transaction.addToBackStack(fragmentName)
                }
                transaction.show(it)
            }
            transaction.commitAllowingStateLoss()
        }
    }
}