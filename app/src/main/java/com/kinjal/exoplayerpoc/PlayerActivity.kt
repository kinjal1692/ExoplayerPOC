package com.kinjal.exoplayerpoc

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.annotation.DrawableRes
import android.support.constraint.ConstraintSet
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*


class PlayerActivity : AppCompatActivity() {

    // Constants
    private val ANIMATION_DURATION: Long = 1 * 1000
    private val DEFAULT_SKIP_INTERVAL = 10 * 1000

    // Variables
    private var player: SimpleExoPlayer? = null

    private var url = ""
    private var status = ""

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    companion object {
        val KEY_URL: String = "url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        if (intent?.hasExtra(KEY_URL) == true) {
            url = intent?.getStringExtra(KEY_URL) ?: ""
        }
        setGestureListener()
    }

    /**
     * Initialise the player
     */
    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(
                    DefaultRenderersFactory(this),
                    DefaultTrackSelector(),
                    DefaultLoadControl()
            )
            playerView?.player = player
            setPlayerControls()
            player?.playWhenReady = true
            player?.seekTo(currentWindow, playbackPosition)
        }
        val mediaSource = buildMediaSource(Uri.parse(url))
        player?.prepare(mediaSource, true, false)
        player?.addListener(object : Player.DefaultEventListener() {
            override fun onPlayerError(error: ExoPlaybackException?) {
                val errorMsg = "Failed to play media Error: ${error?.sourceException?.message}"
                Log.e("ExoPlayer", errorMsg)
                if (errorMsg != "null") {
                    Toast.makeText(this@PlayerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)

                when (playbackState) {
                    Player.STATE_BUFFERING ->
                        showProgressBar(true)
                    Player.STATE_READY -> {
                        showProgressBar(false)
                        status = if (playWhenReady) {
                            "playing"
                            // Video is playing
                        } else {
                            "paused"
                            // Video is paused
                        }
                    }
                    Player.STATE_IDLE -> {
                    }
                    Player.STATE_ENDED -> {
                    }

                }
            }
        })
    }

    /**
     * Builds generic media source
     */
    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory =
                DefaultDataSourceFactory(
                        this, Util.getUserAgent(this, "Application Name"),
                        DefaultBandwidthMeter()
                )
        return ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
    }

    /**
     * Release the player before closing teh activity
     */
    private fun releasePlayer() {
        if (player != null) {
            playbackPosition = player!!.currentPosition
            currentWindow = player!!.currentWindowIndex
            playWhenReady = player!!.playWhenReady
            player!!.release()
            player = null
        }
    }

    /**
     * Shows animated image for rewind and fast forward
     * @param isActionFastForward true if fast forward, false is rewind
     */
    private fun animateImageView(isActionFastForward: Boolean) {
        // Shifts imageView to left or right based on forward or rewind
        // by setting constraint set
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.setHorizontalBias(R.id.imageView, if (isActionFastForward) 0.75f else 0.25f)
        constraintSet.applyTo(constraintLayout)

        // Calculating next position
        val seekPosition =
                if (isActionFastForward) player!!.currentPosition + DEFAULT_SKIP_INTERVAL else
                    player!!.currentPosition - DEFAULT_SKIP_INTERVAL
        player?.seekTo(seekPosition)

        // Set image
        setImageRes(if (isActionFastForward) R.drawable.f1 else R.drawable.r1)
        imageView.visibility = View.VISIBLE
        Handler().postDelayed({
            imageView.visibility = View.GONE
        }, ANIMATION_DURATION)
    }

    /**
     * Sets controls to player
     */
    private fun setPlayerControls() {
        playerControlView.player = player
    }

    /**
     * Sets imageView resource
     * @param imageRes Image's res id
     */
    private fun setImageRes(@DrawableRes imageRes: Int) {
        // Clear previously cached image
        Glide.with(this)
                .clear(imageView)
        // Set new image
        Glide.with(this)
                .asGif()
                .load(imageRes)
                .into(imageView)
    }

    /**
     * Sets gesture(Single tap, double tap) listener
     */
    private fun setGestureListener() {
        // Get width of the view
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        // Calculate horizontal midpoint of the width
        val midPoint = width * 0.5

        playerView.setOnTouchListener(object : OnTouchListener(this@PlayerActivity) {
            override fun onDoubleClick(e: MotionEvent) {
                // If x is more than midpoint, fast forward or rewind
                animateImageView(e.x > midPoint)
            }

            override fun onClick() {
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (playerControlView.isVisible)
                        playerControlView.hide()
                    else
                        playerControlView.show()
                    playerControlView.showTimeoutMs = 10000
                } else {
                    playerControlView.show()
                    playerControlView.showTimeoutMs = 0
                }
            }
        })
    }

    /**
     * Show progressbar if buffering
     * @param show - true/ false
     */
    private fun showProgressBar(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Sets players view window to full screen
     */
    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    /**
     * Sets players view window to full screen
     */
    @SuppressLint("InlinedApi")
    private fun hideSystemUiFullScreen() {
        playerView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    /**
     * Sets up playerView according to configuration
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUiFullScreen()
            /*if (status == "playing") {
                showTextArea(false)
            }*/
        } else {
            hideSystemUi()
            //showTextArea(true)
        }
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        initializePlayer()
    }

    public override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    public override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    /**
     * Custom touch listener to determine double tap
     */
    abstract class OnTouchListener(context: Context) : View.OnTouchListener {

        private val mDetector: GestureDetectorCompat

        init {
            mDetector = GestureDetectorCompat(context, GestureListener())
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return mDetector.onTouchEvent(event)

        }

        fun onSwipeRight() {
            Log.i(TAG, "onSwipeRight: Swiped to the RIGHT")
        }

        fun onSwipeLeft() {
            Log.i(TAG, "onSwipeLeft: Swiped to the LEFT")
        }

        fun onSwipeTop() {
            Log.i(TAG, "onSwipeTop: Swiped to the TOP")
        }

        fun onSwipeBottom() {
            Log.i(TAG, "onSwipeBottom: Swiped to the BOTTOM")
        }

        abstract fun onClick()

        abstract fun onDoubleClick(e: MotionEvent)

        fun onLongClick() {
            Log.i(TAG, "onLongClick: LONG click in the screen")
        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onClick()
                return super.onSingleTapUp(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleClick(e)
                return super.onDoubleTap(e)
            }

            override fun onLongPress(e: MotionEvent) {
                onLongClick()
                super.onLongPress(e)
            }

            override fun onFling(
                    e1: MotionEvent, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
            ): Boolean {
                var result = false
                try {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                                Math.abs(velocityX) > SWIPE_VELOCITY
                        ) {
                            if (diffX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                            result = true
                        }
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD
                            && Math.abs(velocityY) > SWIPE_VELOCITY
                    ) {
                        if (diffY > 0) {
                            onSwipeBottom()
                        } else {
                            onSwipeTop()
                        }
                        result = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return result
            }

            private val SWIPE_THRESHOLD = 100

            private val SWIPE_VELOCITY = 100
        }

        companion object {

            private val TAG = "OnSwipeTouchListener"
        }
    }

}
