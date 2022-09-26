package com.example.kurs7

import UriPathHelper.UriPathHelper
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

private var language = "eng"
private val dataPath =
    Environment.getExternalStoragePublicDirectory(String()).toString() + "/tessdata/"
private lateinit var filePath: String

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Toast.makeText(this, dataPath, Toast.LENGTH_SHORT).show()
        findViewById<Button>(R.id.imgch).setOnClickListener {

            pickFileOrPhoto()

        }

        findViewById<Button>(R.id.imgText).setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setPositiveButton("Русский и английский") { _, _ ->
                language = "eng+osd+rus"
                imgFunc()
            }
            builder.setNeutralButton("Русский") { _, _ ->
                language = "rus"
                imgFunc()
            }
            builder.setNegativeButton("Английский") { _, _ ->
                language = "eng+osd"
                imgFunc()
            }
            builder.setTitle("Выберите язык текста:")
            val alertDialog = builder.create()
            alertDialog.show()

            val firstBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            with(firstBtn) {
                setTextColor(Color.BLACK)
            }
            val secondBtn = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            with(secondBtn) {
                setTextColor(Color.BLACK)
            }
            val thirdBtn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            with(thirdBtn) {
                setTextColor(Color.BLACK)
            }


        }
    }


    private fun imgFunc() {

        val options = BitmapFactory.Options()
        options.inSampleSize =
            1 // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.

        val bitmap = BitmapFactory.decodeFile(filePath, options)

        val result = extractText(bitmap)

        findViewById<EditText>(R.id.getText).setText(result)


    }

    private fun pickFileOrPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultLauncher.launch(intent)


    }


    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.data != null) {
                    val uriPathHelper = UriPathHelper()
                    filePath = uriPathHelper.getPathFromUri(this, data.data!!)!!.toString()

                    val options = BitmapFactory.Options()
                    options.inSampleSize =
                        1 // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.

                    val bitmap = BitmapFactory.decodeFile(filePath, options)



                    findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
                }
            }


        }


    @Throws(Exception::class)
    private fun extractText(bitmap: Bitmap): String? {

        OpenCVLoader.initDebug()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Преобразовать в оттенки серого
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        //blur
        //Imgproc.GaussianBlur(mat, mat, Size(3.0, 3.0), 0.0)
        val thresh = Mat()
        //в бинарную
        Imgproc.adaptiveThreshold(
            mat,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,
            75,
            10.0
        )
        Core.bitwise_not(thresh, thresh)
        val bmp = Bitmap.createBitmap(thresh.width(), thresh.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(thresh, bmp)


        val tessBaseApi = TessBaseAPI()


        tessBaseApi.setDebug(true)
        tessBaseApi.init(dataPath, language)
        tessBaseApi.setImage(bmp)
        val extractedText = tessBaseApi.utF8Text
        tessBaseApi.recycle()
        findViewById<ImageView>(R.id.imageView).setImageBitmap(bmp)

        return extractedText
    }


}