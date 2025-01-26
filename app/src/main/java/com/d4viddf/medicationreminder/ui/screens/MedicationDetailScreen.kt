package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailsScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    var medication by remember { mutableStateOf<Medication?>(null) }
    var currentProgress by remember { mutableStateOf(0.33f) }

    // Fetch the medication details using LaunchedEffect
    LaunchedEffect(key1 = medicationId) {
        medication = viewModel.getMedicationById(medicationId)
    }

    val color = MedicationColor.valueOf((medication?.color ?: MedicationColor.LIGHT_ORANGE).toString())

    // 2. Main Content
    LazyColumn {
        // First Row with Background Color and Rounded Corners
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = color.backgroundColor,
                        shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button with Fully Rounded Corners and Larger Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp) // Slightly smaller size for the box
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = CircleShape // Fully rounded corners for the back button
                            )
                            .clickable { onNavigateBack() }, // Make it clickable
                        contentAlignment = Alignment.Center // Center the icon
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp), // Slightly larger icon
                            tint = Color.White
                        )
                    }

                    // Edit Text with Rounded Corners and Smaller Size
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp) // Rounded corners for the text box
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp) // Adjust padding for smaller size
                            .clickable { /* Handle edit action */ },
                        contentAlignment = Alignment.Center // Center the text
                    ) {
                        Text(
                            text = "Edit",
                            fontSize = 14.sp, // Slightly smaller text
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }



                Spacer(modifier = Modifier.height(      16.dp))
                // 2.1 Medication Name and Dosage
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medication?.name ?: "",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color= color.textColor,
                                    lineHeight = (36.sp * 1.2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = medication?.dosage ?: "", fontSize = 20.sp, color= color.textColor)
                    }
                    // 2.2 Pill Image
                    Spacer(modifier = Modifier.width(16.dp))
                    Image(
                        painter = rememberAsyncImagePainter("pill_image_url"),
                        contentDescription = "Pill Image",
                        modifier = Modifier.size(64.dp)
                    )
                }

                // 3. Progress
                // Use a library like CircularProgressIndicator to create this
                Row {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth().height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column( // Wrap the progress bar and text in another Column
                            modifier = Modifier.height(140.dp), // Adjust height as needed
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(200.dp),
                                progress = { currentProgress },
                                trackColor = color.progressBackColor,
                                color = color.progressBarColor,
                                strokeWidth = 10.dp,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Progress", fontSize = 18.sp,color= color.textColor)
                            Text(text = "8/30", fontWeight = FontWeight.Bold, fontSize = 58.sp,color= color.textColor)
                        }
                    }
                }

                // 4. Counters
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(start = 20.dp, end = 20.dp)
                    .background(
                        color = color.cardColor,
                        shape = RoundedCornerShape(14.dp)
                    ),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically) {
                    CounterItem(value = "1", label = "table",color = color.onBackgroundColor)
                    CounterItem(value = "3x", label = "times a day",color = color.onBackgroundColor)
                    CounterItem(value = "10", label = "days",color = color.onBackgroundColor)
                }

            }
        }

        // Second Row for Counters and Schedule
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {


                // 5. Schedule

                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(34.dp))
                    Text(text = "Today", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(34.dp))
                    ScheduleItem(time = "9:00", label = "After waking up", enabled = true)
                    ScheduleItem(time = "15:00", label = "With lunch", enabled = false)
                }
            }
        }
    }
}


// Helper composables for CounterItem and ScheduleItem
@Composable
fun CounterItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold,color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 14.sp,color= Color.White)
    }
}

@Composable
fun ScheduleItem(time: String, label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = time, fontWeight = FontWeight.Bold)
        }
        Switch(checked = enabled, onCheckedChange = { /* Handle toggle */ })
    }
}