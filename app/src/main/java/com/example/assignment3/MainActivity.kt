package com.example.assignment3

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.assignment3.ui.theme.Assignment3Theme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream
import java.io.IOException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import java.io.InputStream
import okhttp3.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Assignment3Theme {
                OCRAnalyticsPage()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRAnalyticsPage() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var ocrResult by remember { mutableStateOf(TextFieldValue()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OCR Analytics Page",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text("Select Image") }

        imageUri?.let {
            Text("Selected image: ${it.lastPathSegment}")
        }

        Button(
            onClick = {
                imageUri?.let { uri ->
                    isAnalyzing = true
                    analyzeImageWithOCR(context, uri) { result ->
                        isAnalyzing = false
                        ocrResult = TextFieldValue(result)
                    }
                }
            },
            modifier = Modifier.padding(vertical = 8.dp),
            enabled = !isAnalyzing
        ) { Text(if (isAnalyzing) "Analyzing..." else "Analyze Image") }

        // TextField to display OCR response
        OutlinedTextField(
            value = ocrResult,
            onValueChange = { ocrResult = it },
            label = { Text("Response") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            readOnly = true // To only display the response
        )
    }
}

fun analyzeImageWithOCR(context: Context, imageUri: Uri, onResult: (String) -> Unit) {
    val parcelFileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(imageUri, "r")
    val fileDescriptor = parcelFileDescriptor?.fileDescriptor
    val imageStream: InputStream = FileInputStream(fileDescriptor)

    val client = OkHttpClient()
    val requestBody = imageStream.readBytes().toRequestBody("application/octet-stream".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("https://Imageanalyse.cognitiveservices.azure.com/computervision/imageanalysis:analyze?api-version=2023-02-01-preview&features=objects&language=en&gender-neutral-caption=False")
        .post(requestBody)
        .addHeader("Ocp-Apim-Subscription-Key", "2ba8881bfc914cb68f841b1b47b13ce0") // Replace with your actual key
        .addHeader("Content-Type", "application/octet-stream")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult("Failed to analyze image: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    onResult("Unexpected code $response")
                } else {
                    val responseBody = response.body?.string()
                    onResult(responseBody ?: "No OCR result found")
                }
            }
        }
    })
}
