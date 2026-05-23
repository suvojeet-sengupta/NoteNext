NoteNext — Editorial "Ink & Paper" bundled fonts
=================================================

The redesign uses three OFL-licensed (free, redistributable) font families.
Place the static .ttf files listed below into THIS directory
(core/src/main/res/font/) with EXACTLY these lowercase filenames.

Android requires resource filenames to be lowercase letters, digits and
underscores only — so the Google Fonts download filenames must be renamed.

--------------------------------------------------------------------
FRAUNCES  — https://fonts.google.com/specimen/Fraunces  (OFL)
  Download the family, then from the static/ instances copy & rename:
    Fraunces_72pt-Light.ttf          -> fraunces_light.ttf
    Fraunces_72pt-LightItalic.ttf    -> fraunces_light_italic.ttf
    Fraunces_72pt-Regular.ttf        -> fraunces_regular.ttf
    Fraunces_72pt-Italic.ttf         -> fraunces_italic.ttf
    Fraunces_72pt-Medium.ttf         -> fraunces_medium.ttf
    Fraunces_72pt-MediumItalic.ttf   -> fraunces_medium_italic.ttf
    Fraunces_72pt-SemiBold.ttf       -> fraunces_semibold.ttf
  (The "72pt" optical size gives the soft, high-contrast display look.
   Any opsz instance works; just keep the weight/italic pairing.)

--------------------------------------------------------------------
DM SANS   — https://fonts.google.com/specimen/DM+Sans  (OFL)
    DMSans-Regular.ttf   -> dmsans_regular.ttf
    DMSans-Medium.ttf    -> dmsans_medium.ttf
    DMSans-SemiBold.ttf  -> dmsans_semibold.ttf
    DMSans-Bold.ttf      -> dmsans_bold.ttf

--------------------------------------------------------------------
JETBRAINS MONO — https://fonts.google.com/specimen/JetBrains+Mono  (OFL)
    JetBrainsMono-Regular.ttf  -> jetbrainsmono_regular.ttf
    JetBrainsMono-Medium.ttf   -> jetbrainsmono_medium.ttf

--------------------------------------------------------------------
After adding all 13 files, the FontFamily definitions in
core/.../ui/theme/Fonts.kt will resolve and the project will compile.
You may delete this README once the fonts are in place.
