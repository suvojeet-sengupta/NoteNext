"""Generate NoteNext Improvement Guide PDF (Hinglish)."""

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageBreak, PageTemplate, Paragraph,
    Spacer, Table, TableStyle, KeepTogether,
)

OUTPUT = r"F:\NoteNext\NoteNext_Improvement_Guide_Hinglish.pdf"

# ---------- Colors (NoteNext-ish palette) ----------
ACCENT = colors.HexColor("#1F6FEB")          # primary blue
ACCENT_DARK = colors.HexColor("#0B3D91")
ACCENT_SOFT = colors.HexColor("#E8F0FE")
GREEN = colors.HexColor("#16A34A")
ORANGE = colors.HexColor("#EA8E0B")
RED = colors.HexColor("#DC2626")
GREY_DARK = colors.HexColor("#1F2937")
GREY_MID = colors.HexColor("#4B5563")
GREY_LIGHT = colors.HexColor("#E5E7EB")
BG_CODE = colors.HexColor("#F3F4F6")


# ---------- Styles ----------
styles = getSampleStyleSheet()

TITLE = ParagraphStyle(
    "Title", parent=styles["Title"], fontName="Helvetica-Bold",
    fontSize=28, leading=34, textColor=ACCENT_DARK, spaceAfter=8,
    alignment=TA_LEFT,
)
SUBTITLE = ParagraphStyle(
    "Subtitle", parent=styles["Normal"], fontName="Helvetica",
    fontSize=13, leading=18, textColor=GREY_MID, spaceAfter=14,
)
H1 = ParagraphStyle(
    "H1", parent=styles["Heading1"], fontName="Helvetica-Bold",
    fontSize=20, leading=26, textColor=ACCENT_DARK, spaceBefore=18,
    spaceAfter=10, keepWithNext=True,
)
H2 = ParagraphStyle(
    "H2", parent=styles["Heading2"], fontName="Helvetica-Bold",
    fontSize=14, leading=18, textColor=ACCENT, spaceBefore=12,
    spaceAfter=6, keepWithNext=True,
)
H3 = ParagraphStyle(
    "H3", parent=styles["Heading3"], fontName="Helvetica-Bold",
    fontSize=11.5, leading=15, textColor=GREY_DARK, spaceBefore=8,
    spaceAfter=4, keepWithNext=True,
)
BODY = ParagraphStyle(
    "Body", parent=styles["BodyText"], fontName="Helvetica",
    fontSize=10.5, leading=15.5, textColor=GREY_DARK, spaceAfter=6,
    alignment=TA_LEFT,
)
BODY_TIGHT = ParagraphStyle(
    "BodyTight", parent=BODY, spaceAfter=2,
)
BULLET = ParagraphStyle(
    "Bullet", parent=BODY, leftIndent=14, bulletIndent=2,
    spaceAfter=3,
)
QUOTE = ParagraphStyle(
    "Quote", parent=BODY, leftIndent=12, rightIndent=12,
    fontName="Helvetica-Oblique", textColor=GREY_MID,
    borderPadding=(6, 8, 6, 8), backColor=ACCENT_SOFT,
    spaceBefore=6, spaceAfter=10,
)
CODE = ParagraphStyle(
    "Code", parent=BODY, fontName="Courier", fontSize=9.5,
    leading=13, textColor=GREY_DARK, backColor=BG_CODE,
    leftIndent=8, rightIndent=8, borderPadding=(4, 6, 4, 6),
    spaceAfter=8,
)
META = ParagraphStyle(
    "Meta", parent=BODY, fontSize=9.5, textColor=GREY_MID,
    spaceAfter=4,
)
TAG_S = ParagraphStyle("TagS", parent=BODY, fontName="Helvetica-Bold",
                       fontSize=9, textColor=GREEN, spaceAfter=0)
TAG_M = ParagraphStyle("TagM", parent=BODY, fontName="Helvetica-Bold",
                       fontSize=9, textColor=ORANGE, spaceAfter=0)
TAG_L = ParagraphStyle("TagL", parent=BODY, fontName="Helvetica-Bold",
                       fontSize=9, textColor=RED, spaceAfter=0)


# ---------- Helpers ----------
def code(text):
    """Render an inline-ish file path / code line as a styled block."""
    return Paragraph(text, CODE)


def feature_block(title, kya, kyun, kahan, effort):
    """Render a feature improvement card."""
    effort_color = {"S": GREEN, "M": ORANGE, "L": RED}[effort]
    effort_label = {"S": "Small (1-2 din)", "M": "Medium (3-7 din)",
                    "L": "Large (1-2 hafte)"}[effort]

    parts = []
    parts.append(Paragraph(f"<b>{title}</b>", H3))
    parts.append(Paragraph(
        f'<font color="{effort_color.hexval()}"><b>Effort:</b> '
        f'{effort_label}</font>', META))
    parts.append(Paragraph(f"<b>Kya:</b> {kya}", BODY_TIGHT))
    parts.append(Paragraph(f"<b>Kyun:</b> {kyun}", BODY_TIGHT))
    parts.append(Paragraph(f"<b>Kahan:</b>", BODY_TIGHT))
    for path in kahan:
        parts.append(code(path))
    parts.append(Spacer(1, 6))
    return KeepTogether(parts)


def bullet(text):
    return Paragraph(f"&bull;&nbsp;&nbsp;{text}", BULLET)


def hr():
    """Horizontal rule via tiny table."""
    t = Table([[""]], colWidths=[16 * cm], rowHeights=[0.5])
    t.setStyle(TableStyle([("LINEBELOW", (0, 0), (-1, -1), 0.5, GREY_LIGHT)]))
    return t


# ---------- Page decoration ----------
def on_page(canvas, doc):
    canvas.saveState()
    # Header bar (skip on cover)
    if doc.page > 1:
        canvas.setFillColor(ACCENT)
        canvas.rect(0, A4[1] - 0.6 * cm, A4[0], 0.6 * cm, stroke=0, fill=1)
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica-Bold", 9)
        canvas.drawString(2 * cm, A4[1] - 0.42 * cm,
                          "NoteNext Improvement Guide  -  Hinglish")
        canvas.setFont("Helvetica", 9)
        canvas.drawRightString(A4[0] - 2 * cm, A4[1] - 0.42 * cm,
                               f"Page {doc.page}")
    # Footer
    canvas.setFillColor(GREY_MID)
    canvas.setFont("Helvetica", 8)
    canvas.drawString(2 * cm, 1 * cm,
                      "For: Suvojeet  -  Project: NoteNext  -  2026")
    canvas.drawRightString(A4[0] - 2 * cm, 1 * cm,
                           "Generated by Claude Opus 4.7")
    canvas.restoreState()


def on_cover(canvas, doc):
    canvas.saveState()
    # Big accent block on left
    canvas.setFillColor(ACCENT_DARK)
    canvas.rect(0, 0, 1.2 * cm, A4[1], stroke=0, fill=1)
    canvas.setFillColor(ACCENT)
    canvas.rect(1.2 * cm, 0, 0.3 * cm, A4[1], stroke=0, fill=1)
    canvas.restoreState()


# ---------- Build doc ----------
def build():
    doc = BaseDocTemplate(
        OUTPUT, pagesize=A4,
        leftMargin=2 * cm, rightMargin=2 * cm,
        topMargin=1.6 * cm, bottomMargin=1.6 * cm,
        title="NoteNext Improvement Guide (Hinglish)",
        author="Suvojeet (with Claude)",
    )
    cover_frame = Frame(2.2 * cm, 1.6 * cm, A4[0] - 4.2 * cm,
                        A4[1] - 3 * cm, id="cover")
    main_frame = Frame(2 * cm, 1.6 * cm, A4[0] - 4 * cm,
                       A4[1] - 3.2 * cm, id="main")

    doc.addPageTemplates([
        PageTemplate(id="Cover", frames=[cover_frame], onPage=on_cover),
        PageTemplate(id="Main", frames=[main_frame], onPage=on_page),
    ])

    story = []

    # ========== COVER ==========
    story.append(Spacer(1, 4 * cm))
    story.append(Paragraph("NoteNext", TITLE))
    story.append(Paragraph(
        "Improvement Guide", ParagraphStyle(
            "Cover2", parent=TITLE, fontSize=22, textColor=ACCENT,
            spaceAfter=4)))
    story.append(Paragraph(
        "Todo feature ka deep-dive + app-wide roadmap",
        ParagraphStyle("Cover3", parent=SUBTITLE, fontSize=14,
                       textColor=GREY_DARK)))
    story.append(Spacer(1, 1 * cm))
    story.append(Paragraph(
        "Hinglish mein. Casual. Solo dev ke liye.",
        ParagraphStyle("Cover4", parent=SUBTITLE, fontSize=12,
                       textColor=GREY_MID, fontName="Helvetica-Oblique")))

    story.append(Spacer(1, 3 * cm))
    info_table = Table([
        ["Audience", "Suvojeet (solo dev / maintainer)"],
        ["Tone", "Casual Hinglish + English tech terms"],
        ["Scope", "Todo feature + app-wide improvements + tech debt"],
        ["Format", "Roadmap with effort tags (S / M / L)"],
        ["Date", "2026-05-06"],
    ], colWidths=[3.5 * cm, 11 * cm])
    info_table.setStyle(TableStyle([
        ("FONT", (0, 0), (-1, -1), "Helvetica", 10),
        ("FONT", (0, 0), (0, -1), "Helvetica-Bold", 10),
        ("TEXTCOLOR", (0, 0), (0, -1), ACCENT_DARK),
        ("TEXTCOLOR", (1, 0), (1, -1), GREY_DARK),
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [colors.white, ACCENT_SOFT]),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
    ]))
    story.append(info_table)

    story.append(PageBreak())
    story.append(Paragraph("", BODY))  # switch template
    # Switch to main template
    story.append(Spacer(1, 0.1))

    # ========== INTRO ==========
    story.append(Paragraph("0. Intro &mdash; Yeh Guide Kya Hai", H1))
    story.append(Paragraph(
        "Bhai, NoteNext ek solid base pe khada hai &mdash; Kotlin, Compose, "
        "Material 3 Expressive, Room, Hilt, Clean Architecture, AI integration. "
        "Todo feature bhi already chal raha hai (priority, subtasks, due dates, "
        "reminders, projects, AI generation). Lekin agar isse seriously "
        "&ldquo;Google Tasks killer&rdquo; ya &ldquo;Todoist competitor&rdquo; banana hai, "
        "toh kuch gaps hain jo bharne padenge.", BODY))
    story.append(Paragraph(
        "Yeh guide do hisson mein hai:", BODY))
    story.append(bullet("<b>Todo feature ka deep dive</b> &mdash; kya missing hai aur "
                        "har ek improvement ka kya/kyun/kahan/effort"))
    story.append(bullet("<b>App-wide improvements</b> &mdash; baaki features, polish, "
                        "tech debt, testing, priority order"))
    story.append(Paragraph(
        "<b>Effort tags ka matlab:</b>", BODY))
    eff_t = Table([
        [Paragraph("<b>S</b>", TAG_S), "Small &mdash; 1-2 din. Quick win, "
         "data model nahi badalta."],
        [Paragraph("<b>M</b>", TAG_M), "Medium &mdash; 3-7 din. Schema/UI dono "
         "touch hote hain."],
        [Paragraph("<b>L</b>", TAG_L), "Large &mdash; 1-2 hafte. Naya subsystem "
         "ya migration involved hai."],
    ], colWidths=[1.2 * cm, 13 * cm])
    eff_t.setStyle(TableStyle([
        ("FONT", (0, 0), (-1, -1), "Helvetica", 10),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("BOX", (0, 0), (-1, -1), 0.5, GREY_LIGHT),
        ("INNERGRID", (0, 0), (-1, -1), 0.25, GREY_LIGHT),
        ("BACKGROUND", (0, 0), (0, -1), ACCENT_SOFT),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
    ]))
    story.append(eff_t)
    story.append(Spacer(1, 8))
    story.append(Paragraph(
        "<i>Note:</i> Saare file paths repo root <font face='Courier'>F:\\NoteNext\\</font> "
        "ke relative hain.", META))

    # ========== CURRENT STATE ==========
    story.append(PageBreak())
    story.append(Paragraph("1. Todo Feature: Abhi Kya Hai", H1))
    story.append(Paragraph(
        "Pehle dekh lete hain ki todo feature mein already kya implemented hai, "
        "taaki improvements decide karte time clear rahe ki kya add karna hai aur "
        "kya extend karna hai.", BODY))

    story.append(Paragraph("Data Model", H2))
    story.append(Paragraph(
        "<b>TodoItem</b> entity (Room) mein yeh fields hain:", BODY))
    story.append(code(
        "id, title, description, isCompleted, priority (0=Low/1=Med/2=High),<br/>"
        "dueDate (Long?), reminderTime (Long?), position, projectId,<br/>"
        "createdAt, completedAt"))
    story.append(Paragraph(
        "<b>TodoSubtask</b> entity:", BODY))
    story.append(code(
        "id (UUID), todoId (FK CASCADE), text, isChecked, position"))

    story.append(Paragraph("UI &amp; ViewModel", H2))
    story.append(bullet("<b>TodoScreen</b> &mdash; main list, filter (All/Active/"
                        "Completed), productivity dashboard, empty states"))
    story.append(bullet("<b>TodoItemCard</b> &mdash; swipe-to-complete (right) aur "
                        "swipe-to-delete (left), priority color badge, subtask counter"))
    story.append(bullet("<b>AddEditTodoDialog</b> &mdash; title, description, priority, "
                        "due-date, reminder, project, inline subtask editor"))
    story.append(bullet("<b>AiTodoDialog</b> &mdash; Groq API se bulk todo generation"))
    story.append(bullet("<b>TodoViewModel</b> &mdash; pagination (20/page), realtime "
                        "counters, drag-to-reorder, AlarmScheduler integration"))

    story.append(Paragraph("Already Working", H2))
    story.append(bullet("CRUD, subtasks (single level), 3-tier priority, due dates, "
                        "simple reminders"))
    story.append(bullet("Filter (All/Active/Completed), project assignment, swipe actions"))
    story.append(bullet("Drag-to-reorder, AI generation, share, productivity metrics"))

    # ========== TODO IMPROVEMENTS ==========
    story.append(PageBreak())
    story.append(Paragraph("2. Todo Feature: Improvements", H1))
    story.append(Paragraph(
        "Yeh saari woh cheezein hain jo abhi missing hain ya kamzor hain. "
        "Priority order ke liye section 7 dekho &mdash; yahan har item ko "
        "feature-by-feature explain kiya hai.", BODY))

    story.append(feature_block(
        "2.1 Recurring Tasks (daily / weekly / custom)",
        "Task ko ek baar create karke uski frequency set kar do &mdash; \"Roz subah 8 "
        "baje\", \"Har Monday\", \"Har month ki 1st\". Complete hone par next "
        "instance auto-generate ho.",
        "Aaj ke time mein yeh table-stakes hai. Daily standup, gym, medicine, "
        "pay-bills &mdash; har productivity user yeh maangta hai. Iske bina users "
        "Google Tasks ya Todoist pe shift kar denge.",
        [
            "data/.../TodoItem.kt  -> add: recurrenceRule (RFC 5545 RRULE String?), "
            "parentRecurringId (Long?)",
            "data/.../TodoDao.kt  -> migration + queries for recurring instances",
            "feature/todo/.../TodoViewModel.kt  -> on complete, spawn next instance",
            "feature/todo/.../AddEditTodoDialog.kt  -> RecurrencePicker composable",
            "Use existing AlarmScheduler ko extend karke recurring alarm support karein",
        ],
        "L",
    ))

    story.append(feature_block(
        "2.2 Multiple Tags / Labels",
        "Abhi ek todo ka sirf single projectId hai. Multiple labels/tags allow "
        "karo &mdash; ek task &ldquo;Work&rdquo; + &ldquo;Urgent&rdquo; + &ldquo;Client-X&rdquo; sab ho sakta hai.",
        "Real life mein tasks multi-dimensional hote hain. Single project "
        "feel kar deta hai forced. Notes mein already labels system hai &mdash; "
        "wahi pattern reuse karein, do parallel systems mat banao.",
        [
            "data/.../TodoLabelCrossRef.kt  -> new join table (todoId, labelId)",
            "data/.../TodoItem.kt  -> @Relation for labels (Room many-to-many)",
            "feature/todo/.../TodoItemCard.kt  -> render label chips below title",
            "feature/todo/.../AddEditTodoDialog.kt  -> reuse existing LabelPicker from notes",
        ],
        "M",
    ))

    story.append(feature_block(
        "2.3 Time-Blocking + Duration Estimates",
        "Sirf due date nahi &mdash; <i>start time</i>, <i>estimated duration</i> bhi. "
        "&ldquo;3pm - 4pm, ~60 min&rdquo;. Calendar-style time slots.",
        "Productivity power users (especially solopreneurs/students) calendar "
        "ke saath time-block karte hain. Yeh feature analytics ke liye bhi "
        "useful hai &mdash; planned vs actual time tracking.",
        [
            "data/.../TodoItem.kt  -> add: startTime (Long?), estimatedMinutes (Int?)",
            "feature/todo/.../AddEditTodoDialog.kt  -> TimeRangePicker section",
            "feature/todo/.../TodoItemCard.kt  -> small clock icon + duration chip",
        ],
        "M",
    ))

    story.append(feature_block(
        "2.4 Smart / Repeating Reminders + Snooze",
        "Abhi ek single reminderTime hai. Add karo: multiple reminders per todo "
        "(e.g., 1 day before + 1 hour before), snooze button on notification "
        "(5min / 1hr / tomorrow), aur smart reminders (\"location-based\" optional).",
        "Single reminder bhool jaate hain users. Snooze toh basic expectation "
        "hai &mdash; abhi notification dismiss karo toh permanently gone. Yeh ek "
        "frustration point hai.",
        [
            "data/.../TodoReminder.kt  -> new entity (todoId FK, triggerTime, type)",
            "core/reminder/.../ReminderBroadcastReceiver.kt  -> add Snooze action",
            "feature/todo/.../AddEditTodoDialog.kt  -> ReminderListEditor",
            "Notification builder mein 'Snooze 1hr' aur 'Done' actions add karein",
        ],
        "M",
    ))

    story.append(feature_block(
        "2.5 Todo Home-Screen Widget (Glance)",
        "App mein NoteGlanceWidget already hai notes ke liye &mdash; same pattern "
        "todos ke liye banao. Today's tasks dikhana, tap-to-complete, "
        "tap-to-add-new.",
        "Widget = retention. Home screen pe pinned widget daily-active-user "
        "ratio 2-3x kar deta hai. Plus competitive parity (Google Tasks, "
        "Microsoft To Do dono ke widgets hain).",
        [
            "feature/widget/.../TodoGlanceWidget.kt  -> new (mirror NoteGlanceWidget)",
            "feature/widget/.../TodoWidgetReceiver.kt  -> AppWidgetProvider",
            "AndroidManifest.xml  -> register widget provider + widget_info XML",
            "res/xml/todo_widget_info.xml  -> dimensions, preview, resize mode",
        ],
        "M",
    ))

    story.append(feature_block(
        "2.6 Calendar Integration (System Calendar Sync)",
        "Due date wale todos ko system calendar (CalendarContract) mein sync "
        "karo &mdash; user ke Google Calendar / Samsung Calendar mein dikhe. "
        "Two-way sync optional, one-way (NoteNext -> Calendar) se start karo.",
        "Power users ka calendar = single source of truth. Agar todos calendar "
        "mein nahi dikhte toh yaad nahi rahta. One-way write-only sync se "
        "permission scope chhota rahega.",
        [
            "core/calendar/.../CalendarSyncRepository.kt  -> new module",
            "AndroidManifest.xml  -> WRITE_CALENDAR permission",
            "feature/settings/.../SettingsScreen.kt  -> 'Sync to Calendar' toggle",
            "TodoViewModel  -> on save/update/delete, mirror to CalendarContract",
        ],
        "L",
    ))

    story.append(feature_block(
        "2.7 Habit Tracking + Streaks",
        "Recurring tasks ke upar build &mdash; streak counter (\"7 days in a row!\"), "
        "completion heatmap (GitHub-style), monthly stats.",
        "Habit gamification se daily engagement badhta hai. Streak tootne ka "
        "dar = retention. Productivity dashboard mein already metrics hain &mdash; "
        "yeh natural extension hai.",
        [
            "data/.../HabitStats.kt  -> derived view / @Query in TodoDao",
            "feature/todo/.../HabitHeatmap.kt  -> Compose heatmap composable",
            "feature/todo/.../TodoScreen.kt  -> add 'Habits' tab next to filters",
            "DataStore  -> longestStreak, currentStreak persist karo",
        ],
        "M",
    ))

    story.append(feature_block(
        "2.8 Task Dependencies (Blocked By)",
        "Ek todo ko doosre todos par dependent mark karo &mdash; \"deploy\" task "
        "tab tak available nahi hoga jab tak \"tests pass\" complete na ho.",
        "Project management style workflows ke liye useful. Niche audience hai "
        "(developers, PM-types) but high-value. Optional &mdash; don't bloat the "
        "default UX.",
        [
            "data/.../TodoDependency.kt  -> join table (todoId, dependsOnTodoId)",
            "feature/todo/.../TodoItemCard.kt  -> 'blocked' badge if dependencies pending",
            "feature/todo/.../AddEditTodoDialog.kt  -> 'Add dependency' picker",
        ],
        "M",
    ))

    story.append(feature_block(
        "2.9 Pomodoro / Time Tracking",
        "In-app timer &mdash; task select karo, 25min timer chalu, complete pe "
        "actualMinutes log ho jaye. Breaks bhi auto-suggest.",
        "Focus apps (Forest, Focus Keeper) ka segment NoteNext mein integrate "
        "kar sakte ho. Plus, planned vs actual data analytics ke liye gold hai.",
        [
            "data/.../TodoItem.kt  -> add: actualMinutes (Int?)",
            "feature/todo/.../PomodoroSheet.kt  -> bottom sheet with timer",
            "core/foreground/.../PomodoroService.kt  -> ForegroundService for "
            "timer to survive app close",
            "Notification ongoing for active session",
        ],
        "L",
    ))

    story.append(feature_block(
        "2.10 Better Search &amp; Sort within Todos",
        "Abhi sirf state filter hai (All/Active/Completed). Add karo: full-text "
        "search (title + description + subtasks), sort by due-date / priority / "
        "created / alphabetical, multi-criteria filter (label + priority + due range).",
        "Jaise-jaise todos badhte hain, list unmanageable ho jaati hai. Search "
        "missing = bohot bada UX gap. Notes mein already global search hai &mdash; "
        "todos ke liye in-screen search bar add karo.",
        [
            "feature/todo/.../TodoScreen.kt  -> SearchBar composable on top",
            "feature/todo/.../TodoViewModel.kt  -> queryFlow + combine with "
            "todosFlow using debounce",
            "data/.../TodoDao.kt  -> @Query with LIKE on title/description "
            "(use FTS4 if perf needed)",
        ],
        "S",
    ))

    story.append(feature_block(
        "2.11 Bulk Actions (Multi-select)",
        "Long-press se selection mode &mdash; multiple todos select karke ek saath "
        "complete / delete / move-to-project / change-priority.",
        "10+ todos clean karne ke liye one-by-one swipe karna painful hai. "
        "Selection mode notes mein already hai &mdash; consistency ke liye todos mein "
        "bhi laao.",
        [
            "feature/todo/.../TodoViewModel.kt  -> selectedIds: Set<Long> StateFlow",
            "feature/todo/.../TodoScreen.kt  -> contextual action bar when "
            "selection non-empty",
            "feature/todo/.../TodoItemCard.kt  -> selected state visual (border + bg)",
        ],
        "S",
    ))

    story.append(feature_block(
        "2.12 Convert Checklist Note <-> Todo",
        "Notes mein checklist hai, todos alag system hai. Yeh do parallel "
        "universes hain. Right-click / long-press se &ldquo;Convert to Todo&rdquo; option do "
        "checklist notes pe, aur vice-versa.",
        "Users ko forced choice mil rahi hai &mdash; quick checklist note banaye ya "
        "proper todo? Conversion option dene se decision deferred ho jaata hai. "
        "Plus, data model unify karne ka first step hai.",
        [
            "core/converters/.../ChecklistTodoConverter.kt  -> mapping logic",
            "feature/notes/.../NoteContextMenu.kt  -> 'Convert to Todo' action",
            "feature/todo/.../TodoContextMenu.kt  -> 'Convert to Note' action",
            "Subtasks <-> ChecklistItems mapping carefully handle karo",
        ],
        "M",
    ))

    # ========== APP-WIDE ==========
    story.append(PageBreak())
    story.append(Paragraph("3. App-Wide Feature Improvements", H1))
    story.append(Paragraph(
        "Todo se aage badh ke baaki app mein bhi yeh kaam karne layak hain.", BODY))

    story.append(feature_block(
        "3.1 Unified Search (notes + todos + checklists)",
        "Abhi global search sirf notes mein hai. Ek unified search screen "
        "banao &mdash; query daalo, results mein notes, todos, aur checklists sab "
        "tabs / sections mein dikhayein.",
        "Users yaad nahi rakhte ki kaunsi cheez kahan likhi thi. Single "
        "search box = killer UX.",
        [
            "feature/search/.../UnifiedSearchScreen.kt  -> new",
            "feature/search/.../UnifiedSearchViewModel.kt  -> combine flows from "
            "NoteRepository + TodoRepository",
            "MainActivity navigation graph  -> add /search route",
            "FTS4 virtual table consider karo agar perf slow lage",
        ],
        "M",
    ))

    story.append(feature_block(
        "3.2 Encryption for Todos",
        "Notes mein already encryption flag hai (per-note). Same support todos "
        "ke liye laao &mdash; sensitive todos (medical, financial, passwords-related) "
        "encrypted store ho.",
        "Privacy-first positioning ke liye consistent hona chahiye. \"Notes "
        "encrypted, todos plain text\" ek inconsistent hai.",
        [
            "data/.../TodoItem.kt  -> add: isEncrypted, encryptedPayload (BLOB?)",
            "core/crypto/.../EncryptedFieldCodec.kt  -> reuse from notes",
            "TodoRepository  -> on read/write, decrypt/encrypt if flag set",
        ],
        "M",
    ))

    story.append(feature_block(
        "3.3 Biometric Lock for Sensitive Items",
        "App-level biometric lock alpha mein hai. Per-item biometric add karo &mdash; "
        "specific notes / todos ke liye fingerprint maangega open karne se pehle.",
        "App lock all-or-nothing hai. Sensitive items minority hain &mdash; baaki ko "
        "frictionless rakho.",
        [
            "core/security/.../BiometricGate.kt  -> reusable composable wrapper",
            "feature/notes/.../NoteDetailScreen.kt  -> wrap content if isLocked",
            "feature/todo/.../TodoDetail.kt  -> same wrapper",
            "Setting per-item lock toggle in edit dialog",
        ],
        "M",
    ))

    story.append(feature_block(
        "3.4 Haptic Feedback",
        "Swipe-to-complete, long-press, drag-reorder &mdash; in sab pe HapticFeedback "
        "add karo. Abhi totally silent hai.",
        "Material 3 Expressive ka haptic mein already infrastructure hai. "
        "Premium feel aata hai. Cost almost zero.",
        [
            "core/ui/.../HapticUtils.kt  -> wrapper around HapticFeedback",
            "feature/todo/.../TodoItemCard.kt  -> on swipe threshold cross, "
            "performHapticFeedback(LongPress)",
            "Drag handle release pe ConfirmHapticFeedback",
        ],
        "S",
    ))

    story.append(feature_block(
        "3.5 More Languages + Pluralization + RTL",
        "Abhi sirf EN + HI hai. Add: Tamil, Bengali, Marathi, Spanish, Arabic "
        "(RTL test). Plurals.xml use karo &mdash; abhi \"1 todos\" jaise bugs honge.",
        "India market ke liye regional languages must-have. Arabic se RTL "
        "support test ho jayega &mdash; Compose layouts mostly auto-handle karte "
        "hain but corner cases milte hain.",
        [
            "app/src/main/res/values-{ta,bn,mr,es,ar}/strings.xml",
            "Saare 'X items' wale strings ko plurals.xml mein move karo",
            "feature/* mein hardcoded strings audit karo (lint can flag)",
        ],
        "M",
    ))

    story.append(feature_block(
        "3.6 Auto-Backup Scheduling Improvements",
        "Drive backup manual ya app-launch pe trigger hota hai. WorkManager use "
        "karke daily/weekly schedule expose karo with WiFi-only / charging-only "
        "constraints.",
        "Manual backup users bhool jaate hain. Background reliable backup = "
        "data safety = trust. WorkManager ka constraint API perfect fit hai.",
        [
            "feature/backup/.../BackupWorker.kt  -> CoroutineWorker",
            "feature/backup/.../BackupScheduler.kt  -> PeriodicWorkRequest builder",
            "feature/settings/.../BackupSettingsScreen.kt  -> frequency picker, "
            "constraint toggles",
        ],
        "M",
    ))

    story.append(feature_block(
        "3.7 Markdown Export for Notes",
        "Single note ya bulk notes ko Markdown (.md) files mein export karo &mdash; "
        "ZIP bundle ya share intent. Obsidian/Notion users ke liye huge.",
        "Lock-in fear remove karta hai &mdash; \"main apna data hamesha le ja sakta "
        "hoon\". Privacy-first ethos ke saath aligned. Bulk export already "
        "backup ke through hai but markdown user-readable nahi hai.",
        [
            "core/export/.../MarkdownExporter.kt  -> rich-text -> markdown converter",
            "feature/notes/.../NoteContextMenu.kt  -> 'Export as Markdown' action",
            "feature/settings/.../ExportScreen.kt  -> bulk export with folder picker",
        ],
        "S",
    ))

    story.append(feature_block(
        "3.8 Smart Linked Suggestions Beyond AI",
        "AiSmartReminderChip aur linked notes already hain. Lekin offline "
        "fallback bhi do &mdash; simple keyword matching se related notes/todos "
        "suggest karein agar AI key nahi hai.",
        "AI features paid quota wale users pe depend karte hain. Free users ko "
        "bhi value milni chahiye. TF-IDF ya simple keyword overlap sufficient "
        "hai for 80% utility.",
        [
            "core/search/.../KeywordSimilarity.kt  -> simple cosine similarity",
            "feature/notes/.../NoteEditorScreen.kt  -> suggestion strip if AI "
            "disabled",
        ],
        "S",
    ))

    story.append(feature_block(
        "3.9 Quick-Add from Notification Shade",
        "Persistent low-priority notification ya tile (QuickSettings) se ek tap "
        "mein todo / note add karne ki UI khul jaye.",
        "Friction removal. \"Phone unlock -> NoteNext open -> +button\" se 3 "
        "taps kam ho jaate hain. Capture rate improve hota hai.",
        [
            "feature/quickadd/.../QuickAddTileService.kt  -> TileService",
            "feature/quickadd/.../QuickAddActivity.kt  -> Theme.Translucent activity",
            "AndroidManifest.xml  -> register tile service",
        ],
        "S",
    ))

    # ========== POLISH ==========
    story.append(PageBreak())
    story.append(Paragraph("4. Polish &amp; UX Gaps", H1))
    story.append(Paragraph(
        "Choti-choti cheezein jo product ko \"theek hai\" se \"yeh chamak raha hai\" "
        "tak le jaati hain.", BODY))

    polish_items = [
        ("Empty states animations", "Lottie ya Compose-native subtle "
         "animations daalo &mdash; abhi static SVG hai. \"Nothing to do!\" pe ek bird "
         "udta hua type."),
        ("Error retry UX", "Toast pe 'Retry' button do jab koi action fail "
         "ho. Network error pe Snackbar with action better hai &mdash; abhi sirf "
         "Toast hai."),
        ("Accessibility (TalkBack)", "Sab swipe actions ke liye "
         "<font face='Courier'>contentDescription</font> aur custom actions "
         "(<font face='Courier'>customActions</font> in semantics) add karo. "
         "Screen reader users swipe nahi kar paate."),
        ("Keyboard handling in editor", "<font face='Courier'>imePadding()</font> "
         "modifier ensure karo, scroll-into-view on focus. "
         "Subtask add karte time keyboard cover karta hai &mdash; check karo."),
        ("Animations consistency", "Abhi springPress() kuch jagah hai, kuch "
         "nahi. Audit karke buttons / chips / cards par uniformly apply karo."),
        ("Dark mode colors audit", "Hand-written dark colors check karo "
         "WCAG AA contrast ke liye &mdash; specifically priority badges aur AI "
         "chips."),
        ("Onboarding flow", "First-launch pe quick 3-screen onboarding daalo "
         "&mdash; (1) privacy promise, (2) main features, (3) backup setup. "
         "Retention day-1 improve hoga."),
        ("Crash recovery", "Editor crash hone par draft auto-save mechanism. "
         "Note unsaved gone = NPS killer. DataStore mein last-edited buffer rakho."),
    ]
    for title, desc in polish_items:
        story.append(Paragraph(f"<b>{title}</b>", H3))
        story.append(Paragraph(desc, BODY))

    # ========== TECH DEBT ==========
    story.append(PageBreak())
    story.append(Paragraph("5. Tech Debt &amp; Architecture", H1))
    story.append(Paragraph(
        "Code health ke kaam &mdash; user-facing nahi hain par medium term mein "
        "velocity badhate hain.", BODY))

    debt = [
        ("Material 3 alpha &rarr; stable", "M3E v1.5.0-alpha17 abhi alpha hai. "
         "Stable release aate hi pin karo &mdash; alphas mein API breaks aate hain. "
         "<font face='Courier'>libs.versions.toml</font> mein TODO comment "
         "daalo with version constraint."),
        ("AlarmScheduler abstraction", "Abhi <font face='Courier'>TodoViewModel</font> "
         "directly AlarmScheduler use karta hai (line 39). Interface "
         "<font face='Courier'>ReminderScheduler</font> banao &mdash; testing "
         "easy, future mein WorkManager ya different impl swap kar sakte ho."),
        ("Domain use-cases for todos", "Business logic abhi ViewModel mein "
         "scattered hai. <font face='Courier'>CompleteTodoUseCase</font>, "
         "<font face='Courier'>SnoozeReminderUseCase</font>, "
         "<font face='Courier'>SpawnNextRecurringUseCase</font> &mdash; pure "
         "functions extract karo. Notes feature mein already use-case layer hai &mdash; "
         "consistency."),
        ("Notes vs Todos parallel systems", "<font face='Courier'>ChecklistItem</font> "
         "(notes ke andar) aur <font face='Courier'>TodoItem</font> &mdash; do "
         "alag entities. Future mein <font face='Courier'>TaskLike</font> "
         "interface introduce karke gradually merge karo. Section 2.12 ka "
         "conversion feature is migration ka stepping stone hai."),
        ("DataStore migration finish karo", "Partial migration hua hai &mdash; "
         "kuch jagah SharedPreferences abhi bhi hai. Audit karo, ek-ek "
         "module migrate karo. Type-safe + coroutine-friendly."),
        ("Bug-marker comments hatao", "Code mein <font face='Courier'>// "
         "Bug C1 fix</font> jaise comments hain (BackupRestoreViewModel). "
         "Yeh git history aur issue tracker ka kaam hai &mdash; code mein noise add "
         "karta hai."),
        ("OkHttp 5 alpha review", "<font face='Courier'>okhttp-5.0.0-alpha.14</font> "
         "&mdash; alpha hai. Migration path check karo, 5.0 stable wait karo "
         "ya 4.x stable pe rollback consider karo."),
        ("Coil 3 migration verify", "Major version bump hua (io.coil-kt &rarr; "
         "io.coil-kt.coil3). Saare imports migrated hain &mdash; verify karo "
         "<font face='Courier'>./gradlew :app:compileDebugKotlin</font> aur "
         "manual smoke test."),
    ]
    for title, desc in debt:
        story.append(Paragraph(f"<b>{title}</b>", H3))
        story.append(Paragraph(desc, BODY))

    # ========== TESTING ==========
    story.append(PageBreak())
    story.append(Paragraph("6. Testing Roadmap", H1))
    story.append(Paragraph(
        "<b>Reality check:</b> abhi pure project mein essentially 1 test file hai. "
        "Yeh sustainable nahi hai jaise-jaise features badh rahe hain.", QUOTE))

    story.append(Paragraph("Suggested layered approach:", BODY))

    test_t = Table([
        ["Layer", "What to test", "Tool", "Effort"],
        ["DAO / Repository", "Room queries, FK cascades, "
         "migrations correct hain", "Robolectric / "
         "Room in-memory", "M"],
        ["ViewModel", "State transitions, "
         "filter logic, recurring spawn", "Turbine + "
         "MockK + JUnit", "M"],
        ["Use-cases", "Pure logic &mdash; priority calc, "
         "streak counting, conversion", "JUnit only", "S"],
        ["Compose UI", "Critical screens (TodoScreen, "
         "AddEditDialog) smoke", "compose-ui-test", "M"],
        ["Snapshot / screenshot", "TodoItemCard variants &mdash; "
         "states, dark mode, RTL", "Paparazzi / Roborazzi", "S"],
        ["E2E", "Add todo &rarr; "
         "complete &rarr; appears in completed", "MacroBenchmark "
         "+ UiAutomator", "L"],
    ], colWidths=[3.5 * cm, 7 * cm, 3.5 * cm, 1.2 * cm])
    test_t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), ACCENT_DARK),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONT", (0, 0), (-1, 0), "Helvetica-Bold", 10),
        ("FONT", (0, 1), (-1, -1), "Helvetica", 9.5),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, ACCENT_SOFT]),
        ("ALIGN", (3, 1), (3, -1), "CENTER"),
        ("FONT", (3, 1), (3, -1), "Helvetica-Bold", 9.5),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("BOX", (0, 0), (-1, -1), 0.5, GREY_LIGHT),
        ("INNERGRID", (0, 0), (-1, -1), 0.25, GREY_LIGHT),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.append(test_t)
    story.append(Spacer(1, 10))

    story.append(Paragraph("Pragmatic order (jab tak full coverage nahi):", H2))
    story.append(bullet("<b>Pehle ViewModel + use-cases</b> &mdash; logic regressions "
                        "yahan se aate hain. Fastest ROI."))
    story.append(bullet("<b>Phir DAO migrations</b> &mdash; agar v3 -&gt; v4 migration "
                        "todle, users ka data jaa sakta hai. Critical."))
    story.append(bullet("<b>Phir snapshot tests</b> &mdash; UI regressions cheaply catch "
                        "ho jaate hain."))
    story.append(bullet("<b>Last mein E2E</b> &mdash; expensive but happy path "
                        "guarantee deta hai."))

    story.append(Paragraph("CI setup", H2))
    story.append(Paragraph(
        "GitHub Actions / similar mein <font face='Courier'>./gradlew test "
        "ktlintCheck detekt</font> har PR pe chalao. Coverage report "
        "(Kover) artifact mein upload karo &mdash; threshold 60% se start, "
        "gradually 80% tak le jao.", BODY))

    # ========== PRIORITY ==========
    story.append(PageBreak())
    story.append(Paragraph("7. Priority Order (P0 / P1 / P2)", H1))
    story.append(Paragraph(
        "Yeh ek opinionated ranking hai &mdash; user impact x effort x strategic "
        "value ke basis pe. Free to disagree.", BODY))

    pri_t = Table([
        ["Priority", "Item", "Effort", "Why"],
        # P0
        ["P0", "Recurring tasks (2.1)", "L", "Table stakes; users churn karenge bina iske"],
        ["P0", "Smart reminders + snooze (2.4)", "M", "Bina snooze frustration high hai"],
        ["P0", "Search within todos (2.10)", "S", "Cheap, huge UX win"],
        ["P0", "Bulk actions (2.11)", "S", "Cheap, daily friction"],
        ["P0", "ViewModel + DAO tests (Sec 6)", "M", "Reliability foundation"],
        # P1
        ["P1", "Multiple labels (2.2)", "M", "Notes ke saath consistency"],
        ["P1", "Todo widget (2.5)", "M", "Retention boost"],
        ["P1", "Habit tracking (2.7)", "M", "Recurring ke upar build, engagement"],
        ["P1", "Unified search (3.1)", "M", "Cross-feature killer UX"],
        ["P1", "Auto-backup scheduling (3.6)", "M", "Trust + data safety"],
        ["P1", "Haptic feedback (3.4)", "S", "Premium feel, free win"],
        ["P1", "Markdown export (3.7)", "S", "Lock-in fear remove"],
        ["P1", "Quick-add tile (3.9)", "S", "Capture rate"],
        # P2
        ["P2", "Time-blocking (2.3)", "M", "Power users only"],
        ["P2", "Calendar sync (2.6)", "L", "Power users; permission cost"],
        ["P2", "Pomodoro (2.9)", "L", "Adjacent feature; can be later"],
        ["P2", "Task dependencies (2.8)", "M", "Niche; PM-style users"],
        ["P2", "Encryption for todos (3.2)", "M", "Important but not visible"],
        ["P2", "Per-item biometric (3.3)", "M", "Niche use case"],
        ["P2", "More languages (3.5)", "M", "Strategic but slow ROI"],
        ["P2", "Convert checklist <-> todo (2.12)", "M", "Great but architectural"],
    ], colWidths=[1.4 * cm, 6.5 * cm, 1.2 * cm, 6 * cm])

    style_cmds = [
        ("BACKGROUND", (0, 0), (-1, 0), ACCENT_DARK),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONT", (0, 0), (-1, 0), "Helvetica-Bold", 10),
        ("FONT", (0, 1), (-1, -1), "Helvetica", 9.5),
        ("ALIGN", (0, 0), (0, -1), "CENTER"),
        ("ALIGN", (2, 0), (2, -1), "CENTER"),
        ("FONT", (0, 1), (0, -1), "Helvetica-Bold", 10),
        ("FONT", (2, 1), (2, -1), "Helvetica-Bold", 9.5),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("BOX", (0, 0), (-1, -1), 0.5, GREY_LIGHT),
        ("INNERGRID", (0, 0), (-1, -1), 0.25, GREY_LIGHT),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]
    # color priority cells
    for i, row in enumerate(pri_t._cellvalues):
        if i == 0:
            continue
        p = row[0]
        if p == "P0":
            style_cmds.append(("BACKGROUND", (0, i), (0, i), RED))
            style_cmds.append(("TEXTCOLOR", (0, i), (0, i), colors.white))
        elif p == "P1":
            style_cmds.append(("BACKGROUND", (0, i), (0, i), ORANGE))
            style_cmds.append(("TEXTCOLOR", (0, i), (0, i), colors.white))
        else:
            style_cmds.append(("BACKGROUND", (0, i), (0, i), GREEN))
            style_cmds.append(("TEXTCOLOR", (0, i), (0, i), colors.white))
        # alternating row bg for readability
        if i % 2 == 0:
            style_cmds.append(("BACKGROUND", (1, i), (-1, i), ACCENT_SOFT))

    pri_t.setStyle(TableStyle(style_cmds))
    story.append(pri_t)

    # ========== QUICK WINS ==========
    story.append(PageBreak())
    story.append(Paragraph("8. Quick Wins vs Big Bets", H1))

    story.append(Paragraph("Quick Wins (1-2 din mein)", H2))
    story.append(Paragraph(
        "Agar weekend hai aur kuch ship karna hai, yeh order mein pakdo &mdash; "
        "high impact, low risk:", BODY))
    qw = [
        "Search bar within todos (2.10) &mdash; SearchBar + LIKE query",
        "Bulk actions selection mode (2.11) &mdash; ek StateFlow + conditional ActionBar",
        "Haptic feedback on swipe / long-press (3.4) &mdash; few line changes",
        "Markdown export (3.7) &mdash; existing rich-text -&gt; markdown converter",
        "Quick-add tile (3.9) &mdash; small TileService",
        "Bug-marker comments cleanup (Sec 5) &mdash; pure deletion",
        "<font face='Courier'>contentDescription</font> audit pe basic a11y",
    ]
    for item in qw:
        story.append(bullet(item))

    story.append(Paragraph("Big Bets (1-2 hafte each)", H2))
    story.append(Paragraph(
        "In par invest karne se NoteNext ka positioning materially shift hota "
        "hai &mdash; \"ek aur note app\" se \"productivity hub\".", BODY))
    bb = [
        "<b>Recurring tasks (2.1)</b> &mdash; without this, productivity claim incomplete hai",
        "<b>Calendar sync (2.6)</b> &mdash; power users ka sticky surface",
        "<b>Pomodoro (2.9)</b> &mdash; new user segment unlock",
        "<b>Notes &lrarr; Todos unification (2.12 + 5.4)</b> &mdash; long-term simplification",
        "<b>Testing infrastructure (Sec 6)</b> &mdash; agar yeh nahi, bakaayda baaki sab fragile",
    ]
    for item in bb:
        story.append(bullet(item))

    # ========== CLOSING ==========
    story.append(Paragraph("9. Closing Thoughts", H1))
    story.append(Paragraph(
        "Bhai, NoteNext ka base bahut strong hai &mdash; Compose, Clean Architecture, "
        "M3E, AI integration, privacy-first ethos &mdash; sab solid hai. Gap "
        "implementation mein nahi hai, gap <b>scope</b> mein hai. Yaani \"kya "
        "feature priority mein lao\" wala question \"kaise banao\" se zyada "
        "important hai.", BODY))
    story.append(Paragraph(
        "Recommendation:", H2))
    story.append(bullet("Agle 2 hafte: P0 wale pakad lo (recurring + smart "
                        "reminders + search + bulk + tests). Yeh combination "
                        "todo feature ko 'usable' se 'sticky' bana deta hai."))
    story.append(bullet("Phir agla quarter: P1 widgets + habit tracking + "
                        "unified search. Yeh retention curve sambhalega."))
    story.append(bullet("Tech debt parallel mein: jab bhi feature touch karo, "
                        "uske paas wala small refactor opportunistically nibtao "
                        "(boy-scout rule)."))
    story.append(bullet("Testing pehla &mdash; pehle ek <font face='Courier'>"
                        "TodoViewModelTest</font> likho. Pattern set hone ke baad "
                        "baaki easy ho jaata hai."))

    story.append(Spacer(1, 12))
    story.append(Paragraph(
        "Aakhri baat &mdash; perfect roadmap exist nahi karta. Yeh guide ek "
        "starting point hai, opinionated rankings ke saath. Apne user feedback "
        "(jo bhi available ho), Play Store reviews, aur apni intuition ke saath "
        "mix karke decide karo. Ship karte raho.", QUOTE))

    story.append(Spacer(1, 12))
    story.append(Paragraph(
        "<i>&mdash; Generated for Suvojeet, NoteNext maintainer. 2026-05-06.</i>",
        META))

    doc.build(story)
    print(f"PDF generated: {OUTPUT}")


if __name__ == "__main__":
    build()
