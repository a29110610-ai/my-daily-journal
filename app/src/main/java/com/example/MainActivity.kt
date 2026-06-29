package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: JournalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    JournalAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalAppScreen(
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entries by viewModel.allEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Dialog form states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var inputTitle by remember { mutableStateOf("") }
    var inputContent by remember { mutableStateOf("") }
    var selectedAlarmTime by remember { mutableStateOf<Long?>(null) }
    var isAlarmActive by remember { mutableStateOf(false) }

    // Live Clock State
    var currentTimeString by remember { mutableStateOf("") }
    var currentPersianDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeString = SimpleDateFormat("HH:mm:ss", Locale("fa", "IR")).format(Date(now))
            currentPersianDateString = SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR")).format(Date(now))
            delay(1000)
        }
    }

    // Permission tracking
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                Toast.makeText(context, "مجوز اعلان‌ها فعال شد!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "یادآوری‌ها بدون مجوز اعلان به شکل پیام‌های صوتی/تصویری نمایش داده نخواهند شد.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Request permission once at launch if not granted
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Clock and Info
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "امروز: $currentPersianDateString",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Left
                    )
                    Text(
                        text = "ساعت: $currentTimeString",
                        color = AmberAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Left
                    )
                }

                // App Title
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "روزنامه من 📝",
                        color = IceWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "دفترچه خاطرات روزانه آفلاین",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notification Permission Warning banner if missing
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("request_permission_button")
                        ) {
                            Text("فعال‌سازی", color = DeepSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = "برای دریافت هشدارهای دقیق، مجوز اعلان را فعال کنید.",
                            color = IceWhite,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
                    .padding(bottom = 12.dp),
                placeholder = {
                    Text(
                        "جستجو در خاطرات و یادداشت‌ها...",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "جستجو",
                        tint = AmberAccent
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    color = IceWhite,
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl,
                    fontSize = 14.sp
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = SlateLight,
                    focusedContainerColor = SlateMedium,
                    unfocusedContainerColor = SlateMedium
                )
            )

            // Journal Entries List
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Custom vector style drawn on Canvas / Circle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(SlateLight, Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "زنگوله",
                                tint = AmberAccent.copy(alpha = 0.6f),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "دفترچه شما خالی است",
                            color = IceWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "هیچ یادداشتی با عبارت جستجوی شما مطابقت ندارد." 
                                   else "خاطرات یا کارهای روزانه خود را ثبت کنید و ساعت یادآوری دقیقی برای آن تنظیم نمایید تا در زمان مشخص به شما اعلام شود.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        JournalEntryRow(
                            entry = entry,
                            onEdit = {
                                editingEntry = entry
                                inputTitle = entry.title
                                inputContent = entry.content
                                selectedAlarmTime = entry.alarmTime
                                isAlarmActive = entry.isAlarmActive
                                showAddEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteEntry(entry)
                                Toast.makeText(context, "یادداشت حذف شد", Toast.LENGTH_SHORT).show()
                            },
                            onToggleAlarm = { active ->
                                viewModel.toggleAlarm(entry, active)
                                val msg = if (active) "یادآور فعال شد" else "یادآور غیرفعال شد"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add New Entry
        FloatingActionButton(
            onClick = {
                editingEntry = null
                inputTitle = ""
                inputContent = ""
                selectedAlarmTime = null
                isAlarmActive = false
                showAddEditDialog = true
            },
            containerColor = AmberAccent,
            contentColor = DeepSlate,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_journal_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "ثبت یادداشت جدید",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        Dialog(onDismissRequest = { showAddEditDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateLight, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    // Dialog Header
                    Text(
                        text = if (editingEntry == null) "ثبت یادداشت جدید 📝" else "ویرایش یادداشت ✏️",
                        color = AmberAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title TextField
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_input"),
                        placeholder = {
                            Text(
                                "عنوان یادداشت (مثلا: داروهای عصر یا خاطره سفر)",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = IceWhite,
                            textAlign = TextAlign.Right,
                            textDirection = TextDirection.Rtl,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = SlateLight,
                            focusedContainerColor = DeepSlate,
                            unfocusedContainerColor = DeepSlate
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content TextField
                    OutlinedTextField(
                        value = inputContent,
                        onValueChange = { inputContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("content_input"),
                        placeholder = {
                            Text(
                                "متن یادداشت یا روزنامه خود را اینجا بنویسید...",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = IceWhite,
                            textAlign = TextAlign.Right,
                            textDirection = TextDirection.Rtl,
                            fontSize = 13.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = SlateLight,
                            focusedContainerColor = DeepSlate,
                            unfocusedContainerColor = DeepSlate
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alarm Scheduler Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepSlate, RoundedCornerShape(12.dp))
                            .clickable {
                                // Launch standard android Date & Time picker
                                val calendar = Calendar.getInstance()
                                selectedAlarmTime?.let { calendar.timeInMillis = it }

                                val timePickerDialog = TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        calendar.set(Calendar.MILLISECOND, 0)
                                        
                                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                                            Toast.makeText(context, "لطفاً زمان آینده را انتخاب کنید", Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedAlarmTime = calendar.timeInMillis
                                            isAlarmActive = true
                                        }
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                )

                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        timePickerDialog.show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                datePickerDialog.show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "تنظیم ساعت",
                            tint = if (selectedAlarmTime != null) AmberAccent else TextMuted
                        )

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            Text(
                                "تنظیم زمان زنگ و هشدار",
                                color = IceWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = if (selectedAlarmTime != null) {
                                    "یادآوری: " + SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR")).format(Date(selectedAlarmTime!!))
                                } else {
                                    "هیچ زمانی انتخاب نشده است"
                                },
                                color = if (selectedAlarmTime != null) AmberAccent else TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    // Enable Alarm Switch (only if alarm time is set)
                    if (selectedAlarmTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = isAlarmActive,
                                onCheckedChange = { isAlarmActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmberAccent,
                                    checkedTrackColor = SlateLight
                                )
                            )
                            Text(
                                "فعال بودن زنگ هشدار",
                                color = IceWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = { showAddEditDialog = false },
                            modifier = Modifier.weight(1f).testTag("dialog_cancel_button")
                        ) {
                            Text("انصراف", color = SoftRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Save button
                        Button(
                            onClick = {
                                if (inputTitle.isBlank() || inputContent.isBlank()) {
                                    Toast.makeText(context, "لطفاً عنوان و متن یادداشت را وارد کنید", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveEntry(
                                        id = editingEntry?.id ?: 0,
                                        title = inputTitle,
                                        content = inputContent,
                                        alarmTime = selectedAlarmTime,
                                        isAlarmActive = isAlarmActive
                                    )
                                    showAddEditDialog = false
                                    Toast.makeText(context, "با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).testTag("dialog_save_button")
                        ) {
                            Text("ذخیره یادداشت", color = DeepSlate, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalEntryRow(
    entry: JournalEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAlarm: (Boolean) -> Unit
) {
    val persianCreatedDate = remember(entry.timestamp) {
        SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR")).format(Date(entry.timestamp))
    }

    val persianAlarmDate = remember(entry.alarmTime) {
        entry.alarmTime?.let {
            SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR")).format(Date(it))
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SlateLight, RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .testTag("journal_entry_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Row with Date of creation & Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete & Edit quick actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = SoftRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Time and Date of Log creation
                Text(
                    text = persianCreatedDate,
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Entry Title
            Text(
                text = entry.title,
                color = AmberAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Entry Content snippet
            Text(
                text = entry.content,
                color = IceWhite,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Right,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )

            // Alarm Indicator Area if alarm exists
            if (persianAlarmDate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SlateLight, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox or compact Switch to toggle alarm state
                    Switch(
                        checked = entry.isAlarmActive,
                        onCheckedChange = onToggleAlarm,
                        modifier = Modifier
                            .scale(0.75f)
                            .testTag("alarm_switch_${entry.id}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberAccent,
                            checkedTrackColor = SlateLight
                        )
                    )

                    // Alarm Details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = persianAlarmDate,
                            color = if (entry.isAlarmActive) AmberAccent else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (entry.isAlarmActive) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Right
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "ساعت زنگ هشدار",
                            tint = if (entry.isAlarmActive) AmberAccent else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

