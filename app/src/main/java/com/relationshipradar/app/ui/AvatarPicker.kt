package com.relationshipradar.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relationshipradar.app.ui.theme.StatusColors

/** Bottom sheet: Contact photo · Initials Gemstone Colors · 24 bundled 3D avatars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPicker(
    current: String?,
    hasContactPhoto: Boolean,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Customize Icon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            // Contact Photo & Auto Initials Quick Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasContactPhoto) {
                    val isPhoto = current == "photo"
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x180F172A))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPhoto) StatusColors.Cobalt else Color(0xFFF1F5F9))
                            .border(1.dp, if (isPhoto) StatusColors.Cobalt else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable { onPick("photo") }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Contact photo",
                            fontWeight = FontWeight.Bold,
                            color = if (isPhoto) Color.White else Color(0xFF334155),
                            fontSize = 13.sp
                        )
                    }
                }

                val isAutoInitials = current == null
                Box(
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x180F172A))
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAutoInitials) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isAutoInitials) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .clickable { onPick(null) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Auto Initials Color",
                        fontWeight = FontWeight.Bold,
                        color = if (isAutoInitials) Color.White else Color(0xFF334155),
                        fontSize = 13.sp
                    )
                }
            }

            // Initials Gemstone Color Palette Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Initials Background Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Pick a bespoke gemstone gradient for this contact's initials monogram:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                // 2 Rows of 5 Tactile Color Gems
                val colorEntries = GemstonePalettes.named
                val chunkedColors = colorEntries.chunked(5)

                chunkedColors.forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowColors.forEach { (colorKey, pair) ->
                            val (_, gradientList) = pair
                            val isSelected = current == "color:$colorKey"
                            val gemShape = RoundedCornerShape(14.dp)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .shadow(if (isSelected) 6.dp else 2.dp, gemShape, spotColor = gradientList.first().copy(alpha = 0.5f))
                                    .clip(gemShape)
                                    .background(Brush.verticalGradient(gradientList))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                        shape = gemShape
                                    )
                                    .clickable { onPick("color:$colorKey") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.9f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = gradientList.first(),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3D Bundled Character Avatars Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "3D Character Avatars",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(280.dp),
                ) {
                    items((1..BundledAvatars.COUNT).toList()) { n ->
                        val key = BundledAvatars.key(n)
                        val isSelected = current == key
                        val squircleShape = RoundedCornerShape(14.dp)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .shadow(if (isSelected) 4.dp else 1.dp, squircleShape, spotColor = Color(0x150F172A))
                                .clip(squircleShape)
                                .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) StatusColors.Cobalt else Color(0xFFE2E8F0),
                                    shape = squircleShape
                                )
                                .clickable { onPick(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(BundledAvatars.resId(ctx, n)),
                                contentDescription = null,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
