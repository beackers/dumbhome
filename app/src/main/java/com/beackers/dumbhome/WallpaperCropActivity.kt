package com.beackers.dumbhome

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WallpaperCropActivity : AppCompatActivity() {
    private lateinit var cropImageView: CropImageView
    private lateinit var overlayView: CropOverlayView
    private var sourceBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_crop)

        cropImageView = findViewById(R.id.cropImageView)
        overlayView = findViewById(R.id.cropOverlay)

        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        if (imageUri == null) {
            finish()
            return
        }

        sourceBitmap = decodeBitmap(imageUri)
        val bitmap = sourceBitmap
        if (bitmap == null) {
            Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cropImageView.post {
            val cropRect = calculateCropRect()
            cropImageView.cropRect = cropRect
            overlayView.cropRect = cropRect
            cropImageView.setImageBitmapAndReset(bitmap)
        }

        findViewById<Button>(R.id.cancelCropButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.saveCropButton).setOnClickListener { saveCroppedWallpaper() }
    }

    override fun onDestroy() {
        super.onDestroy()
        sourceBitmap?.recycle()
        sourceBitmap = null
    }

    private fun saveCroppedWallpaper() {
        val source = sourceBitmap ?: return
        val cropped = cropImageView.createCroppedBitmap(source)
        WallpaperStorage.save(this, cropped)
        cropped.recycle()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun calculateCropRect(): android.graphics.RectF {
        val screenRatio = resources.displayMetrics.widthPixels.toFloat() / resources.displayMetrics.heightPixels.toFloat()
        val viewWidth = cropImageView.width.toFloat()
        val viewHeight = cropImageView.height.toFloat()

        val maxWidth = viewWidth * 0.92f
        val maxHeight = viewHeight * 0.92f

        val cropWidth: Float
        val cropHeight: Float
        if (maxWidth / maxHeight > screenRatio) {
            cropHeight = maxHeight
            cropWidth = cropHeight * screenRatio
        } else {
            cropWidth = maxWidth
            cropHeight = cropWidth / screenRatio
        }

        val left = (viewWidth - cropWidth) / 2f
        val top = (viewHeight - cropHeight) / 2f
        return android.graphics.RectF(left, top, left + cropWidth, top + cropHeight)
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(src)
        } else {
            contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
