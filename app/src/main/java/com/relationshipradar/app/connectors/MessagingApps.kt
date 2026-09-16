package com.relationshipradar.app.connectors

/** Packages whose notifications we read. Anything else is ignored before parsing. */
object MessagingApps {
    val known: Map<String, String> = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram",
        "org.telegram.messenger" to "Telegram",
        "org.thoughtcrime.securesms" to "Signal",
        "com.discord" to "Discord",
        "com.Slack" to "Slack",
        "com.microsoft.teams" to "Teams",
        "com.google.android.apps.messaging" to "Google Messages",
        "com.snapchat.android" to "Snapchat",
        "com.google.android.gm" to "Gmail",
        "com.microsoft.office.outlook" to "Outlook",
        "com.yahoo.mobile.client.android.mail" to "Yahoo Mail",
        "com.twitter.android" to "X",
        "com.linkedin.android" to "LinkedIn",
    )

    fun label(pkg: String) = known[pkg] ?: pkg.substringAfterLast('.')
}
