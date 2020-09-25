package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.*
import java.time.LocalTime


class MainActivity : AppCompatActivity(){
    private var recordRunning = false
    private lateinit var inputStream1: InputStream
    private lateinit var recFile: File

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

       btnStart.setOnClickListener {
           Log.i("DBG", "start clicked")
           GlobalScope.launch(Dispatchers.IO){
               recordRunning = true
               async(Dispatchers.Default){ recordAudio()}.onAwait
               Log.i("DBG", "recording")
           }
       }

        btnStop.setOnClickListener { recordRunning = false }

        btnPlay.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                val recFileName = "testkjs.raw"
                val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                try {
                    recFile = File(storageDir.toString() + "/" + recFileName)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                Log.i("DBG", "$recFile is file")
                inputStream1 = FileInputStream(recFile)
                Log.i("DBG", "$inputStream1")
                val ft = async(Dispatchers.Default) { playAudio(inputStream1) }
                showTimes(ft.await())
            }
        }

    }


    @SuppressLint("SetTextI18n")
    fun showTimes(f: String) {
        txt.text = "first: $f"
    }

    private fun recordAudio() {
        val recFileName = "testkjs.raw"
        val storageDir= getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        try{
            recFile = File(storageDir.toString() + "/" + recFileName)
        } catch (e: IOException) {
           e.printStackTrace()
        }
        try {
            val outputStream= FileOutputStream(recFile)
            val bufferedOutputStream= BufferedOutputStream(outputStream)
            val dataOutputStream= DataOutputStream(bufferedOutputStream)
            val minBufferSize= AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val aFormat= AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            val recorder = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(aFormat)
                    .setBufferSizeInBytes(minBufferSize)
                    .build()
            val audioData= ByteArray(minBufferSize)
            recorder.startRecording()

            while(recordRunning) {
                val numofBytes= recorder.read(audioData, 0, minBufferSize)
                if(numofBytes>0) {
                    dataOutputStream.write(audioData)
                }
            }
            recorder.stop()
            dataOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun playAudio(inpt_stream: InputStream): String {
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
        track.setVolume(0.2f)
        val startTime = LocalTime.now().toString()
        track.play()
        var ip: Int
        val buffer = ByteArray(minBufferSize)
        try {
            ip = inpt_stream.read(buffer, 0, minBufferSize)
            while (ip != -1) {
                track.write(buffer, 0, ip)
                ip = inpt_stream.read(buffer, 0, minBufferSize)
            }
        } catch (e: IOException) {

        }
        try {
            inpt_stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        track.stop()
        track.release()
        return startTime
    }
}