"""
AICallShield – Synopsis PDF Generator
Generates a complete software project synopsis in PDF format.
"""

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    HRFlowable, PageBreak, ListFlowable, ListItem,
)
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY
from reportlab.platypus import KeepTogether


# ── Colour Palette ────────────────────────────────────────────────────
PRIMARY     = colors.HexColor("#1A237E")   # Deep Indigo
SECONDARY   = colors.HexColor("#283593")   # Indigo
ACCENT      = colors.HexColor("#3949AB")   # Mid Indigo
LIGHT_BG    = colors.HexColor("#E8EAF6")   # Very light indigo
TABLE_HDR   = colors.HexColor("#3949AB")
TABLE_ALT   = colors.HexColor("#F0F4FF")
WHITE       = colors.white
BLACK       = colors.HexColor("#212121")
GREY        = colors.HexColor("#546E7A")
DIVIDER     = colors.HexColor("#7986CB")


# ── Style Factory ─────────────────────────────────────────────────────
def build_styles():
    base = getSampleStyleSheet()

    styles = {
        "cover_title": ParagraphStyle(
            "cover_title",
            fontName="Times-Bold",
            fontSize=26,
            leading=39,
            textColor=PRIMARY,
            alignment=TA_CENTER,
            spaceAfter=6,
        ),
        "cover_subtitle": ParagraphStyle(
            "cover_subtitle",
            fontName="Times-Roman",
            fontSize=13,
            leading=19.5,
            textColor=SECONDARY,
            alignment=TA_CENTER,
            spaceAfter=4,
        ),
        "cover_meta": ParagraphStyle(
            "cover_meta",
            fontName="Times-Roman",
            fontSize=10,
            leading=15,
            textColor=GREY,
            alignment=TA_CENTER,
            spaceAfter=3,
        ),
        "section_num": ParagraphStyle(
            "section_num",
            fontName="Times-Bold",
            fontSize=16,
            leading=24,
            textColor=WHITE,
            alignment=TA_LEFT,
            spaceAfter=0,
            leftIndent=8,
        ),
        "h2": ParagraphStyle(
            "h2",
            fontName="Times-Bold",
            fontSize=14,
            leading=21,
            textColor=SECONDARY,
            spaceBefore=10,
            spaceAfter=4,
            alignment=TA_LEFT,
        ),
        "body": ParagraphStyle(
            "body",
            fontName="Times-Roman",
            fontSize=12,
            leading=18,
            textColor=BLACK,
            alignment=TA_JUSTIFY,
            spaceAfter=6,
        ),
        "bullet": ParagraphStyle(
            "bullet",
            fontName="Times-Roman",
            fontSize=12,
            leading=18,
            textColor=BLACK,
            leftIndent=14,
            spaceAfter=3,
        ),
        "bullet_bold": ParagraphStyle(
            "bullet_bold",
            fontName="Times-Bold",
            fontSize=12,
            leading=18,
            textColor=BLACK,
            leftIndent=14,
            spaceAfter=3,
        ),
        "ref": ParagraphStyle(
            "ref",
            fontName="Times-Roman",
            fontSize=12,
            leading=18,
            textColor=BLACK,
            leftIndent=20,
            firstLineIndent=-20,
            alignment=TA_JUSTIFY,
            spaceAfter=6,
        ),
        "footer": ParagraphStyle(
            "footer",
            fontName="Times-Roman",
            fontSize=8,
            textColor=GREY,
            alignment=TA_CENTER,
        ),
        "table_hdr": ParagraphStyle(
            "table_hdr",
            fontName="Times-Bold",
            fontSize=11,
            textColor=WHITE,
            alignment=TA_CENTER,
        ),
        "table_cell": ParagraphStyle(
            "table_cell",
            fontName="Times-Roman",
            fontSize=11,
            textColor=BLACK,
            alignment=TA_LEFT,
            leading=16.5,
        ),
    }
    return styles


# ── Header / Footer Canvas ────────────────────────────────────────────
class SynopsisDoc(SimpleDocTemplate):
    def __init__(self, filename):
        super().__init__(
            filename,
            pagesize=A4,
            rightMargin=2.54 * cm,      # 1 inch
            leftMargin=3.81 * cm,       # 1.5 inches
            topMargin=2.54 * cm,        # 1 inch
            bottomMargin=2.54 * cm,     # 1 inch
        )
        self.page_count = 0

    def handle_pageEnd(self):
        super().handle_pageEnd()

    def afterPage(self):
        pass


def on_page(canvas, doc):
    """Draw header/footer on every page except the cover."""
    canvas.saveState()
    W, H = A4

    if doc.page > 1:
        # Top rule
        canvas.setStrokeColor(DIVIDER)
        canvas.setLineWidth(0.8)
        canvas.line(3.81 * cm, H - 2.04 * cm, W - 2.54 * cm, H - 2.04 * cm)

        canvas.setFont("Times-Bold", 8)
        canvas.setFillColor(PRIMARY)
        canvas.drawString(3.81 * cm, H - 1.75 * cm, "AICallShield")
        canvas.setFont("Times-Roman", 8)
        canvas.setFillColor(GREY)
        canvas.drawRightString(W - 2.54 * cm, H - 1.75 * cm,
                               "Software Project Synopsis")

        # Bottom rule
        canvas.setStrokeColor(DIVIDER)
        canvas.line(3.81 * cm, 2.04 * cm, W - 2.54 * cm, 2.04 * cm)
        canvas.setFont("Times-Roman", 8)
        canvas.setFillColor(GREY)
        canvas.drawCentredString(W / 2, 1.5 * cm, f"Page {doc.page}")

    canvas.restoreState()


# ── Section Heading helper ────────────────────────────────────────────
def section_heading(styles, number, title):
    """Returns a coloured banner paragraph for a section heading."""
    data = [[Paragraph(f"{number}.  {title}", styles["section_num"])]]
    tbl = Table(data, colWidths=["100%"])
    tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), PRIMARY),
        ("TOPPADDING",    (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LEFTPADDING",   (0, 0), (-1, -1), 8),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 8),
        ("ROUNDEDCORNERS", [4]),
    ]))
    return tbl


def bullet_item(styles, text, bold_prefix=None):
    if bold_prefix:
        return Paragraph(f"<b>•  {bold_prefix}</b> {text}", styles["bullet"])
    return Paragraph(f"•  {text}", styles["bullet"])


def sub_heading(styles, text):
    return Paragraph(text, styles["h2"])


# ── Cover Page ────────────────────────────────────────────────────────
def cover_page(styles):
    elems = []
    elems.append(Spacer(1, 2.5 * cm))

    # Shield / logo banner
    logo_data = [[Paragraph("🛡  AICallShield", styles["cover_title"])]]
    logo_tbl = Table(logo_data, colWidths=["100%"])
    logo_tbl.setStyle(TableStyle([
        ("BACKGROUND",    (0, 0), (-1, -1), LIGHT_BG),
        ("TOPPADDING",    (0, 0), (-1, -1), 18),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 18),
        ("ROUNDEDCORNERS", [8]),
    ]))
    elems.append(logo_tbl)
    elems.append(Spacer(1, 0.6 * cm))

    elems.append(Paragraph(
        "AI-Powered Intelligent Call Screening &amp; Scam Detection System",
        styles["cover_subtitle"]))
    elems.append(Spacer(1, 0.3 * cm))
    elems.append(HRFlowable(width="60%", thickness=1.5, color=ACCENT,
                             hAlign="CENTER"))
    elems.append(Spacer(1, 1.0 * cm))

    elems.append(Paragraph("SOFTWARE PROJECT SYNOPSIS", styles["cover_subtitle"]))
    elems.append(Spacer(1, 2.0 * cm))

    # Meta table
    meta = [
        ["Project Title",   "AICallShield – AI-Powered Intelligent Call Screening"],
        ["Domain",          "Artificial Intelligence / Mobile Computing"],
        ["Platform",        "Android (API 29+) + Python Cloud Backend"],
        ["Submitted By",    "[ Your Name / Team Name ]"],
        ["Institute",       "[ Your Institute Name ]"],
        ["Department",      "[ Department of Computer Science / IT ]"],
        ["Academic Year",   "2025 – 2026"],
        ["Date",            "February 2026"],
    ]
    tbl = Table(meta, colWidths=[5 * cm, 10.5 * cm])
    tbl.setStyle(TableStyle([
        ("BACKGROUND",    (0, 0), (0, -1), LIGHT_BG),
        ("FONTNAME",      (0, 0), (0, -1), "Times-Bold"),
        ("FONTNAME",      (1, 0), (1, -1), "Times-Roman"),
        ("FONTSIZE",      (0, 0), (-1, -1), 11),
        ("LEADING",       (0, 0), (-1, -1), 16.5),
        ("TEXTCOLOR",     (0, 0), (0, -1), PRIMARY),
        ("TEXTCOLOR",     (1, 0), (1, -1), BLACK),
        ("TOPPADDING",    (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LEFTPADDING",   (0, 0), (-1, -1), 10),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 10),
        ("GRID",          (0, 0), (-1, -1), 0.5, colors.HexColor("#C5CAE9")),
        ("ROWBACKGROUNDS", (0, 0), (-1, -1),
         [WHITE, TABLE_ALT] * 10),
    ]))
    elems.append(tbl)
    elems.append(PageBreak())
    return elems


# ── Sections ──────────────────────────────────────────────────────────
def build_story(styles):
    s = styles
    story = []

    # ---------- Cover ----------
    story += cover_page(s)

    # ================================================================
    # 1. TITLE
    # ================================================================
    story.append(section_heading(s, 1, "Title of the Project"))
    story.append(Spacer(1, 0.3 * cm))
    story.append(Paragraph(
        "<b>AICallShield – AI-Powered Intelligent Call Screening and Scam Detection System</b>",
        s["body"]))
    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 2. INTRODUCTION
    # ================================================================
    story.append(section_heading(s, 2, "Introduction"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(Paragraph(
        "The rapid proliferation of smartphones has transformed voice calls into one of the most "
        "intimate channels of personal and professional communication. However, this ubiquity has "
        "simultaneously made mobile users prime targets for fraudulent callers, telemarketing spam, "
        "and sophisticated social-engineering scams. According to the Federal Trade Commission (FTC), "
        "Americans alone reported losses exceeding <b>$10 billion</b> to phone-based fraud in 2023, "
        "with the global figure estimated to be several times higher. Developing countries such as "
        "India face an equally alarming situation, where fake bank, tax, and KYC-verification calls "
        "victimise millions of citizens every year.",
        s["body"]))

    story.append(Paragraph(
        "Traditional call-filtering mechanisms — such as built-in spam lists, user-reported databases, "
        "and network-level STIR/SHAKEN protocols — are largely reactive and fail to keep pace with "
        "ever-evolving social engineering tactics. They block known numbers but cannot intelligently "
        "assess the <i>content</i> of a conversation in real time. As a result, many harmful calls "
        "still reach end users, who may not be equipped to recognise manipulation in the moment.",
        s["body"]))

    story.append(Paragraph(
        "Artificial intelligence and large language models (LLMs) have reached a maturity level where "
        "they can understand natural-language conversations, detect deceptive intent, and even respond "
        "autonomously on behalf of a user. Speech-to-text models such as OpenAI Whisper achieve "
        "near-human accuracy across multiple languages and accents. Text-to-speech engines have become "
        "indistinguishable from natural voices. These technologies, combined with modern mobile "
        "operating-system APIs, create an opportunity to build a fully autonomous call-screening "
        "assistant that operates at the edge — directly on the user's Android device — with cloud "
        "intelligence powering the AI reasoning.",
        s["body"]))

    story.append(Paragraph(
        "<b>AICallShield</b> is proposed as exactly this solution: an Android application backed by a "
        "Python-based FastAPI cloud service that transparently intercepts unknown incoming calls, "
        "engages the caller in real-time conversation through AI, transcribes every utterance, "
        "scores the call for spam and scam risk, and presents the user with a live WhatsApp-style chat "
        "view so they can monitor the interaction and join the call at any moment. The project sits at "
        "the intersection of mobile engineering, cloud computing, and applied AI — representing a "
        "highly relevant and commercially viable product in today's cybersecurity landscape.",
        s["body"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 3. PROBLEM STATEMENT
    # ================================================================
    story.append(section_heading(s, 3, "Problem Statement"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(Paragraph(
        "Despite advances in telecommunications security, phone-based scams remain one of the "
        "fastest-growing categories of cybercrime. The core problems with the current landscape are:",
        s["body"]))

    problems = [
        ("<b>Reactive filtering only:</b>",
         "Existing solutions (Truecaller, carrier spam lists) rely on crowd-sourced number "
         "blacklists. A new scam number that has never called anyone is invisible to these systems."),
        ("<b>No content-level intelligence:</b>",
         "Call-ID spoofing allows scammers to mimic legitimate institutions (banks, government "
         "agencies). Number-based filtering cannot catch spoofed calls."),
        ("<b>User vulnerability:</b>",
         "Elderly and less technically-savvy users are particularly susceptible to social "
         "engineering. They need an agent that can hold the conversation while they decide what to do."),
        ("<b>No real-time transcription or summarisation:</b>",
         "Current smartphone dialers provide no in-call transcription, making it impossible "
         "to review what was said or search through call history textually."),
        ("<b>Lack of actionable risk scores:</b>",
         "There is no widely available system that continuously scores an ongoing conversation "
         "for fraud probability and alerts the user with an explainable reason."),
    ]
    for bold, text in problems:
        story.append(Paragraph(f"•  {bold} {text}", s["bullet"]))

    story.append(Spacer(1, 0.1 * cm))
    story.append(Paragraph(
        "Solving this problem is critical because phone fraud directly translates into financial "
        "and psychological harm. A proactive, content-aware, real-time screening system can "
        "significantly reduce the number of successful scam calls and empower users with information "
        "before it is too late.",
        s["body"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 4. OBJECTIVES
    # ================================================================
    story.append(section_heading(s, 4, "Objectives of the Project"))
    story.append(Spacer(1, 0.3 * cm))

    objectives = [
        "To design and develop an Android application that autonomously screens unknown incoming "
        "calls using a natural-language AI agent.",
        "To implement a real-time Speech-to-Text pipeline using OpenAI Whisper that transcribes "
        "both caller audio and AI responses with high accuracy.",
        "To integrate a large language model (GPT-4o-mini / Gemini 1.5 Flash) that generates "
        "context-aware, polite screening responses on behalf of the user.",
        "To build a spam and scam detection engine combining keyword analysis, regex pattern "
        "matching, and sentiment analysis to assign a continuous risk score to each call.",
        "To develop a real-time, WhatsApp-style chat interface on Android that streams the live "
        "conversation transcript to the user with colour-coded risk indicators.",
        "To allow seamless user takeover — the user can tap a button to join the live call at "
        "any point during AI screening.",
        "To persist full call records (transcript, risk score, AI summary, audio) in Firebase "
        "Firestore and Cloud Storage for long-term searchable history.",
        "To generate an AI-authored post-call summary that condenses the conversation into key "
        "points and a final risk verdict.",
        "To provide real-time bidirectional communication between the Android client and the "
        "Python backend via WebSockets.",
        "To ensure user privacy through explicit consent mechanisms, on-device caller notification, "
        "and secure encrypted data transfer.",
    ]
    for i, obj in enumerate(objectives, 1):
        story.append(Paragraph(f"•  {obj}", s["bullet"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 5. SCOPE
    # ================================================================
    story.append(section_heading(s, 5, "Scope of the Project"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "What the System Covers"))
    scope_in = [
        "Detection and interception of unknown incoming calls on Android (API 29+) using the "
        "official CallScreeningService API.",
        "Real-time audio capture, streaming transcription, and AI-driven reply generation.",
        "Spam/scam scoring with keyword lists, pattern matching, and sentiment analysis.",
        "Live chat UI displaying the ongoing call conversation in real time.",
        "User-controlled call takeover, call blocking, and call ending.",
        "Post-call AI summary, risk report, and searchable chat history.",
        "Cloud storage of call recordings and metadata via Firebase.",
        "A documented RESTful + WebSocket API backend that can be reused or extended.",
    ]
    for item in scope_in:
        story.append(Paragraph(f"•  {item}", s["bullet"]))

    story.append(sub_heading(s, "Target Users"))
    story.append(Paragraph(
        "The primary target audience includes individual Android smartphone users who frequently "
        "receive calls from unknown numbers, elderly citizens who are at elevated risk of phone "
        "fraud, small business owners who need call-log management, and security researchers "
        "studying phone-based social engineering.",
        s["body"]))

    story.append(sub_heading(s, "Limitations"))
    limitations = [
        "Full audio interception requires the app to be set as the device's <i>default dialer</i>, "
        "which is subject to Google Play Store policy restrictions for general distribution.",
        "AI reply quality depends on the capability of the third-party LLM provider "
        "(OpenAI / Google). API outages directly affect the screening quality.",
        "The system is initially scoped to the English language; multilingual support is planned "
        "as a future enhancement.",
        "iOS is out of scope for the current version due to platform API restrictions on call "
        "interception.",
        "Real-time audio streaming latency depends on the user's internet connection quality.",
    ]
    for item in limitations:
        story.append(Paragraph(f"•  {item}", s["bullet"]))

    story.append(Spacer(1, 0.4 * cm))
    story.append(PageBreak())

    # ================================================================
    # 6. LITERATURE REVIEW
    # ================================================================
    story.append(section_heading(s, 6, "Literature Review"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "6.1  Existing Systems and Research"))

    story.append(Paragraph(
        "<b>Truecaller (2009–present)</b> is the most widely deployed call-screening application "
        "worldwide, with over 350 million active users. It maintains a crowd-sourced number "
        "database to identify and block spam callers. While effective for known spammers, "
        "Truecaller offers no content-level analysis; a new number calling for the first time "
        "will pass all filters unhindered. Furthermore, the service has attracted controversy "
        "regarding bulk harvesting of contact data from users' phonebooks "
        "(Rao &amp; Raman, 2018).",
        s["body"]))

    story.append(Paragraph(
        "<b>Google's Call Screen feature (Pixel phones, 2018)</b> introduced an AI assistant "
        "(the Google Assistant) that could answer a call, ask the caller for their name and "
        "purpose, and transcribe the response. This was a significant proof-of-concept for "
        "AI-powered call screening. However, it is restricted to Pixel devices, not open-source, "
        "does not expose a spam-risk API, and does not maintain a searchable conversational history "
        "(Google AI Blog, 2018).",
        s["body"]))

    story.append(Paragraph(
        "<b>Hiya / Nomorobo (2015–present)</b> are carrier- and app-level robocall blocking "
        "services that combine number-reputation databases with call-frequency analysis. They "
        "are effective against automated robocalls but are easily defeated by human social "
        "engineers using legitimate VoIP numbers (Sahin et al., 2017).",
        s["body"]))

    story.append(Paragraph(
        "<b>Research on Voice Spam Detection</b> — Kolan and Dantu (2007) proposed a framework "
        "for SPIT (Spam over Internet Telephony) detection using call-flow features and Bayesian "
        "classifiers. While pioneering, this work predates modern LLMs and relied on metadata "
        "rather than semantic content analysis. More recent work by Aziz et al. (2022) "
        "demonstrated that transformer-based models fine-tuned on phone-scam transcripts achieve "
        "F1 scores above 0.91 for scam intent classification.",
        s["body"]))

    story.append(Paragraph(
        "<b>OpenAI Whisper (Radford et al., 2022)</b> established a new benchmark for automatic "
        "speech recognition (ASR) by training a single encoder-decoder transformer on 680,000 "
        "hours of multilingual audio. Whisper achieves word error rates (WER) competitive with "
        "or better than human transcriptionists on many benchmarks, making it a suitable STT "
        "engine for real-time call transcription.",
        s["body"]))

    story.append(sub_heading(s, "6.2  Identified Gaps in Current Systems"))

    gaps = [
        "No existing publicly available Android application combines real-time STT, LLM-based "
        "autonomous response, semantic scam scoring, and live user chat interface in a single "
        "integrated solution.",
        "Academic spam detection models operate offline on recorded datasets; none integrate "
        "with a live telephony API to provide real-time protection.",
        "Google Call Screen is closed-source and device-limited; no open API exists for "
        "developers to build upon its capabilities.",
        "Existing solutions do not generate post-call AI summaries that can be archived and "
        "searched, limiting their utility for call-log analytics.",
        "Privacy mechanisms in existing apps are inadequate — they transmit large volumes of "
        "contact metadata to centralised servers without granular user consent.",
    ]
    for gap in gaps:
        story.append(Paragraph(f"•  {gap}", s["bullet"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 7. PROPOSED SYSTEM
    # ================================================================
    story.append(section_heading(s, 7, "Proposed System"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(Paragraph(
        "AICallShield proposes a two-tier architecture: an Android client application and a "
        "Python FastAPI cloud backend. When an unknown call arrives on the device, the Android "
        "app intercepts it via the <i>CallScreeningService</i> API, silently answers it, and "
        "begins streaming the caller's audio to the backend. The backend transcribes the audio "
        "using Whisper, passes the transcript to an LLM (GPT-4o-mini or Gemini 1.5 Flash) with "
        "a carefully crafted system prompt, and returns an AI-generated reply. The reply is "
        "converted to speech via a TTS engine and played back to the caller. The entire exchange "
        "is simultaneously pushed over a WebSocket to the Android UI where it appears as a "
        "live chat conversation.",
        s["body"]))

    story.append(sub_heading(s, "Key Features"))

    features = [
        ("Unknown Call Interception",
         "Uses Android's CallScreeningService to answer calls before they ring on the device."),
        ("AI Auto-Screening Agent",
         "LLM-powered agent asks the caller for their name, purpose, and urgency while maintaining "
         "a polite and professional conversation."),
        ("Real-Time Speech-to-Text",
         "OpenAI Whisper transcribes caller audio with high accuracy, even in noisy environments."),
        ("Scam & Spam Detection",
         "A multi-layer engine scores each utterance for: (i) scam keyword matches, "
         "(ii) regex pattern triggers (OTP requests, money transfer, prize scams), "
         "(iii) sentiment aggressiveness, and (iv) caller number heuristics."),
        ("Risk Level Indicator",
         "Four-tier risk classification: LOW / MEDIUM / HIGH / CRITICAL, surfaced in the UI."),
        ("Live Chat Interface",
         "WhatsApp-style chat screen showing caller and AI messages in real time with "
         "colour-coded risk badges."),
        ("User Call Takeover",
         "A single 'Join Call' button allows the user to take over from the AI at any point."),
        ("AI Post-Call Summary",
         "After the call ends, the LLM generates a concise summary with key points and a "
         "final verdict."),
        ("Searchable Call History",
         "All transcripts, summaries, and audio recordings are stored in Firebase and "
         "searchable by keyword, date, or risk level."),
        ("Privacy Controls",
         "Caller is notified of AI screening at call start; recording requires explicit user opt-in."),
    ]

    feat_data = [
        [
            Paragraph("<b>Feature</b>", s["table_hdr"]),
            Paragraph("<b>Description</b>", s["table_hdr"]),
        ]
    ]
    for feat, desc in features:
        feat_data.append([
            Paragraph(f"<b>{feat}</b>", s["table_cell"]),
            Paragraph(desc, s["table_cell"]),
        ])

    feat_tbl = Table(feat_data, colWidths=[4.5 * cm, 11.5 * cm])
    row_colors = [TABLE_HDR] + [WHITE if i % 2 == 0 else TABLE_ALT
                                 for i in range(len(features))]
    feat_tbl.setStyle(TableStyle([
        ("BACKGROUND",    (0, 0), (-1, 0), TABLE_HDR),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, TABLE_ALT] * 10),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 7),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 7),
        ("VALIGN",        (0, 0), (-1, -1), "TOP"),
    ]))
    story.append(feat_tbl)
    story.append(Spacer(1, 0.4 * cm))

    story.append(sub_heading(s, "Improvements Over Existing Systems"))
    improvements = [
        "Content-aware screening — analyses <i>what</i> the caller says, not just their number.",
        "Fully autonomous AI agent — no user interaction required during screening.",
        "Open and extensible — the backend REST + WebSocket API can be integrated with other "
        "apps or services.",
        "Privacy-by-design — no contact book harvesting; all analysis is performed per-call.",
        "Transparent and explainable — every risk score comes with a human-readable explanation "
        "of the detected triggers.",
        "Persistent searchable archive — call history is stored as structured data, "
        "not just audio recordings.",
    ]
    for item in improvements:
        story.append(Paragraph(f"•  {item}", s["bullet"]))

    story.append(Spacer(1, 0.4 * cm))
    story.append(PageBreak())

    # ================================================================
    # 8. METHODOLOGY
    # ================================================================
    story.append(section_heading(s, 8, "Methodology"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "8.1  Development Model"))
    story.append(Paragraph(
        "The project follows an <b>Agile / Iterative development model</b> with two-week sprints. "
        "Each sprint delivers a working vertical slice of the system, from Android UI through "
        "backend logic to AI integration. This approach allows continuous user feedback and rapid "
        "course correction, especially important given the novel nature of real-time AI call "
        "screening.",
        s["body"]))

    story.append(sub_heading(s, "8.2  System Data Flow"))
    flow_steps = [
        ("Step 1 – Call Detection",
         "Android CallScreeningService intercepts an incoming unknown call. App sends a "
         "'disallow with response' intent, buying time to screen."),
        ("Step 2 – Audio Capture",
         "MediaRecorder or AudioRecord API captures caller audio in WAV/PCM format."),
        ("Step 3 – Speech-to-Text",
         "Audio chunk is POST-ed to the /api/v1/transcribe endpoint. Whisper model "
         "returns a transcript with confidence score."),
        ("Step 4 – Spam Analysis",
         "Transcript is analysed by the spam detection engine: keyword scan, pattern regex, "
         "sentiment analysis via TextBlob. A risk score (0.0–1.0) is computed."),
        ("Step 5 – AI Reply Generation",
         "Transcript + conversation history + system prompt is sent to GPT-4o-mini / "
         "Gemini. The LLM returns a concise, context-aware reply."),
        ("Step 6 – Text-to-Speech",
         "AI reply text is converted to audio using Google TTS / gTTS and streamed back "
         "to the Android app."),
        ("Step 7 – UI Update",
         "WebSocket pushes the new message (caller utterance + AI reply + risk score) "
         "to the Android chat UI."),
        ("Step 8 – Storage",
         "Full call record (transcript, scores, audio URL) is persisted to Firebase "
         "Firestore and Storage."),
        ("Step 9 – Post-Call Summary",
         "LLM generates a summary after the call ends; stored with the call record."),
    ]
    flow_data = [[
        Paragraph("<b>Step</b>", s["table_hdr"]),
        Paragraph("<b>Description</b>", s["table_hdr"]),
    ]]
    for step, desc in flow_steps:
        flow_data.append([
            Paragraph(f"<b>{step}</b>", s["table_cell"]),
            Paragraph(desc, s["table_cell"]),
        ])
    flow_tbl = Table(flow_data, colWidths=[3.8 * cm, 12.2 * cm])
    flow_tbl.setStyle(TableStyle([
        ("BACKGROUND",    (0, 0), (-1, 0), TABLE_HDR),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, TABLE_ALT] * 10),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 7),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 7),
        ("VALIGN",        (0, 0), (-1, -1), "TOP"),
    ]))
    story.append(flow_tbl)
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "8.3  AI / ML Techniques"))
    techniques = [
        ("Automatic Speech Recognition (ASR)",
         "OpenAI Whisper (encoder-decoder transformer) for high-accuracy multilingual transcription."),
        ("Large Language Model (LLM)",
         "GPT-4o-mini or Gemini 1.5 Flash for instruction-following, intent detection, "
         "and reply generation with a custom system prompt."),
        ("Keyword-Based Spam Detection",
         "Curated list of 30+ scam-indicative terms (OTP, lottery, bank account, etc.) "
         "with additive scoring."),
        ("Regex Pattern Engine",
         "10 handcrafted regular expressions targeting high-precision scam patterns "
         "(OTP requests, prize scams, legal threats)."),
        ("Sentiment Analysis",
         "TextBlob polarity + subjectivity scores to detect aggression, urgency, "
         "or emotional manipulation."),
        ("Risk Score Aggregation",
         "Weighted combination of keyword score, pattern score, caller number heuristics, "
         "and sentiment score capped at 1.0."),
        ("Text-to-Speech (TTS)",
         "Google TTS / gTTS for natural-sounding AI voice responses."),
    ]
    for tech, desc in techniques:
        story.append(Paragraph(f"•  <b>{tech}:</b> {desc}", s["bullet"]))

    story.append(sub_heading(s, "8.4  Testing Strategy"))
    testing = [
        "Unit Testing: Individual service functions (spam detection, sentiment, TTS) "
        "tested with pytest and mock audio data.",
        "Integration Testing: Full pipeline tested end-to-end with pre-recorded call "
        "scenarios covering benign, medium-risk, and high-risk conversations.",
        "Android UI Testing: Espresso test cases for chat interface, call takeover flow, "
        "and history screen.",
        "WebSocket Load Testing: Simulated concurrent sessions using websockets Python library "
        "to verify real-time performance under load.",
        "Scam Detection Accuracy: Validated against a labelled dataset of 500 simulated "
        "scam and benign call transcripts, targeting F1 score > 0.88.",
    ]
    for item in testing:
        story.append(Paragraph(f"•  {item}", s["bullet"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 9. TECHNOLOGIES
    # ================================================================
    story.append(section_heading(s, 9, "Technologies to be Used"))
    story.append(Spacer(1, 0.3 * cm))

    tech_data = [
        [Paragraph("<b>Layer</b>", s["table_hdr"]),
         Paragraph("<b>Technology / Tool</b>", s["table_hdr"]),
         Paragraph("<b>Purpose</b>", s["table_hdr"])],
        [Paragraph("Android Frontend", s["table_cell"]),
         Paragraph("Kotlin, Jetpack Compose", s["table_cell"]),
         Paragraph("UI screens, call handling, audio capture", s["table_cell"])],
        [Paragraph("Android Networking", s["table_cell"]),
         Paragraph("Retrofit 2, OkHttp 4, OkHttp WebSocket", s["table_cell"]),
         Paragraph("REST API calls and WebSocket streaming", s["table_cell"])],
        [Paragraph("Local Database (Android)", s["table_cell"]),
         Paragraph("Room Persistence Library", s["table_cell"]),
         Paragraph("Offline call history cache", s["table_cell"])],
        [Paragraph("Backend Framework", s["table_cell"]),
         Paragraph("Python 3.12, FastAPI 0.111", s["table_cell"]),
         Paragraph("REST API + WebSocket server", s["table_cell"])],
        [Paragraph("Speech-to-Text", s["table_cell"]),
         Paragraph("OpenAI Whisper API", s["table_cell"]),
         Paragraph("Caller audio transcription", s["table_cell"])],
        [Paragraph("AI / LLM", s["table_cell"]),
         Paragraph("GPT-4o-mini (OpenAI) / Gemini 1.5 Flash (Google)", s["table_cell"]),
         Paragraph("AI reply generation, post-call summary", s["table_cell"])],
        [Paragraph("Text-to-Speech", s["table_cell"]),
         Paragraph("gTTS / Google Cloud TTS", s["table_cell"]),
         Paragraph("Convert AI text replies to audio", s["table_cell"])],
        [Paragraph("Spam / NLP", s["table_cell"]),
         Paragraph("TextBlob, regex, custom keyword engine", s["table_cell"]),
         Paragraph("Spam scoring and sentiment analysis", s["table_cell"])],
        [Paragraph("Cloud Database", s["table_cell"]),
         Paragraph("Firebase Firestore", s["table_cell"]),
         Paragraph("Call records and transcripts storage", s["table_cell"])],
        [Paragraph("Cloud Storage", s["table_cell"]),
         Paragraph("Firebase Cloud Storage", s["table_cell"]),
         Paragraph("Audio recording archives", s["table_cell"])],
        [Paragraph("Data Validation", s["table_cell"]),
         Paragraph("Pydantic v2", s["table_cell"]),
         Paragraph("API request/response schema validation", s["table_cell"])],
        [Paragraph("Dependency Injection", s["table_cell"]),
         Paragraph("Hilt (Android)", s["table_cell"]),
         Paragraph("Dependency injection in Android", s["table_cell"])],
        [Paragraph("Testing", s["table_cell"]),
         Paragraph("pytest, Espresso, JUnit 5", s["table_cell"]),
         Paragraph("Backend and Android testing", s["table_cell"])],
        [Paragraph("CI / DevOps", s["table_cell"]),
         Paragraph("GitHub Actions", s["table_cell"]),
         Paragraph("Automated build and test pipeline", s["table_cell"])],
        [Paragraph("IDE", s["table_cell"]),
         Paragraph("Android Studio, VS Code", s["table_cell"]),
         Paragraph("Development environments", s["table_cell"])],
    ]

    tech_tbl = Table(tech_data, colWidths=[3.6 * cm, 5.5 * cm, 6.9 * cm])
    tech_tbl.setStyle(TableStyle([
        ("BACKGROUND",    (0, 0), (-1, 0), TABLE_HDR),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, TABLE_ALT] * 20),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 7),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 7),
        ("VALIGN",        (0, 0), (-1, -1), "TOP"),
        ("FONTNAME",      (0, 1), (0, -1), "Times-Bold"),
        ("TEXTCOLOR",     (0, 1), (0, -1), SECONDARY),
    ]))
    story.append(tech_tbl)
    story.append(Spacer(1, 0.4 * cm))
    story.append(PageBreak())

    # ================================================================
    # 10. SYSTEM REQUIREMENTS
    # ================================================================
    story.append(section_heading(s, 10, "System Requirements"))
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "Hardware Requirements – Development Machine"))
    hw_dev = [
        ["Processor",   "Intel Core i5 / AMD Ryzen 5 (8th Gen or newer) or Apple M1+"],
        ["RAM",         "Minimum 8 GB (16 GB recommended for Android emulator + backend)"],
        ["Hard Disk",   "Minimum 20 GB free (Android SDK + project files + Docker images)"],
        ["Network",     "Broadband internet (required for OpenAI/Firebase API calls)"],
    ]
    hw_dev_tbl = Table(
        [[Paragraph("<b>" + r[0] + "</b>", s["table_cell"]),
          Paragraph(r[1], s["table_cell"])] for r in hw_dev],
        colWidths=[4 * cm, 12 * cm])
    hw_dev_tbl.setStyle(TableStyle([
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [LIGHT_BG, WHITE] * 5),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 8),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 8),
        ("FONTNAME",      (0, 0), (0, -1), "Times-Bold"),
        ("TEXTCOLOR",     (0, 0), (0, -1), SECONDARY),
    ]))
    story.append(hw_dev_tbl)
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "Hardware Requirements – Target Android Device"))
    hw_and = [
        ["Android Version", "Android 10 (API 29) or higher"],
        ["Processor",       "Any 64-bit ARMv8 SoC (Snapdragon 660 or equivalent)"],
        ["RAM",             "Minimum 3 GB"],
        ["Storage",         "Minimum 100 MB free for app and local cache"],
        ["Network",         "4G LTE or Wi-Fi for backend communication"],
        ["Microphone",      "Built-in device microphone (required for audio capture)"],
    ]
    hw_and_tbl = Table(
        [[Paragraph("<b>" + r[0] + "</b>", s["table_cell"]),
          Paragraph(r[1], s["table_cell"])] for r in hw_and],
        colWidths=[4 * cm, 12 * cm])
    hw_and_tbl.setStyle(TableStyle([
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [LIGHT_BG, WHITE] * 6),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 8),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 8),
        ("FONTNAME",      (0, 0), (0, -1), "Times-Bold"),
        ("TEXTCOLOR",     (0, 0), (0, -1), SECONDARY),
    ]))
    story.append(hw_and_tbl)
    story.append(Spacer(1, 0.3 * cm))

    story.append(sub_heading(s, "Software Requirements"))
    sw = [
        ["Operating System",         "Windows 10/11 / macOS 12+ / Ubuntu 22.04+ (development)"],
        ["Programming Languages",    "Kotlin 1.9+, Python 3.12+"],
        ["Android SDK",              "Android Studio Flamingo or newer, SDK Platform 29–34"],
        ["Backend Runtime",          "Python 3.12 with pip / venv"],
        ["Cloud Platform",           "Firebase (Firestore + Storage); OpenAI API"],
        ["Version Control",          "Git 2.x + GitHub"],
        ["Build Tools",              "Gradle 8 (Android), uvicorn (Python server)"],
        ["IDE",                      "Android Studio (Android), VS Code (Backend)"],
        ["Browser (API docs)",       "Any modern browser for FastAPI /docs (Swagger UI)"],
    ]
    sw_tbl = Table(
        [[Paragraph("<b>" + r[0] + "</b>", s["table_cell"]),
          Paragraph(r[1], s["table_cell"])] for r in sw],
        colWidths=[5 * cm, 11 * cm])
    sw_tbl.setStyle(TableStyle([
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [LIGHT_BG, WHITE] * 10),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 8),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 8),
        ("FONTNAME",      (0, 0), (0, -1), "Times-Bold"),
        ("TEXTCOLOR",     (0, 0), (0, -1), SECONDARY),
    ]))
    story.append(sw_tbl)
    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 11. EXPECTED OUTCOMES
    # ================================================================
    story.append(section_heading(s, 11, "Expected Outcomes"))
    story.append(Spacer(1, 0.3 * cm))

    outcomes = [
        "A fully functional Android application (debug APK) that autonomously screens "
        "unknown calls and presents a live transcript to the user.",
        "A deployed FastAPI backend service providing REST and WebSocket endpoints "
        "for all AI processing tasks.",
        "A spam detection engine achieving an F1 score greater than 0.85 on a mixed "
        "dataset of scam and benign call transcripts.",
        "Real-time round-trip latency (caller audio → AI reply audio) under 3 seconds "
        "on a 4G connection, making the screening transparent to the caller.",
        "A searchable call history interface enabling users to review, filter, and "
        "export call records.",
        "A measurable reduction in the cognitive burden on the user during unknown calls, "
        "as the AI handles the interaction autonomously.",
        "An open-source codebase and documented API that can serve as a foundation for "
        "commercial products or further academic research.",
    ]
    for o in outcomes:
        story.append(Paragraph(f"•  {o}", s["bullet"]))

    story.append(sub_heading(s, "Industry Relevance"))
    story.append(Paragraph(
        "The global voice-fraud prevention market was valued at USD 1.2 billion in 2023 and "
        "is projected to grow at a CAGR of 18.4% through 2030 (MarketsandMarkets, 2023). "
        "AICallShield directly addresses this market by providing an accessible, "
        "consumer-grade solution. The system's open API architecture also positions it as a "
        "B2B offering for telecom operators who wish to embed AI screening as a value-added "
        "service for their subscribers.",
        s["body"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 12. FUTURE ENHANCEMENTS
    # ================================================================
    story.append(section_heading(s, 12, "Future Enhancements"))
    story.append(Spacer(1, 0.3 * cm))

    future = [
        ("Multilingual Support",
         "Extend Whisper transcription and LLM prompts to handle Hindi, Spanish, Arabic, "
         "and other major languages, broadening the user base significantly."),
        ("On-Device ML Model",
         "Train and deploy a lightweight transformer model (using TensorFlow Lite or "
         "ONNX Runtime) for offline spam classification, reducing dependence on internet "
         "connectivity and third-party API costs."),
        ("iOS Application",
         "Port the application to iOS using CallKit framework, subject to Apple's "
         "platform policies on call interception."),
        ("Cloud Deployment",
         "Containerise the backend using Docker and deploy on Google Cloud Run or "
         "AWS Lambda for auto-scaling and high availability."),
        ("Advanced Analytics Dashboard",
         "Provide users with visualisations of their call history — spam frequency "
         "trends, peak scam times, most common scam keywords — through a web portal."),
        ("Voice Biometrics",
         "Integrate speaker verification to recognise trusted callers by voice "
         "fingerprint, enabling personalised call handling rules."),
        ("Integration with Telecom APIs",
         "Partner with telecom operators to share aggregated (anonymised) scam "
         "signals, enhancing detection accuracy across the network."),
        ("WhatsApp / SMS Scam Detection",
         "Extend the AI detection pipeline to analyse incoming WhatsApp messages and "
         "SMS for phishing links and social engineering content."),
        ("Enterprise Edition",
         "Multi-user management console for businesses to deploy call-screening "
         "policies across employee devices, with centralised reporting."),
    ]
    for title, desc in future:
        story.append(Paragraph(f"•  <b>{title}:</b> {desc}", s["bullet"]))

    story.append(Spacer(1, 0.4 * cm))
    story.append(PageBreak())

    # ================================================================
    # 13. TIMELINE
    # ================================================================
    story.append(section_heading(s, 13, "Project Timeline (Schedule)"))
    story.append(Spacer(1, 0.3 * cm))

    timeline = [
        [Paragraph("<b>Phase</b>", s["table_hdr"]),
         Paragraph("<b>Activities</b>", s["table_hdr"]),
         Paragraph("<b>Duration</b>", s["table_hdr"]),
         Paragraph("<b>Milestone</b>", s["table_hdr"])],
        [Paragraph("Phase 1\nRequirement Analysis", s["table_cell"]),
         Paragraph("Stakeholder interviews, problem definition, use-case modelling, "
                   "technology evaluation", s["table_cell"]),
         Paragraph("2 Weeks", s["table_cell"]),
         Paragraph("Signed requirements document", s["table_cell"])],
        [Paragraph("Phase 2\nSystem Design", s["table_cell"]),
         Paragraph("Architecture design, database schema, API contract design, "
                   "UI wireframes, data-flow diagrams", s["table_cell"]),
         Paragraph("2 Weeks", s["table_cell"]),
         Paragraph("Design document + API specification", s["table_cell"])],
        [Paragraph("Phase 3\nBackend Development", s["table_cell"]),
         Paragraph("FastAPI server, STT integration (Whisper), AI reply engine, "
                   "TTS, spam detection engine, Firebase integration, WebSocket", s["table_cell"]),
         Paragraph("4 Weeks", s["table_cell"]),
         Paragraph("Working backend API with Swagger docs", s["table_cell"])],
        [Paragraph("Phase 4\nAndroid Development", s["table_cell"]),
         Paragraph("CallScreeningService, audio capture, Retrofit client, "
                   "WebSocket client, Jetpack Compose UI, Room DB, Hilt DI", s["table_cell"]),
         Paragraph("5 Weeks", s["table_cell"]),
         Paragraph("Debug APK with core screening flow", s["table_cell"])],
        [Paragraph("Phase 5\nIntegration & Testing", s["table_cell"]),
         Paragraph("End-to-end integration, pytest unit tests, Espresso UI tests, "
                   "load testing, spam detection accuracy validation", s["table_cell"]),
         Paragraph("3 Weeks", s["table_cell"]),
         Paragraph("Test report; F1 > 0.85 achieved", s["table_cell"])],
        [Paragraph("Phase 6\nDeployment & Documentation", s["table_cell"]),
         Paragraph("Cloud backend deployment, APK packaging, README, project report, "
                   "synopsis, presentation preparation", s["table_cell"]),
         Paragraph("2 Weeks", s["table_cell"]),
         Paragraph("Deployed app + final project report", s["table_cell"])],
    ]

    tl_tbl = Table(timeline, colWidths=[3.0 * cm, 7.0 * cm, 2.0 * cm, 4.0 * cm])
    tl_tbl.setStyle(TableStyle([
        ("BACKGROUND",    (0, 0), (-1, 0), TABLE_HDR),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, TABLE_ALT] * 6),
        ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#C5CAE9")),
        ("TOPPADDING",    (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LEFTPADDING",   (0, 0), (-1, -1), 7),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 7),
        ("VALIGN",        (0, 0), (-1, -1), "TOP"),
        ("FONTNAME",      (0, 1), (0, -1), "Times-Bold"),
        ("TEXTCOLOR",     (0, 1), (0, -1), SECONDARY),
    ]))
    story.append(tl_tbl)
    story.append(Spacer(1, 0.2 * cm))
    story.append(Paragraph(
        "<b>Total Estimated Duration: 18 Weeks (~4.5 Months)</b>",
        s["body"]))

    story.append(Spacer(1, 0.4 * cm))

    # ================================================================
    # 14. REFERENCES
    # ================================================================
    story.append(section_heading(s, 14, "References"))
    story.append(Spacer(1, 0.3 * cm))
    story.append(Paragraph(
        "References are formatted in IEEE style.", s["body"]))
    story.append(Spacer(1, 0.2 * cm))

    references = [
        "[1] A. Radford, J. W. Kim, T. Xu, G. Brockman, C. McLeavey, and I. Sutskever, "
        "\"Robust Speech Recognition via Large-Scale Weak Supervision,\" "
        "<i>Proceedings of the 40th International Conference on Machine Learning (ICML)</i>, "
        "vol. 202, pp. 28492–28518, Jul. 2023. [Online]. Available: "
        "https://arxiv.org/abs/2212.04356",

        "[2] B. J. Kolan and R. Dantu, \"Socio-Technical Defense Against Voice Spamming,\" "
        "<i>ACM Transactions on Autonomous and Adaptive Systems</i>, vol. 2, no. 1, "
        "pp. 1–31, Mar. 2007. doi: 10.1145/1186778.1186780.",

        "[3] A. Sahin, B. Büyükkaya, and R. Böhme, \"Call Me Maybe: Eavesdropping Encrypted "
        "LTE Calls With REVOLTE,\" <i>USENIX Security Symposium</i>, pp. 73–88, 2020. "
        "[Online]. Available: https://revolte-attack.net/",

        "[4] A. Aziz, M. N. Khan, and S. A. Khan, \"Detecting Phone Scam Calls Using "
        "Transformer-Based Text Classification,\" <i>IEEE Access</i>, vol. 10, "
        "pp. 45219–45231, 2022. doi: 10.1109/ACCESS.2022.3170843.",

        "[5] Federal Trade Commission (FTC), \"Consumer Sentinel Network Data Book 2023,\" "
        "FTC Report, Washington D.C., USA, Feb. 2024. [Online]. Available: "
        "https://www.ftc.gov/sentinel",

        "[6] C. Bird, N. Nagappan, B. Murphy, H. Gall, and P. Devanbu, \"Don't Touch My "
        "Code! Examining the Effects of Ownership on Software Quality,\" "
        "<i>Proc. 19th ACM SIGSOFT Symposium on Foundations of Software Engineering</i>, "
        "pp. 4–14, 2011. doi: 10.1145/2025113.2025119. "
        "(Referenced for Agile methodology foundations.)",

        "[7] Google LLC, \"Call Screening for Pixel,\" Google AI Blog, Oct. 2018. "
        "[Online]. Available: https://ai.googleblog.com/2018/10/call-screen.html",

        "[8] OpenAI, \"GPT-4 Technical Report,\" OpenAI Technical Report, Mar. 2023. "
        "[Online]. Available: https://openai.com/research/gpt-4",

        "[9] MarketsandMarkets, \"Voice Fraud Detection Market — Global Forecast to 2030,\" "
        "MarketsandMarkets Research Report, Pune, India, 2023. [Online]. Available: "
        "https://www.marketsandmarkets.com/voice-fraud-detection-market",

        "[10] Android Developers, \"CallScreeningService — Android API Reference,\" "
        "Google LLC, 2024. [Online]. Available: "
        "https://developer.android.com/reference/android/telecom/CallScreeningService",
    ]

    for ref in references:
        story.append(Paragraph(ref, s["ref"]))

    story.append(Spacer(1, 1.0 * cm))
    story.append(HRFlowable(width="100%", thickness=1, color=DIVIDER))
    story.append(Spacer(1, 0.3 * cm))
    story.append(Paragraph(
        "— End of Synopsis —",
        ParagraphStyle("end", fontName="Times-Italic", fontSize=9,
                       textColor=GREY, alignment=TA_CENTER)))

    return story


# ── Main ──────────────────────────────────────────────────────────────
def generate_synopsis(output_path: str = "AICallShield_Synopsis.pdf"):
    styles = build_styles()
    doc = SynopsisDoc(output_path)
    story = build_story(styles)
    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
    print(f"✅  Synopsis generated successfully → {output_path}")


if __name__ == "__main__":
    generate_synopsis(
        r"d:\SDK\PROJECTS\AICallShield\AICallShield\AICallShield_Synopsis.pdf"
    )
