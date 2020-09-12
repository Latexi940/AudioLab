package com.example.audiolab

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment

import android.util.Log
import java.io.*

class Record(context: Context) : Runnable {
    private val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    private var recFile: File? = null

    override fun run() {
        try {
            Log.i("RCRDLAB", "Creating file")
            recFile = File(storageDir.toString() + "/" + recFileName)
        } catch (e: IOException) {
            Log.d("err", "error: $e")
        }

        try {
            val outputStream = FileOutputStream(recFile)
            val bufferedOutputStream = BufferedOutputStream(outputStream)
            val dataOutputStream = DataOutputStream(bufferedOutputStream)
            val minBufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val aFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            val recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(aFormat)
                .setBufferSizeInBytes(minBufferSize)
                .build()
            val audioData = ByteArray(minBufferSize)
            recorder.startRecording()
            while (recRunning) {
                Log.i("RCRDLAB", "Recording...")
                val numofBytes = recorder.read(audioData, 0, minBufferSize)
                if (numofBytes > 0) {
                    dataOutputStream.write(audioData)
                }
            }
            recorder.stop()
            Log.i("RCRDLAB", "Recording stopped")
            dataOutputStream.close()
        } catch (e: IOException) {
            Log.d("err", "error: $e")
        }
    }
}