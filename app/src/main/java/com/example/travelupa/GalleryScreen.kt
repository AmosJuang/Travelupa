package com.example.travelupa

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import android.widget.Toast

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    imageDao: ImageDao,
    onImageSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val images by imageDao.getAllImages().collectAsState(initial = emptyList())
    var showAddImageDialog by remember { mutableStateOf(false) }
    var selectedImageEntity by remember { mutableStateOf<ImageEntity?>(null) }
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf<ImageEntity?>(null) }

    LaunchedEffect(images) {
        Log.d("GalleryScreen", "Total images: ${images.size}")
        images.forEachIndexed { index, image ->
            Log.d("GalleryScreen", "Image $index path: ${image.localPath}")
            val file = File(image.localPath)
            Log.d("GalleryScreen", "File exists: ${file.exists()}, is readable: ${file.canRead()}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddImageDialog = true },
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Image")
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(paddingValues)
        ) {
            items(images) { image ->
                Image(
                    painter = rememberAsyncImagePainter(
                        model = image.localPath
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                        .clickable {
                            selectedImageEntity = image
                            onImageSelected(Uri.parse(image.localPath))
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    if (showAddImageDialog) {
        AddImageDialog(
            onDismiss = { showAddImageDialog = false },
            onImageAdded = { uri ->
                try {
                    val localPath = saveImageLocally(context, uri)
                    val newImage = ImageEntity(localPath = localPath)
                    CoroutineScope(Dispatchers.IO).launch {
                        imageDao.insert(newImage)
                    }
                    showAddImageDialog = false
                } catch (e: Exception) {
                    Log.e("ImageSave", "Failed to save image", e)
                }
            }
        )
    }

    selectedImageEntity?.let { imageEntity ->
        ImageDetailDialog(
            imageEntity = imageEntity,
            onDismiss = { selectedImageEntity = null },
            onDelete = { imageToDelete ->
                showDeleteConfirmation = imageToDelete
            }
        )
    }

    showDeleteConfirmation?.let { imageToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Image") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            imageDao.delete(imageToDelete)
                            val file = File(imageToDelete.localPath)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                        showDeleteConfirmation = null
                        selectedImageEntity = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddImageDialog(
    onDismiss: () -> Unit,
    onImageAdded: (Uri) -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToUri(context, it)
            imageUri = uri
        }
    }


    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Image") },
        text = {
            Column {
                imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select from File")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Request camera permission before launching camera
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Take Photo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    imageUri?.let { uri ->
                        onImageAdded(uri)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImageDetailDialog(
    imageEntity: ImageEntity,
    onDismiss: () -> Unit,
    onDelete: (ImageEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Image(
                painter = rememberAsyncImagePainter(model = imageEntity.localPath),
                contentDescription = "Detailed Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        },
        confirmButton = {
            Row {
                Button(onClick = { onDelete(imageEntity) }) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

// ✅ FIXED: Use FileProvider instead of Uri.fromFile() to prevent crash
fun saveBitmapToUri(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    outputStream.close()

    // Use FileProvider to get content:// URI instead of file:// URI
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
