package com.fexed.checklist.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.fexed.checklist.R
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Coordinate conversions for OpenStreetMap Slippy Map (Web Mercator)
 */
object WebMercatorProjection {
    fun lonToTileX(lon: Double, zoom: Int): Double {
        return (lon + 180.0) / 360.0 * (1 shl zoom)
    }

    fun latToTileY(lat: Double, zoom: Int): Double {
        val clampedLat = min(max(lat, -85.05112878), 85.05112878)
        val latRad = Math.toRadians(clampedLat)
        return (1.0 - asinh(tan(latRad)) / PI) / 2.0 * (1 shl zoom)
    }

    fun tileXToLon(x: Double, zoom: Int): Double {
        return x / (1 shl zoom) * 360.0 - 180.0
    }

    fun tileYToLat(y: Double, zoom: Int): Double {
        val n = PI - 2.0 * PI * y / (1 shl zoom)
        return Math.toDegrees(atan(sinh(n)))
    }

    /**
     * Approximate meters per pixel at latitude and zoom level
     */
    fun metersPerPixel(lat: Double, zoom: Int): Double {
        val latRad = Math.toRadians(lat)
        return 156543.03392 * cos(latRad) / (2.0.pow(zoom.toDouble()))
    }
}

/**
 * Interactive map dialog allowing the user to select latitude and longitude
 * by tapping, dragging, zooming, searching addresses, or using device location.
 */
@Composable
fun LocationPickerMapDialog(
    initialLat: Double?,
    initialLng: Double?,
    initialRadius: Float = 200f,
    onDismiss: () -> Unit,
    onLocationSelected: (lat: Double, lng: Double, suggestedName: String?) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()

    // Default to initial location or Rome / default European coordinates
    var centerLat by remember { mutableDoubleStateOf(initialLat ?: 41.9028) }
    var centerLng by remember { mutableDoubleStateOf(initialLng ?: 12.4964) }
    var zoomLevel by remember { mutableFloatStateOf(if (initialLat != null) 15f else 13f) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var resolvedPlaceName by remember { mutableStateOf<String?>(null) }

    // In-memory tile cache: "z/x/y" -> Bitmap
    val tileCache = remember { mutableStateMapOf<String, Bitmap>() }

    // Attempt to center on user's current GPS location if no initial coords were passed
    LaunchedEffect(Unit) {
        if (isPreview) return@LaunchedEffect
        if (initialLat == null || initialLng == null) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                @SuppressLint("MissingPermission")
                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnSuccessListener { loc ->
                    if (loc != null) {
                        centerLat = loc.latitude
                        centerLng = loc.longitude
                        zoomLevel = 15f
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Debounced reverse geocode to suggest a friendly name whenever centerLat or centerLng settles
    LaunchedEffect(centerLat, centerLng) {
        if (isPreview) return@LaunchedEffect
        delay(400)
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    geocoder.getFromLocation(centerLat, centerLng, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            val name = addr.featureName ?: addr.thoroughfare ?: addr.locality
                            if (!name.isNullOrBlank()) {
                                resolvedPlaceName = name
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(centerLat, centerLng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val name = addr.featureName ?: addr.thoroughfare ?: addr.locality
                        if (!name.isNullOrBlank()) {
                            resolvedPlaceName = name
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    searchError = null
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.map_search_placeholder),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                singleLine = true,
                                trailingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            searchError = null
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        isSearching = true
                                        searchError = null
                                        val errNotFound = context.getString(R.string.map_no_locations_found)
                                        val errFailed = context.getString(R.string.map_search_failed)
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val geocoder = Geocoder(context, Locale.getDefault())
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    geocoder.getFromLocationName(searchQuery, 1) { results ->
                                                        isSearching = false
                                                        if (results.isNotEmpty()) {
                                                            val first = results[0]
                                                            centerLat = first.latitude
                                                            centerLng = first.longitude
                                                            zoomLevel = 15f
                                                            resolvedPlaceName = first.featureName ?: first.locality
                                                        } else {
                                                            searchError = errNotFound
                                                        }
                                                    }
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    val results = geocoder.getFromLocationName(searchQuery, 1)
                                                    isSearching = false
                                                    if (!results.isNullOrEmpty()) {
                                                        val first = results[0]
                                                        centerLat = first.latitude
                                                        centerLng = first.longitude
                                                        zoomLevel = 15f
                                                        resolvedPlaceName = first.featureName ?: first.locality
                                                    } else {
                                                        searchError = errNotFound
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                isSearching = false
                                                searchError = errFailed
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.action_search),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (searchError != null) {
                            Text(
                                text = searchError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 56.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resolvedPlaceName ?: stringResource(R.string.map_default_place_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.map_coords_format,
                                        centerLat,
                                        centerLng,
                                        initialRadius.toInt()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }

                            Button(
                                onClick = {
                                    onLocationSelected(centerLat, centerLng, resolvedPlaceName)
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.map_confirm_location), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val halfW = canvasWidth / 2f
                val halfH = canvasHeight / 2f

                val tileZoom = zoomLevel.toInt().coerceIn(1, 18)
                val scale = 2.0.pow((zoomLevel - tileZoom).toDouble()).toFloat()
                val tileSize = 256f * scale

                val centerTileX = WebMercatorProjection.lonToTileX(centerLng, tileZoom)
                val centerTileY = WebMercatorProjection.latToTileY(centerLat, tileZoom)

                val tilesWide = (canvasWidth / tileSize).toInt() + 3
                val tilesHigh = (canvasHeight / tileSize).toInt() + 3

                val minTileX = floor(centerTileX - tilesWide / 2f).toInt()
                val maxTileX = floor(centerTileX + tilesWide / 2f).toInt()
                val minTileY = floor(centerTileY - tilesHigh / 2f).toInt()
                val maxTileY = floor(centerTileY + tilesHigh / 2f).toInt()

                val maxTileIndex = (1 shl tileZoom) - 1

                // Fetch missing tiles
                LaunchedEffect(tileZoom, minTileX, maxTileX, minTileY, maxTileY) {
                    if (isPreview) return@LaunchedEffect
                    for (x in minTileX..maxTileX) {
                        for (y in minTileY..maxTileY) {
                            if (y in 0..maxTileIndex) {
                                val wrappedX = ((x % (maxTileIndex + 1)) + (maxTileIndex + 1)) % (maxTileIndex + 1)
                                val key = "$tileZoom/$wrappedX/$y"
                                if (!tileCache.containsKey(key)) {
                                    launch(Dispatchers.IO) {
                                        try {
                                            val url = URL("https://tile.openstreetmap.org/$tileZoom/$wrappedX/$y.png")
                                            val conn = url.openConnection() as HttpURLConnection
                                            conn.setRequestProperty("User-Agent", "ChecklistApp/1.0 (Android; OpenStreetMap)")
                                            conn.connectTimeout = 4000
                                            conn.readTimeout = 4000
                                            if (conn.responseCode == 200) {
                                                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                                                if (bmp != null) {
                                                    withContext(Dispatchers.Main) {
                                                        tileCache[key] = bmp
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Map Canvas: handles Pan (drag), Pinch-to-Zoom, and Tap
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var pastTouchSlop = false
                                var panTotal = Offset.Zero
                                val touchSlop = viewConfiguration.touchSlop

                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (canceled) break

                                    if (event.changes.size > 1) {
                                        pastTouchSlop = true
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        if (zoomChange != 1f) {
                                            val zoomDelta = (ln(zoomChange.toDouble()) / ln(1.4)).toFloat()
                                            zoomLevel = (zoomLevel + zoomDelta).coerceIn(3f, 18.5f)
                                        }

                                        if (panChange != Offset.Zero) {
                                            val currentTileZoom = zoomLevel.toInt().coerceIn(1, 18)
                                            val currentScale = 2.0.pow((zoomLevel - currentTileZoom).toDouble())
                                            val curTileSize = 256.0 * currentScale

                                            val curCenterX = WebMercatorProjection.lonToTileX(centerLng, currentTileZoom)
                                            val curCenterY = WebMercatorProjection.latToTileY(centerLat, currentTileZoom)

                                            val newX = curCenterX - (panChange.x / curTileSize)
                                            val newY = curCenterY - (panChange.y / curTileSize)

                                            centerLng = WebMercatorProjection.tileXToLon(newX, currentTileZoom)
                                            centerLat = WebMercatorProjection.tileYToLat(newY, currentTileZoom)
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (event.changes.size == 1) {
                                        val change = event.changes[0]
                                        if (change.pressed) {
                                            val positionChange = change.position - change.previousPosition
                                            panTotal += positionChange

                                            if (!pastTouchSlop && panTotal.getDistance() > touchSlop) {
                                                pastTouchSlop = true
                                            }

                                            if (pastTouchSlop && positionChange != Offset.Zero) {
                                                val currentTileZoom = zoomLevel.toInt().coerceIn(1, 18)
                                                val currentScale = 2.0.pow((zoomLevel - currentTileZoom).toDouble())
                                                val curTileSize = 256.0 * currentScale

                                                val curCenterX = WebMercatorProjection.lonToTileX(centerLng, currentTileZoom)
                                                val curCenterY = WebMercatorProjection.latToTileY(centerLat, currentTileZoom)

                                                val newX = curCenterX - (positionChange.x / curTileSize)
                                                val newY = curCenterY - (positionChange.y / curTileSize)

                                                centerLng = WebMercatorProjection.tileXToLon(newX, currentTileZoom)
                                                centerLat = WebMercatorProjection.tileYToLat(newY, currentTileZoom)
                                                change.consume()
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                // If finger lifted without panning past slop: user tapped!
                                if (!pastTouchSlop) {
                                    val currentTileZoom = zoomLevel.toInt().coerceIn(1, 18)
                                    val currentScale = 2.0.pow((zoomLevel - currentTileZoom).toDouble())
                                    val curTileSize = 256.0 * currentScale

                                    val offsetX = down.position.x - halfW
                                    val offsetY = down.position.y - halfH

                                    val curCenterX = WebMercatorProjection.lonToTileX(centerLng, currentTileZoom)
                                    val curCenterY = WebMercatorProjection.latToTileY(centerLat, currentTileZoom)

                                    val tappedTileX = curCenterX + (offsetX / curTileSize)
                                    val tappedTileY = curCenterY + (offsetY / curTileSize)

                                    centerLng = WebMercatorProjection.tileXToLon(tappedTileX, currentTileZoom)
                                    centerLat = WebMercatorProjection.tileYToLat(tappedTileY, currentTileZoom)
                                }
                            }
                        }
                ) {
                    // Draw map tiles
                    val curTileZoom = zoomLevel.toInt().coerceIn(1, 18)
                    val curScale = 2.0.pow((zoomLevel - curTileZoom).toDouble()).toFloat()
                    val curTileSize = 256f * curScale

                    val curCenterTileX = WebMercatorProjection.lonToTileX(centerLng, curTileZoom)
                    val curCenterTileY = WebMercatorProjection.latToTileY(centerLat, curTileZoom)

                    val maxIndex = (1 shl curTileZoom) - 1

                    for (x in minTileX..maxTileX) {
                        for (y in minTileY..maxTileY) {
                            if (y in 0..maxIndex) {
                                val wrappedX = ((x % (maxIndex + 1)) + (maxIndex + 1)) % (maxIndex + 1)
                                val key = "$curTileZoom/$wrappedX/$y"

                                val pixelX = halfW + ((x - curCenterTileX) * curTileSize).toFloat()
                                val pixelY = halfH + ((y - curCenterTileY) * curTileSize).toFloat()

                                val bmp = tileCache[key]
                                if (bmp != null && !bmp.isRecycled) {
                                    drawImage(
                                        image = bmp.asImageBitmap(),
                                        dstOffset = IntOffset(pixelX.toInt(), pixelY.toInt()),
                                        dstSize = IntSize(ceil(curTileSize).toInt() + 1, ceil(curTileSize).toInt() + 1)
                                    )
                                } else {
                                    // Parchment placeholder tile
                                    drawRect(
                                        color = Color(0xFFF3ECE0),
                                        topLeft = Offset(pixelX, pixelY),
                                        size = Size(curTileSize, curTileSize)
                                    )
                                    drawRect(
                                        color = Color(0xFFE5DCCE),
                                        topLeft = Offset(pixelX, pixelY),
                                        size = Size(curTileSize, curTileSize),
                                        style = Stroke(width = 1f)
                                    )
                                }
                            }
                        }
                    }

                    // 1. Draw Geofence radius circle around center
                    val mpp = WebMercatorProjection.metersPerPixel(centerLat, curTileZoom) / curScale
                    val radiusPx = (initialRadius / mpp).toFloat()

                    drawCircle(
                        color = Color(0xFF6E4D30).copy(alpha = 0.20f),
                        radius = radiusPx,
                        center = Offset(halfW, halfH)
                    )
                    drawCircle(
                        color = Color(0xFF6E4D30).copy(alpha = 0.70f),
                        radius = radiusPx,
                        center = Offset(halfW, halfH),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 2. Center Pin Marker (teardrop pin pointing directly at halfW, halfH)
                    // Pin shadow
                    drawOval(
                        color = Color.Black.copy(alpha = 0.25f),
                        topLeft = Offset(halfW - 8.dp.toPx(), halfH - 3.dp.toPx()),
                        size = Size(16.dp.toPx(), 6.dp.toPx())
                    )

                    val pinHeadY = halfH - 22.dp.toPx()
                    val pinPath = Path().apply {
                        moveTo(halfW - 10.dp.toPx(), pinHeadY)
                        lineTo(halfW, halfH)
                        lineTo(halfW + 10.dp.toPx(), pinHeadY)
                        close()
                    }
                    drawPath(pinPath, color = Color(0xFF9E5943))
                    drawCircle(
                        color = Color(0xFF9E5943),
                        radius = 12.dp.toPx(),
                        center = Offset(halfW, pinHeadY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(halfW, pinHeadY)
                    )
                }

                // Map Control Floating Buttons (Zoom In, Zoom Out, My Location)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            zoomLevel = (zoomLevel + 1f).coerceAtMost(18.5f)
                        },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.zoom_in_desc))
                    }

                    FloatingActionButton(
                        onClick = {
                            zoomLevel = (zoomLevel - 1f).coerceAtLeast(3f)
                        },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.zoom_out_desc))
                    }

                    FloatingActionButton(
                        onClick = {
                            try {
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                @SuppressLint("MissingPermission")
                                val locationResult = fusedLocationClient.lastLocation
                                locationResult.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        centerLat = loc.latitude
                                        centerLng = loc.longitude
                                        zoomLevel = 15f
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.my_location_desc))
                    }
                }
            }
        }
    }

    @Preview(name = "Location picker map", showBackground = true)
@Composable
private fun LocationPickerMapDialogPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        LocationPickerMapDialog(
            initialLat = 47.3769,
            initialLng = 8.5417,
            initialRadius = 200f,
            onDismiss = {},
            onLocationSelected = { _, _, _ -> }
        )
    }
}
