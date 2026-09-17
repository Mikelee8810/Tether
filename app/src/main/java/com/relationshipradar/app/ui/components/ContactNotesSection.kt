package com.relationshipradar.app.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.Haptics
import com.relationshipradar.app.ui.theme.StatusColors
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AtomicNote(
    val id: String,
    val text: String,
    val timestamp: Long
)

object NotesSerializer {
    fun parse(raw: String?): List<AtomicNote> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return try {
                val array = JSONArray(trimmed)
                val list = mutableListOf<AtomicNote>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AtomicNote(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            text = obj.optString("text", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list.sortedByDescending { it.timestamp }
            } catch (_: Exception) {
                listOf(AtomicNote(UUID.randomUUID().toString(), raw, System.currentTimeMillis()))
            }
        }
        // Legacy plain text: convert to single note
        return listOf(AtomicNote(UUID.randomUUID().toString(), raw, System.currentTimeMillis()))
    }

    fun serialize(notes: List<AtomicNote>): String {
        val array = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("text", note.text)
                put("timestamp", note.timestamp)
            }
            array.put(obj)
        }
        return array.toString()
    }
}

@Composable
fun ContactNotesSection(
    rawNotes: String,
    onNotesUpdated: (String) -> Unit,
    onAwardSparks: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var noteInput by remember { mutableStateOf("") }
    val notesList = remember(rawNotes) { NotesSerializer.parse(rawNotes) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Add Note Input Box (Glass Tactile Card) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(18.dp),
                    elevation = 4.dp,
                    surfaceAlphaTop = 0.85f,
                    surfaceAlphaBottom = 0.55f
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = {
                        Text(
                            "Add a memory, coffee order, kids' info, or idea...",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0A0F172A),
                        unfocusedContainerColor = Color(0x050F172A),
                        focusedBorderColor = StatusColors.Cobalt.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFFCBD5E1).copy(alpha = 0.5f),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF1E293B),
                    ),
                    minLines = 2,
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Notes earn +5 Sparks",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )

                    // 3D Tactile Add Button
                    val canAdd = noteInput.isNotBlank()
                    Box(
                        modifier = Modifier
                            .shadow(if (canAdd) 4.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = Color(0x332563EB))
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (canAdd) Brush.verticalGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                                else Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                            )
                            .clickable(enabled = canAdd) {
                                val newNote = AtomicNote(
                                    id = UUID.randomUUID().toString(),
                                    text = noteInput.trim(),
                                    timestamp = System.currentTimeMillis()
                                )
                                val updated = listOf(newNote) + notesList
                                onNotesUpdated(NotesSerializer.serialize(updated))
                                noteInput = ""
                                Haptics.crunch(context)
                                onAwardSparks?.invoke(5)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Add Note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- List of Notes ---
        if (notesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        surfaceAlphaTop = 0.75f,
                        surfaceAlphaBottom = 0.45f
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "No notes saved yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        "Save little things you learn so you never forget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            notesList.forEach { note ->
                IndividualNoteCard(
                    note = note,
                    onDelete = {
                        val filtered = notesList.filter { it.id != note.id }
                        onNotesUpdated(NotesSerializer.serialize(filtered))
                        Haptics.tick(context)
                    }
                )
            }
        }
    }
}

@Composable
fun IndividualNoteCard(
    note: AtomicNote,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = shape,
                elevation = 3.dp,
                surfaceAlphaTop = 0.90f,
                surfaceAlphaBottom = 0.65f,
                specularAlphaTop = 0.90f,
                specularAlphaBottom = 0.20f
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0F172A),
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp with subtle clock icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = Format.ago(note.timestamp).lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Sleek Tactile Delete Action Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2).copy(alpha = 0.6f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete note",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
