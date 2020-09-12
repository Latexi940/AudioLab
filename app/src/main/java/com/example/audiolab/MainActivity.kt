package com.example.audiolab

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageState
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException

var recRunning: Boolean = false
var isPlayingAudio = false
const val recFileName = "audioLabAudio.raw"
var audioFile: File? = null
lateinit var audioStream: FileInputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("RCRDLAB", "App starting")

        if (hasPermissions()) {

            record_button.setOnClickListener {
                Log.i("RCRDLAB", "Record button pressed")
                if (!recRunning) {
                    recRunning = true
                    record_button.text = "Stop recording"
                    var runnable = Record(this)
                    val thread = Thread(runnable)
                    thread.start()
                    info_text.text = "Recording..."
                } else {
                    recRunning = false
                    record_button.text = "Start recording"
                    info_text.text = "Not recording"
                }
                play_button.setOnClickListener {
                    Log.i("RCRDLAB", "Play button pressed")
                    if (this.getExternalFilesDir(Environment.DIRECTORY_MUSIC) != null) {
                        audioFile = File(this.getExternalFilesDir(Environment.DIRECTORY_MUSIC), recFileName)
                        audioStream = FileInputStream(audioFile)
                    }
                    if (!isPlayingAudio) {
                        isPlayingAudio = true
                        GlobalScope.launch(Dispatchers.Main) {
                            async(Dispatchers.Default) { playAudio(audioStream) }
                        }
                        Toast.makeText(this, "Playing audio...", Toast.LENGTH_SHORT).show()
                    } else {
                        isPlayingAudio = false
                        Toast.makeText(this, "Playing stopped", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } else {
            play_button.isEnabled = false
            record_button.isEnabled = false
            info_text.text = "No permissions"
        }
    }

    private fun hasPermissions(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i("BLEinfo", "No permission to record audio")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1);
            return true
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("BLEinfo", "No permission to record audio")
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
            return true
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("BLEinfo", "No permission to record audio")
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1);
            return true
        }
        return true
    }

    private fun playAudio(istream: FileInputStream) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            44100, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val aBuilder = AudioTrack.Builder()
        val aAttr: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val aFormat: AudioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val track = aBuilder.setAudioAttributes(aAttr)
            .setAudioFormat(aFormat)
            .setBufferSizeInBytes(minBufferSize)
            .build()
        track.setVolume(1f)
        Log.i("RCRDLAB", "Playing track")
        track.play()
        var i = 0
        val buffer = ByteArray(minBufferSize)
        try {
            i = istream.read(buffer, 0, minBufferSize)
            while (i != -1 && isPlayingAudio) {
                Log.d("RCRDLAB", "Playing...")
                track.write(buffer, 0, i)
                i = istream.read(buffer, 0, minBufferSize)
            }
            isPlayingAudio = false
            Log.d("RCRDLAB", "Playing stopped")
        } catch (e: IOException) {
            Log.d("RCRDLAB", "Error playing audio: $e")
        }
        track.stop()
    }
}