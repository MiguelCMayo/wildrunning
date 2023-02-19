package com.migueldev.wildrunning

import android.animation.ObjectAnimator
import android.view.View
import android.widget.LinearLayout
import java.util.concurrent.TimeUnit

object Utility {
    fun getSecFromWatch (watch: String): Int{

        var secs = 0
        var w: String = watch
        if (w.length == 5) w= "00:" + w

        // 00:00:00
        secs += w.subSequence(0,2).toString().toInt() * 3600
        secs += w.subSequence(3,5).toString().toInt() * 60
        secs += w.subSequence(6,8).toString().toInt()

        return secs
    }

    fun setHeightLinearLayout(ly: LinearLayout,value: Int){
        val params: LinearLayout.LayoutParams = ly.layoutParams as LinearLayout.LayoutParams
        params.height = value
        ly.layoutParams = params
    }

    fun animateViewofInt(view: View,attr: String, value:Int,time: Long){
        ObjectAnimator.ofInt(view,attr,value).apply {
            duration = time
            start()
        }
    }
    fun animateViewofFloat(view: View,attr: String, value:Float,time: Long){
        ObjectAnimator.ofFloat(view,attr,value).apply {
            duration = time
            start()
        }
    }

    fun getSecFromMatch(watch: String): Int{
        var secs = 0
        var w: String = watch
        if(w.length == 5) w = "00:"+w

        //00:00:00
        secs += w.subSequence(0,2).toString().toInt()*36300
        secs += w.subSequence(3,5).toString().toInt()*60
        secs += w.subSequence(6,8).toString().toInt()

        return secs
    }

    fun getFormattedStopWatch(ms: Long): String{
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds"
    }

    fun roundNumber(data: String, decimals: Int) : String{
        var d : String = data
        var p= d.indexOf(".", 0)

        if (p != null){
            var limit: Int = p+decimals +1
            if (d.length <= p+decimals+1) limit = d.length //-1
            d = d.subSequence(0, limit).toString()
        }

        return d
    }
}