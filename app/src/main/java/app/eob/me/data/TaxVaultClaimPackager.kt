package app.eob.me.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object TaxVaultClaimPackager {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 48

    fun buildClaimPackage(
        context: Context,
        coverRows: List<TaxVaultExportRow>,
        evidenceImageUrls: List<String>
    ): Result<Uri> {
        return runCatching {
            val bitmaps = evidenceImageUrls.mapNotNull { url -> downloadBitmap(url) }
            val pdfFile = writePdf(context, coverRows, bitmaps)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        }
    }

    private fun writePdf(
        context: Context,
        coverRows: List<TaxVaultExportRow>,
        evidenceBitmaps: List<Bitmap>
    ): File {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 11f
            isFakeBoldText = true
        }

        val coverPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val coverPage = document.startPage(coverPageInfo)
        val coverCanvas = coverPage.canvas
        var y = MARGIN.toFloat()
        coverCanvas.drawText("FSA / HSA Claim Package Summary", MARGIN.toFloat(), y, titlePaint)
        y += 32f
        coverCanvas.drawText("Date", MARGIN.toFloat(), y, headerPaint)
        coverCanvas.drawText("Provider", 150f, y, headerPaint)
        coverCanvas.drawText("CPT", 330f, y, headerPaint)
        coverCanvas.drawText("Patient Resp.", 430f, y, headerPaint)
        y += 18f
        coverRows.forEach { row ->
            coverCanvas.drawText(row.date.ifBlank { "—" }, MARGIN.toFloat(), y, bodyPaint)
            coverCanvas.drawText(row.provider.take(24), 150f, y, bodyPaint)
            coverCanvas.drawText(row.cptCode.ifBlank { "—" }, 330f, y, bodyPaint)
            coverCanvas.drawText(row.patientResponsibility.asCurrency(), 430f, y, bodyPaint)
            y += 16f
        }
        document.finishPage(coverPage)

        evidenceBitmaps.forEachIndexed { index, bitmap ->
            val pageNumber = index + 2
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            drawEvidenceBitmap(page.canvas, bitmap)
            document.finishPage(page)
        }

        val outputDir = File(context.cacheDir, "tax_vault_exports").apply { mkdirs() }
        val outputFile = File(outputDir, "claim_package_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
        document.close()
        evidenceBitmaps.forEach { it.recycle() }
        return outputFile
    }

    private fun drawEvidenceBitmap(canvas: Canvas, bitmap: Bitmap) {
        val availableWidth = PAGE_WIDTH - MARGIN * 2
        val availableHeight = PAGE_HEIGHT - MARGIN * 2
        val scale = minOf(
            availableWidth.toFloat() / bitmap.width.toFloat(),
            availableHeight.toFloat() / bitmap.height.toFloat()
        )
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        val left = MARGIN + (availableWidth - width) / 2
        val top = MARGIN + (availableHeight - height) / 2
        val dest = Rect(left, top, left + width, top + height)
        canvas.drawBitmap(bitmap, null, dest, null)
    }

    private fun downloadBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            instanceFollowRedirects = true
        }
        return try {
            connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } finally {
            connection.disconnect()
        }
    }
}
