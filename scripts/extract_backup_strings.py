"""Replace hardcoded user-facing strings in BackupScreen.kt with stringResource calls.

Each (old, new) pair below was hand-verified against the file's content.
Repeats use surrounding context for uniqueness. Run from project root.
"""
import re
from pathlib import Path

FILE = Path("app/src/main/java/com/suvojeet/notenext/ui/settings/BackupScreen.kt")

# Each entry: (search, replace, expected_count). count=None means must match exactly once.
# Use multi-line search where uniqueness needs more context.
EDITS = [
    # --- Confirm restore dialog (LOCAL) ---
    (
        '            title = { Text("How would you like to restore?") },\n'
        '            text = {\n'
        '                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {\n'
        '                    Text("Select a restoration method for your backup.")',
        '            title = { Text(stringResource(id = R.string.backup_restore_method_title)) },\n'
        '            text = {\n'
        '                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {\n'
        '                    Text(stringResource(id = R.string.backup_restore_method_subtitle_local))',
    ),
    # First merge card
    (
        '                            Text("Merge with Current Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)\n'
        '                            Text("Keep your current notes and add everything from the backup as new entries.", style = MaterialTheme.typography.bodySmall)',
        '                            Text(stringResource(id = R.string.backup_merge_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)\n'
        '                            Text(stringResource(id = R.string.backup_merge_description), style = MaterialTheme.typography.bodySmall)',
    ),
    # First overwrite card
    (
        '                            Text("Full Overwrite (Clean Restore)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)\n'
        '                            Text("ERASE all current data and replace it entirely with the backup content.", style = MaterialTheme.typography.bodySmall)\n'
        '                        }\n'
        '                    }\n'
        '                }\n'
        '            },\n'
        '            confirmButton = {},\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { \n'
        '                    showConfirmDialog = null \n'
        '                    restoreType = null\n'
        '                }) {\n'
        '                    Text(stringResource(id = R.string.cancel))\n'
        '                }',
        '                            Text(stringResource(id = R.string.backup_overwrite_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)\n'
        '                            Text(stringResource(id = R.string.backup_overwrite_description), style = MaterialTheme.typography.bodySmall)\n'
        '                        }\n'
        '                    }\n'
        '                }\n'
        '            },\n'
        '            confirmButton = {},\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { \n'
        '                    showConfirmDialog = null \n'
        '                    restoreType = null\n'
        '                }) {\n'
        '                    Text(stringResource(id = R.string.cancel))\n'
        '                }',
    ),
    # Section headers
    ('SectionHeader("Manual Backup")', 'SectionHeader(stringResource(id = R.string.backup_section_manual))'),
    ('SectionHeader("Backup Settings")', 'SectionHeader(stringResource(id = R.string.backup_section_settings))'),
    ('SectionHeader("Smart & Incremental")', 'SectionHeader(stringResource(id = R.string.backup_section_smart))'),
    # Delete drive backup dialog
    (
        '            title = { Text("Delete Drive Backup") },\n'
        '            text = { Text("Are you sure you want to permanently delete the backup from Google Drive? This action cannot be undone.") },',
        '            title = { Text(stringResource(id = R.string.backup_delete_drive_title)) },\n'
        '            text = { Text(stringResource(id = R.string.backup_delete_drive_message)) },',
    ),
    # The drive-delete button label "Delete" + "Cancel"
    (
        '                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)\n'
        '                ) { Text("Delete") }\n'
        '            },\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }\n'
        '            }\n'
        '        )\n'
        '    }\n'
        '\n'
        '    if (showUnlinkDialog) {',
        '                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)\n'
        '                ) { Text(stringResource(id = R.string.delete)) }\n'
        '            },\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(id = R.string.cancel)) }\n'
        '            }\n'
        '        )\n'
        '    }\n'
        '\n'
        '    if (showUnlinkDialog) {',
    ),
    # Unlink dialog title + body
    (
        '            title = { Text("Unlink Account") },\n'
        '            text = { Text("Unlinking will stop automatic backups to Drive. Local backups will not be affected.") },',
        '            title = { Text(stringResource(id = R.string.backup_unlink_title)) },\n'
        '            text = { Text(stringResource(id = R.string.backup_unlink_message)) },',
    ),
    # Unlink confirm + cancel
    (
        '                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)\n'
        '                ) { Text("Unlink") }\n'
        '            },\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancel") }\n'
        '            }',
        '                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)\n'
        '                ) { Text(stringResource(id = R.string.backup_unlink_button)) }\n'
        '            },\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { showUnlinkDialog = false }) { Text(stringResource(id = R.string.cancel)) }\n'
        '            }',
    ),
    # Delete backup version dialog
    (
        '            title = { Text("Delete Backup Version") },\n'
        '            text = { Text("Are you sure you want to permanently delete this backup version? This cannot be undone.") },',
        '            title = { Text(stringResource(id = R.string.backup_delete_version_title)) },\n'
        '            text = { Text(stringResource(id = R.string.backup_delete_version_message)) },',
    ),
    # Delete-version confirm + cancel
    (
        '                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)\n'
        '                ) { Text("Delete") }\n'
        '            },\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { versionToDelete = null }) { Text("Cancel") }\n'
        '            }',
        '                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)\n'
        '                ) { Text(stringResource(id = R.string.delete)) }\n'
        '            },\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { versionToDelete = null }) { Text(stringResource(id = R.string.cancel)) }\n'
        '            }',
    ),
    # Restore-version dialog (DRIVE)
    (
        '            title = { Text("How would you like to restore?") },\n'
        '            text = {\n'
        '                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {\n'
        '                    Text("Select a restoration method for this version.")',
        '            title = { Text(stringResource(id = R.string.backup_restore_method_title)) },\n'
        '            text = {\n'
        '                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {\n'
        '                    Text(stringResource(id = R.string.backup_restore_method_subtitle_drive))',
    ),
    # Drive merge card
    (
        '                            Text("Merge with Current Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)\n'
        '                            Text("Keep current notes and add everything from the backup as new entries.", style = MaterialTheme.typography.bodySmall)',
        '                            Text(stringResource(id = R.string.backup_merge_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)\n'
        '                            Text(stringResource(id = R.string.backup_merge_description_drive), style = MaterialTheme.typography.bodySmall)',
    ),
    # Drive overwrite card + cancel
    (
        '                            Text("Full Overwrite (Clean Restore)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)\n'
        '                            Text("ERASE all current data and replace it entirely with the backup content.", style = MaterialTheme.typography.bodySmall)\n'
        '                        }\n'
        '                    }\n'
        '                }\n'
        '            },\n'
        '            confirmButton = {},\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { versionToRestore = null }) { Text("Cancel") }\n'
        '            }',
        '                            Text(stringResource(id = R.string.backup_overwrite_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)\n'
        '                            Text(stringResource(id = R.string.backup_overwrite_description), style = MaterialTheme.typography.bodySmall)\n'
        '                        }\n'
        '                    }\n'
        '                }\n'
        '            },\n'
        '            confirmButton = {},\n'
        '            dismissButton = {\n'
        '                TextButton(onClick = { versionToRestore = null }) { Text(stringResource(id = R.string.cancel)) }\n'
        '            }',
    ),
    # Change password dialog
    (
        '            title = "Change Password",\n'
        '            confirmText = "Update Password"',
        '            title = stringResource(id = R.string.backup_change_password_title),\n'
        '            confirmText = stringResource(id = R.string.backup_update_password_button)',
    ),
    # ManualDriveBackupCard - Google Drive header & status
    (
        '                     Text("Google Drive", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\n'
        '                     Spacer(Modifier.height(4.dp))\n'
        '                     if (state.googleAccountEmail != null) {\n'
        '                         Text("Linked: ${state.googleAccountEmail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                     } else {\n'
        '                         Text("Sign in to backup your data to the cloud.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                     }',
        '                     Text(stringResource(id = R.string.backup_google_drive), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\n'
        '                     Spacer(Modifier.height(4.dp))\n'
        '                     if (state.googleAccountEmail != null) {\n'
        '                         Text(stringResource(id = R.string.backup_drive_linked, state.googleAccountEmail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                     } else {\n'
        '                         Text(stringResource(id = R.string.backup_drive_signin_prompt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                     }',
    ),
    # Include attachments
    (
        '                     Text("Include Attachments", style = MaterialTheme.typography.bodyMedium)',
        '                     Text(stringResource(id = R.string.backup_include_attachments), style = MaterialTheme.typography.bodyMedium)',
    ),
    # Drive encryption row
    (
        '                         Text("Encrypt Backup", style = MaterialTheme.typography.bodyMedium)\n'
        '                         if (isEncryptionEnabled) {\n'
        '                             Text("Password set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)\n'
        '                         }\n'
        '                     }\n'
        '                     if (isEncryptionEnabled) {\n'
        '                         TextButton(onClick = onChangePassword) {\n'
        '                             Text("Change Password", fontSize = 12.sp)\n'
        '                         }\n'
        '                     }\n'
        '                     IconButton(onClick = onShowInfo) {\n'
        '                         Icon(\n'
        '                             imageVector = Icons.Default.Info,\n'
        '                             contentDescription = "About Encryption",',
        '                         Text(stringResource(id = R.string.backup_encrypt), style = MaterialTheme.typography.bodyMedium)\n'
        '                         if (isEncryptionEnabled) {\n'
        '                             Text(stringResource(id = R.string.backup_password_set), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)\n'
        '                         }\n'
        '                     }\n'
        '                     if (isEncryptionEnabled) {\n'
        '                         TextButton(onClick = onChangePassword) {\n'
        '                             Text(stringResource(id = R.string.backup_change_password), fontSize = 12.sp)\n'
        '                         }\n'
        '                     }\n'
        '                     IconButton(onClick = onShowInfo) {\n'
        '                         Icon(\n'
        '                             imageVector = Icons.Default.Info,\n'
        '                             contentDescription = stringResource(id = R.string.backup_about_encryption),',
    ),
    # Backup Now / progress
    (
        '                             Text(state.uploadProgress ?: "Backing up...")\n'
        '                         } else {\n'
        '                             Text("Backup Now")\n'
        '                         }',
        '                             Text(state.uploadProgress ?: stringResource(id = R.string.backup_backing_up))\n'
        '                         } else {\n'
        '                             Text(stringResource(id = R.string.backup_now))\n'
        '                         }',
    ),
    # Restore latest
    (
        '                            } else {\n'
        '                                 Text("Restore Latest")\n'
        '                            }',
        '                            } else {\n'
        '                                 Text(stringResource(id = R.string.backup_restore_latest))\n'
        '                            }',
    ),
    # Backup history
    (
        '                     Text("Backup History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)',
        '                     Text(stringResource(id = R.string.backup_history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)',
    ),
    # Unlink Account TextButton
    (
        '                 TextButton(onClick = onUnlink, modifier = Modifier.align(Alignment.End)) {\n'
        '                     Text("Unlink Account", color = MaterialTheme.colorScheme.error)\n'
        '                 }',
        '                 TextButton(onClick = onUnlink, modifier = Modifier.align(Alignment.End)) {\n'
        '                     Text(stringResource(id = R.string.backup_unlink_title), color = MaterialTheme.colorScheme.error)\n'
        '                 }',
    ),
    # Sign In button
    (
        '                     Text("Sign In")',
        '                     Text(stringResource(id = R.string.backup_sign_in))',
    ),
    # BackupVersionItem - Unknown date
    (
        '    } ?: "Unknown Date"',
        '    } ?: stringResource(id = R.string.backup_unknown_date)',
    ),
    # BackupVersionItem - Restore + Delete icon descriptions
    (
        '             IconButton(onClick = onRestore, enabled = !isRestoring && !isDeleting) {\n'
        '                 Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)\n'
        '             }\n'
        '             IconButton(onClick = onDelete, enabled = !isRestoring && !isDeleting) {\n'
        '                  Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)\n'
        '             }',
        '             IconButton(onClick = onRestore, enabled = !isRestoring && !isDeleting) {\n'
        '                 Icon(Icons.Default.Restore, stringResource(id = R.string.backup_action_restore), tint = MaterialTheme.colorScheme.primary)\n'
        '             }\n'
        '             IconButton(onClick = onDelete, enabled = !isRestoring && !isDeleting) {\n'
        '                  Icon(Icons.Default.Delete, stringResource(id = R.string.backup_action_delete), tint = MaterialTheme.colorScheme.error)\n'
        '             }',
    ),
    # ManualLocalBackupCard - Local Storage header
    (
        '                    Text("Local Storage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\n'
        '                    Text("Backup to or restore from device storage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)',
        '                    Text(stringResource(id = R.string.backup_local_storage), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\n'
        '                    Text(stringResource(id = R.string.backup_local_storage_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    ),
    # Local "Backup" label
    (
        '            Text("Backup", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)',
        '            Text(stringResource(id = R.string.backup_label_backup), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)',
    ),
    # Local encryption row
    (
        '                    Text("Encrypt Backup", style = MaterialTheme.typography.bodyMedium)\n'
        '                    if (isEncryptionEnabled) {\n'
        '                        Text("Password set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)\n'
        '                    }\n'
        '                }\n'
        '                if (isEncryptionEnabled) {\n'
        '                    TextButton(onClick = onChangePassword) {\n'
        '                        Text("Change Password", fontSize = 12.sp)\n'
        '                    }\n'
        '                }\n'
        '                IconButton(onClick = onShowInfo) {\n'
        '                    Icon(\n'
        '                        imageVector = Icons.Default.Info,\n'
        '                        contentDescription = "About Encryption",',
        '                    Text(stringResource(id = R.string.backup_encrypt), style = MaterialTheme.typography.bodyMedium)\n'
        '                    if (isEncryptionEnabled) {\n'
        '                        Text(stringResource(id = R.string.backup_password_set), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)\n'
        '                    }\n'
        '                }\n'
        '                if (isEncryptionEnabled) {\n'
        '                    TextButton(onClick = onChangePassword) {\n'
        '                        Text(stringResource(id = R.string.backup_change_password), fontSize = 12.sp)\n'
        '                    }\n'
        '                }\n'
        '                IconButton(onClick = onShowInfo) {\n'
        '                    Icon(\n'
        '                        imageVector = Icons.Default.Info,\n'
        '                        contentDescription = stringResource(id = R.string.backup_about_encryption),',
    ),
    # SD Card backup button
    (
        '                 if (state.isBackingUp && state.backupResult?.contains("SD Card") == true) {\n'
        '                     LoadingIndicator(modifier = Modifier.size(16.dp))\n'
        '                     Spacer(Modifier.width(8.dp))\n'
        '                     Text("Backing up...")\n'
        '                 } else {\n'
        '                     Text(if (state.sdCardFolderUri != null) "Backup to Selected Folder" else "Select Folder & Backup")\n'
        '                 }',
        '                 if (state.isBackingUp && state.backupResult?.contains("SD Card") == true) {\n'
        '                     LoadingIndicator(modifier = Modifier.size(16.dp))\n'
        '                     Spacer(Modifier.width(8.dp))\n'
        '                     Text(stringResource(id = R.string.backup_backing_up))\n'
        '                 } else {\n'
        '                     Text(if (state.sdCardFolderUri != null) stringResource(id = R.string.backup_to_selected_folder) else stringResource(id = R.string.backup_select_folder))\n'
        '                 }',
    ),
    # Local "Restore" label
    (
        '            Text("Restore", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)',
        '            Text(stringResource(id = R.string.backup_label_restore), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)',
    ),
    # Restoring + Restore from File
    (
        '                     Text("Restoring...")\n'
        '                } else {\n'
        '                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))\n'
        '                    Spacer(Modifier.width(8.dp))\n'
        '                    Text("Restore from File")\n'
        '                }',
        '                     Text(stringResource(id = R.string.backup_restoring))\n'
        '                } else {\n'
        '                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))\n'
        '                    Spacer(Modifier.width(8.dp))\n'
        '                    Text(stringResource(id = R.string.backup_restore_from_file))\n'
        '                }',
    ),
    # Scanning + Selective Restore
    (
        '                      Text("Scanning...")\n'
        '                 } else {\n'
        '                     Text("Selective Restore")\n'
        '                 }',
        '                      Text(stringResource(id = R.string.backup_scanning))\n'
        '                 } else {\n'
        '                     Text(stringResource(id = R.string.backup_selective_restore))\n'
        '                 }',
    ),
    # Auto Backup title
    (
        '                Text("Automatic Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))',
        '                Text(stringResource(id = R.string.backup_auto_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))',
    ),
    # Auto encrypt
    (
        '                    Text("Encrypt Auto-Backups", style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text("Protect background backups with a password", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
        '                    Text(stringResource(id = R.string.backup_auto_encrypt), style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text(stringResource(id = R.string.backup_auto_encrypt_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    ),
    # Frequency label + chips - replace whole list block
    (
        '            Text("Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)\n'
        '            Spacer(Modifier.height(8.dp))\n'
        '            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n'
        '                listOf("Daily", "Weekly").forEach { freq ->\n'
        '                    FilterChip(\n'
        '                        selected = state.backupFrequency == freq,\n'
        '                        onClick = { onFrequencyChange(freq) },\n'
        '                        label = { Text(freq) },\n'
        '                        leadingIcon = if (state.backupFrequency == freq) {\n'
        '                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }\n'
        '                        } else null\n'
        '                    )\n'
        '                }\n'
        '            }',
        '            Text(stringResource(id = R.string.backup_frequency), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)\n'
        '            Spacer(Modifier.height(8.dp))\n'
        '            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n'
        '                listOf(\n'
        '                    "Daily" to stringResource(id = R.string.backup_frequency_daily),\n'
        '                    "Weekly" to stringResource(id = R.string.backup_frequency_weekly)\n'
        '                ).forEach { (freq, label) ->\n'
        '                    FilterChip(\n'
        '                        selected = state.backupFrequency == freq,\n'
        '                        onClick = { onFrequencyChange(freq) },\n'
        '                        label = { Text(label) },\n'
        '                        leadingIcon = if (state.backupFrequency == freq) {\n'
        '                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }\n'
        '                        } else null\n'
        '                    )\n'
        '                }\n'
        '            }',
    ),
    # Google Drive section title (lower) + signin required
    (
        '                    Text("Google Drive", style = MaterialTheme.typography.bodyLarge)\n'
        '                    if (state.googleAccountEmail == null && state.isAutoBackupEnabled) {\n'
        '                        Text("Sign in required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)',
        '                    Text(stringResource(id = R.string.backup_google_drive), style = MaterialTheme.typography.bodyLarge)\n'
        '                    if (state.googleAccountEmail == null && state.isAutoBackupEnabled) {\n'
        '                        Text(stringResource(id = R.string.backup_signin_required), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)',
    ),
    # SD Card folder + path fallback
    (
        '                    Text("SD Card / Local Folder", style = MaterialTheme.typography.bodyLarge)\n'
        '                    state.sdCardFolderUri?.let { uri ->\n'
        '                        val path = try { android.net.Uri.parse(uri).path?.substringAfterLast(":") ?: "Selected" } catch(e:Exception){"Selected"}\n'
        '                        Text(path, style = MaterialTheme.typography.labelSmall, maxLines=1, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                    }',
        '                    Text(stringResource(id = R.string.backup_sd_card_folder), style = MaterialTheme.typography.bodyLarge)\n'
        '                    state.sdCardFolderUri?.let { uri ->\n'
        '                        val selectedFallback = stringResource(id = R.string.backup_folder_selected)\n'
        '                        val path = try { android.net.Uri.parse(uri).path?.substringAfterLast(":") ?: selectedFallback } catch(e:Exception){ selectedFallback }\n'
        '                        Text(path, style = MaterialTheme.typography.labelSmall, maxLines=1, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                    }',
    ),
    # Change folder button
    (
        '                TextButton(onClick = onChangeSdLocation) {\n'
        '                    Text("Change Folder")\n'
        '                }',
        '                TextButton(onClick = onChangeSdLocation) {\n'
        '                    Text(stringResource(id = R.string.backup_change_folder))\n'
        '                }',
    ),
    # BackupDashboardCard title
    (
        '                        text = "Backup Health",',
        '                        text = stringResource(id = R.string.backup_health),',
    ),
    # Status chip
    (
        '                            text = if (state.lastBackupTime == 0L) "No Backups Yet" else if (isSuccess) "Everything Secure" else "Backup Failed",',
        '                            text = if (state.lastBackupTime == 0L) stringResource(id = R.string.backup_health_no_backups) else if (isSuccess) stringResource(id = R.string.backup_health_secure) else stringResource(id = R.string.backup_health_failed),',
    ),
    # Last backup "Never"
    (
        '            } else {\n'
        '                "Never"\n'
        '            }',
        '            } else {\n'
        '                stringResource(id = R.string.backup_never)\n'
        '            }',
    ),
    # Last attempt + storage used
    (
        '                    Text("Last Attempt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                    Text(text = lastBackupText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)\n'
        '                }\n'
        '                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)\n'
        '                Column(modifier = Modifier.weight(1f)) {\n'
        '                    Text("Storage Used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
        '                    Text(stringResource(id = R.string.backup_last_attempt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n'
        '                    Text(text = lastBackupText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)\n'
        '                }\n'
        '                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)\n'
        '                Column(modifier = Modifier.weight(1f)) {\n'
        '                    Text(stringResource(id = R.string.backup_storage_used), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    ),
    # UsageStat labels
    (
        '                        label = "Notes",\n'
        '                        color = MaterialTheme.colorScheme.primary',
        '                        label = stringResource(id = R.string.backup_count_notes),\n'
        '                        color = MaterialTheme.colorScheme.primary',
    ),
    (
        '                        label = "Projects",\n'
        '                        color = MaterialTheme.colorScheme.tertiary',
        '                        label = stringResource(id = R.string.backup_count_projects),\n'
        '                        color = MaterialTheme.colorScheme.tertiary',
    ),
    (
        '                        label = "Labels",\n'
        '                        color = MaterialTheme.colorScheme.secondary',
        '                        label = stringResource(id = R.string.backup_count_labels),\n'
        '                        color = MaterialTheme.colorScheme.secondary',
    ),
    (
        '                        label = "Files",\n'
        '                        color = MaterialTheme.colorScheme.error',
        '                        label = stringResource(id = R.string.backup_count_files),\n'
        '                        color = MaterialTheme.colorScheme.error',
    ),
    # Delete backup card button text
    (
        '                 Text("Delete Backup from Drive", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)',
        '                 Text(stringResource(id = R.string.backup_delete_drive_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)',
    ),
    # BackupScanResultDialog title
    (
        '                Text("Backup Contents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)\n'
        '                scanResult.backupTimestamp?.let { \n'
        '                    Text(\n'
        '                        text = "Created: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))}",',
        '                Text(stringResource(id = R.string.backup_contents_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)\n'
        '                scanResult.backupTimestamp?.let { \n'
        '                    Text(\n'
        '                        text = stringResource(id = R.string.backup_contents_created, SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))),',
    ),
    # ScanStatItem labels
    (
        '                    ScanStatItem(count = scanResult.notesCount, label = "Notes", icon = Icons.Default.Description)\n'
        '                    ScanStatItem(count = scanResult.labelsCount, label = "Labels", icon = Icons.Default.Label)\n'
        '                    ScanStatItem(count = scanResult.attachmentsCount, label = "Files", icon = Icons.Default.AttachFile)',
        '                    ScanStatItem(count = scanResult.notesCount, label = stringResource(id = R.string.backup_count_notes), icon = Icons.Default.Description)\n'
        '                    ScanStatItem(count = scanResult.labelsCount, label = stringResource(id = R.string.backup_count_labels), icon = Icons.Default.Label)\n'
        '                    ScanStatItem(count = scanResult.attachmentsCount, label = stringResource(id = R.string.backup_count_files), icon = Icons.Default.AttachFile)',
    ),
    # Select Projects to Restore
    (
        '                Text("Select Projects to Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)',
        '                Text(stringResource(id = R.string.backup_contents_select_projects), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)',
    ),
    # Merge note
    (
        '                Text(\n'
        '                    "Note: Selected projects will be merged with your current data.",\n'
        '                    style = MaterialTheme.typography.labelSmall,',
        '                Text(\n'
        '                    stringResource(id = R.string.backup_contents_merge_note),\n'
        '                    style = MaterialTheme.typography.labelSmall,',
    ),
    # Restore Selected + Cancel
    (
        '                Text("Restore Selected")\n'
        '            }\n'
        '        },\n'
        '        dismissButton = {\n'
        '            TextButton(onClick = onDismiss) {\n'
        '                Text("Cancel")\n'
        '            }\n'
        '        }',
        '                Text(stringResource(id = R.string.backup_restore_selected))\n'
        '            }\n'
        '        },\n'
        '        dismissButton = {\n'
        '            TextButton(onClick = onDismiss) {\n'
        '                Text(stringResource(id = R.string.cancel))\n'
        '            }\n'
        '        }',
    ),
    # Optimization
    (
        '                Text("Optimization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))',
        '                Text(stringResource(id = R.string.backup_optimization), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))',
    ),
    # Incremental
    (
        '                    Text("Incremental Backup", style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text("Only save changes since last backup to save data and storage.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
        '                    Text(stringResource(id = R.string.backup_incremental), style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text(stringResource(id = R.string.backup_incremental_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    ),
    # Smart trigger
    (
        '                    Text("Smart Backup Trigger", style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text("Automatically backup after a certain number of edits.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
        '                    Text(stringResource(id = R.string.backup_smart_trigger), style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text(stringResource(id = R.string.backup_smart_trigger_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    ),
    # Backup after edits + current edits
    (
        '                Text("Backup after ${state.editsThreshold} edits", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)',
        '                Text(stringResource(id = R.string.backup_after_edits, state.editsThreshold), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)',
    ),
    (
        '                Text(\n'
        '                    "Current edits: ${state.currentEditCount}", \n'
        '                    style = MaterialTheme.typography.labelSmall,',
        '                Text(\n'
        '                    stringResource(id = R.string.backup_current_edits, state.currentEditCount),\n'
        '                    style = MaterialTheme.typography.labelSmall,',
    ),
    # Charging only
    (
        '                    Text("Backup while Charging Only", style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text("Conserve battery by only backing up when plugged in.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
        '                    Text(stringResource(id = R.string.backup_charging_only), style = MaterialTheme.typography.bodyLarge)\n'
        '                    Text(stringResource(id = R.string.backup_charging_only_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    ),
]


def main():
    content = FILE.read_text(encoding="utf-8")
    misses = []
    applied = 0
    for old, new in EDITS:
        count = content.count(old)
        if count == 0:
            misses.append(old[:80].replace("\n", " | "))
            continue
        if count > 1:
            misses.append(f"NON-UNIQUE ({count}x): {old[:80].replace(chr(10), ' | ')}")
            continue
        content = content.replace(old, new, 1)
        applied += 1

    print(f"Applied: {applied}/{len(EDITS)} edits")
    if misses:
        print(f"Misses ({len(misses)}):")
        for m in misses:
            print(f"  - {m}")
        print("Aborting write.")
        return 1

    FILE.write_text(content, encoding="utf-8")
    print(f"Wrote {FILE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
