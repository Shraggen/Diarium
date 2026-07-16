package com.shraggen.diarium

import com.shraggen.diarium.speech.SpeechLanguage

internal data class AppCopy(
    val title: String,
    val language: String,
    val whisperMissing: String,
    val ready: String,
    val selectWhisper: String,
    val changeWhisper: String,
    val command: String,
    val commandPlaceholder: String,
    val process: String,
    val recordVoice: String,
    val stopRecording: String,
    val listening: String,
    val transcribing: String,
    val detectedLanguage: String,
    val result: String,
    val confirmWrite: String,
    val nothingSaved: String,
    val confirm: String,
    val cancel: String,
    val journal: String,
    val emptyJournal: String,
    val hive: String,
    val queenSeen: String,
    val queenNotSeen: String,
    val record: String,
    val yes: String,
    val no: String,
    val unknown: String,
    val spokenHive: String,
    val proposedHive: String,
    val identifierMismatch: String,
    val identifierCannotVerify: String,
    val transcriptQueenSeen: String,
    val proposedQueenSeen: String,
    val queenMismatch: String,
    val queenCannotVerify: String,
)

internal fun copyFor(language: SpeechLanguage): AppCopy =
    when (language) {
        SpeechLanguage.ENGLISH -> ENGLISH_COPY
        SpeechLanguage.GERMAN -> GERMAN_COPY
        SpeechLanguage.SERBIAN -> SERBIAN_COPY
    }

private val ENGLISH_COPY = AppCopy(
    title = "Diarium local inspection journal",
    language = "Language",
    whisperMissing = "Select a multilingual Whisper .bin model for voice input.",
    ready = "Ready",
    selectWhisper = "Select Whisper model",
    changeWhisper = "Change Whisper model",
    command = "Command",
    commandPlaceholder = "I inspected hive 4 and saw the queen.",
    process = "Process locally",
    recordVoice = "Record voice",
    stopRecording = "Stop recording",
    listening = "Listening… Silero will stop after you finish speaking.",
    transcribing = "Transcribing locally with Whisper…",
    detectedLanguage = "Whisper language",
    result = "Result",
    confirmWrite = "Confirm journal write",
    nothingSaved = "Nothing is saved until you confirm.",
    confirm = "Confirm",
    cancel = "Cancel",
    journal = "Inspection journal",
    emptyJournal = "No inspections recorded yet.",
    hive = "Hive",
    queenSeen = "Queen seen",
    queenNotSeen = "Queen not seen",
    record = "Record",
    yes = "Yes",
    no = "No",
    unknown = "Unknown",
    spokenHive = "Hive in transcript",
    proposedHive = "Hive in planned action",
    identifierMismatch =
        "The transcript and planned hive disagree. Confirmation is blocked.",
    identifierCannotVerify =
        "A single explicit hive identifier could not be verified. Confirmation is blocked.",
    transcriptQueenSeen = "Queen seen in transcript",
    proposedQueenSeen = "Queen seen in planned action",
    queenMismatch =
        "The transcript and planned queen observation disagree. Confirmation is blocked.",
    queenCannotVerify =
        "An explicit queen observation could not be verified. Confirmation is blocked.",
)

private val GERMAN_COPY = AppCopy(
    title = "Lokales Diarium-Inspektionsjournal",
    language = "Sprache",
    whisperMissing = "Wähle ein mehrsprachiges Whisper-.bin-Modell für Spracheingaben aus.",
    ready = "Bereit",
    selectWhisper = "Whisper-Modell auswählen",
    changeWhisper = "Whisper-Modell wechseln",
    command = "Befehl",
    commandPlaceholder = "Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.",
    process = "Lokal verarbeiten",
    recordVoice = "Sprache aufnehmen",
    stopRecording = "Aufnahme beenden",
    listening = "Aufnahme läuft… Silero stoppt, sobald du fertig gesprochen hast.",
    transcribing = "Whisper transkribiert lokal…",
    detectedLanguage = "Whisper-Sprache",
    result = "Ergebnis",
    confirmWrite = "Journaleintrag bestätigen",
    nothingSaved = "Bis zur Bestätigung wird nichts gespeichert.",
    confirm = "Bestätigen",
    cancel = "Abbrechen",
    journal = "Inspektionsjournal",
    emptyJournal = "Noch keine Inspektionen erfasst.",
    hive = "Bienenstock",
    queenSeen = "Königin gesehen",
    queenNotSeen = "Königin nicht gesehen",
    record = "Eintrag",
    yes = "Ja",
    no = "Nein",
    unknown = "Unbekannt",
    spokenHive = "Bienenstock im Transkript",
    proposedHive = "Bienenstock in der geplanten Aktion",
    identifierMismatch =
        "Transkript und geplanter Bienenstock stimmen nicht überein. " +
            "Die Bestätigung ist gesperrt.",
    identifierCannotVerify =
        "Es konnte keine einzelne eindeutige Bienenstocknummer geprüft werden. " +
            "Die Bestätigung ist gesperrt.",
    transcriptQueenSeen = "Königin laut Transkript gesehen",
    proposedQueenSeen = "Königin-Sichtung in der geplanten Aktion",
    queenMismatch =
        "Transkript und geplante Königin-Sichtung stimmen nicht überein. " +
            "Die Bestätigung ist gesperrt.",
    queenCannotVerify =
        "Eine eindeutige Aussage zur Königin-Sichtung konnte nicht geprüft werden. " +
            "Die Bestätigung ist gesperrt.",
)

private val SERBIAN_COPY = AppCopy(
    title = "Локални дневник прегледа",
    language = "Језик",
    whisperMissing = "Изаберите вишејезични Whisper .bin модел за гласовни унос.",
    ready = "Спремно",
    selectWhisper = "Изабери Whisper модел",
    changeWhisper = "Промени Whisper модел",
    command = "Наредба",
    commandPlaceholder = "Прегледао сам кошницу 4 и видео матицу.",
    process = "Обради локално",
    recordVoice = "Сними глас",
    stopRecording = "Заустави снимање",
    listening = "Слушам… Silero ће стати када завршите.",
    transcribing = "Whisper локално транскрибује…",
    detectedLanguage = "Whisper језик",
    result = "Резултат",
    confirmWrite = "Потврди упис у дневник",
    nothingSaved = "Ништа се не чува док не потврдите.",
    confirm = "Потврди",
    cancel = "Откажи",
    journal = "Дневник прегледа",
    emptyJournal = "Још нема сачуваних прегледа.",
    hive = "Кошница",
    queenSeen = "Матица виђена",
    queenNotSeen = "Матица није виђена",
    record = "Запис",
    yes = "Да",
    no = "Не",
    unknown = "Непознато",
    spokenHive = "Кошница у транскрипту",
    proposedHive = "Кошница у планираној радњи",
    identifierMismatch =
        "Транскрипт и планирана кошница се не подударају. Потврда је блокирана.",
    identifierCannotVerify =
        "Није могуће проверити једну јасно наведену кошницу. Потврда је блокирана.",
    transcriptQueenSeen = "Матица виђена у транскрипту",
    proposedQueenSeen = "Матица виђена у планираној радњи",
    queenMismatch =
        "Транскрипт и планирани податак о матици се не подударају. " +
            "Потврда је блокирана.",
    queenCannotVerify =
        "Није могуће проверити јасну тврдњу о виђеној матици. Потврда је блокирана.",
)
